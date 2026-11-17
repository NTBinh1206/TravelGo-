package com.travelgo.app.data.place

import com.travelgo.app.data.model.GeoPoint
import java.text.Normalizer
/**
 * QUY ƯỚC CHUNG: nơi lấy dữ liệu địa điểm.
 * Bản thật: OsmPlaceRepository (bộ dữ liệu OpenStreetMap làm sẵn + Wikipedia + Nominatim).
 */
interface PlaceRepository {
    /** Điểm du lịch quanh [center] trong bán kính [radiusKm] (Nearby Search). */
    suspend fun getNearbyPlaces(center: GeoPoint, radiusKm: Int = 25): List<Place>

    /** Tìm theo tên địa điểm hoặc thành phố (Text Search). */
    suspend fun searchPlaces(query: String): List<Place>

    /** Lấy chi tiết các điểm theo id (màn Lộ trình dùng). */
    suspend fun getPlacesByIds(ids: List<String>): List<Place>

    /** Thành phố, điểm đến nổi bật cho Trang chủ. */
    suspend fun getFeaturedDestinations(): List<Place>

    /**
     * Quán ăn, quán nước quanh [center], gần nhất trước (category = quan_an / quan_nuoc, xem [FoodPlaces]).
     * Không có điểm đánh giá: dữ liệu OpenStreetMap không cho biết quán nào ngon.
     */
    suspend fun getFoodPlaces(center: GeoPoint): List<Place> = emptyList()
}

private fun res(name: String) = "android.resource://com.travelgo.app/drawable/$name"
private fun String.removeAccents(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
/** Dữ liệu giả để dựng giao diện khi chưa nối API. Ảnh lấy từ bản thiết kế. */
class FakePlaceRepository : PlaceRepository {

    private val samples = listOf(
        Place("p1", "Cầu Rồng", "thanh_pho", "Hải Châu, Đà Nẵng", 16.0611, 108.2273, 4.7, 30, res("img_cau_rong"), "Biểu tượng thành phố"),
        Place("p2", "Bãi biển Mỹ Khê", "bien", "Sơn Trà, Đà Nẵng", 16.0600, 108.2470, 4.8, 60, res("img_my_khe"), "Bãi biển"),
        Place("p3", "Ngũ Hành Sơn", "nui", "Ngũ Hành Sơn, Đà Nẵng", 15.9998, 108.2631, 4.6, 90, res("img_ngu_hanh_son"), "Danh thắng"),
        Place("p4", "Chùa Linh Ứng", "van_hoa", "Sơn Trà, Đà Nẵng", 16.1078, 108.2778, 4.8, 60, res("img_linh_ung"), "Văn hóa · Tâm linh"),
        Place("p5", "Bán đảo Sơn Trà", "thien_nhien", "Sơn Trà, Đà Nẵng", 16.1186, 108.2560, 4.6, 90, res("img_son_tra"), "Thiên nhiên"),
        Place("p6", "Bà Nà Hills", "nui", "Hòa Vang, Đà Nẵng", 15.9977, 107.9877, 4.7, 240, res("img_ba_na"), "Khu du lịch"),
        Place("p7", "Phố cổ Hội An", "van_hoa", "Quảng Nam", 15.8801, 108.3380, 4.9, 180, res("img_hoi_an"), "Di sản văn hóa"),
    )

    private val featured = listOf(
        Place("c1", "Đà Nẵng", "thanh_pho", "Miền Trung", 16.0544, 108.2022, 4.8, 0, res("img_da_nang")),
        Place("c2", "Nha Trang", "bien", "Khánh Hòa", 12.2388, 109.1967, 4.7, 0, res("img_nha_trang")),
        Place("c3", "Hội An", "van_hoa", "Quảng Nam", 15.8801, 108.3380, 4.9, 0, res("img_hoi_an")),
    )

    override suspend fun getNearbyPlaces(center: GeoPoint, radiusKm: Int): List<Place> = samples

    override suspend fun searchPlaces(query: String): List<Place> {
        if (query.isBlank()) return samples
        val q = query.removeAccents()
        return samples.filter {
            it.name.removeAccents().contains(q, ignoreCase = true) ||
                    it.address.removeAccents().contains(q, ignoreCase = true)
        }
    }
    override suspend fun getPlacesByIds(ids: List<String>): List<Place> =
        ids.mapNotNull { id -> (samples + foodSamples).firstOrNull { it.id == id } }

    override suspend fun getFeaturedDestinations(): List<Place> = featured

    /** Quán mẫu (tên giả) để dựng giao diện và kiểm thử, không phải quán thật. */
    private val foodSamples = listOf(
        Place("f1", "Quán phở mẫu", FoodPlaces.QUAN_AN, "Hải Châu, Đà Nẵng", 16.0610, 108.2200, 0.0, 45, tagline = "Quán ăn · Phở"),
        Place("f2", "Quán cà phê mẫu", FoodPlaces.QUAN_NUOC, "Hải Châu, Đà Nẵng", 16.0560, 108.2050, 0.0, 30, tagline = "Quán cà phê"),
        Place("f3", "Quán hải sản mẫu", FoodPlaces.QUAN_AN, "Sơn Trà, Đà Nẵng", 16.0700, 108.2450, 0.0, 45, tagline = "Quán ăn · Hải sản"),
    )

    override suspend fun getFoodPlaces(center: GeoPoint): List<Place> =
        foodSamples.sortedBy { center.distanceKmTo(it.location) }
}
