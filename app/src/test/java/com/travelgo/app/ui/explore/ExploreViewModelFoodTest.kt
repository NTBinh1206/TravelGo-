package com.travelgo.app.ui.explore

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.FoodPlaces
import com.travelgo.app.ui.common.dataOrNull
import kotlinx.coroutines.Dispatchers
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

    @After fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = ExploreViewModel(
        placeRepository = FakePlaceRepository(),
        location = InMemoryLocationProvider().apply { updateLocation(me, "Đà Nẵng") },
    )

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
