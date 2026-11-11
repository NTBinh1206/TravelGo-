package com.travelgo.app.data.place

import com.travelgo.app.data.model.GeoPoint

/**
 * QUY ƯỚC CHUNG: dữ liệu một điểm đến. Người 2 tạo dữ liệu, cả nhóm dùng.
 * Đổi field nào phải báo cả nhóm trước.
 */
data class Place(
    val id: String,          // mã OSM ("n123", "w123", "r123") hoặc "c1" cho thành phố nổi bật
    val name: String,
    val category: String,    // bien, nui, van_hoa, thanh_pho, thien_nhien
    val address: String,     // "Sơn Trà, Đà Nẵng"
    val lat: Double,
    val lng: Double,
    /**
     * Độ nổi bật 0–5 do nhóm tự tính từ dữ liệu OSM (có bài Wikipedia, là di tích, có ảnh…).
     * KHÔNG phải điểm đánh giá sao của người dùng: OpenStreetMap không có số sao.
     */
    val score: Double,
    val stayMinutes: Int,    // thời gian tham quan ước lượng theo loại địa điểm
    val imageUrl: String = "",
    val tagline: String = "", // mô tả ngắn: "Bãi biển", "Khu du lịch"...
) {
    val location: GeoPoint get() = GeoPoint(lat, lng)
}
