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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Session
import com.berkahrejeki.pos.ui.theme.AppTheme
import com.berkahrejeki.pos.ui.theme.themeColors

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
    val archiveMsg by vm.archiveInfo.collectAsState()

    LaunchedEffect(showArchiveDialog) {
        if (showArchiveDialog) {
            dbSize = vm.getDatabaseSizeMb()
            archiveSize = vm.getArchiveSizeMb()
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        // 1. Kartu User
        item {
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.fullName, style = MaterialTheme.typography.titleMedium)
                        Text("@${session.username} - ${session.role}", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.Logout, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Keluar")
                    }
                }
            }
        }

        // 2. Kartu Info Database (Tap untuk buka dialog)
        item {
            Card(
                Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable { showArchiveDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Info Database & Arsip", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Database aktif:", fontSize = 13.sp)
                        Text("${"%.2f".format(dbSize)} MB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("File arsip:", fontSize = 13.sp)
                        Text("${"%.2f".format(archiveSize)} MB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. Daftar Tema
        item {
            Text("Tema Aplikasi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        }
        items(AppTheme.entries.toList()) { t ->
            val cs = themeColors(t)
            Card(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onThemeChange(t) },
                colors = CardDefaults.cardColors(containerColor = if (t == currentTheme) cs.primaryContainer else MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(36.dp), color = cs.primary, shape = MaterialTheme.shapes.medium) {}
                    Spacer(Modifier.width(12.dp))
                    Text(t.label, Modifier.weight(1f))
                    if (t == currentTheme) Icon(Icons.Default.CheckCircle, null, tint = cs.primary)
                }
            }
        }
    }

    // Dialog Info Database
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            icon = { Icon(Icons.Default.Storage, null, modifier = Modifier.size(40.dp)) },
            title = { Text("Info Database") },
            text = {
                Column {
                    Text("Database Aktif: ${"%.2f".format(dbSize)} MB")
                    Spacer(Modifier.height(8.dp))
                    Text("File Arsip CSV: ${"%.2f".format(archiveSize)} MB")
                    if (archiveMsg != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(archiveMsg!!, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.autoArchiveOldData()
                    showArchiveDialog = false
                }) { Text("Arsip Sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Tutup") }
            }
        )
    }
}
