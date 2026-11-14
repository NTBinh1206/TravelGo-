package com.travelgo.app.domain.route

import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFinderTest {

    private fun p(id: String, lat: Double, lng: Double, score: Double = 4.5) =
        Place(id, id, "thanh_pho", "addr", lat, lng, score, 30, "")

    private val start = GeoPoint(16.0, 108.0)

    @Test fun roadTable_changesOrder_andGivesRoadMinutes() {
        // Theo đường chim bay a gần hơn b, nhưng đường bộ tới a phải vòng xa (ví dụ qua cầu).
        val a = p("a", 16.0, 108.1)
        val b = p("b", 16.0, 108.2)
        val ids = listOf(TravelTable.START, "a", "b")
        val km = arrayOf(
            doubleArrayOf(0.0, 30.0, 14.0),
            doubleArrayOf(30.0, 0.0, 12.0),
            doubleArrayOf(14.0, 12.0, 0.0),
        )
        val minutes = Array(3) { i -> DoubleArray(3) { j -> km[i][j] * 2 } }
        val road = TravelTable(ids, km, minutes, byRoad = true)

        val r = findBestRoute(start, listOf(a, b), RouteMode.SHORTEST, road)

        assertEquals(listOf("b", "a"), r.order.map { it.id })
        assertEquals(26.0, r.totalKm, 1e-9)
        assertEquals(listOf(28, 24), r.legMinutes)
        assertEquals(52, r.totalMinutes)
        assertTrue(r.byRoad)
        // Không có bảng đường bộ thì đi a trước như cũ, và đánh dấu là ước lượng.
        val estimate = findBestRoute(start, listOf(a, b), RouteMode.SHORTEST)
        assertEquals(listOf("a", "b"), estimate.order.map { it.id })
        assertTrue(!estimate.byRoad)
    }

    @Test fun roadTable_missingAPlace_fallsBackToEstimate() {
        val a = p("a", 16.0, 108.1)
        val road = TravelTable(listOf(TravelTable.START), arrayOf(doubleArrayOf(0.0)), arrayOf(doubleArrayOf(0.0)), true)
        val r = evaluateRoute(start, listOf(a), RouteMode.SHORTEST, road)
        assertTrue(!r.byRoad)
        assertTrue(r.totalKm > 0)
    }

    @Test fun noPlaces_returnsEmptyResult() {
        val r = findBestRoute(start, emptyList(), RouteMode.SHORTEST)
        assertTrue(r.order.isEmpty())
        assertEquals(0.0, r.totalKm, 1e-9)
        assertEquals(0, r.totalMinutes)
    }

    @Test fun shortest_onALine_visitsInLineOrder() {
        val a = p("a", 16.0, 108.1)
        val b = p("b", 16.0, 108.2)
        val c = p("c", 16.0, 108.3)
        val r = findBestRoute(start, listOf(c, a, b), RouteMode.SHORTEST)
        assertEquals(listOf("a", "b", "c"), r.order.map { it.id })
    }

    @Test fun legKm_matchesPlaceCount_andSumsToTotal() {
        val places = listOf(p("a", 16.0, 108.1), p("b", 16.0, 108.2))
        val r = findBestRoute(start, places, RouteMode.SHORTEST)
        assertEquals(2, r.legKm.size)
        assertEquals(r.legKm.sum(), r.totalKm, 1e-9)
    }

    @Test fun roundTrip_isLongerThanShortest() {
        val places = listOf(p("a", 16.0, 108.1), p("b", 16.0, 108.2))
        val open = findBestRoute(start, places, RouteMode.SHORTEST)
        val round = findBestRoute(start, places, RouteMode.ROUND_TRIP)
        assertTrue(round.totalKm > open.totalKm)
    }

    @Test fun bestRated_putsHighRatingFirst_whereShortestDoesNot() {
        val low = p("low", 16.0, 108.10, score = 3.0)
        val high = p("high", 16.0, 108.11, score = 5.0)
        val shortest = findBestRoute(start, listOf(low, high), RouteMode.SHORTEST)
        val rated = findBestRoute(start, listOf(low, high), RouteMode.BEST_RATED)
        assertEquals("low", shortest.order.first().id)
        assertEquals("high", rated.order.first().id)
    }

    @Test fun evaluateRoute_keepsGivenOrder() {
        val a = p("a", 16.0, 108.1)
        val b = p("b", 16.0, 108.2)
        val r = evaluateRoute(start, listOf(b, a), RouteMode.SHORTEST)
        assertEquals(listOf("b", "a"), r.order.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun moreThanFivePlaces_throws() {
        val six = (1..6).map { p("p$it", 16.0, 108.0 + it * 0.01) }
        findBestRoute(start, six, RouteMode.SHORTEST)
    }
}