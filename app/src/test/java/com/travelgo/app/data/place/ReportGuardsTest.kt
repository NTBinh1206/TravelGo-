package com.travelgo.app.data.place

import com.travelgo.app.data.place.ClosedReportRepository.Companion.flaggedPlaceIds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chống một người dùng nhiều tài khoản để báo sai quán đóng cửa. */
class ReportGuardsTest {

    private val hour = 60 * 60 * 1000L

    @Test fun accountMustBeOneDayOld() {
        val created = 1_000_000L
        assertFalse(ReportGuards.oldEnough(created, created + 23 * hour))
        assertTrue(ReportGuards.oldEnough(created, created + 24 * hour))
    }

    @Test fun fiveActionsPer24Hours_thenNewWindow() {
        val start = 5_000_000L
        assertEquals(1, ReportGuards.nextCount(start, used = 0, nowMs = start + hour))
        assertEquals(5, ReportGuards.nextCount(start, used = 4, nowMs = start + hour))
        assertNull(ReportGuards.nextCount(start, used = 5, nowMs = start + 23 * hour))   // hết lượt
        assertEquals(1, ReportGuards.nextCount(start, used = 5, nowMs = start + 24 * hour)) // khung mới
    }

    @Test fun morePeopleViewing_needMoreReports() {
        assertEquals(2, ReportGuards.requiredReports(viewers = 0))
        assertEquals(2, ReportGuards.requiredReports(viewers = 9))
        assertEquals(3, ReportGuards.requiredReports(viewers = 10))
        assertEquals(4, ReportGuards.requiredReports(viewers = 25))
        assertEquals(5, ReportGuards.requiredReports(viewers = 500))                     // không vượt quá 5
    }

    @Test fun popularPlace_twoReportsAreNotEnough() {
        val twoReports = ReportTally(reporters = listOf("a", "b"))
        val viewers = (1..12).map { "v$it" }
        assertEquals(setOf("quiet"), flaggedPlaceIds(mapOf("quiet" to twoReports, "busy" to twoReports.copy(viewers = viewers))))
        val threeReports = twoReports.copy(reporters = listOf("a", "b", "c"), viewers = viewers)
        assertEquals(setOf("busy"), flaggedPlaceIds(mapOf("busy" to threeReports)))
    }

    @Test fun inMemory_viewsCountOnlyForReportedPlaces_andNotForGuests() = runTest {
        val place = Place("n1", "Quán A", "quan_an", "", 10.0, 106.0, 0.0, 0)
        val repo = InMemoryClosedReports(myUid = "binh")
        repo.recordView(place)                                    // chưa ai báo: không lưu
        repo.addReport("n1", "an")
        repo.recordView(place)
        repo.recordView(place)                                    // xem lại không tính thêm
        val guest = InMemoryClosedReports(myUid = "khach", guest = true).apply { addReport("n1", "an") }
        guest.recordView(place)

        repo.addReport("n1", "tuyen")
        assertTrue("n1" in repo.flaggedIds.value)                 // 1 người xem: vẫn cần 2 người báo
        assertFalse(repo.isGuest)
        assertTrue(guest.isGuest)
    }
}
