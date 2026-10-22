package com.berkahrejeki.pos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berkahrejeki.pos.AppConfig
import com.berkahrejeki.pos.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    vm: MainViewModel,
    bioEnabled: Boolean = false,
    onBioLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (AppConfig.LOGO_NAME.isNotEmpty()) {
                    val logoResId = ctx.resources.getIdentifier(
                        AppConfig.LOGO_NAME,
                        "mipmap",
                        ctx.packageName
                    )
                    if (logoResId != 0) {
                        Image(
                            painter = painterResource(id = logoResId),
                            contentDescription = "Logo",
                            modifier = Modifier.size(96.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Storefront, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.Storefront, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    AppConfig.NAMA_TOKO,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    AppConfig.TAGLINE,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                if (showPwd) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                null
                            )
                        }
                    },
                    visualTransformation = if (showPwd) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            error = "Lengkapi username & password"
                            return@Button
                        }
                        loading = true
                        vm.login(username, password) { ok, msg ->
                            loading = false
                            if (!ok) error = msg
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("MASUK", fontWeight = FontWeight.Bold)
                    }
                }

                // ===== TOMBOL SIDIK JARI =====
                if (bioEnabled) {
                    Spacer(Modifier.height(16.dp))
                    IconButton(
                        onClick = onBioLogin,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Login Sidik Jari",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Login Sidik Jari",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // =============================

                Spacer(Modifier.height(16.dp))
                Text(
                    "Default admin: admin / admin123\nKasir: kasir / kasir123",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        }
    }
}
