package com.travelgo.app.domain.route

import com.travelgo.app.data.place.Place

/** QUY ƯỚC CHUNG: 3 kiểu lộ trình. */
enum class RouteMode(val label: String) {
    SHORTEST("Ngắn nhất"),
    BEST_RATED("Ưu tiên điểm hay"),
    ROUND_TRIP("Khứ hồi"),
}

/** QUY ƯỚC CHUNG: kết quả tính lộ trình. */
data class RouteResult(
    val order: List<Place>,   // thứ tự đi đã sắp xếp
    val legKm: List<Double>,  // km từng chặng, chặng đầu tính từ vị trí người dùng
    val totalKm: Double,
    val totalMinutes: Int,    // thời gian di chuyển
)
