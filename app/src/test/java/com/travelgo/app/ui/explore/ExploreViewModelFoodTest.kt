package com.travelgo.app.ui.explore

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.FoodPlaces
import com.travelgo.app.data.place.InMemoryClosedReports
import com.travelgo.app.ui.common.dataOrNull
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Chip "Ăn uống" ở Khám phá, dùng 3 quán mẫu của FakePlaceRepository. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelFoodTest {

    private val me = GeoPoint(16.0544, 108.2022)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    /** ViewModel của test trước vẫn đang nghe ExploreRequest nên phải huỷ, nếu không nó "giành" yêu cầu của test sau. */
    private val created = mutableListOf<ExploreViewModel>()

    @After fun tearDown() {
        created.forEach { it.viewModelScope.cancel() }
        ExploreRequest.category.value = null
        Dispatchers.resetMain()
    }

    private val reports = InMemoryClosedReports(myUid = "me")

    private fun newViewModel() = ExploreViewModel(
        placeRepository = FakePlaceRepository(),
        location = InMemoryLocationProvider().apply { updateLocation(me, "Đà Nẵng") },
        closedReports = reports,
    ).also { created += it }

    @Test fun foodChip_showsOnlyFoodPlaces_nearestFirst() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.selectCategory(FOOD_GROUP)
        advanceUntilIdle()

        val places = vm.uiState.value.places.dataOrNull!!
        assertEquals(3, places.size)
        assertTrue(places.all { FoodPlaces.isFood(it.category) })
        val distances = places.map { me.distanceKmTo(it.location) }
        assertEquals(distances.sorted(), distances)
    }

    @Test fun foodKind_filtersCafes() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.selectCategory(FOOD_GROUP)
        vm.selectFoodKind(FoodKind.QUAN_NUOC)
        advanceUntilIdle()

        val places = vm.uiState.value.places.dataOrNull!!
        assertEquals(listOf("f2"), places.map { it.id })
    }

    @Test fun reportedClosed_isHidden_rightAway() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.selectCategory(FOOD_GROUP)
        advanceUntilIdle()
        val f1 = vm.uiState.value.places.dataOrNull!!.first { it.id == "f1" }

        reports.reportClosed(f1)
        advanceUntilIdle()

        assertEquals(listOf("f2", "f3"), vm.uiState.value.places.dataOrNull!!.map { it.id }.sorted())
    }

    @Test fun requestFromHome_opensFoodChip() = runTest {
        ExploreRequest.open(FOOD_GROUP)
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isFood)
        assertEquals(null, ExploreRequest.category.value) // yêu cầu chỉ dùng một lần
    }

    @Test fun otherChips_neverShowFoodPlaces() = runTest {
        val vm = newViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.selectCategory(null)
        advanceUntilIdle()

        val places = vm.uiState.value.places.dataOrNull!!
        assertTrue(places.isNotEmpty())
        assertTrue(places.none { FoodPlaces.isFood(it.category) })
    }
}
