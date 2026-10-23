package com.goodsbuy.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.goodsbuy.app.ui.components.enterEasing
import com.goodsbuy.app.ui.components.exitEasing
import com.goodsbuy.app.ui.components.pageEnterSpec
import com.goodsbuy.app.ui.components.pageExitSpec
import com.goodsbuy.app.ui.components.tabEnterSpec
import com.goodsbuy.app.ui.components.tabExitSpec
import com.goodsbuy.app.ui.gallery.GalleryScreen
import com.goodsbuy.app.ui.profile.ProfileScreen
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import com.goodsbuy.app.ui.statistics.StatisticsScreen

private const val NAV_ANIM_DURATION = 300

/**
 * 底部 Tab 之间切换：淡入 + 轻微放大（shared axis 的简化版），
 * 让"换了一个大板块"这件事比纯淡入更容易被感知。
 */
private fun tabEnter(): EnterTransition =
    fadeIn(tabEnterSpec) + scaleIn(initialScale = 0.94f, animationSpec = tabEnterSpec)

private fun tabExit(): ExitTransition =
    fadeOut(tabExitSpec) + scaleOut(targetScale = 1.02f, animationSpec = tabExitSpec)

/** 二级页面（详情/表单/图鉴）：从右侧推入 + 淡入。 */
private fun pageEnter(): EnterTransition =
    slideInHorizontally(tween(NAV_ANIM_DURATION, easing = enterEasing)) { it } + fadeIn(pageEnterSpec)

private fun pageExit(): ExitTransition =
    fadeOut(pageExitSpec) + scaleOut(targetScale = 0.96f, animationSpec = pageExitSpec)

/** 返回：上一级从左侧淡入并轻微放大，当前页向右推出。 */
private fun pagePopEnter(): EnterTransition =
    fadeIn(tabEnterSpec) + scaleIn(initialScale = 0.96f, animationSpec = tabEnterSpec)

private fun pagePopExit(): ExitTransition =
    slideOutHorizontally(tween(NAV_ANIM_DURATION, easing = exitEasing)) { it } + fadeOut(pageExitSpec)

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    preferencesRepository: PreferencesRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CollectibleList.route,
        modifier = modifier,
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabEnter() },
        popExitTransition = { tabExit() }
    ) {
        composable(route = Screen.CollectibleList.route) {
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
            enterTransition = { pageEnter() },
            exitTransition = { pageExit() },
            popEnterTransition = { pagePopEnter() },
            popExitTransition = { pagePopExit() }
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
            enterTransition = { pageEnter() },
            exitTransition = { pageExit() },
            popEnterTransition = { pagePopEnter() },
            popExitTransition = { pagePopExit() }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id")?.takeIf { it > 0 }
            CollectibleFormScreen(
                collectibleId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Statistics.route) {
            StatisticsScreen()
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                preferencesRepository = preferencesRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToForm = { id -> navController.navigate(Screen.CollectibleForm.createRoute(id)) }
            )
        }
        composable(
            route = Screen.Gallery.route,
            enterTransition = { pageEnter() },
            exitTransition = { pageExit() },
            popEnterTransition = { pagePopEnter() },
            popExitTransition = { pagePopExit() }
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