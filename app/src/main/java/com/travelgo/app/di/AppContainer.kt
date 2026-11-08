package com.travelgo.app.di

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.travelgo.app.data.auth.AuthRepository
import com.travelgo.app.data.location.FusedLocationProvider
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.place.OsmPlaceRepository
import com.travelgo.app.data.place.PlaceRepository
import okhttp3.OkHttpClient

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
        // Ảnh Wikipedia/Wikimedia: máy chủ yêu cầu ghi rõ tên ứng dụng (User-Agent), nếu không có thể bị từ chối.
        Coil.setImageLoader(
            ImageLoader.Builder(appContext)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("User-Agent", "TravelGo/1.0 (Android student project)")
                                    .build(),
                            )
                        }
                        .build()
                }
                .build(),
        )
    }

    val locationProvider: LocationProvider by lazy { FusedLocationProvider(appContext) }
    // Dữ liệu thật: bộ dữ liệu OSM làm sẵn (assets/osm_vn.tsv) + Wikipedia.
    // Muốn quay lại dữ liệu mẫu: FakePlaceRepository().
    val placeRepository: PlaceRepository by lazy { OsmPlaceRepository(appContext, locationProvider) }
    val authRepository: AuthRepository by lazy { AuthRepository() }
}
