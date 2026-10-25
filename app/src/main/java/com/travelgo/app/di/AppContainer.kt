package com.travelgo.app.di

import android.content.Context
import com.travelgo.app.data.auth.AuthRepository
import com.travelgo.app.data.location.FusedLocationProvider
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.PlaceRepository

/**
 * Nơi tạo các đối tượng dùng chung cho cả app.
 * Khi có bản thật, chỉ cần đổi ở đây, các màn hình không phải sửa.
 * (Có thể chuyển sang Hilt sau khi khung đã chạy ổn.)
 */
object AppContainer {
    private lateinit var appContext: Context

    /** Gọi một lần trong MainActivity.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val locationProvider: LocationProvider by lazy { FusedLocationProvider(appContext) }
    val placeRepository: PlaceRepository = FakePlaceRepository()
    val authRepository: AuthRepository by lazy { AuthRepository() }
}
