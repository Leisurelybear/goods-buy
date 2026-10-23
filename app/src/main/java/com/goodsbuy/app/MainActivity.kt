package com.goodsbuy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goodsbuy.app.ui.navigation.NavGraph
import com.goodsbuy.app.ui.navigation.Screen
import com.goodsbuy.app.ui.theme.AppThemes
import com.goodsbuy.app.ui.theme.GoodsBuyTheme
import dagger.hilt.android.AndroidEntryPoint
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeId = preferencesRepository.preferencesState.value.themeId
            GoodsBuyTheme(theme = AppThemes.byId(themeId)) {
                MainScreen(preferencesRepository)
            }
        }
    }
}

data class BottomNavItem(val label: String, val icon: @Composable () -> Unit, val route: String)

@Composable
fun MainScreen(preferencesRepository: PreferencesRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("藏品柜", { Icon(Icons.Default.Inventory2, contentDescription = null) }, Screen.CollectibleList.route),
        BottomNavItem("统计", { Icon(Icons.Default.BarChart, contentDescription = null) }, Screen.Statistics.route),
        BottomNavItem("我的", { Icon(Icons.Default.Person, contentDescription = null) }, Screen.Profile.route)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { AnimatedNavIcon(selected = selected) { item.icon() } },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            preferencesRepository = preferencesRepository
        )
    }
}

/** 底部导航图标：选中时放大并轻微上浮，带一点弹簧回弹。 */
@Composable
private fun AnimatedNavIcon(selected: Boolean, icon: @Composable () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_icon_scale"
    )
    val lift by animateDpAsState(
        targetValue = if (selected) (-1).dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "nav_icon_lift"
    )
    Box(
        modifier = Modifier
            .offset(y = lift)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        icon()
    }
}