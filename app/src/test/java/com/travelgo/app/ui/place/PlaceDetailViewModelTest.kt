package com.travelgo.app.ui.place

import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FakePlaceRepository
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.data.place.WikiInfo
import com.travelgo.app.ui.common.LoadState
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Màn Chi tiết địa điểm: tìm điểm theo id, khoảng cách, giới thiệu Wikipedia. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceDetailViewModelTest {

    private val daNang = GeoPoint(16.0544, 108.2022)
    private val location = InMemoryLocationProvider().apply { updateLocation(daNang, "Đà Nẵng") }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    /** Kho mẫu có thêm giới thiệu Wikipedia; [wikiError] = true thì giả lập mất mạng. */
    private class WikiRepo(private val wiki: WikiInfo?, private val wikiError: Boolean = false) :
        PlaceRepository by FakePlaceRepository() {
        override suspend fun getWikiInfo(placeId: String): WikiInfo? =
            if (wikiError) error("mất mạng") else wiki
    }

    @Test fun showsPlace_distance_andWikipediaText() = runTest {
        val vm = PlaceDetailViewModel("p1", WikiRepo(WikiInfo("https://img/cau-rong.jpg", "Cầu Rồng bắc qua sông Hàn.")), location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val ui = vm.uiState.value
        val place = (ui.place as LoadState.Ready).data
        assertEquals("Cầu Rồng", place.name)
        assertEquals(daNang.distanceKmTo(place.location), ui.distanceKm!!, 1e-9)
        assertEquals("Cầu Rồng bắc qua sông Hàn.", ui.description)
        assertFalse(ui.loadingDescription)
    }

    @Test fun wikipediaFails_stillShowsPlace_withoutDescription() = runTest {
        val vm = PlaceDetailViewModel("p2", WikiRepo(wiki = null, wikiError = true), location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.place is LoadState.Ready)
        assertNull(vm.uiState.value.description)
        assertFalse(vm.uiState.value.loadingDescription)
    }

    @Test fun placeWithoutPhoto_usesWikipediaPhoto() = runTest {
        // f1 là quán mẫu, không có ảnh.
        val vm = PlaceDetailViewModel("f1", WikiRepo(WikiInfo("https://img/wiki.jpg", "")), location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val place = (vm.uiState.value.place as LoadState.Ready).data
        assertEquals("https://img/wiki.jpg", place.imageUrl)
        assertNull(vm.uiState.value.description)            // đoạn giới thiệu rỗng thì coi như không có
    }

    @Test fun unknownId_isError() = runTest {
        val vm = PlaceDetailViewModel("khong-co", WikiRepo(null), location)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(LoadState.Error, vm.uiState.value.place)
    }
}
