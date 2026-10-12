package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(vm: MainViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(8.dp)) {
            items(products, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text("Harga: ${formatRp(p.price)} | Stok: ${p.stock}",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Kategori: ${p.category}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { editing = p; showForm = true }) {
                            Icon(Icons.Default.Edit, null)
                        }
                        IconButton(onClick = { vm.deleteProduct(p) }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        ProductFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { prod ->
                if (prod.id == 0L) vm.addProduct(prod) else vm.updateProduct(prod)
                showForm = false
            }
        )
    }
}

@Composable
fun ProductFormDialog(initial: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Umum") }
    var price by remember { mutableStateOf(initial?.price?.toInt()?.toString() ?: "") }
    var cost by remember { mutableStateOf(initial?.costPrice?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk" else "Edit Produk") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode (opsional)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Jual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(cost, { cost = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Modal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stock, { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stok") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && price.isNotBlank()) {
                    onSave(Product(
                        id = initial?.id ?: 0,
                        name = name, barcode = barcode, category = category,
                        price = price.toDouble(),
                        costPrice = cost.toDoubleOrNull() ?: 0.0,
                        stock = stock.toIntOrNull() ?: 0
                    ))
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
