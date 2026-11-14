package com.travelgo.app.data.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.domain.route.estimateTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Đọc câu trả lời của máy chủ OSRM (mẫu theo đúng định dạng tài liệu OSRM). */
class OsrmTest {

    private val start = GeoPoint(10.7769, 106.7009)
    private val a = Place("a", "A", "van_hoa", "", 10.7798, 106.6990, 4.0, 60, "")
    private val b = Place("b", "B", "van_hoa", "", 10.7720, 106.6983, 4.0, 60, "")

    @Test fun coordinates_areLngFirst() {
        assertEquals("106.700900,10.776900;106.699000,10.779800", Osrm.coordinates(listOf(start, a.location)))
    }

    @Test fun parseTable_convertsMetersAndSeconds_andFillsGapsFromEstimate() {
        val estimate = estimateTable(start, listOf(a, b))
        val json = """
            {"code":"Ok",
             "distances":[[0,1250.5,900],[1300,0,null],[950,1400,0]],
             "durations":[[0,180,120],[190,0,null],[130,200,0]]}
        """.trimIndent()

        val t = Osrm.parseTable(json, estimate)!!

        assertTrue(t.byRoad)
        assertEquals(1.2505, t.km[0][1], 1e-9)
        assertEquals(3.0, t.minutes[0][1], 1e-9)
        assertEquals(estimate.km[1][2], t.km[1][2], 1e-9)       // ô null: lấy số ước lượng
        assertEquals(estimate.minutes[1][2], t.minutes[1][2], 1e-9)
        assertEquals(listOf("#start", "a", "b"), t.ids)
    }

    @Test fun parseTable_rejectsErrorsAndWrongSize() {
        val estimate = estimateTable(start, listOf(a, b))
        assertNull(Osrm.parseTable("""{"code":"NoRoute","message":"x"}""", estimate))
        assertNull(Osrm.parseTable("""{"code":"Ok","distances":[[0]],"durations":[[0]]}""", estimate))
    }

    @Test fun parsePath_readsGeoJsonLine() {
        val json = """
            {"code":"Ok","routes":[{"distance":1500.2,"duration":240,
              "geometry":{"type":"LineString","coordinates":[[106.7009,10.7769],[106.6995,10.7781],[106.6990,10.7798]]}}]}
        """.trimIndent()
        val path = Osrm.parsePath(json)!!
        assertEquals(3, path.size)
        assertEquals(10.7769, path.first().lat, 1e-9)
        assertEquals(106.6990, path.last().lng, 1e-9)
        assertNull(Osrm.parsePath("""{"code":"InvalidQuery"}"""))
    }
}
