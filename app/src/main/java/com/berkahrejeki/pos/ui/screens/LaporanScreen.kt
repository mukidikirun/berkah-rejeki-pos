package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Session
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(vm: MainViewModel, session: Session) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("HARIAN") }
    var report by remember { mutableStateOf<MainViewModel.Laporan?>(null) }
    var loading by remember { mutableStateOf(false) }

    val cal = remember { Calendar.getInstance() }
    var dayOfMonth by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var month by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }

    fun load() {
        loading = true
        scope.launch {
            val c = Calendar.getInstance()
            val (from, to) = if (mode == "HARIAN") {
                c.set(year, month, dayOfMonth, 0, 0, 0)
                c.set(Calendar.MILLISECOND, 0)
                val f = c.timeInMillis
                c.set(year, month, dayOfMonth, 23, 59, 59)
                c.set(Calendar.MILLISECOND, 999)
                f to c.timeInMillis
            } else {
                c.set(year, month, 1, 0, 0, 0)
                val f = c.timeInMillis
                c.set(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                f to c.timeInMillis
            }
            report = vm.generateLaporan(from, to,
                cashierId = if (session.isAdmin) null else session.userId)
            loading = false
        }
    }

    LaunchedEffect(mode, dayOfMonth, month, year) { load() }

    val monthNames = listOf("Jan","Feb","Mar","Apr","Mei","Jun",
                            "Jul","Agu","Sep","Okt","Nov","Des")

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            FilterChip(selected = mode == "HARIAN", onClick = { mode = "HARIAN" },
                label = { Text("Harian") },
                leadingIcon = { Icon(Icons.Default.Today, null, Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == "BULANAN", onClick = { mode = "BULANAN" },
                label = { Text("Bulanan") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        if (mode == "HARIAN") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { dayOfMonth = (dayOfMonth - 1).coerceAtLeast(1) }) {
                    Text("-")
                }
                Text(" $dayOfMonth ${monthNames[month]} $year ",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp))
                OutlinedButton(onClick = { dayOfMonth = (dayOfMonth + 1).coerceAtMost(31) }) {
                    Text("+")
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(onClick = {
                    if (month > 0) month-- else { month = 11; year-- }
                }) { Text("<") }
                OutlinedButton(onClick = {
                    if (month < 11) month++ else { month = 0; year++ }
                }) { Text(">") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    if (month > 0) month-- else { month = 11; year-- }
                }) { Icon(Icons.Default.ChevronLeft, null) }
                Text(" ${monthNames[month]} $year ",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp))
                OutlinedButton(onClick = {
                    if (month < 11) month++ else { month = 0; year++ }
                }) { Icon(Icons.Default.ChevronRight, null) }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val r = report
            LazyColumn(Modifier.fillMaxSize()) {
                if (r == null) {
                    item { Text("Tidak ada data") }
                } else {
                    item {
                        Card(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("TOTAL OMZET", fontSize = 12.sp)
                                Text(formatRp(r.totalOmzet), fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("${r.countTrx} transaksi", fontSize = 12.sp)
                            }
                        }
                    }

                    item { StatCard("Tunai", formatRp(r.totalTunai), Icons.Default.Payments) }
                    item { StatCard("QRIS", formatRp(r.totalQris), Icons.Default.QrCode) }
                    item { StatCard("Modal", formatRp(r.totalModal), Icons.Default.ShoppingBag) }
                    item { StatCard("Laba Kotor", formatRp(r.labaKotor),
                        Icons.Default.TrendingUp, highlight = true) }

                    item {
                        Spacer(Modifier.height(12.dp))
                        Text("Rekap per Kasir", fontWeight = FontWeight.Bold)
                    }
                    if (r.perCashier.isEmpty()) {
                        item { Text("Belum ada transaksi", fontSize = 12.sp) }
                    } else {
                        items(r.perCashier.entries.toList()) { (name, data) ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold)
                                        Text("${data.first} transaksi", fontSize = 12.sp)
                                    }
                                    Text(formatRp(data.second),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                        Text("Top 10 Produk Terlaris", fontWeight = FontWeight.Bold)
                    }
                    items(r.topProducts.size) { idx ->
                        val (name, data) = r.topProducts[idx]
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold)
                                    Text("Terjual ${data.first} pcs", fontSize = 12.sp)
                                }
                                Text(formatRp(data.second), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String, value: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.tertiaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, Modifier.weight(1f))
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}
