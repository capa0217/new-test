package com.kapa.ailedger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.kapa.ailedger.ui.theme.IosBlue
import com.kapa.ailedger.ui.theme.NavBg
import com.kapa.ailedger.ui.theme.TextPrimary
import com.kapa.ailedger.ui.theme.TextSecondary
import com.kapa.ailedger.vm.AppViewModel
import com.kapa.ailedger.vm.ChatViewModel
import kotlinx.coroutines.flow.collectLatest

data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun AppRoot(vm: AppViewModel = viewModel(), chatVm: ChatViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.toast.collectLatest { snackbar.showSnackbar(it) }
    }

    val tabs = listOf(
        Tab("home", "明细") { Icon(Icons.AutoMirrored.Filled.ReceiptLong, null) },
        Tab("debts", "借还") { Icon(Icons.Filled.Handshake, null) },
        Tab("chat", "助手") { Icon(Icons.Filled.SmartToy, null) },
        Tab("settings", "设置") { Icon(Icons.Filled.Settings, null) }
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.White,
                    contentColor = TextPrimary
                )
            }
        },
        bottomBar = {
            val backStack by nav.currentBackStackEntryAsState()
            val current = backStack?.destination?.route
            NavigationBar(
                containerColor = NavBg,
                contentColor = IosBlue,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IosBlue,
                            selectedTextColor = IosBlue,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = IosBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            composable("home") { HomeScreen(vm) }
            composable("debts") { DebtScreen(vm) }
            composable("chat") { ChatScreen(vm, chatVm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
