package com.routesms.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.routesms.ui.screen.FilterScreen
import com.routesms.ui.screen.MainScreen
import com.routesms.ui.screen.SettingsScreen

@Composable
fun RouteSmsNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.route
    ) {
        composable(NavRoute.Main.route) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(NavRoute.Settings.route)
                },
                onNavigateToFilters = {
                    navController.navigate(NavRoute.Filters.route)
                }
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Filters.route) {
            FilterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
