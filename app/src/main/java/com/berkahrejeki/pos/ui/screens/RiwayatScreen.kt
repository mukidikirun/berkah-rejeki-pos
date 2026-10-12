package com.berkahrejeki.pos.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.printer.ThermalPrinter
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun RiwayatScreen(vm: MainViewModel) {
    val trxs by vm.transactions.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.loadTransactions()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(trxs, key = { it.id }) { trx ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(trx.invoiceNo, style = MaterialTheme.typography.titleSmall)
                            Text(fmt.format(Date(trx.timestamp)),
                                style = MaterialTheme.typography.bodySmall)
                            Text("${trx.paymentMethod} - ${formatRp(trx.total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val adapter = BluetoothAdapter.getDefaultAdapter()
                            val device = adapter?.bondedDevices?.firstOrNull()
                            if (device != null) {
                                val printer = ThermalPrinter(ctx)
                                if (printer.connect(device)) {
                                    printer.printReceipt(trx)
                                    printer.disconnect()
                                    message = "Nota tercetak"
                                } else message = "Gagal cetak"
                            } else message = "Tidak ada printer paired"
                        }) { Icon(Icons.Default.Print, "Cetak") }
                    }
                }
            }
        }

        message?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = {
                    TextButton(onClick = { message = null }) { Text("OK") }
                }
            ) { Text(it) }
        }
    }
}
