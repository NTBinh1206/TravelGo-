package com.travelgo.app.ui.food

import com.travelgo.app.data.location.GpsFix
import com.travelgo.app.data.location.InMemoryLocationProvider
import com.travelgo.app.data.model.GeoPoint
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

/** Báo quán đóng cửa: chỉ gửi ghi chú lên OpenStreetMap khi người dùng đồng ý và quán bị gắn nhãn. */
@OptIn(ExperimentalCoroutinesApi::class)
class FoodSheetViewModelTest {

    private val cafe = Place("n4516891589", "Gà Rán 28", "quan_an", "", 9.17713, 105.14927, 0.0, 0, "")

    /** GPS trả về [point]; mặc định đứng ngay tại quán. */
    private fun gps(point: GeoPoint? = cafe.location, mock: Boolean = false) =
        InMemoryLocationProvider().apply { fix = point?.let { GpsFix(it, mock) } }

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
        val vm = FoodSheetViewModel(InMemoryClosedReports(), notes, gps())
        vm.setShareToOsm(true)

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertTrue(notes.sent.isEmpty())
        assertTrue(vm.uiState.value.reported)
        assertTrue("ẩn khỏi danh sách của bạn" in vm.uiState.value.message!!)
        assertTrue("Chưa gửi ghi chú" in vm.uiState.value.message!!)
    }

    @Test fun secondRealReport_withConsent_sendsOneNote() = runTest {
        val reports = InMemoryClosedReports().apply { addReport(cafe.id, "someone-else") }
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(reports, notes, gps())
        vm.setShareToOsm(true)

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertEquals(listOf(cafe.id to 2), notes.sent)
        assertTrue("gắn nhãn" in vm.uiState.value.message!!)
        assertTrue("Đã gửi ghi chú lên OpenStreetMap" in vm.uiState.value.message!!)
        assertTrue(!vm.uiState.value.shareToOsm)             // lần báo sau phải tick lại
    }

    @Test fun guestReports_neverSendNote_norFlag() = runTest {
        // Hai tài khoản Khách báo: quán chỉ ẩn với người báo, không gắn nhãn, không có ghi chú OSM.
        val reports = InMemoryClosedReports(myUid = "khach-1", guest = true)
            .apply { addGuestReport(cafe.id, "khach-0") }
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(reports, notes, gps())
        vm.setShareToOsm(true)

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertTrue(notes.sent.isEmpty())
        assertTrue(cafe.id in reports.hiddenIds.value)
        assertTrue(cafe.id !in reports.flaggedIds.value)
        assertTrue("Chưa gửi ghi chú" in vm.uiState.value.message!!)
    }

    @Test fun withoutConsent_orNoteFailure_quanIsStillFlagged() = runTest {
        val reports = InMemoryClosedReports().apply { addReport(cafe.id, "someone-else") }
        val notes = FakeNotes()
        val vm = FoodSheetViewModel(reports, notes, gps())

        vm.reportClosed(cafe)
        advanceUntilIdle()
        assertTrue(notes.sent.isEmpty())                       // không tick ô đồng ý thì không gửi
        assertTrue(cafe.id in reports.flaggedIds.value)

        val failing = FakeNotes(Result.failure(RuntimeException("HTTP 403")))
        val vm2 = FoodSheetViewModel(reports, failing, gps())
        vm2.setShareToOsm(true)
        vm2.reportClosed(cafe)
        advanceUntilIdle()
        assertTrue(vm2.uiState.value.reported)                 // báo quán vẫn thành công
        assertTrue("Chưa gửi được ghi chú" in vm2.uiState.value.message!!)
    }

    @Test fun confirmOpen_marksReported_withOwnMessage() = runTest {
        val reports = InMemoryClosedReports(myUid = "an")
            .apply { addReport(cafe.id, "b"); addReport(cafe.id, "c") }
        val vm = FoodSheetViewModel(reports, FakeNotes(), gps())

        vm.confirmOpen(cafe)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.reported)
        assertEquals(FoodSheetViewModel.OPEN_THANKS, vm.uiState.value.message)
        assertTrue(cafe.id in reports.flaggedIds.value)        // 1 xác nhận < 2 người báo: nhãn còn
        reports.addOpenConfirm(cafe.id, "d")
        assertTrue(cafe.id !in reports.flaggedIds.value)       // đủ người xác nhận: nhãn được gỡ
    }

    @Test fun realAccount_farFromPlace_cannotReport() = runTest {
        val reports = InMemoryClosedReports()
        // Cách quán khoảng 1,1 km về phía bắc.
        val vm = FoodSheetViewModel(reports, FakeNotes(), gps(GeoPoint(cafe.lat + 0.01, cafe.lng)))

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertTrue(cafe.id !in reports.hiddenIds.value)       // không ghi gì
        assertTrue(!vm.uiState.value.reported)
        assertTrue("Cần đứng trong vòng 200 m" in vm.uiState.value.message!!)
    }

    @Test fun mockLocation_orNoGps_cannotReportOrConfirm() = runTest {
        val reports = InMemoryClosedReports().apply { addReport(cafe.id, "b"); addReport(cafe.id, "c") }
        val faked = FoodSheetViewModel(reports, FakeNotes(), gps(mock = true))
        faked.confirmOpen(cafe)
        advanceUntilIdle()
        assertEquals(FoodSheetViewModel.MOCK_LOCATION, faked.uiState.value.message)
        assertTrue(cafe.id in reports.flaggedIds.value)        // nhãn còn nguyên

        val noGps = FoodSheetViewModel(reports, FakeNotes(), gps(point = null))
        noGps.reportClosed(cafe)
        advanceUntilIdle()
        assertEquals(FoodSheetViewModel.NO_GPS, noGps.uiState.value.message)
    }

    @Test fun guest_canHideFromFarAway_becauseItDoesNotCount() = runTest {
        val reports = InMemoryClosedReports(myUid = "khach", guest = true)
        val vm = FoodSheetViewModel(reports, FakeNotes(), gps(point = null))

        vm.reportClosed(cafe)
        advanceUntilIdle()

        assertTrue(cafe.id in reports.hiddenIds.value)
        assertTrue(vm.uiState.value.reported)
    }

    @Test fun within200m_isAllowed() {
        val near = GpsFix(GeoPoint(cafe.lat + 0.0015, cafe.lng))   // khoảng 167 m
        val far = GpsFix(GeoPoint(cafe.lat + 0.0020, cafe.lng))    // khoảng 222 m
        assertEquals(null, FoodSheetViewModel.locationProblem(near, cafe))
        assertTrue("222 m" in FoodSheetViewModel.locationProblem(far, cafe)!!)
    }
}
