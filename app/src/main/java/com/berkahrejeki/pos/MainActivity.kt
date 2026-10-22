package com.berkahrejeki.pos

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.berkahrejeki.pos.data.Session
import com.berkahrejeki.pos.ui.screens.*
import com.berkahrejeki.pos.ui.theme.*

class MainActivity : ComponentActivity() {
    class MainActivity : androidx.fragment.app.FragmentActivity() {
        
        // Minta izin otomatis (Khusus Android 12 ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                100
            )
        }

        setContent {
            val vm: MainViewModel = viewModel()
            LaunchedEffect(Unit) {
    vm.seedDefaultUsers()
    vm.seedIfEmpty()
    vm.autoArchiveOldData()
            }

            val ctx = LocalContext.current
            var theme by remember { mutableStateOf(ThemeManager.load(ctx)) }

            BerkahRejekiTheme(theme) {
                val session by vm.session.collectAsStateWithLifecycle()
                if (session == null) {
                    LoginScreen(vm)
                } else {
                    BerkahRejekiApp(vm, session!!, theme) {
                        theme = it
                        ThemeManager.save(ctx, it)
                    }
                }
            }
        }
    }
}

data class MenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BerkahRejekiApp(
    vm: MainViewModel,
    session: Session,
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    val nav = rememberNavController()

    val menu = buildList {
        add(MenuItem("kasir", "Kasir", Icons.Default.ShoppingCart))
        if (session.isAdmin) add(MenuItem("produk", "Produk", Icons.Default.Inventory2))
        add(MenuItem("riwayat", "Riwayat", Icons.Default.ReceiptLong))
        add(MenuItem("laporan", "Laporan", Icons.Default.BarChart))
        if (session.isAdmin) add(MenuItem("users", "User", Icons.Default.Group))
    }

    val current = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BERKAH REJEKI", style = MaterialTheme.typography.titleMedium)
                        Text(session.fullName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // === IKON PRINTER ===
                    IconButton(onClick = {
                        nav.navigate("printer") {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.Print, "Printer")
                    }
                    // === IKON TEMA (PALETTE) ===
                    IconButton(onClick = {
                        nav.navigate("settings") {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.Palette, "Tema")
                    }
                    // === IKON LOGOUT ===
                    IconButton(onClick = { vm.logout() }) {
                        Icon(Icons.Default.Logout, "Keluar")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                menu.forEach { m ->
                    NavigationBarItem(
                        selected = current == m.route,
                        onClick = {
                            nav.navigate(m.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(m.icon, m.label) },
                        label = { Text(m.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "kasir", modifier = Modifier.padding(pad)) {
            composable("kasir") { KasirScreen(vm) }
            composable("produk") { ProdukScreen(vm) }
            composable("riwayat") { RiwayatScreen(vm) }
            composable("laporan") { LaporanScreen(vm, session) }
            composable("users") { UserManagementScreen(vm, session) }
            composable("printer") { PrinterScreen() }
            composable("settings") { SettingsScreen(vm, session, theme, onThemeChange) }
        }
    }
}
