package com.travelgo.app.di

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.travelgo.app.data.auth.AuthRepository
import com.travelgo.app.data.location.FusedLocationProvider
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.FirestoreClosedReports
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.place.OsmNoteSender
import com.travelgo.app.data.place.OsmNotesClient
import com.travelgo.app.data.place.OsmPlaceRepository
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.data.route.OsrmRouter
import com.travelgo.app.data.route.RoadRouter
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Nơi tạo các đối tượng dùng chung cho cả app (Model trong MVVM: Repository, LocationProvider).
 * ViewModel lấy các đối tượng này qua tham số mặc định, nên khi test có thể truyền bản giả vào.
 * Khi có bản thật, chỉ cần đổi ở đây, các màn hình và ViewModel không phải sửa.
 */
object AppContainer {
    private const val USER_AGENT = "TravelGo/1.0 (Android student project)"

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
                                    .header("User-Agent", USER_AGENT)
                                    .build(),
                            )
                        }
                        .build()
                }
                .build(),
        )
        // Bản đồ OpenStreetMap (osmdroid): máy chủ ô bản đồ cũng yêu cầu User-Agent riêng.
        // Lưu ô bản đồ trong bộ nhớ đệm của app, không cần quyền ghi bộ nhớ ngoài.
        Configuration.getInstance().apply {
            userAgentValue = USER_AGENT
            osmdroidBasePath = File(appContext.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }

    val locationProvider: LocationProvider by lazy { FusedLocationProvider(appContext) }
    // Dữ liệu thật: bộ dữ liệu OSM làm sẵn (assets/places_vn.tsv) + Wikipedia.
    // Muốn quay lại dữ liệu mẫu: FakePlaceRepository().
    val placeRepository: PlaceRepository by lazy { OsmPlaceRepository(appContext, locationProvider) }
    val authRepository: AuthRepository by lazy { AuthRepository() }
    // Người dùng báo quán đã đóng cửa (Cloud Firestore). Muốn chạy không cần Firebase: InMemoryClosedReports().
    val closedReports: ClosedReportRepository by lazy { FirestoreClosedReports() }
    // Gửi ghi chú "quán có thể đã đóng" lên OpenStreetMap khi người dùng đồng ý (ẩn danh, không cần tài khoản).
    val osmNotes: OsmNoteSender by lazy { OsmNotesClient() }
    // Quãng đường, thời gian, đường đi theo đường bộ (OSRM). Muốn tắt: NoRoadRouter, app quay về số ước lượng.
    val roadRouter: RoadRouter by lazy { OsrmRouter() }
}
