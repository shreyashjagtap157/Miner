package com.meetmyartist.miner.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Configuration : Screen("configuration")
    object Wallets : Screen("wallets")
    object ResourceControl : Screen("resource_control")
    object AdvancedResource : Screen("advanced_resource")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
    object Achievements : Screen("achievements")
    object Profitability : Screen("profitability")
    object SystemMonitor : Screen("system_monitor")
    object UnifiedMining : Screen("unified_mining")
    object RemoteControl : Screen("remote_control")
}
