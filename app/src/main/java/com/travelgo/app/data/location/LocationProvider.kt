package com.travelgo.app.data.location

import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * QUY ƯỚC CHUNG: vị trí hiện tại của người dùng. Người 1 cung cấp.
 * null nghĩa là chưa có vị trí (chưa cho quyền hoặc đang lấy).
 */
interface LocationProvider {
    val currentLocation: StateFlow<GeoPoint?>
    fun updateLocation(point: GeoPoint)
}

/**
 * Bản tạm: giữ vị trí trong bộ nhớ.
 * TODO(Người 1): thay bằng FusedLocationProviderClient (play-services-location).
 */
class InMemoryLocationProvider : LocationProvider {
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    override fun updateLocation(point: GeoPoint) {
        _currentLocation.value = point
    }
}
