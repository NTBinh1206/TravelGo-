package com.travelgo.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Vị trí thật bằng Google Play Services (FusedLocationProviderClient).
 * - Lấy toạ độ GPS hiện tại, nếu không được thì dùng vị trí gần nhất máy còn nhớ.
 * - Đổi toạ độ ra tên tỉnh/thành (Geocoder) để hiển thị.
 * - Lưu vị trí cuối cùng vào máy, lần mở app sau vẫn còn.
 */
class FusedLocationProvider(context: Context) : LocationProvider {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val prefs = appContext.getSharedPreferences("travelgo_location", Context.MODE_PRIVATE)

    private val _currentLocation = MutableStateFlow(loadSavedPoint())
    private val _placeName = MutableStateFlow(prefs.getString(KEY_NAME, null))
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()
    override val placeName: StateFlow<String?> = _placeName.asStateFlow()

    override fun updateLocation(point: GeoPoint, placeName: String?) {
        _currentLocation.value = point
        _placeName.value = placeName
        prefs.edit()
            .putString(KEY_LAT, point.lat.toString())
            .putString(KEY_LNG, point.lng.toString())
            .putString(KEY_NAME, placeName)
            .apply()
    }

    override fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // đã kiểm tra bằng hasPermission()
    override suspend fun refreshFromGps(): Result<GeoPoint> {
        if (!hasPermission()) return Result.failure(SecurityException("Chưa được cấp quyền vị trí"))
        return try {
            val cts = CancellationTokenSource()
            val fresh = withTimeoutOrNull(TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).awaitOrNull()
            }
            if (fresh == null) cts.cancel()
            val location = fresh ?: client.lastLocation.awaitOrNull()
                ?: return Result.failure(LocationUnavailableException())

            val point = GeoPoint(location.latitude, location.longitude)
            val name = reverseGeocode(point)
            updateLocation(point, name ?: "Vị trí của bạn")
            Log.d(TAG, "GPS: $point → $name")
            Result.success(point)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy GPS", e)
            Result.failure(e)
        }
    }

    /** Toạ độ → tên ngắn, ví dụ "Đà Nẵng". Không có mạng thì trả về null. */
    private suspend fun reverseGeocode(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale.forLanguageTag("vi"))
        val address: Address? = try {
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(point.lat, point.lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(point.lat, point.lng, 1)?.firstOrNull()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder lỗi", e)
            null
        }
        val raw = address?.adminArea ?: address?.locality ?: address?.subAdminArea
        return raw?.let(::shortName)
    }

    private fun loadSavedPoint(): GeoPoint? {
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
        return GeoPoint(lat, lng)
    }

    companion object {
        private const val TAG = "TravelGoLocation"
        private const val TIMEOUT_MS = 15_000L
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_NAME = "name"

        /** "Thành phố Đà Nẵng" → "Đà Nẵng", "Tỉnh Lâm Đồng" → "Lâm Đồng". */
        fun shortName(raw: String): String {
            val prefixes = listOf("Thành phố ", "Thành Phố ", "TP. ", "TP ", "Tp. ", "Tỉnh ")
            val p = prefixes.firstOrNull { raw.startsWith(it) } ?: return raw.trim()
            return raw.removePrefix(p).trim()
        }
    }
}

/** Chờ Task của Google xong, lỗi thì trả về null. */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
    addOnCanceledListener { if (cont.isActive) cont.resume(null) }
}
