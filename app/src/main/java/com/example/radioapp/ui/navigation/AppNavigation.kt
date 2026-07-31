package com.example.radioapp.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.radioapp.data.StationRepository
import com.example.radioapp.player.RadioController
import com.example.radioapp.ui.screens.BrowseScreen
import com.example.radioapp.ui.screens.FavouritesScreen
import com.example.radioapp.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Browse : Screen("browse")
    object Favourites : Screen("favourites")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues,
    repository: StationRepository,
    radioController: RadioController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Browse.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Browse.route) {
            BrowseScreen(repository, radioController)
        }
        composable(Screen.Favourites.route) {
            FavouritesScreen(repository, radioController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(repository)
        }
    }
}
