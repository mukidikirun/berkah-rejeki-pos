package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        null,
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

        item {
            Text(
                "Tema Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(AppTheme.entries.toList()) { t ->
            val cs = themeColors(t)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onThemeChange(t) },
                colors = CardDefaults.cardColors(
                    containerColor = if (t == currentTheme) cs.primaryContainer
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

        item {
            Spacer(Modifier.height(24.dp))
            Text("Tentang", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "BERKAH REJEKI POS",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Versi 2.0", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Aplikasi kasir retail dengan multi-user, tunai & QRIS, cetak thermal, dan laporan harian/bulanan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
