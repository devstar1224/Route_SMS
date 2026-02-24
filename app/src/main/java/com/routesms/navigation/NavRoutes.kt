package com.routesms.navigation

sealed class NavRoute(val route: String) {
    object Main : NavRoute("main")
    object Settings : NavRoute("settings")
    object Filters : NavRoute("filters")
}
