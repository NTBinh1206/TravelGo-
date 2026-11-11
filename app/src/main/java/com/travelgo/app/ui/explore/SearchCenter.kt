package com.travelgo.app.ui.explore


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.travelgo.app.data.model.GeoPoint
/**
 * Điểm mốc để tải gợi ý: chỉ đổi khi người dùng di chuyển xa hơn [thresholdKm]
 * so với lần tải trước. Dùng làm "chìa khóa" cho produceState.
 */
@Composable
fun rememberSearchCenter(current: GeoPoint, thresholdKm: Double = 2.0): GeoPoint {
    var anchor by remember { mutableStateOf(current) }
    LaunchedEffect(current) {
        if (current.distanceKmTo(anchor) > thresholdKm) anchor = current
    }
    return anchor
}