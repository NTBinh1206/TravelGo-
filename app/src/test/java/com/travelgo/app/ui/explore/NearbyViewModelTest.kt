package com.travelgo.app.ui.explore

import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.ui.common.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Màn Gần bạn (mục 21): chọn sẵn 5 điểm gần nhất, chọn 2 đến 5 điểm gửi sang Lộ trình. 7 điểm mẫu ở Đà Nẵng, Hội An. */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModelTest {

    private val daNang = GeoPoint(16.0544, 108.2022)
    private val hoiAn = GeoPoint(15.8801, 108.3380)
    private val location = InMemoryLocationProvider().apply { updateLocation(daNang, "Đà Nẵng") }
    private val created = mutableListOf<NearbyViewModel>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun newViewModel() = NearbyViewModel(FakePlaceRepository(), location).also { created += it }

    /** Id các điểm mẫu, gần [from] trước. */
    private suspend fun nearestIds(from: GeoPoint) =
        FakePlaceRepository().getNearbyPlaces(from).sortedBy { from.distanceKmTo(it.location) }.map { it.id }

    @Test fun opens_withFiveNearestPreselected_inDistanceOrder() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        val expected = nearestIds(daNang)
        assertEquals(expected, state.places.dataOrNull!!.map { it.id })
        assertEquals(expected.take(MAX_STOPS), state.selectedInOrder)
        assertTrue(state.canRoute)
    }

    @Test fun cannotSelectMoreThanFive_andDeselectWorks() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val ids = nearestIds(daNang)

        assertFalse(vm.toggle(ids[5]))                       // đã đủ 5 điểm
        assertEquals(MAX_STOPS, vm.uiState.value.selectedIds.size)

        assertTrue(vm.toggle(ids[0]))                        // bỏ điểm gần nhất
        assertTrue(vm.toggle(ids[5]))                        // giờ chọn thêm được
        assertEquals(ids.subList(1, 6), vm.uiState.value.selectedInOrder)
    }

    @Test fun needAtLeastTwoStops_toRoute() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val ids = nearestIds(daNang)

        ids.take(3).forEach { vm.toggle(it) }                // còn 2 điểm
        assertTrue(vm.uiState.value.canRoute)
        vm.toggle(ids[3])                                    // còn 1 điểm
        assertFalse(vm.uiState.value.canRoute)
        assertEquals(listOf(ids[4]), vm.uiState.value.selectedInOrder)
    }

    @Test fun movingFar_resetsSelection_toNearestOfNewPlace() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.toggle(nearestIds(daNang)[0])

        location.updateLocation(hoiAn, "Hội An")
        advanceUntilIdle()

        assertEquals(nearestIds(hoiAn).take(MAX_STOPS), vm.uiState.value.selectedInOrder)
    }
}
