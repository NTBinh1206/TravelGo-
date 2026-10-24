package com.travelgo.app.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.travelgo.app.components.BottomTabBar
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.auth.LocationPermissionScreen
import com.travelgo.app.ui.auth.LocationPickerScreen
import com.travelgo.app.ui.auth.LoginScreen
import com.travelgo.app.ui.explore.ExploreScreen
import com.travelgo.app.ui.explore.NearbyScreen
import com.travelgo.app.ui.explore.SearchResultScreen
import com.travelgo.app.ui.home.HomeScreen
import com.travelgo.app.ui.profile.ProfileScreen
import com.travelgo.app.ui.route.SmartRouteScreen

@Composable
fun TravelGoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute != null && currentRoute in Routes.tabRoutes
    // Đã đăng nhập từ lần trước thì vào thẳng Trang chủ.
    val startDestination = remember {
        if (AppContainer.authRepository.currentUser != null) Routes.HOME else Routes.LOGIN
    }

    // Đã có quyền vị trí từ trước thì cập nhật GPS ngầm mỗi lần mở app.
    LaunchedEffect(Unit) {
        val location = AppContainer.locationProvider
        if (location.hasPermission()) location.refreshFromGps()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Mỗi màn tự xử lý thanh trạng thái (statusBarsPadding) để ảnh nền tràn lên trên cùng.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showTabs) {
                BottomTabBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateToTab(route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        navController.navigate(Routes.LOCATION_PERMISSION) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.LOCATION_PERMISSION) {
                LocationPermissionScreen(
                    onLocated = { navController.navigateHomeFrom(Routes.LOCATION_PERMISSION) },
                    onChooseManually = { navController.navigate(Routes.LOCATION_PICKER) },
                )
            }
            composable(Routes.LOCATION_PICKER) {
                LocationPickerScreen(
                    onBack = { navController.popBackStack() },
                    onPicked = {
                        // Mở từ Trang chủ thì quay lại; mở từ màn 02 thì vào Trang chủ.
                        val fromHome = navController.previousBackStackEntry?.destination?.route == Routes.HOME
                        if (fromHome) navController.popBackStack()
                        else navController.navigateHomeFrom(Routes.LOCATION_PERMISSION)
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenExplore = { navController.navigateToTab(Routes.EXPLORE) },
                    onOpenNearby = { navController.navigate(Routes.NEARBY) },
                    onChangeLocation = { navController.navigate(Routes.LOCATION_PICKER) },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(onOpenSearch = { navController.navigate(Routes.SEARCH) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchResultScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NEARBY) {
                NearbyScreen(
                    onBack = { navController.popBackStack() },
                    onSmartRoute = { ids -> navController.navigate(Routes.smartRoute(ids)) },
                )
            }
            composable(
                route = Routes.SMART_ROUTE,
                arguments = listOf(navArgument(Routes.ARG_PLACE_IDS) { type = NavType.StringType }),
            ) { entry ->
                val ids = entry.arguments?.getString(Routes.ARG_PLACE_IDS)
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                SmartRouteScreen(placeIds = ids, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateHomeFrom(route: String) {
    navigate(Routes.HOME) {
        popUpTo(route) { inclusive = true }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
