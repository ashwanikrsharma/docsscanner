package com.pocketscan.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.pocketscan.app.di.AppContainer
import com.pocketscan.app.ui.home.HomeScreen
import com.pocketscan.app.ui.home.HomeViewModel
import com.pocketscan.app.ui.privacy.PrivacyScreen
import com.pocketscan.app.ui.viewer.ViewerScreen
import com.pocketscan.app.ui.viewer.ViewerViewModel

object Routes {
    const val Home = "home"
    const val Privacy = "privacy"
    const val Viewer = "viewer/{id}"
    fun viewer(id: Long) = "viewer/$id"
}

@Composable
fun PocketScanNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            HomeScreen(
                viewModel = vm,
                onOpenScan = { id -> navController.navigate(Routes.viewer(id)) },
                onOpenPrivacy = { navController.navigate(Routes.Privacy) },
            )
        }
        composable(
            route = Routes.Viewer,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val vm: ViewerViewModel = viewModel(factory = ViewerViewModel.factory(container, id))
            ViewerScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Privacy) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}
