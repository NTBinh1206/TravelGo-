package com.travelgo.app.data.place

import com.travelgo.app.domain.route.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class StayTimeTest {

    @Test fun cityParks_areShortVisits() {
        // Trước đây công viên rơi vào loại "thiên nhiên" nên bị gán 120 phút, lâu hơn cả bảo tàng.
        assertEquals(40, estimateStayMinutes("Công viên Chi Lăng", "thien_nhien"))
        assertEquals(40, estimateStayMinutes("Công viên Bách Tùng Diệp", "thien_nhien"))
        assertEquals(90, estimateStayMinutes("Bảo tàng Thành phố Hồ Chí Minh", "van_hoa"))
        assertEquals(30, estimateStayMinutes("Nhà hát Thành phố", "van_hoa"))
    }

    @Test fun waterPark_isNotTreatedAsSmallPark() {
        assertEquals(360, estimateStayMinutes("Công viên nước Đầm Sen", "thien_nhien"))
    }

    @Test fun formatDuration_readable() {
        assertEquals("<1 phút", formatDuration(0))
        assertEquals("45 phút", formatDuration(45))
        assertEquals("2 giờ", formatDuration(120))
        assertEquals("7 giờ 32 phút", formatDuration(452))
    }
}
