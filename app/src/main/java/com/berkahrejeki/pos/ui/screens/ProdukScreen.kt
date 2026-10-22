package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(vm: MainViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }

    // 0 = Master Barang, 1 = Barang Terjual (Habis)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var showDupDialog by remember { mutableStateOf(false) }
    var dupList by remember { mutableStateOf<List<Pair<String, List<Product>>>>(emptyList()) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    var loadingDup by remember { mutableStateOf(false) }
    
    // State untuk scanner restock
    var showRestockScanner by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // LOGIKA FILTER TAB
    val displayProducts = if (selectedTab == 0) {
        // MASTER BARANG: Hanya ambil 1 produk unik per nama (mengabaikan duplikat)
        products.distinctBy { it.name.trim().lowercase() } 
    } else {
        // TERJUAL: Hanya tampilkan yang stoknya 0 atau minus
        products.filter { it.stock <= 0 } 
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                loadingDup = true
                                val dupes = vm.findDuplicates()
                                dupList = dupes
                                loadingDup = false
                                if (dupes.isEmpty()) {
                                    snackbarMsg = "Tidak ada produk duplikat"
                                } else {
                                    showDupDialog = true
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        if (loadingDup) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.ContentCut, "Hapus Duplikat")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { editing = null; showForm = true }
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                // ===== TAB MENU =====
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Master Barang", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.List, null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Terjual (Habis)", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.ShoppingBag, null) }
                    )
                }

                // ===== TOMBOL SCAN RESTOCK (Hanya muncul di Tab Master Barang) =====
                if (selectedTab == 0) {
                    OutlinedButton(
                        onClick = { showRestockScanner = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan Barcode untuk Tambah Stok")
                    }
                }

                if (displayProducts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (selectedTab == 0) Icons.Default.List else Icons.Default.ShoppingBag,
                                null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (selectedTab == 0) "Belum ada Master Barang" else "Belum ada barang terjual",
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                "Total: ${displayProducts.size} produk",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(displayProducts, key = { it.id }) { p ->
                            ProductItem(
                                product = p,
                                isSoldOut = selectedTab == 1,
                                onEdit = { editing = p; showForm = true },
                                onDelete = { vm.deleteProduct(p) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        snackbarMsg?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { snackbarMsg = null }) { Text("OK") }
                }
            ) { Text(it) }
        }
    }

    // Form Tambah/Edit Produk
    if (showForm) {
        ProductFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { prod ->
                if (prod.id == 0L) vm.addProduct(prod) else vm.updateProduct(prod)
                showForm = false
                snackbarMsg = "Produk disimpan"
            }
        )
    }

    // Dialog Hapus Duplikat
    if (showDupDialog) {
        DuplicateDialog(
            duplicates = dupList,
            onDismiss = { showDupDialog = false },
            onConfirm = {
                scope.launch {
                    val removed = vm.removeDuplicates()
                    showDupDialog = false
                    snackbarMsg = "$removed produk duplikat berhasil dihapus"
                }
            }
        )
    }

    // ===== LAYAR SCANNER RESTOCK =====
    if (showRestockScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { barcode ->
                showRestockScanner = false
                // Cari produk berdasarkan barcode yang discan
                val found = products.firstOrNull { it.barcode == barcode || it.name.equals(barcode, true) }
                if (found != null) {
                    // Jika ketemu, langsung buka form Edit
                    editing = found
                    showForm = true
                } else {
                    snackbarMsg = "Produk dengan barcode tersebut belum terdaftar"
                }
            },
            onClose = { showRestockScanner = false }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    isSoldOut: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSoldOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                getCategoryIconLocal(product.category),
                fontSize = 32.sp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatRp(product.price),
                        color = if (isSoldOut) Color.Gray else MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isSoldOut) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (isSoldOut) "HABIS" else "Stok ${product.stock}",
                            fontSize = 10.sp,
                            color = if (isSoldOut) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    product.category,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun getCategoryIconLocal(category: String): String {
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
fun DuplicateDialog(
    duplicates: List<Pair<String, List<Product>>>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val totalDuplicates = duplicates.sumOf { it.second.size - 1 }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ContentCut, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
        title = { Text("Produk Duplikat Ditemukan") },
        text = {
            Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                Text("Ditemukan ${duplicates.size} nama produk duplikat. Total $totalDuplicates produk akan dihapus, sisakan 1 per nama.", fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                duplicates.forEach { (name, list) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f))) {
                        Column(Modifier.padding(10.dp)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text("${list.size} salinan → sisakan 1", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Hapus $totalDuplicates")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun ProductFormDialog(
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Umum") }
    var price by remember { mutableStateOf(initial?.price?.toInt()?.toString() ?: "") }
    var cost by remember { mutableStateOf(initial?.costPrice?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var showScanner by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk" else "Edit Produk") },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama Produk *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    isError = errorMsg != null && name.isBlank()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = barcode, onValueChange = { barcode = it },
                    label = { Text("Barcode (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) { Icon(Icons.Default.QrCodeScanner, "Scan", tint = MaterialTheme.colorScheme.primary) }
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Harga Jual *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true, isError = errorMsg != null && price.isBlank()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Modal (opsional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = stock, onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.isBlank() -> errorMsg = "Nama wajib diisi"
                    price.isBlank() -> errorMsg = "Harga wajib diisi"
                    else -> {
                        onSave(Product(
                            id = initial?.id ?: 0, name = name.trim(), barcode = barcode.trim(),
                            category = category.trim().ifBlank { "Umum" }, price = price.toDoubleOrNull() ?: 0.0,
                            costPrice = cost.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0
                        ))
                    }
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { result -> barcode = result; showScanner = false },
            onClose = { showScanner = false }
        )
    }
}
