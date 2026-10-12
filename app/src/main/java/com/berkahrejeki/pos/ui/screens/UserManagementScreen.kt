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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berkahrejeki.pos.MainViewModel
import com.berkahrejeki.pos.data.Session
import com.berkahrejeki.pos.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(vm: MainViewModel, session: Session) {
    val users by vm.users.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<User?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.PersonAdd, null)
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(8.dp)) {
            items(users, key = { it.id }) { u ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (u.role == "ADMIN") Icons.Default.AdminPanelSettings
                            else Icons.Default.Person,
                            null,
                            tint = if (u.role == "ADMIN") MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.fullName, style = MaterialTheme.typography.titleMedium)
                            Text("@${u.username} - ${u.role}",
                                style = MaterialTheme.typography.bodySmall)
                            if (!u.active) Text("NONAKTIF",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { editing = u; showForm = true }) {
                            Icon(Icons.Default.Edit, null)
                        }
                        if (u.id != session.userId) {
                            IconButton(onClick = { vm.deleteUser(u) }) {
                                Icon(Icons.Default.Delete, null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        UserFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { u, pwd ->
                if (u.id == 0L) vm.addUser(u, pwd) else vm.updateUser(u)
                showForm = false
            }
        )
    }
}

@Composable
fun UserFormDialog(
    initial: User?,
    onDismiss: () -> Unit,
    onSave: (User, String) -> Unit
) {
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initial?.role ?: "KASIR") }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah User" else "Edit User") },
        text = {
            Column {
                OutlinedTextField(fullName, { fullName = it }, label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(username, { username = it }, label = { Text("Username") },
                    enabled = initial == null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it },
                    label = { Text(if (initial == null) "Password"
                                    else "Password Baru (kosongkan jika tidak diubah)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Role:", style = MaterialTheme.typography.labelMedium)
                Row {
                    FilterChip(selected = role == "ADMIN", onClick = { role = "ADMIN" },
                        label = { Text("Admin") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = role == "KASIR", onClick = { role = "KASIR" },
                        label = { Text("Kasir") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Aktif")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.isNotBlank() && username.isNotBlank()) {
                    onSave(
                        User(
                            id = initial?.id ?: 0,
                            username = username.trim(),
                            password = initial?.password ?: "",
                            fullName = fullName.trim(),
                            role = role,
                            active = active
                        ),
                        password
                    )
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
