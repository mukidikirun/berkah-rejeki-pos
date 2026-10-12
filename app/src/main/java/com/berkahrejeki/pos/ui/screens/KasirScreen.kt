package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Transaction
import com.berkahrejeki.pos.payment.QrisGenerator
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatRp(v: Double): String =
    "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(vm: MainViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val total by vm.cartTotal.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var showCheckout by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf("CASH") }
    var paidInput by remember { mutableStateOf("") }
    var lastTrx by remember { mutableStateOf<Transaction?>(null) }

    val filtered = products.filter {
        it.name.contains(search, true) || it.barcode.contains(search)
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = search, onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text("Cari produk...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        Row(Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { p ->
                    Card(
                        Modifier.fillMaxWidth().clickable { vm.addToCart(p) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(p.name, fontWeight = FontWeight.Bold, maxLines = 2, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(formatRp(p.price),
                                color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Text("Stok: ${p.stock}", fontSize = 11.sp)
                        }
                    }
                }
            }

            Card(Modifier.width(280.dp).fillMaxHeight().padding(4.dp)) {
                Column(Modifier.fillMaxSize()) {
                    Text("Keranjang", Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    LazyColumn(Modifier.weight(1f)) {
                        items(cart, key = { it.product.id }) { item ->
                            Row(Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.product.name, fontSize = 12.sp, maxLines = 1)
                                    Text(formatRp(item.product.price * item.qty),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { vm.updateQty(item.product.id, item.qty - 1) }) {
                                    Icon(Icons.Default.Remove, null)
                                }
                                Text("${item.qty}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { vm.addToCart(item.product) }) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    HorizontalDivider()
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(formatRp(total), fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showCheckout = true },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, null)
                            Spacer(Modifier.width(4.dp))
                            Text("BAYAR")
                        }
                        TextButton(
                            onClick = { vm.clearCart() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = cart.isNotEmpty()
                        ) { Text("Kosongkan") }
                    }
                }
            }
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
                    lastTrx = trx
                    showCheckout = false
                    paidInput = ""
                }
            }
        )
    }

    // ===== PREVIEW NOTA =====
    lastTrx?.let { trx ->
        ReceiptPreviewDialog(
            trx = trx,
            onDismiss = { lastTrx = null },
            onPrint = { trxToPrint ->
                // Kirim ke printer lewat vm atau langsung (nanti di Riwayat)
                lastTrx = null
            }
        )
    }
}

@Composable
fun CheckoutDialog(
    total: Double, method: String, paidInput: String,
    onMethod: (String) -> Unit, onPaid: (String) -> Unit,
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column {
                Text("Total: ${formatRp(total)}",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Row {
                    FilterChip(selected = method == "CASH", onClick = { onMethod("CASH") },
                        label = { Text("Tunai") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = method == "QRIS", onClick = { onMethod("QRIS") },
                        label = { Text("QRIS") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                if (method == "CASH") {
                    OutlinedTextField(
                        value = paidInput,
                        onValueChange = { onPaid(it.filter { c -> c.isDigit() }) },
                        label = { Text("Uang Diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val paid = paidInput.toDoubleOrNull() ?: 0.0
                    if (paid >= total) {
                        Text("Kembalian: ${formatRp(paid - total)}",
                            color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                } else {
                    val payload = QrisGenerator.buildDynamicQris(total)
                    val bmp = remember(payload) { QrisGenerator.generateQrBitmap(payload, 500) }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QRIS",
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text("Scan QRIS untuk bayar ${formatRp(total)}", fontSize = 12.sp)
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Konfirmasi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

// =============================================================
//  DIALOG PREVIEW NOTA — TAMPIL SETELAH TRANSAKSI BERHASIL
// =============================================================
@Composable
fun ReceiptPreviewDialog(
    trx: Transaction,
    onDismiss: () -> Unit,
    onPrint: (Transaction) -> Unit
) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val items = remember(trx.id) { JSONArray(trx.itemsJson) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Transaksi Berhasil", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "BERKAH REJEKI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Toko Retail & Sembako",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Jl. Raya No. 123",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
    "Dsn. Ringin Kembar",
    fontSize = 11.sp,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)
Text(
    "Telp: +62 858-4386-6617",
    fontSize = 11.sp,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
)
                )
                Spacer(Modifier.height(8.dp))
                Text("--------------------------------",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))

                Text("No : ${trx.invoiceNo}",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Tgl: ${dateFmt.format(Date(trx.timestamp))}",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Kasir: ${trx.cashierName}",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)

                Spacer(Modifier.height(4.dp))
                Text("--------------------------------",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))

                for (i in 0 until items.length()) {
                    val o = items.getJSONObject(i)
                    Text(o.getString("name"),
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("  ${o.getInt("qty")} x ${formatRp(o.getDouble("price"))} = ${formatRp(o.getDouble("subtotal"))}",
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(Modifier.height(2.dp))
                }

                Spacer(Modifier.height(4.dp))
                Text("--------------------------------",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))

                Text("TOTAL : ${formatRp(trx.total)}",
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
                Text("BAYAR (${trx.paymentMethod}) : ${formatRp(trx.amountPaid)}",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                if (trx.paymentMethod == "CASH") {
                    Text("KEMBALI : ${formatRp(trx.change)}",
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                        color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text("================================",
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("Terima kasih telah berbelanja",
                    fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text("Di Toko BERKAH REJEKI",
                    fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold)
                Text("Semoga Rezeki Anda Berkah",
                    fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onPrint(trx) }) {
                Icon(Icons.Default.Print, null)
                Spacer(Modifier.width(4.dp))
                Text("Cetak")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
