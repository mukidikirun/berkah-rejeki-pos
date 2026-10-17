package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Session
import com.berkahrejeki.pos.ui.theme.AppTheme
import com.berkahrejeki.pos.ui.theme.themeColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    session: Session,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    var dbSize by remember { mutableStateOf(vm.getDatabaseSizeMb()) }
    var archiveSize by remember { mutableStateOf(vm.getArchiveSizeMb()) }
    val archiveFiles = remember { vm.getArchiveFiles() }
    val archiveMsg by vm.archiveInfo.collectAsState()

    LaunchedEffect(showArchiveDialog) {
        if (showArchiveDialog) {
            dbSize = vm.getDatabaseSizeMb()
            archiveSize = vm.getArchiveSizeMb()
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        // ===== USER CARD =====
        item {
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle, null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "@${session.username} - ${session.role}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Keluar")
                    }
                }
            }
        }

        // ===== DATABASE INFO =====
        item {
            Card(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                onClick = { showArchiveDialog = true }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Info Database",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Database aktif:", fontSize = 13.sp)
                        Text("${"%.2f".format(dbSize)} MB",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("File arsip:", fontSize = 13.sp)
                        Text("${"%.2f".format(archiveSize)} MB (${archiveFiles.size} file)",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap untuk lihat detail & arsip",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(0.7f)
                    )
                }
            }
        }

        // ===== TEMA =====
        item {
            Text("Tema Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        items(AppTheme.entries.toList()) { t ->
            val cs = themeColors(t)
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onThemeChange(t) },
                colors = CardDefaults.cardColors(
                    containerColor = if (t == currentTheme) cs.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        color = cs.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {}
                    Spacer(Modifier.width(12.dp))
                    Text(t.label, Modifier.weight(1f))
                    if (t == currentTheme) {
                        Icon(Icons.Default.CheckCircle, null, tint = cs.primary)
                    }
                }
            }
        }

        // ===== TENTANG =====
        item {
            Spacer(Modifier.height(24.dp))
            Text("Tentang", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("BERKAH REJEKI POS", style = MaterialTheme.typography.titleMedium)
                    Text("Versi 2.3", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aplikasi kasir retail dengan multi-user, tunai & QRIS, cetak thermal, scan barcode, laporan, dan arsip otomatis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // ===== DIALOG INFO DATABASE & ARSIP =====
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            icon = {
                Icon(Icons.Default.Storage, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp))
            },
            title = { Text("Info Database") },
            text = {
                Column(Modifier.heightIn(max = 400.dp)) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Database Aktif", fontWeight = FontWeight.Bold)
                            Text("${"%.2f".format(dbSize)} MB",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text("Transaksi aktif < 90 hari", fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("File Arsip", fontWeight = FontWeight.Bold)
                            Text("${archiveFiles.size} file • ${"%.2f".format(archiveSize)} MB",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            Text("Transaksi > 90 hari (format CSV)",
                                fontSize = 11.sp)
                        }
                    }

                    if (archiveFiles.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Daftar File:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        archiveFiles.forEach { file ->
                            val date = SimpleDateFormat("yyyyMM", Locale.getDefault())
                                .parse(file.name.replace("arsip_", "").replace(".csv", ""))
                            val readable = if (date != null) {
                                SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                    .format(date)
                            } else file.name
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📄 $readable", fontSize = 11.sp)
                                Text("${"%.1f".format(file.length() / 1024.0)} KB",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (archiveMsg != null) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(archiveMsg!!, fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "💡 Arsip otomatis: transaksi > 90 hari dipindah ke CSV saat aplikasi dibuka.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.autoArchiveOldData()
                    showArchiveDialog = false
                }) {
                    Icon(Icons.Default.Archive, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Arsip Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Tutup") }
            }
        )
    }
}
