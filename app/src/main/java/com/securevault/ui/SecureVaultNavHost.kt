package com.securevault.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.securevault.ui.screens.*

@Composable
fun SecureVaultNavHost() {
    val root = rememberNavController()
    NavHost(root, "lock") {
        composable("lock") {
            LockScreen(
                onUnlocked = { root.navigate("main") { popUpTo("lock") { inclusive = true } } },
                onBiometricRequest = {}
            )
        }
        composable("main") {
            MainShell(onLock = { root.navigate("lock") { popUpTo("main") { inclusive = true } } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(onLock: () -> Unit) {
    data class Tab(val route: String, val label: String, val icon: ImageVector)
    val tabs = listOf(
        Tab("passwords", "Пароли",    Icons.Default.VpnKey),
        Tab("favorites", "Избранное", Icons.Default.Star),
        Tab("settings",  "Настройки", Icons.Default.Settings)
    )
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val cur = back?.destination?.route?.substringBefore("?")

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = cur == tab.route || (tab.route == "passwords" && cur == null),
                    onClick = {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, "passwords", Modifier.padding(pad)) {
            composable("passwords") {
                VaultListScreen(
                    onAdd = { nav.navigate("entry") },
                    onEdit = { nav.navigate("entry?id=$it") },
                    onLock = onLock
                )
            }
            composable("favorites") {
                VaultListScreen(
                    onAdd = { nav.navigate("entry") },
                    onEdit = { nav.navigate("entry?id=$it") },
                    onLock = onLock,
                    favOnly = true
                )
            }
            composable("settings") { SettingsScreen(onResetDone = onLock) }
            composable(
                "entry?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { back ->
                val rawId = back.arguments?.getLong("id") ?: -1L
                EntryEditScreen(
                    entryId = if (rawId == -1L) null else rawId,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
