package com.travelgo.app.domain.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import kotlin.math.roundToInt

private const val ROAD_FACTOR = 1.3      // ước lượng đường bộ từ đường chim bay
private const val AVG_SPEED_KMH = 28.0   // tốc độ trung bình nội thành
private const val MAX_PLACES = 5         // 5! = 120 thứ tự, vét cạn vẫn tức thì
private const val RATING_WEIGHT_KM = 3.0 // 1 điểm rating "đáng giá" bao nhiêu km (chỉnh được)

/**
 * Task 23: ma trận khoảng cách (km đường bộ ước lượng) giữa mọi cặp điểm.
 * Chỉ số 0 là vị trí xuất phát, chỉ số i + 1 là places[i].
 */
fun distanceMatrix(points: List<GeoPoint>): Array<DoubleArray> =
    Array(points.size) { i ->
        DoubleArray(points.size) { j -> points[i].distanceKmTo(points[j]) * ROAD_FACTOR }
    }

/** Mọi hoán vị của [items]. Với tối đa 5 phần tử là 120 trường hợp. */
private fun <T> permutations(items: List<T>): List<List<T>> {
    if (items.size <= 1) return listOf(items)
    return items.flatMap { x -> permutations(items - x).map { listOf(x) + it } }
}

/**
 * Hàm tìm lộ trình tối ưu. Người 2 gọi từ nút "Lên lộ trình thông minh".
 * Duyệt mọi thứ tự đi, chọn thứ tự có chi phí thấp nhất theo [mode].
 */
fun findBestRoute(start: GeoPoint, places: List<Place>, mode: RouteMode): RouteResult {
    require(places.size <= MAX_PLACES) { "Tối đa $MAX_PLACES điểm" }
    if (places.isEmpty()) return evaluateRoute(start, places, mode)

    val m = distanceMatrix(listOf(start) + places.map { it.location })

    // order: danh sách chỉ số trong places
    fun pathKm(order: List<Int>): Double {
        var km = 0.0
        var prev = 0
        for (i in order) { km += m[prev][i + 1]; prev = i + 1 }
        return km
    }

    fun cost(order: List<Int>): Double = when (mode) {
        RouteMode.SHORTEST -> pathKm(order)
        // Tính cả chặng quay về điểm xuất phát
        RouteMode.ROUND_TRIP -> pathKm(order) + m[order.last() + 1][0]
        // Điểm rating cao đi càng sớm càng được thưởng (trừ vào chi phí)
        RouteMode.BEST_RATED -> pathKm(order) - RATING_WEIGHT_KM *
                order.withIndex().sumOf { (pos, i) ->
                    places[i].rating.toDouble() * (order.size - pos)
                }
    }

    val bestOrder = permutations(places.indices.toList()).minByOrNull { cost(it) }!!
    return evaluateRoute(start, bestOrder.map { places[it] }, mode)
}

/**
 * Task 26: tính km, phút cho một thứ tự CHO TRƯỚC (không tối ưu lại).
 * Task 30 (kéo thả đổi thứ tự) gọi thẳng hàm này.
 */
fun evaluateRoute(start: GeoPoint, ordered: List<Place>, mode: RouteMode): RouteResult {
    val legs = mutableListOf<Double>()
    var prev = start
    for (p in ordered) {
        legs += prev.distanceKmTo(p.location) * ROAD_FACTOR
        prev = p.location
    }
    var total = legs.sum()
    if (mode == RouteMode.ROUND_TRIP && ordered.isNotEmpty()) {
        total += prev.distanceKmTo(start) * ROAD_FACTOR
    }
    return RouteResult(
        order = ordered,
        legKm = legs,
        totalKm = total,
        totalMinutes = (total / AVG_SPEED_KMH * 60).roundToInt(),
    )
}

/** Phút đi hết một chặng [km]. */
fun legMinutes(km: Double): Int = (km / AVG_SPEED_KMH * 60).roundToInt()