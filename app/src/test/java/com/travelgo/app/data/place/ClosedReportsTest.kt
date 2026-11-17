package com.travelgo.app.data.place

import com.travelgo.app.data.place.ClosedReportRepository.Companion.hiddenPlaceIds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Quy tắc ẩn quán bị báo đã đóng cửa. */
class ClosedReportsTest {

    @Test fun oneReport_hidesOnlyForTheReporter() {
        val reports = mapOf("n1" to listOf("an"))
        assertEquals(setOf("n1"), hiddenPlaceIds(reports, myUid = "an"))
        assertEquals(emptySet<String>(), hiddenPlaceIds(reports, myUid = "binh"))
    }

    @Test fun enoughReports_hideForEveryone_andRepeatsCountOnce() {
        val reports = mapOf("n1" to listOf("an", "tuyen"), "n2" to listOf("an", "an"))
        assertEquals(setOf("n1"), hiddenPlaceIds(reports, myUid = "binh"))
        assertEquals(setOf("n1"), hiddenPlaceIds(reports, myUid = null))
    }

    @Test fun inMemory_countsEachPersonOnce() = runTest {
        val repo = InMemoryClosedReports(myUid = "binh")
        val quan = Place(id = "n9", name = "Quán A", category = FoodPlaces.QUAN_AN, address = "", lat = 10.0, lng = 106.0, score = 0.0, stayMinutes = 45)
        assertEquals(1, repo.reportClosed(quan).getOrThrow())
        assertEquals(1, repo.reportClosed(quan).getOrThrow()) // báo lại không tăng
        repo.addReport("n9", "tin")
        assertEquals(setOf("n9"), repo.hiddenIds.value)
    }
}
