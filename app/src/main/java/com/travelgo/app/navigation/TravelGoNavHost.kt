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
import com.travelgo.app.ui.route.SmartRouteViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TravelGoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute != null && currentRoute in Routes.tabRoutes
    // Đã đăng nhập và đã có vị trí từ lần trước thì vào thẳng Trang chủ.
    // Đăng nhập rồi nhưng chưa có vị trí (tắt app giữa chừng) thì quay lại bước xin quyền vị trí,
    // tránh vào Trang chủ với vị trí mặc định không đúng.
    val startDestination = remember {
        when {
            AppContainer.authRepository.currentUser == null -> Routes.LOGIN
            AppContainer.locationProvider.currentLocation.value == null -> Routes.LOCATION_PERMISSION
            else -> Routes.HOME
        }
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
                    onChooseManually = { navController.navigate(Routes.LOCATION_PICKER) { launchSingleTop = true } },
                )
            }
            composable(Routes.LOCATION_PICKER) {
                // Chỉ đóng đúng màn chọn vị trí: bấm nhanh hai lần không làm mất luôn Trang chủ (màn trắng).
                LocationPickerScreen(
                    onBack = { navController.popBackStack(Routes.LOCATION_PICKER, inclusive = true) },
                    onPicked = {
                        // Mở từ màn 02 (lúc mới đăng nhập) thì vào Trang chủ; mở từ màn khác thì quay lại màn đó.
                        val fromPermission =
                            navController.previousBackStackEntry?.destination?.route == Routes.LOCATION_PERMISSION
                        if (fromPermission) navController.navigateHomeFrom(Routes.LOCATION_PERMISSION)
                        else navController.popBackStack(Routes.LOCATION_PICKER, inclusive = true)
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                    onOpenExplore = { navController.navigateToTab(Routes.EXPLORE) },
                    onOpenNearby = { navController.navigate(Routes.NEARBY) { launchSingleTop = true } },
                    onChangeLocation = { navController.navigate(Routes.LOCATION_PICKER) { launchSingleTop = true } },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                    onChangeLocation = { navController.navigate(Routes.LOCATION_PICKER) { launchSingleTop = true } },
                )
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
                    onChangeLocation = { navController.navigate(Routes.LOCATION_PICKER) { launchSingleTop = true } },
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
                // ViewModel gắn với màn này: giữ chế độ và thứ tự tự xếp khi xoay máy / quay lại từ Google Maps.
                val routeViewModel = viewModel(entry) { SmartRouteViewModel(placeIds = ids) }
                SmartRouteScreen(viewModel = routeViewModel, onBack = { navController.popBackStack() })
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
