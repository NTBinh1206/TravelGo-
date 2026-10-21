package com.travelgo.app.di

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.PlaceRepository

/**
 * Nơi tạo các đối tượng dùng chung cho cả app.
 * Khi có bản thật, chỉ cần đổi ở đây, các màn hình không phải sửa.
 * (Có thể chuyển sang Hilt sau khi khung đã chạy ổn.)
 */
object AppContainer {
    val locationProvider: LocationProvider = InMemoryLocationProvider()
    val placeRepository: PlaceRepository = FakePlaceRepository()
}
