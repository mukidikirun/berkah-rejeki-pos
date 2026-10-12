package com.berkahrejeki.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berkahrejeki.pos.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val productDao = db.productDao()
    private val trxDao = db.transactionDao()
    private val userDao = db.userDao()

    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session

    val products: StateFlow<List<Product>> =
        productDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val users: StateFlow<List<User>> =
        userDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
        

private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
val transactions: StateFlow<List<Transaction>> = _transactions

fun loadTransactions() = viewModelScope.launch {
    trxDao.getAll().collect { _transactions.value = it }
}
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart
    val cartTotal: StateFlow<Double> = _cart.map { list ->
        list.sumOf { it.product.price * it.qty }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    fun addToCart(p: Product, qty: Int = 1) {
        val current = _cart.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == p.id }
        if (idx >= 0) current[idx].qty += qty
        else current.add(CartItem(p, qty))
        _cart.value = current
    }

    fun updateQty(productId: Long, qty: Int) {
        _cart.value = _cart.value.map {
            if (it.product.id == productId) it.copy(qty = qty) else it
        }.filter { it.qty > 0 }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() { _cart.value = emptyList() }

    fun hashPassword(p: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(p.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun login(username: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val u = userDao.findByUsername(username.trim())
            if (u == null) { onResult(false, "Username tidak ditemukan"); return@launch }
            if (u.password != hashPassword(password)) { onResult(false, "Password salah"); return@launch }
            if (!u.active) { onResult(false, "Akun tidak aktif"); return@launch }
            _session.value = Session(
                userId = u.id, username = u.username, fullName = u.fullName,
                role = UserRole.valueOf(u.role)
            )
            onResult(true, null)
        }
    }

    fun logout() { _session.value = null; clearCart() }

    fun seedDefaultUsers() = viewModelScope.launch {
        if (userDao.count() == 0) {
            userDao.insert(User(
                username = "admin", password = hashPassword("admin123"),
                fullName = "Administrator", role = "ADMIN"
            ))
            userDao.insert(User(
                username = "kasir", password = hashPassword("kasir123"),
                fullName = "Kasir 1", role = "KASIR"
            ))
        }
    }

    fun addUser(u: User, pwd: String, onDone: () -> Unit = {}) = viewModelScope.launch {
        userDao.insert(u.copy(password = hashPassword(pwd))); onDone()
    }
    fun updateUser(u: User) = viewModelScope.launch { userDao.update(u) }
    fun deleteUser(u: User) = viewModelScope.launch { userDao.delete(u) }

    fun addProduct(p: Product, onDone: () -> Unit = {}) = viewModelScope.launch {
        productDao.insert(p); onDone()
    }
    fun updateProduct(p: Product) = viewModelScope.launch { productDao.update(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { productDao.delete(p) }

    fun seedIfEmpty() = viewModelScope.launch {
        if (products.value.isEmpty()) {
            productDao.insert(Product(name = "Beras 5kg", price = 65000.0, stock = 20, category = "Sembako"))
            productDao.insert(Product(name = "Minyak Goreng 2L", price = 35000.0, stock = 30, category = "Sembako"))
            productDao.insert(Product(name = "Gula 1kg", price = 15000.0, stock = 25, category = "Sembako"))
            productDao.insert(Product(name = "Aqua 600ml", price = 4000.0, stock = 48, category = "Minuman"))
            productDao.insert(Product(name = "Indomie Goreng", price = 3500.0, stock = 100, category = "Makanan"))
        }
    }

    fun checkout(method: String, amountPaid: Double, onSuccess: (Transaction) -> Unit) {
        viewModelScope.launch {
            val items = _cart.value
            if (items.isEmpty()) return@launch
            val total = items.sumOf { it.product.price * it.qty }

            val jsonArr = JSONArray()
            items.forEach {
                jsonArr.put(JSONObject().apply {
                    put("name", it.product.name)
                    put("price", it.product.price)
                    put("qty", it.qty)
                    put("subtotal", it.product.price * it.qty)
                })
            }

            val invoice = "BR-" + SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(Date())
            val change = if (method == "CASH") amountPaid - total else 0.0
            val s = _session.value

            val trx = Transaction(
                invoiceNo = invoice,
                itemsJson = jsonArr.toString(),
                total = total,
                paymentMethod = method,
                amountPaid = amountPaid,
                change = change,
                cashierId = s?.userId ?: 0,
                cashierName = s?.fullName ?: "-"
            )
            trxDao.insert(trx)
            items.forEach { productDao.reduceStock(it.product.id, it.qty) }
            _cart.value = emptyList()
            onSuccess(trx)
        }
    }

    data class Laporan(
        val countTrx: Int,
        val totalOmzet: Double,
        val totalTunai: Double,
        val totalQris: Double,
        val totalModal: Double,
        val labaKotor: Double,
        val perCashier: Map<String, Pair<Int, Double>>,
        val topProducts: List<Pair<String, Pair<Int, Double>>>
    )

    suspend fun generateLaporan(from: Long, to: Long, cashierId: Long? = null): Laporan {
        val list = if (cashierId != null) trxDao.getByCashier(cashierId, from, to)
                   else trxDao.getByRangeOnce(from, to)

        var total = 0.0; var tunai = 0.0; var qris = 0.0
        val perCashier = mutableMapOf<String, Pair<Int, Double>>()
        val prodMap = mutableMapOf<String, Pair<Int, Double>>()
        var totalModal = 0.0

        list.forEach { t ->
            total += t.total
            if (t.paymentMethod == "CASH") tunai += t.total else qris += t.total

            val key = t.cashierName.ifBlank { "Tanpa Nama" }
            val cur = perCashier[key] ?: (0 to 0.0)
            perCashier[key] = (cur.first + 1) to (cur.second + t.total)

            val arr = JSONArray(t.itemsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.getString("name")
                val qty = o.getInt("qty")
                val sub = o.getDouble("subtotal")
                val curP = prodMap[name] ?: (0 to 0.0)
                prodMap[name] = (curP.first + qty) to (curP.second + sub)

                val master = products.value.firstOrNull { it.name == name }
                if (master != null) totalModal += master.costPrice * qty
            }
        }

        return Laporan(
            countTrx = list.size,
            totalOmzet = total,
            totalTunai = tunai,
            totalQris = qris,
            totalModal = totalModal,
            labaKotor = total - totalModal,
            perCashier = perCashier,
            topProducts = prodMap.entries
                .sortedByDescending { it.value.second }
                .take(10)
                .map { it.key to it.value }
        )
    }
}
