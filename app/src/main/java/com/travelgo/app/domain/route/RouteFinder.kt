package com.travelgo.app.domain.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import kotlin.math.roundToInt

private const val ROAD_FACTOR = 1.3      // ước lượng đường bộ từ đường chim bay
private const val AVG_SPEED_KMH = 28.0   // tốc độ trung bình nội thành
private const val MAX_PLACES = 5         // 5! = 120 thứ tự, vét cạn vẫn tức thì
private const val SCORE_WEIGHT_KM = 3.0  // 1 điểm "nổi bật" đáng giá bao nhiêu km (chỉnh được)

/**
 * Task 23: ma trận khoảng cách ước lượng (km) giữa mọi cặp điểm: đường chim bay × 1,3.
 * Chỉ số 0 là vị trí xuất phát, chỉ số i + 1 là điểm thứ i.
 */
fun distanceMatrix(points: List<GeoPoint>): Array<DoubleArray> =
    Array(points.size) { i ->
        DoubleArray(points.size) { j -> points[i].distanceKmTo(points[j]) * ROAD_FACTOR }
    }

/**
 * Bảng quãng đường (km) và thời gian (phút) giữa mọi cặp điểm.
 * [ids][0] là [START] (vị trí xuất phát), các phần tử sau là id địa điểm.
 * [byRoad] = true khi số liệu đo theo đường bộ thật (OSRM), false khi chỉ là ước lượng.
 */
class TravelTable(
    val ids: List<String>,
    val km: Array<DoubleArray>,
    val minutes: Array<DoubleArray>,
    val byRoad: Boolean,
) {
    private val index = ids.withIndex().associate { (i, id) -> id to i }

    fun indexOf(id: String): Int = index[id] ?: -1

    /** Bảng có đủ mọi điểm trong [places] không. */
    fun covers(places: List<Place>): Boolean = places.all { it.id in index }

    companion object {
        const val START = "#start"
    }
}

/** Bảng ước lượng: km = đường chim bay × 1,3, phút = km chia tốc độ trung bình 28 km/h. */
fun estimateTable(start: GeoPoint, places: List<Place>): TravelTable {
    val km = distanceMatrix(listOf(start) + places.map { it.location })
    val minutes = Array(km.size) { i -> DoubleArray(km.size) { j -> km[i][j] / AVG_SPEED_KMH * 60 } }
    return TravelTable(listOf(TravelTable.START) + places.map { it.id }, km, minutes, byRoad = false)
}

/** Mọi hoán vị của [items]. Với tối đa 5 phần tử là 120 trường hợp. */
private fun <T> permutations(items: List<T>): List<List<T>> {
    if (items.size <= 1) return listOf(items)
    return items.flatMap { x -> permutations(items - x).map { listOf(x) + it } }
}

/**
 * Hàm tìm lộ trình tối ưu. Người 2 gọi từ nút "Lên lộ trình thông minh".
 * Duyệt mọi thứ tự đi, chọn thứ tự có chi phí thấp nhất theo [mode].
 *
 * @param table bảng km, phút theo đường bộ (OSRM); không có hoặc thiếu điểm thì dùng bảng ước lượng.
 */
fun findBestRoute(start: GeoPoint, places: List<Place>, mode: RouteMode, table: TravelTable? = null): RouteResult {
    require(places.size <= MAX_PLACES) { "Tối đa $MAX_PLACES điểm" }
    val t = table?.takeIf { it.covers(places) } ?: estimateTable(start, places)
    if (places.isEmpty()) return evaluateRoute(start, places, mode, t)

    val idx = places.map { t.indexOf(it.id) }
    val m = t.km

    // order: danh sách chỉ số trong places
    fun pathKm(order: List<Int>): Double {
        var km = 0.0
        var prev = 0
        for (i in order) { km += m[prev][idx[i]]; prev = idx[i] }
        return km
    }

    fun cost(order: List<Int>): Double = when (mode) {
        RouteMode.SHORTEST -> pathKm(order)
        // Tính cả chặng quay về điểm xuất phát
        RouteMode.ROUND_TRIP -> pathKm(order) + m[idx[order.last()]][0]
        // Điểm có độ nổi bật cao đi càng sớm càng được thưởng (trừ vào chi phí)
        RouteMode.BEST_RATED -> pathKm(order) - SCORE_WEIGHT_KM *
                order.withIndex().sumOf { (pos, i) ->
                    places[i].score * (order.size - pos)
                }
    }

    val bestOrder = permutations(places.indices.toList()).minByOrNull { cost(it) }!!
    return evaluateRoute(start, bestOrder.map { places[it] }, mode, t)
}

/**
 * Task 26: tính km, phút cho một thứ tự CHO TRƯỚC (không tối ưu lại).
 * Task 30 (đổi thứ tự bằng nút mũi tên) gọi thẳng hàm này.
 */
fun evaluateRoute(start: GeoPoint, ordered: List<Place>, mode: RouteMode, table: TravelTable? = null): RouteResult {
    val t = table?.takeIf { it.covers(ordered) } ?: estimateTable(start, ordered)
    val legKm = mutableListOf<Double>()
    val legMin = mutableListOf<Double>()
    var prev = 0
    for (p in ordered) {
        val i = t.indexOf(p.id)
        legKm += t.km[prev][i]
        legMin += t.minutes[prev][i]
        prev = i
    }
    val roundTrip = mode == RouteMode.ROUND_TRIP && ordered.isNotEmpty()
    val backKm = if (roundTrip) t.km[prev][0] else 0.0
    val backMin = if (roundTrip) t.minutes[prev][0] else 0.0
    return RouteResult(
        order = ordered,
        legKm = legKm,
        totalKm = legKm.sum() + backKm,
        totalMinutes = (legMin.sum() + backMin).roundToInt(),
        returnKm = backKm,
        legMinutes = legMin.map { it.roundToInt() },
        returnMinutes = backMin.roundToInt(),
        byRoad = t.byRoad,
    )
}

/** Phút đi hết một chặng [km] theo tốc độ ước lượng (dùng khi không có số liệu đường bộ). */
fun legMinutes(km: Double): Int = (km / AVG_SPEED_KMH * 60).roundToInt()

/**
 * Hiển thị thời lượng: 0 -> "<1 phút", 45 -> "45 phút", 452 -> "7 giờ 32 phút", 120 -> "2 giờ".
 * Chặng rất ngắn trước đây hiện "0 phút", trông như lỗi.
 */
fun formatDuration(minutes: Int): String = when {
    minutes < 1 -> "<1 phút"
    minutes < 60 -> "$minutes phút"
    minutes % 60 == 0 -> "${minutes / 60} giờ"
    else -> "${minutes / 60} giờ ${minutes % 60} phút"
}