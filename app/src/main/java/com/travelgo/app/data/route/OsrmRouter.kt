package com.travelgo.app.data.route

import android.util.Log
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.domain.route.TravelTable
import com.travelgo.app.domain.route.estimateTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Quãng đường, thời gian theo đường bộ từ máy chủ OSRM công cộng (miễn phí, không cần khoá API).
 *
 * - Thử lần lượt hai máy chủ; máy nào trả lời được thì dùng.
 * - Mỗi màn Lộ trình chỉ gọi table một lần (đổi chế độ, đổi thứ tự không gọi lại) và route mỗi khi thứ tự đổi.
 * - Kết quả được nhớ trong bộ nhớ để quay lại màn không phải hỏi lại.
 * - Máy chủ công cộng chỉ dành cho thử nghiệm, nhiều nhất khoảng 1 yêu cầu mỗi giây: hợp với đồ án,
 *   khi phát hành thật cần tự dựng máy chủ OSRM hoặc dùng dịch vụ trả phí.
 * - Thời gian OSRM tính theo đường thông thoáng cho ô tô, chưa tính kẹt xe.
 * - Dùng OkHttp thay cho HttpURLConnection: trên máy thử, HttpURLConnection bị máy chủ OSRM từ chối
 *   ở bước bắt tay TLS (SSLV3_ALERT_HANDSHAKE_FAILURE), còn Chrome trên cùng máy kết nối bình thường.
 */
class OsrmRouter(
    private val servers: List<String> = listOf(
        "https://router.project-osrm.org",
        "https://routing.openstreetmap.de/routed-car",
    ),
) : RoadRouter {

    private val http = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    private val tables = ConcurrentHashMap<String, TravelTable>()
    private val paths = ConcurrentHashMap<String, List<GeoPoint>>()

    override suspend fun table(start: GeoPoint, places: List<Place>): TravelTable? {
        if (places.isEmpty()) return null
        val estimate = estimateTable(start, places)
        val points = listOf(start) + places.map { it.location }
        val key = estimate.ids.joinToString("|") + "@" + Osrm.coordinates(listOf(start))
        tables[key]?.let { return it }
        return firstAnswer { base -> Osrm.parseTable(get(Osrm.tableUrl(base, points)), estimate) }
            ?.also { tables[key] = it }
    }

    override suspend fun path(points: List<GeoPoint>): List<GeoPoint>? {
        if (points.size < 2) return null
        val key = Osrm.coordinates(points)
        paths[key]?.let { return it }
        return firstAnswer { base -> Osrm.parsePath(get(Osrm.routeUrl(base, points))) }
            ?.also { paths[key] = it }
    }

    private suspend fun <T> firstAnswer(ask: (String) -> T?): T? = withContext(Dispatchers.IO) {
        for (base in servers) {
            val result = runCatching { ask(base) }
                .onFailure { Log.w(TAG, "OSRM $base lỗi: ${it.javaClass.simpleName}: ${it.message}") }
                .getOrNull()
            if (result != null) return@withContext result
        }
        null
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", AGENT).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string() ?: error("Không có nội dung")
        }
    }

    private companion object {
        const val TAG = "OsrmRouter"
        const val AGENT = "TravelGo/1.0 (Android student project)"
        const val TIMEOUT_S = 8L
    }
}
