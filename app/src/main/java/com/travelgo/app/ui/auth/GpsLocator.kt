package com.travelgo.app.ui.auth

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.launch

/** Kết quả một lần lấy vị trí GPS. */
sealed interface GpsResult {
    data object Success : GpsResult
    data object PermissionDenied : GpsResult
    data object GpsOff : GpsResult
    data class Failed(val message: String) : GpsResult
}

@Stable
class GpsLocator internal constructor() {
    var isLoading by mutableStateOf(false)
        internal set
    internal var startAction: () -> Unit = {}

    /** Xin quyền (nếu chưa có) → nhờ bật GPS (nếu đang tắt) → lấy vị trí. */
    fun start() = startAction()
}

/**
 * Gói toàn bộ quy trình lấy GPS để dùng lại ở nhiều màn:
 * màn 02 Xin quyền vị trí, màn Chọn vị trí.
 */
@Composable
fun rememberGpsLocator(
    provider: LocationProvider = AppContainer.locationProvider,
    onResult: (GpsResult) -> Unit,
): GpsLocator {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnResult by rememberUpdatedState(onResult)
    val locator = remember { GpsLocator() }

    fun finish(result: GpsResult) {
        locator.isLoading = false
        latestOnResult(result)
    }

    fun fetch() {
        locator.isLoading = true
        scope.launch {
            provider.refreshFromGps()
                .onSuccess { finish(GpsResult.Success) }
                .onFailure { finish(GpsResult.Failed("Không lấy được vị trí. Hãy bật GPS hoặc chọn tỉnh thành.")) }
        }
    }

    // Hộp thoại "Bật vị trí thiết bị?" của Google
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) fetch() else finish(GpsResult.GpsOff)
    }

    fun checkSettingsThenFetch() {
        locator.isLoading = true
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build())
            .build()
        LocationServices.getSettingsClient(context).checkLocationSettings(request)
            .addOnSuccessListener { fetch() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    settingsLauncher.launch(IntentSenderRequest.Builder(e.resolution).build())
                } else {
                    fetch() // không hỏi được thì vẫn thử lấy
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) checkSettingsThenFetch() else finish(GpsResult.PermissionDenied)
    }

    locator.startAction = {
        if (!locator.isLoading) {
            if (provider.hasPermission()) {
                checkSettingsThenFetch()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
    return locator
}
