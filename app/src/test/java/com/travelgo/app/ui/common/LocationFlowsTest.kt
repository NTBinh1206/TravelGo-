package com.travelgo.app.ui.common

import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationFlowsTest {

    @Test fun anchored_ignoresSmallGpsMoves_butFollowsLongMoves() = runTest {
        val daNang = GeoPoint.DEFAULT
        val daNangNearby = GeoPoint(daNang.lat + 0.005, daNang.lng) // ~0,55 km
        val hoiAn = GeoPoint(15.8801, 108.3380)                    // ~24 km
        val hoiAnNearby = GeoPoint(15.8801 + 0.009, 108.3380)      // ~1 km

        val anchors = flowOf(null, daNang, daNangNearby, hoiAn, hoiAnNearby).anchored().toList()

        // null = chưa có vị trí -> dùng mặc định (Đà Nẵng); xê dịch dưới 2 km không phát lại.
        assertEquals(listOf(daNang, hoiAn), anchors)
    }
}
