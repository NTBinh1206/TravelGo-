package com.travelgo.app.ui.food

import com.travelgo.app.data.place.InMemoryClosedReports
import com.travelgo.app.data.place.NoteOutcome
import com.travelgo.app.data.place.OsmNoteSender
import com.travelgo.app.data.place.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** Báo quán đóng cửa: chỉ gửi ghi chú lên OpenStreetMap khi người dùng đồng ý và đủ người báo. */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodSheetViewModelTest {

    private val cafe = Place("n4516891589", "Gà Rán 28", "quan_an", "", 9.17713, 105.14927, 0.0, 0, "")

    /** Ghi lại các lần gửi thay vì gọi mạng. */
    private class FakeNotes(private val answer: Result<NoteOutcome> = Result.success(NoteOutcome.Created(1))) : OsmNoteSender {
        val sent = mutableListOf<Pair<String, Int>>()
        override suspend fun reportClosed(place: Place, reporters: Int): Result<NoteOutcome> {
            sent += place.id to reporters
            return answer
        }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun firstReport_doesNotSendNote_evenWhenUserAgrees() = runTest {
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(InMemoryClosedReports(), notes)
        vm.setShareToOsm(true)

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertTrue(notes.sent.isEmpty())
        assertTrue(vm.uiState.value.reported)
        assertTrue("mới có 1 người báo" in vm.uiState.value.message!!)
    }

    @Test fun secondReport_withConsent_sendsOneNote() = runTest {
        val reports = InMemoryClosedReports().apply { addReport(cafe.id, "someone-else") }
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(reports, notes)
        vm.setShareToOsm(true)

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertEquals(listOf(cafe.id to 2), notes.sent)
        assertTrue("Đã gửi ghi chú lên OpenStreetMap" in vm.uiState.value.message!!)
    }

    @Test fun withoutConsent_orNoteFailure_quanIsStillHidden() = runTest {
        val reports = InMemoryClosedReports().apply { addReport(cafe.id, "someone-else") }
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(reports, notes)

        vm.reportClosed(cafe)
        advanceUntilIdle()
        assertTrue(notes.sent.isEmpty())                       // không tick ô đồng ý thì không gửi
        assertTrue(cafe.id in reports.hiddenIds.value)

        val failing = FakeNotes(Result.failure(RuntimeException("HTTP 403")))
        val vm2 = FoodSheetViewModel(reports, failing)
        vm2.setShareToOsm(true)
        vm2.reportClosed(cafe)
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.reported)                 // báo quán vẫn thành công
        assertTrue("Chưa gửi được ghi chú" in vm2.uiState.value.message!!)
    }
}
