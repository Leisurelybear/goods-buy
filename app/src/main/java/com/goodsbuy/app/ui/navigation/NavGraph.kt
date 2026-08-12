package com.goodsbuy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.ui.collectible.detail.CollectibleDetailScreen
import com.goodsbuy.app.ui.collectible.form.CollectibleFormScreen
import com.goodsbuy.app.ui.collectible.list.CollectibleListScreen
import com.goodsbuy.app.ui.profile.ProfileScreen
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.statistics.StatisticsScreen

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.CollectibleList.route, modifier = modifier) {
        composable(Screen.CollectibleList.route) {
            val context = LocalContext.current
            val preferencesRepository = remember { PreferencesRepository(context) }
            CollectibleListScreen(
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { navController.navigate(Screen.CollectibleForm.createRoute()) },
                preferencesRepository = preferencesRepository
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
            val context = LocalContext.current
            val preferencesRepository = remember { PreferencesRepository(context) }
            ProfileScreen(
                preferencesRepository = preferencesRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
