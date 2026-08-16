package com.goodsbuy.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.goodsbuy.app.ui.collectible.detail.CollectibleDetailScreen
import com.goodsbuy.app.ui.collectible.form.CollectibleFormScreen
import com.goodsbuy.app.ui.collectible.list.CollectibleListScreen
import com.goodsbuy.app.ui.gallery.GalleryScreen
import com.goodsbuy.app.ui.profile.ProfileScreen
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.statistics.StatisticsScreen

private const val NAV_ANIM_DURATION = 300

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    preferencesRepository: PreferencesRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CollectibleList.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.CollectibleList.route,
            enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
        ) {
            CollectibleListScreen(
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                preferencesRepository = preferencesRepository
            )
        }
        composable(
            route = Screen.CollectibleDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeOut(tween(NAV_ANIM_DURATION)) }
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
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeOut(tween(NAV_ANIM_DURATION)) }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")?.takeIf { it > 0 }
            CollectibleFormScreen(
                collectibleId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Statistics.route,
            enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
        ) {
            StatisticsScreen()
        }
        composable(
            route = Screen.Profile.route,
            enterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) }
        ) {
            ProfileScreen(
                preferencesRepository = preferencesRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) }
            )
        }
        composable(
            route = Screen.Gallery.route,
            enterTransition = { slideInHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeIn(tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(NAV_ANIM_DURATION)) },
            popEnterTransition = { fadeIn(tween(NAV_ANIM_DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(NAV_ANIM_DURATION)) { it } + fadeOut(tween(NAV_ANIM_DURATION)) }
        ) {
            GalleryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Screen.CollectibleDetail.createRoute(it)) },
                onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) },
                preferencesRepository = preferencesRepository
            )
        }
    }
}
