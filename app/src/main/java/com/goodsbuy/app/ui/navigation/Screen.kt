package com.goodsbuy.app.ui.navigation

sealed class Screen(val route: String) {
    data object CollectibleList : Screen("collectible_list")
    data object CollectibleDetail : Screen("collectible_detail/{id}") {
        fun createRoute(id: Long) = "collectible_detail/$id"
    }
    data object CollectibleForm : Screen("collectible_form?id={id}") {
        fun createRoute(id: Long? = null) = if (id != null) "collectible_form?id=$id" else "collectible_form"
    }
    data object Statistics : Screen("statistics")
    data object Profile : Screen("profile")
}
