package com.travelgo.app.data.location

import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * QUY ƯỚC CHUNG: vị trí hiện tại của người dùng. Người 1 cung cấp.
 *
 * Cách dùng ở màn khác (Người 2, Người 3):
 *   val me by AppContainer.locationProvider.currentLocation.collectAsState()
 *   val center = me ?: GeoPoint.DEFAULT
 *
 * - currentLocation: toạ độ, null nghĩa là chưa có vị trí (chưa cho quyền / chưa chọn).
 * - placeName: tên ngắn để hiển thị, ví dụ "Đà Nẵng".
 */
interface LocationProvider {
    val currentLocation: StateFlow<GeoPoint?>
    val placeName: StateFlow<String?>

    /** Đặt vị trí bằng tay (ví dụ người dùng chọn tỉnh thành). */
    fun updateLocation(point: GeoPoint, placeName: String? = null)

    /** Đã được cấp quyền vị trí chưa. */
    fun hasPermission(): Boolean

    /** Lấy vị trí GPS hiện tại. Chỉ gọi khi đã có quyền. */
    suspend fun refreshFromGps(): Result<GeoPoint>
}

/** Không lấy được vị trí (GPS tắt, máy ảo chưa đặt vị trí, hết thời gian chờ…). */
class LocationUnavailableException : Exception("Không lấy được vị trí hiện tại")

/** Bản giả, giữ vị trí trong bộ nhớ. Dùng cho test hoặc Preview. */
class InMemoryLocationProvider : LocationProvider {
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    private val _placeName = MutableStateFlow<String?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()
    override val placeName: StateFlow<String?> = _placeName.asStateFlow()

    override fun updateLocation(point: GeoPoint, placeName: String?) {
        _currentLocation.value = point
        _placeName.value = placeName
    }

    override fun hasPermission(): Boolean = false

    override suspend fun refreshFromGps(): Result<GeoPoint> = Result.failure(LocationUnavailableException())
}
