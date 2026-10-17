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
import androidx.compose.ui.unit.sp
import com.berkahrejeki.pos.data.Transaction
import com.berkahrejeki.pos.printer.ThermalPrinter

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen() {
    val ctx = LocalContext.current
    val devices = remember {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    var connected by remember { mutableStateOf<String?>(null) }
    val printer = remember { ThermalPrinter(ctx) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Printer Thermal Bluetooth", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Pair printer thermal Anda di Pengaturan Bluetooth Android, lalu pilih di bawah ini.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tidak ada perangkat Bluetooth yang terdeteksi.", 
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Pastikan Printer sudah di-PAIR di Pengaturan HP dan izin Bluetooth sudah diberikan.", 
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        } else {
            LazyColumn {
                items(devices) { d ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name ?: "Unknown")
                                Text(d.address, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                val ok = printer.connect(d)
                                if (ok) {
                                    connected = d.name
                                } else {
                                    connected = null
                                }
                            }) { 
                                Text(if (connected == d.name) "Terhubung" else "Hubungkan") 
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                try {
                    val testTrx = Transaction(
                        invoiceNo = "TEST-" + System.currentTimeMillis(),
                        itemsJson = """[{"name":"Test Item","price":10000,"qty":1,"subtotal":10000}]""",
                        total = 10000.0,
                        paymentMethod = "CASH",
                        amountPaid = 10000.0,
                        cashierName = "Test"
                    )
                    printer.printReceipt(testTrx)
                } catch (e: Exception) {
                    // Abaikan error cetak agar tidak crash
                }
            },
            enabled = connected != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Print, null)
            Spacer(Modifier.width(8.dp))
            Text("Test Print")
        }
    }
}
