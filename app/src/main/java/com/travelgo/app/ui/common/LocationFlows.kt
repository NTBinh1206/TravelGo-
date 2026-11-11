package com.travelgo.app.ui.common

import com.travelgo.app.data.model.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Đi xa hơn ngần này (km) so với lần tải trước thì mới tải lại gợi ý. */
const val RELOAD_DISTANCE_KM = 2.0

/** Bán kính tìm "Gần bạn" (km), khớp mặc định của PlaceRepository.getNearbyPlaces. */
const val NEARBY_RADIUS_KM = 25

/**
 * Điểm mốc để tải gợi ý: chỉ phát giá trị mới khi người dùng đi xa hơn [thresholdKm]
 * so với mốc trước đó. GPS xê dịch vài mét sẽ không làm các màn tải lại.
 * (Thay cho hàm rememberSearchCenter trước đây, nay nằm trong ViewModel.)
 */
fun Flow<GeoPoint?>.anchored(thresholdKm: Double = RELOAD_DISTANCE_KM): Flow<GeoPoint> =
    map { it ?: GeoPoint.DEFAULT }
        .distinctUntilChanged { anchor, next -> anchor.distanceKmTo(next) <= thresholdKm }

/**
 * Câu hiện khi quanh vị trí không có điểm nào. Thường gặp khi máy ảo còn để vị trí mặc định ở Mỹ
 * (bộ dữ liệu chỉ có Việt Nam), nên nhắc người dùng chọn tỉnh thành.
 */
const val NO_NEARBY_TEXT =
    "Chưa có điểm du lịch nào trong $NEARBY_RADIUS_KM km quanh bạn. " +
        "Nếu bạn đang ở ngoài Việt Nam hoặc máy chưa bật vị trí, hãy chọn một tỉnh thành."
