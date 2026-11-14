package com.travelgo.app.data.place

import com.travelgo.app.data.place.ClosedReportRepository.Companion.flaggedPlaceIds
import com.travelgo.app.data.place.ClosedReportRepository.Companion.hiddenPlaceIds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Quy tắc ẩn / gắn nhãn quán bị báo đã đóng cửa. */
class ClosedReportsTest {

    @Test fun reporting_hidesOnlyForTheReporter_guestIncluded() {
        val all = mapOf(
            "n1" to ReportTally(reporters = listOf("an")),
            "n2" to ReportTally(guests = listOf("khach")),
        )
        assertEquals(setOf("n1"), hiddenPlaceIds(all, myUid = "an"))
        assertEquals(setOf("n2"), hiddenPlaceIds(all, myUid = "khach"))
        assertEquals(emptySet<String>(), hiddenPlaceIds(all, myUid = "binh"))
        assertEquals(emptySet<String>(), hiddenPlaceIds(all, myUid = null))
    }

    @Test fun flag_needsTwoRealAccounts_guestsDontCount() {
        val all = mapOf(
            "n1" to ReportTally(reporters = listOf("an", "tuyen")),            // 2 tài khoản thật → nhãn
            "n2" to ReportTally(reporters = listOf("an", "an")),               // 1 người báo 2 lần → không
            "n3" to ReportTally(guests = listOf("k1", "k2", "k3")),            // toàn Khách → không
            "n4" to ReportTally(reporters = listOf("an"), guests = listOf("k1")), // 1 thật + 1 Khách → không
        )
        assertEquals(setOf("n1"), flaggedPlaceIds(all))
    }

    @Test fun enoughOpenConfirms_removeTheFlag() {
        val reported = ReportTally(reporters = listOf("an", "tuyen"))
        assertEquals(setOf("n1"), flaggedPlaceIds(mapOf("n1" to reported.copy(openers = listOf("c")))))
        assertEquals(emptySet<String>(), flaggedPlaceIds(mapOf("n1" to reported.copy(openers = listOf("c", "d")))))
    }

    @Test fun inMemory_reportConfirmFlow() = runTest {
        val repo = InMemoryClosedReports(myUid = "binh")
        val quan = Place(id = "n9", name = "Quán A", category = FoodPlaces.QUAN_AN, address = "", lat = 10.0, lng = 106.0, score = 0.0, stayMinutes = 45)

        assertEquals(1, repo.reportClosed(quan).getOrThrow())
        assertEquals(1, repo.reportClosed(quan).getOrThrow()) // báo lại không tăng
        assertEquals(setOf("n9"), repo.hiddenIds.value)       // ẩn với mình ngay
        assertEquals(emptySet<String>(), repo.flaggedIds.value)

        repo.addReport("n9", "tin")                           // tài khoản thật thứ hai → nhãn
        assertEquals(setOf("n9"), repo.flaggedIds.value)

        repo.confirmOpen(quan)                                // 1 xác nhận < 2 người báo: nhãn còn
        assertEquals(setOf("n9"), repo.flaggedIds.value)
        repo.addOpenConfirm("n9", "an")                       // đủ 2 xác nhận: gỡ nhãn
        assertEquals(emptySet<String>(), repo.flaggedIds.value)
    }

    @Test fun inMemory_guestReportDoesNotFlag() = runTest {
        val repo = InMemoryClosedReports(myUid = "khach-1", guest = true)
        val quan = Place(id = "n9", name = "Quán A", category = FoodPlaces.QUAN_AN, address = "", lat = 10.0, lng = 106.0, score = 0.0, stayMinutes = 45)
        repo.addGuestReport("n9", "khach-0")

        assertEquals(0, repo.reportClosed(quan).getOrThrow()) // Khách: số tài khoản thật vẫn 0
        assertEquals(setOf("n9"), repo.hiddenIds.value)       // vẫn ẩn với chính mình
        assertEquals(emptySet<String>(), repo.flaggedIds.value)
    }
}
