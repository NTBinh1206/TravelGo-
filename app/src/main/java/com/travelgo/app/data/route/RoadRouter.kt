package com.travelgo.app.data.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.domain.route.TravelTable

/**
 * Quãng đường, thời gian và đường đi theo đường bộ thật.
 * Bản thật: [OsrmRouter]. Không có mạng hoặc máy chủ lỗi thì trả null, màn Lộ trình dùng số ước lượng.
 */
interface RoadRouter {
    /** Bảng km, phút theo đường bộ giữa [start] và mọi điểm trong [places]; null nếu không lấy được. */
    suspend fun table(start: GeoPoint, places: List<Place>): TravelTable?

    /** Các toạ độ của đường đi theo đường bộ qua [points] theo đúng thứ tự, để vẽ lên bản đồ; null nếu không lấy được. */
    suspend fun path(points: List<GeoPoint>): List<GeoPoint>?
}

/** Không gọi máy chủ nào: dùng cho unit test hoặc khi muốn tắt OSRM. */
object NoRoadRouter : RoadRouter {
    override suspend fun table(start: GeoPoint, places: List<Place>): TravelTable? = null
    override suspend fun path(points: List<GeoPoint>): List<GeoPoint>? = null
}
