package com.goodsbuy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goodsbuy.app.ui.navigation.NavGraph
import com.goodsbuy.app.ui.navigation.Screen
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
            GoodsBuyTheme {
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
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(tween(250)) { it },
                exit = slideOutVertically(tween(250)) { it },
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
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
