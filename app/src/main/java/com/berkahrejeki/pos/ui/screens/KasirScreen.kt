package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.CartItem
import com.berkahrejeki.pos.data.Product
import com.berkahrejeki.pos.data.Transaction
import com.berkahrejeki.pos.payment.QrisGenerator
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatRp(v: Double): String =
    "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(v)

fun getCategoryIcon(category: String): String {
    val c = category.lowercase()
    return when {
        c.contains("sembako") -> "🛒"
        c.contains("minum") -> "🥤"
        c.contains("makan") || c.contains("snack") -> "🍜"
        c.contains("rokok") -> "🚬"
        c.contains("sabun") || c.contains("mandi") -> "🧼"
        c.contains("obat") -> "💊"
        c.contains("sayur") -> "🥬"
        c.contains("buah") -> "🍎"
        c.contains("kopi") || c.contains("teh") -> "☕"
        else -> "📦"
    }
}

@Composable
fun getStockColor(stock: Int): Color {
    return when {
        stock <= 0 -> Color(0xFFD32F2F)
        stock < 5 -> Color(0xFFD32F2F)
        stock < 10 -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(vm: MainViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val total by vm.cartTotal.collectAsStateWithLifecycle()
    
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCheckout by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf("CASH") }
    var paidInput by remember { mutableStateOf("") }
    var lastTrx by remember { mutableStateOf<Transaction?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    // Auto-focus setup
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        try { focusRequester.requestFocus() } catch (e: Exception) {}
    }

    val categories = remember(products) {
        listOf("Semua") + products.map { it.category }.distinct().sorted()
    }

    val filtered = products.filter {
        val matchSearch = it.name.contains(search, true) || it.barcode.contains(search)
        val matchCategory = selectedCategory == "Semua" || it.category == selectedCategory
        matchSearch && matchCategory
    }

    Column(Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Cari produk atau scan barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Default.Close, "Hapus")
                        }
                    }
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, "Scan", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val found = products.firstOrNull { it.barcode == search || it.name.equals(search, true) }
                    if (found != null) {
                        vm.addToCart(found)
                        search = ""
                    }
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
            )
        )

        // Filter Kategori
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { selectedCategory = cat }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cat != "Semua") {
                            Text(getCategoryIcon(cat), fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            cat, fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(Modifier.weight(1f)) {
            // Grid Produk
            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Produk tidak ditemukan", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { p ->
                        ProductCard(product = p, onClick = { vm.addToCart(p) })
                    }
                }
            }

            // Panel Keranjang
            CartPanel(
                cart = cart, total = total,
                onAdd = { vm.addToCart(it) },
                onReduce = { vm.updateQty(it.product.id, it.qty - 1) },
                onClear = { vm.clearCart() },
                onCheckout = { showCheckout = true }
            )
        }
    }

    if (showCheckout) {
        CheckoutDialog(
            total = total, method = method, paidInput = paidInput,
            onMethod = { method = it }, onPaid = { paidInput = it },
            onDismiss = { showCheckout = false },
            onConfirm = {
                val paid = if (method == "CASH") paidInput.toDoubleOrNull() ?: 0.0 else total
                if (method == "CASH" && paid < total) return@CheckoutDialog
                vm.checkout(method, paid) { trx ->
                    lastTrx = trx; showCheckout = false; paidInput = ""
                }
            }
        )
    }

    lastTrx?.let { trx ->
        ReceiptPreviewDialog(trx = trx, onDismiss = { lastTrx = null }, onPrint = { lastTrx = null })
    }

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { barcode ->
                showScanner = false
                val found = products.firstOrNull { it.barcode == barcode || it.name.equals(barcode, true) }
                if (found != null) {
                    vm.addToCart(found)
                    search = ""
                } else {
                    search = barcode
                }
            },
            onClose = { showScanner = false }
        )
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    val stockColor = getStockColor(product.stock)
    val isLowStock = product.stock < 5

    Card(
        modifier = Modifier.fillMaxWidth().height(135.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(getCategoryIcon(product.category), fontSize = 20.sp)
                if (isLowStock) {
                    Surface(shape = RoundedCornerShape(6.dp), color = stockColor.copy(alpha = 0.15f)) {
                        Text(
                            if (product.stock <= 0) "HABIS" else "${product.stock}",
                            fontSize = 9.sp, color = stockColor, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp
            )
            Column {
                Text(formatRp(product.price), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(stockColor))
                    Spacer(Modifier.width(3.dp))
                    Text("Stok:${product.stock}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
        }
    }
}

@Composable
fun CartPanel(cart: List<CartItem>, total: Double, onAdd: (Product) -> Unit, onReduce: (CartItem) -> Unit, onClear: () -> Unit, onCheckout: () -> Unit) {
    Card(Modifier.width(280.dp).fillMaxHeight().padding(4.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (cart.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                        Text("${cart.sumOf { it.qty }}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            HorizontalDivider()
            if (cart.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada item", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(cart, key = { it.product.id }) { item ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatRp(item.product.price * item.qty), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { onReduce(item) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, null, Modifier.size(18.dp)) }
                            Text("${item.qty}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onAdd(item.product) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(formatRp(total), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onCheckout, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("BAYAR", fontWeight = FontWeight.Bold)
                }
                if (cart.isNotEmpty()) {
                    TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Kosongkan", fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(total: Double, method: String, paidInput: String, onMethod: (String) -> Unit, onPaid: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column {
                Text("Total: ${formatRp(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Row {
                    FilterChip(selected = method == "CASH", onClick = { onMethod("CASH") }, label = { Text("Tunai") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = method == "QRIS", onClick = { onMethod("QRIS") }, label = { Text("QRIS") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                if (method == "CASH") {
                    OutlinedTextField(
                        value = paidInput, onValueChange = { onPaid(it.filter { c -> c.isDigit() }) },
                        label = { Text("Uang Diterima") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val paid = paidInput.toDoubleOrNull() ?: 0.0
                    if (paid >= total) Text("Kembalian: ${formatRp(paid - total)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                } else {
                    val payload = QrisGenerator.buildDynamicQris(total)
                    val bmp = remember(payload) { QrisGenerator.generateQrBitmap(payload, 500) }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Image(bmp.asImageBitmap(), "QRIS", modifier = Modifier.size(220.dp)) }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Konfirmasi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun ReceiptPreviewDialog(trx: Transaction, onDismiss: () -> Unit, onPrint: (Transaction) -> Unit) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val items = remember(trx.id) { JSONArray(trx.itemsJson) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaksi Berhasil") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text(com.berkahrejeki.pos.AppConfig.NAMA_TOKO, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(com.berkahrejeki.pos.AppConfig.TAGLINE, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(com.berkahrejeki.pos.AppConfig.ALAMAT, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Telp: " + com.berkahrejeki.pos.AppConfig.TELEPON, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("No : ${trx.invoiceNo}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Tgl: ${dateFmt.format(Date(trx.timestamp))}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Kasir: ${trx.cashierName}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                for (i in 0 until items.length()) {
                    val o = items.getJSONObject(i)
                    Text(o.getString("name"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("  ${o.getInt("qty")} x ${formatRp(o.getDouble("price"))} = ${formatRp(o.getDouble("subtotal"))}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("TOTAL : ${formatRp(trx.total)}", fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("BAYAR (${trx.paymentMethod}) : ${formatRp(trx.amountPaid)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                if (trx.paymentMethod == "CASH") Text("KEMBALI : ${formatRp(trx.change)}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("================================", fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(com.berkahrejeki.pos.AppConfig.NOTA_FOOTER_1, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(com.berkahrejeki.pos.AppConfig.NOTA_FOOTER_2, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold)
                Text(com.berkahrejeki.pos.AppConfig.NOTA_FOOTER_3, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onPrint(trx) }) { Icon(Icons.Default.Print, null); Spacer(Modifier.width(4.dp)); Text("Cetak") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
