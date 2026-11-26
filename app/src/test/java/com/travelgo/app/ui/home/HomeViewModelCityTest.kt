package com.travelgo.app.ui.home

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.InMemoryClosedReports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Mục 18: bấm thẻ "Điểm đến nổi bật" thì đổi vị trí sang thành phố đó. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCityTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun tapCity_movesThere_onlyOnce() = runTest {
        val location = InMemoryLocationProvider().apply { updateLocation(GeoPoint(16.0544, 108.2022), "Đà Nẵng") }
        val repo = FakePlaceRepository()
        val vm = HomeViewModel(repo, location, InMemoryClosedReports())
        val hoiAn = repo.getFeaturedDestinations().first { it.name == "Hội An" }

        assertTrue(vm.goToCity(hoiAn))
        assertEquals("Hội An", location.placeName.value)
        assertEquals(hoiAn.location, location.currentLocation.value)
        assertFalse(vm.goToCity(hoiAn))                      // đang ở Hội An rồi: không đổi gì
    }
}
