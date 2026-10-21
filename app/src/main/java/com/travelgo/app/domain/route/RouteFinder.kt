package com.travelgo.app.domain.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import kotlin.math.roundToInt

private const val ROAD_FACTOR = 1.3      // ước lượng đường bộ từ đường chim bay
private const val AVG_SPEED_KMH = 28.0   // tốc độ trung bình nội thành

/**
 * QUY ƯỚC CHUNG: hàm tìm lộ trình tối ưu. Người 3 viết phần bên trong.
 * Người 2 gọi hàm này từ nút "Lên lộ trình thông minh".
 *
 * TODO(Người 3):
 *  - ma trận khoảng cách giữa mọi cặp điểm
 *  - duyệt mọi thứ tự đi (tối đa 5! = 120) và chọn chi phí thấp nhất theo [mode]
 *
 * Bản tạm hiện tại: giữ nguyên thứ tự đầu vào, chỉ tính km và phút từng chặng
 * để màn hình có số liệu hiển thị.
 */
fun findBestRoute(start: GeoPoint, places: List<Place>, mode: RouteMode): RouteResult {
    val legs = mutableListOf<Double>()
    var prev = start
    for (p in places) {
        legs += prev.distanceKmTo(p.location) * ROAD_FACTOR
        prev = p.location
    }
    var total = legs.sum()
    if (mode == RouteMode.ROUND_TRIP && places.isNotEmpty()) {
        total += prev.distanceKmTo(start) * ROAD_FACTOR
    }
    return RouteResult(
        order = places,
        legKm = legs,
        totalKm = total,
        totalMinutes = (total / AVG_SPEED_KMH * 60).roundToInt(),
    )
}

/** Phút đi hết một chặng [km]. */
fun legMinutes(km: Double): Int = (km / AVG_SPEED_KMH * 60).roundToInt()
