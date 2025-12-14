package com.meetmyartist.miner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meetmyartist.miner.data.preferences.PreferencesManager
import com.meetmyartist.miner.ui.navigation.Screen
import com.meetmyartist.miner.ui.screens.*
import com.meetmyartist.miner.ui.theme.MinerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            
            MinerTheme(darkTheme = darkTheme) {
                MinerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentDestination?.route) {
                            Screen.Dashboard.route -> "Crypto Miner"
                            Screen.Configuration.route -> "Configuration"
                            Screen.Wallets.route -> "Wallets"
                            Screen.ResourceControl.route -> "Resource Control"
                            Screen.Statistics.route -> "Statistics"
                            Screen.Settings.route -> "Settings"
                            Screen.Achievements.route -> "Achievements"
                            Screen.Profitability.route -> "Profitability"
                            Screen.SystemMonitor.route -> "System Monitor"
                            Screen.UnifiedMining.route -> "Unified Mining"
                            Screen.RemoteControl.route -> "Remote Control"
                            else -> "Crypto Miner"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Achievements.route) {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Achievements")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Profitability.route) {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Filled.Calculate, contentDescription = "Profitability")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Dashboard.route } == true,
                    onClick = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Config") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Configuration.route } == true,
                    onClick = {
                        navController.navigate(Screen.Configuration.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Wallets") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Wallets.route } == true,
                    onClick = {
                        navController.navigate(Screen.Wallets.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
                
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Memory, contentDescription = null) },
                    label = { Text("Resources") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.ResourceControl.route } == true,
                    onClick = {
                        navController.navigate(Screen.ResourceControl.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                    label = { Text("Stats") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Statistics.route } == true,
                    onClick = {
                        navController.navigate(Screen.Statistics.route) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            
            composable(Screen.Configuration.route) {
                ConfigurationScreen()
            }
            
            composable(Screen.Wallets.route) {
                WalletsScreen()
            }
            
            composable(Screen.ResourceControl.route) {
                ResourceControlScreen(navController = navController)
            }
            
            composable(Screen.AdvancedResource.route) {
                AdvancedResourceScreen()
            }
            
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            
            composable(Screen.Achievements.route) {
                AchievementsScreen()
            }
            
            composable(Screen.Profitability.route) {
                ProfitabilityScreen()
            }
            
            composable(Screen.SystemMonitor.route) {
                SystemMonitorScreen()
            }
            
            composable(Screen.UnifiedMining.route) {
                UnifiedMiningDashboard()
            }
            
            composable(Screen.RemoteControl.route) {
                RemoteMiningControlScreen()
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}