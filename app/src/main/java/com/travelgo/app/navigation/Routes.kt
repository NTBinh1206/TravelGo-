package com.travelgo.app.navigation

/** Tên các màn hình (route). Thêm màn mới thì khai báo ở đây. */
object Routes {
    const val LOGIN = "login"                         // 01 Đăng nhập (Người 1)
    const val LOCATION_PERMISSION = "location"        // 02 Xin quyền vị trí (Người 1)
    const val LOCATION_PICKER = "location_picker"  // Chọn vị trí: GPS hoặc tỉnh thành (Người 1)
    const val HOME = "home"                           // 03 Trang chủ (Người 2)
    const val EXPLORE = "explore"                     // 04 Khám phá (Người 2)
    const val PROFILE = "profile"                     // 05 Cá nhân (Người 1)
    const val SEARCH = "search"                       // 06 Kết quả tìm kiếm (Người 2)
    const val NEARBY = "nearby"                       // 07 Địa điểm gần bạn (Người 2)
    const val SMART_ROUTE = "smart_route/{placeIds}"  // 08 Lộ trình thông minh (Người 3)

    const val ARG_PLACE_IDS = "placeIds"

    fun smartRoute(placeIds: List<String>) = "smart_route/${placeIds.joinToString(",")}"

    /** Các màn có thanh tab ở dưới. */
    val tabRoutes = setOf(HOME, EXPLORE, PROFILE)
}
