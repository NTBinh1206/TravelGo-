package com.travelgo.app.data.place

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Người dùng báo "quán đã đóng cửa". OpenStreetMap có thể chưa cập nhật quán đã đóng,
 * nên app nhờ chính người dùng báo lại rồi ẩn quán đó khỏi mục Ăn uống.
 *
 * Bản thật: [FirestoreClosedReports] (Cloud Firestore, gói Spark miễn phí).
 * Bản giả để chạy test: [InMemoryClosedReports].
 */
interface ClosedReportRepository {
    /** Id các quán cần ẩn với người dùng hiện tại (xem [hiddenPlaceIds]). */
    val hiddenIds: StateFlow<Set<String>>

    /** Báo [place] đã đóng cửa. Trả về số người đã báo quán này (kể cả mình). */
    suspend fun reportClosed(place: Place): Result<Int>

    companion object {
        /** Đủ ngần này người báo thì ẩn quán với mọi người. Một người báo thì chỉ ẩn với chính họ. */
        const val HIDE_FOR_EVERYONE = 2

        /**
         * Quán bị ẩn khi: đã có từ [hideAfter] người báo, hoặc chính [myUid] đã báo.
         * Tách riêng (không dùng Firebase) để kiểm thử bằng unit test thường.
         */
        fun hiddenPlaceIds(
            reporters: Map<String, List<String>>,
            myUid: String?,
            hideAfter: Int = HIDE_FOR_EVERYONE,
        ): Set<String> = reporters
            .filter { (_, uids) -> uids.distinct().size >= hideAfter || (myUid != null && myUid in uids) }
            .keys
    }
}

/** Lưu trong bộ nhớ, dùng cho unit test và khi chạy với dữ liệu mẫu. */
class InMemoryClosedReports(private val myUid: String = "me") : ClosedReportRepository {
    private val reporters = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    private val hidden = MutableStateFlow<Set<String>>(emptySet())
    override val hiddenIds: StateFlow<Set<String>> = hidden.asStateFlow()

    /** Giả lập người khác báo quán [placeId]. */
    fun addReport(placeId: String, uid: String) = add(placeId, uid)

    override suspend fun reportClosed(place: Place): Result<Int> = Result.success(add(place.id, myUid))

    private fun add(placeId: String, uid: String): Int {
        reporters.update { all -> all + (placeId to (all[placeId].orEmpty() + uid).distinct()) }
        hidden.value = ClosedReportRepository.hiddenPlaceIds(reporters.value, myUid)
        return reporters.value[placeId].orEmpty().size
    }
}
