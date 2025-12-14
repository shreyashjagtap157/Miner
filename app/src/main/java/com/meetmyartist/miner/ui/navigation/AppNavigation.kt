package com.meetmyartist.miner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meetmyartist.miner.ui.screens.*
import com.meetmyartist.miner.ui.viewmodel.MiningViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavigation(navController: NavHostController) {
    val miningViewModel: MiningViewModel = hiltViewModel()
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(viewModel = miningViewModel) }
        composable("achievements") { AchievementsScreen() }
        composable("profitability") { ProfitabilityScreen() }
        // Add other destinations
    }
}
