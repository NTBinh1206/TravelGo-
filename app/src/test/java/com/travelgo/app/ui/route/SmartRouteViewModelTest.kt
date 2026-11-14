package com.travelgo.app.ui.route

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.route.NoRoadRouter
import com.travelgo.app.data.route.RoadRouter
import com.travelgo.app.domain.route.TravelTable
import com.travelgo.app.domain.route.estimateTable
import com.travelgo.app.domain.route.RouteMode
import com.travelgo.app.domain.route.findBestRoute
import com.travelgo.app.ui.common.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Kiểm thử ViewModel màn Lộ trình bằng dữ liệu mẫu (FakePlaceRepository, 7 điểm ở Đà Nẵng). */
@OptIn(ExperimentalCoroutinesApi::class)
class SmartRouteViewModelTest {

    private val start = GeoPoint(16.0544, 108.2022)
    private val ids = listOf("p1", "p2", "p4", "p5")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(router: RoadRouter = NoRoadRouter): SmartRouteViewModel {
        val location = InMemoryLocationProvider().apply { updateLocation(start, "Đà Nẵng") }
        return SmartRouteViewModel(placeIds = ids, placeRepository = FakePlaceRepository(), location = location, router = router)
    }

    /** Máy chủ chỉ đường giả: đường bộ dài gấp đôi số ước lượng, đường vẽ là nối thẳng các điểm. */
    private object FakeRouter : RoadRouter {
        override suspend fun table(start: GeoPoint, places: List<Place>): TravelTable {
            val e = estimateTable(start, places)
            return TravelTable(e.ids, Array(e.km.size) { i -> DoubleArray(e.km.size) { j -> e.km[i][j] * 2 } }, e.minutes, byRoad = true)
        }

        override suspend fun path(points: List<GeoPoint>): List<GeoPoint> = points
    }

    @Test fun withoutRoadServer_showsEstimate() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(DistanceSource.ESTIMATE, state.distance)
        assertFalse(state.route.dataOrNull!!.byRoad)
        assertNull(state.roadPath)
    }

    @Test fun withRoadServer_usesRoadDistances_andDrawsRoadPath() = runTest {
        val estimateKm = run {
            val vm = newViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            vm.uiState.value.route.dataOrNull!!.totalKm
        }
        val vm = newViewModel(FakeRouter)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        val route = state.route.dataOrNull!!
        assertEquals(DistanceSource.ROAD, state.distance)
        assertTrue(route.byRoad)
        assertEquals(estimateKm * 2, route.totalKm, 1e-6)
        assertNotNull(state.roadPath)
        assertEquals(route.order.size + 1, state.roadPath!!.size) // vị trí của bạn + các điểm
    }

    @Test fun loads_andUsesOptimalOrder() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val route = vm.uiState.value.route.dataOrNull!!
        val places = runBlocking { FakePlaceRepository().getPlacesByIds(ids) }
        assertEquals(findBestRoute(start, places, RouteMode.SHORTEST).order.map { it.id }, route.order.map { it.id })
        assertFalse(vm.uiState.value.customOrder)
    }

    @Test fun moveDown_swapsTwoStops_andMarksCustomOrder() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val before = vm.uiState.value.route.dataOrNull!!.order.map { it.id }

        vm.moveDown(0)

        val after = vm.uiState.value.route.dataOrNull!!.order.map { it.id }
        assertEquals(listOf(before[1], before[0]) + before.drop(2), after)
        assertTrue(vm.uiState.value.customOrder)
    }

    @Test fun moveUp_onFirstStop_doesNothing() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val before = vm.uiState.value.route.dataOrNull!!.order.map { it.id }

        vm.moveUp(0)

        assertEquals(before, vm.uiState.value.route.dataOrNull!!.order.map { it.id })
        assertFalse(vm.uiState.value.customOrder)
    }

    @Test fun changingMode_dropsCustomOrder_andAddsReturnLegForRoundTrip() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.moveDown(0)

        vm.selectMode(RouteMode.ROUND_TRIP)

        val state = vm.uiState.value
        val route = state.route.dataOrNull!!
        assertFalse(state.customOrder)
        assertEquals(RouteMode.ROUND_TRIP, state.mode)
        assertTrue(route.returnKm > 0.0)
        assertEquals(route.legKm.sum() + route.returnKm, route.totalKm, 1e-9)
        assertTrue(route.tripMinutes > route.totalMinutes) // có cộng thời gian tham quan
    }
}
