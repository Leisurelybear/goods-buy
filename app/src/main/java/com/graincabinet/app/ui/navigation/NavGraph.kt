package com.graincabinet.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.graincabinet.app.ui.collectible.detail.CollectibleDetailScreen
import com.graincabinet.app.ui.collectible.form.CollectibleFormScreen
import com.graincabinet.app.ui.collectible.list.CollectibleListScreen
import com.graincabinet.app.ui.profile.ProfileScreen
import com.graincabinet.app.ui.statistics.StatisticsScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.CollectibleList.route, modifier = modifier) {
        composable(Screen.CollectibleList.route) {
            CollectibleListScreen(
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { navController.navigate(Screen.CollectibleForm.createRoute()) }
            )
        }
        composable(
            route = Screen.CollectibleDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            CollectibleDetailScreen(
                collectibleId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.CollectibleForm.createRoute(id)) }
            )
        }
        composable(
            route = Screen.CollectibleForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")?.takeIf { it > 0 }
            CollectibleFormScreen(
                collectibleId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
    }
}
