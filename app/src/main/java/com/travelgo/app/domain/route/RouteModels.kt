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
    val returnKm: Double = 0.0, // chặng quay về điểm xuất phát (chỉ khi Khứ hồi)
    val legMinutes: List<Int> = emptyList(), // phút từng chặng, cùng thứ tự với legKm
    val returnMinutes: Int = 0,
    val byRoad: Boolean = false, // true: km, phút đo theo đường bộ (OSRM); false: ước lượng từ đường chim bay
) {
    /** Tổng thời gian tham quan ước lượng ở các điểm dừng. */
    val stayMinutes: Int get() = order.sumOf { it.stayMinutes }

    /** Cả chuyến = di chuyển + tham quan. */
    val tripMinutes: Int get() = totalMinutes + stayMinutes
}
