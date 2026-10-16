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

    var showDupDialog by remember { mutableStateOf(false) }
    var dupList by remember { mutableStateOf<List<Pair<String, List<Product>>>>(emptyList()) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    var loadingDup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
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
            if (products.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2, null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Belum ada produk",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap tombol + untuk tambah produk",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            "Total: ${products.size} produk",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(products, key = { it.id }) { p ->
                        ProductItem(
                            product = p,
                            onEdit = { editing = p; showForm = true },
                            onDelete = { vm.deleteProduct(p) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
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
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(getCategoryIconLocal(product.category), fontSize = 32.sp)
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
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            "Stok ${product.stock}",
                            fontSize = 10.sp,
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
                Icon(Icons.Default.Edit, "Edit",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus",
                    tint = MaterialTheme.colorScheme.error)
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
        icon = {
            Icon(Icons.Default.ContentCut, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp))
        },
        title = { Text("Produk Duplikat Ditemukan") },
        text = {
            Column(
                Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Ditemukan ${duplicates.size} nama produk duplikat. " +
                        "Total $totalDuplicates produk akan dihapus, sisakan 1 per nama.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                duplicates.forEach { (name, list) ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                        )
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(name, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text("${list.size} salinan → sisakan 1",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("⚠️ Produk yang dihapus tidak bisa dikembalikan.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hapus $totalDuplicates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ============================================================
//  FORM TAMBAH/EDIT PRODUK — URUTAN FIELD SUDAH BENAR
// ============================================================
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
            Column(
                Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // ===== NAMA PRODUK (WAJIB) =====
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Contoh: Indomie Goreng") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMsg != null && name.isBlank()
                )
                Spacer(Modifier.height(8.dp))

                // ===== BARCODE (opsional, dengan tombol scan) =====
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode (opsional)") },
                    placeholder = { Text("Scan atau ketik manual") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, "Scan Barcode",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                // ===== KATEGORI =====
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    placeholder = { Text("Sembako, Minuman, dll") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // ===== HARGA JUAL (WAJIB) =====
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() }; errorMsg = null },
                    label = { Text("Harga Jual *") },
                    placeholder = { Text("Contoh: 3500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMsg != null && price.isBlank()
                )
                Spacer(Modifier.height(8.dp))

                // ===== HARGA MODAL =====
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Modal (opsional)") },
                    placeholder = { Text("Contoh: 2800") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))

                // ===== STOK =====
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok") },
                    placeholder = { Text("Contoh: 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ===== ERROR MESSAGE =====
                if (errorMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validasi
                    when {
                        name.isBlank() -> errorMsg = "Nama produk wajib diisi"
                        price.isBlank() -> errorMsg = "Harga jual wajib diisi"
                        else -> {
                            onSave(
                                Product(
                                    id = initial?.id ?: 0,
                                    name = name.trim(),
                                    barcode = barcode.trim(),
                                    category = category.trim().ifBlank { "Umum" },
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    costPrice = cost.toDoubleOrNull() ?: 0.0,
                                    stock = stock.toIntOrNull() ?: 0
                                )
                            )
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showScanner) {
        BarcodeScannerScreen(
            onBarcodeDetected = { result ->
                barcode = result
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }
}
