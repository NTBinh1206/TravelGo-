package com.travelgo.app.data.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.domain.route.TravelTable
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Định dạng câu hỏi và đọc câu trả lời của OSRM (Open Source Routing Machine, chạy trên dữ liệu OpenStreetMap).
 * Không dùng Android nên kiểm thử được bằng unit test thường (OsrmTest).
 *
 * - Dịch vụ table: quãng đường (mét) và thời gian (giây) giữa mọi cặp điểm, chỉ một lần gọi.
 * - Dịch vụ route: đường đi qua các điểm theo thứ tự, trả về dãy toạ độ để vẽ.
 */
object Osrm {
    /** "lng,lat;lng,lat;…" (OSRM ghi kinh độ trước). */
    fun coordinates(points: List<GeoPoint>): String =
        points.joinToString(";") { String.format(Locale.US, "%.6f,%.6f", it.lng, it.lat) }

    fun tableUrl(base: String, points: List<GeoPoint>): String =
        "$base/table/v1/driving/${coordinates(points)}?annotations=distance,duration"

    fun routeUrl(base: String, points: List<GeoPoint>): String =
        "$base/route/v1/driving/${coordinates(points)}?overview=full&geometries=geojson&steps=false"

    /**
     * Đọc kết quả table thành [TravelTable].
     * Ô nào OSRM không tìm được đường (null) thì lấy giá trị ở [fallback] (bảng ước lượng cùng thứ tự điểm).
     * Trả null nếu máy chủ báo lỗi hoặc kích thước bảng không khớp.
     */
    fun parseTable(json: String, fallback: TravelTable): TravelTable? {
        val root = JSONObject(json)
        if (root.optString("code") != "Ok") return null
        val n = fallback.ids.size
        val dist = root.optJSONArray("distances") ?: return null
        val dur = root.optJSONArray("durations") ?: return null
        if (dist.length() != n || dur.length() != n) return null
        val km = Array(n) { i -> DoubleArray(n) { j -> cell(dist, i, j)?.div(1000.0) ?: fallback.km[i][j] } }
        val minutes = Array(n) { i -> DoubleArray(n) { j -> cell(dur, i, j)?.div(60.0) ?: fallback.minutes[i][j] } }
        return TravelTable(fallback.ids, km, minutes, byRoad = true)
    }

    /** Đọc đường đi của kết quả route: routes[0].geometry.coordinates = [[lng, lat], …]. */
    fun parsePath(json: String): List<GeoPoint>? {
        val root = JSONObject(json)
        if (root.optString("code") != "Ok") return null
        val coords = root.optJSONArray("routes")?.optJSONObject(0)
            ?.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
        val out = ArrayList<GeoPoint>(coords.length())
        for (k in 0 until coords.length()) {
            val c = coords.optJSONArray(k) ?: continue
            out += GeoPoint(c.getDouble(1), c.getDouble(0))
        }
        return out.takeIf { it.size >= 2 }
    }

    private fun cell(rows: JSONArray, i: Int, j: Int): Double? {
        val row = rows.optJSONArray(i) ?: return null
        if (j >= row.length() || row.isNull(j)) return null
        return row.optDouble(j).takeIf { !it.isNaN() && it >= 0 }
    }
}
