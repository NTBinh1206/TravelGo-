package com.travelgo.app.data.place

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Số người đã báo / xác nhận cho một quán.
 * [reporters] uid tài khoản Google đã báo đóng cửa; [guests] uid tài khoản Khách (ẩn danh) đã báo;
 * [openers] uid đã bấm "Quán vẫn mở cửa"; [viewers] uid tài khoản thật đã mở xem quán sau khi quán bị báo
 * (quán càng nhiều người xem thì càng cần nhiều người báo, xem [ReportGuards.requiredReports]).
 */
data class ReportTally(
    val reporters: List<String> = emptyList(),
    val guests: List<String> = emptyList(),
    val openers: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
)

/**
 * Người dùng báo "quán đã đóng cửa". OpenStreetMap có thể chưa cập nhật quán đã đóng,
 * nên app nhờ chính người dùng báo lại.
 *
 * Để một người không thể tự phá (chế độ Khách tạo tài khoản mới mỗi lần đăng nhập):
 * - Ai báo thì quán ẩn ngay, nhưng CHỈ với chính họ.
 * - Đủ số tài khoản thật (Google) báo thì quán không biến mất mà được gắn nhãn
 *   "Có thể đã đóng cửa" với mọi người: mờ đi, xếp cuối danh sách. Tài khoản Khách không được tính.
 *   Số người cần báo là 2, quán càng nhiều người xem thì cần càng nhiều ([ReportGuards.requiredReports]).
 * - Chống một người dùng nhiều tài khoản để phá ([ReportGuards]): tài khoản phải dùng app từ 1 ngày,
 *   mỗi tài khoản báo tối đa 5 lần/24 giờ, phải đứng gần quán (kiểm tra ở FoodSheetViewModel).
 * - Ai thấy quán vẫn mở thì bấm "Quán vẫn mở cửa" (cũng chỉ tài khoản thật); số người xác nhận bằng số người
 *   báo là nhãn được gỡ.
 *
 * Bản thật: [FirestoreClosedReports] (Cloud Firestore, gói Spark miễn phí).
 * Bản giả để chạy test: [InMemoryClosedReports].
 */
interface ClosedReportRepository {
    /** Id các quán ẩn với riêng người dùng hiện tại (họ đã tự báo). */
    val hiddenIds: StateFlow<Set<String>>

    /** Id các quán gắn nhãn "Có thể đã đóng cửa" với mọi người (xem [flaggedPlaceIds]). */
    val flaggedIds: StateFlow<Set<String>>

    /** Người dùng hiện tại là Khách: báo chỉ ẩn quán với chính họ, không được tính, không cần đứng gần quán. */
    val isGuest: Boolean

    /** Báo [place] đã đóng cửa. Trả về số TÀI KHOẢN THẬT đã báo quán này (kể cả mình, nếu không phải Khách). */
    suspend fun reportClosed(place: Place): Result<Int>

    /** Xác nhận [place] vẫn mở cửa (gỡ nhãn khi đủ người xác nhận). Khách không được xác nhận. */
    suspend fun confirmOpen(place: Place): Result<Unit>

    /** Ghi nhận người dùng (tài khoản thật) vừa mở xem một quán ĐÃ bị báo. Quán chưa ai báo thì không ghi gì. */
    suspend fun recordView(place: Place) {}

    companion object {
        /** Câu báo khi tài khoản Khách bấm "Quán vẫn mở cửa". */
        const val GUEST_CANNOT_CONFIRM = "Đăng nhập bằng Google để xác nhận quán vẫn mở cửa."

        /** Quán ẩn với [myUid]: chính họ đã báo (bằng tài khoản thật hay Khách). */
        fun hiddenPlaceIds(all: Map<String, ReportTally>, myUid: String?): Set<String> {
            if (myUid == null) return emptySet()
            return all.filterValues { myUid in it.reporters || myUid in it.guests }.keys
        }

        /**
         * Quán gắn nhãn "Có thể đã đóng cửa": đủ số tài khoản thật báo (tuỳ số người đã xem quán),
         * và số người xác nhận còn mở ít hơn số người báo. Tách riêng (không dùng Firebase) để unit test.
         */
        fun flaggedPlaceIds(all: Map<String, ReportTally>): Set<String> =
            all.filterValues { t ->
                val closed = t.reporters.distinct().size
                closed >= ReportGuards.requiredReports(t.viewers.distinct().size) && t.openers.distinct().size < closed
            }.keys
    }
}

/** Lỗi khi báo quán, kèm câu giải thích hiện cho người dùng. */
class ReportClosedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Lưu trong bộ nhớ, dùng cho unit test và khi chạy với dữ liệu mẫu. */
class InMemoryClosedReports(
    private val myUid: String = "me",
    private val guest: Boolean = false,
) : ClosedReportRepository {
    private val tallies = MutableStateFlow<Map<String, ReportTally>>(emptyMap())
    private val hidden = MutableStateFlow<Set<String>>(emptySet())
    private val flagged = MutableStateFlow<Set<String>>(emptySet())
    override val hiddenIds: StateFlow<Set<String>> = hidden.asStateFlow()
    override val flaggedIds: StateFlow<Set<String>> = flagged.asStateFlow()
    override val isGuest: Boolean get() = guest

    /** Giả lập một tài khoản thật khác báo quán [placeId]. */
    fun addReport(placeId: String, uid: String) = change(placeId) { it.copy(reporters = it.reporters + uid) }

    /** Giả lập một tài khoản Khách khác báo quán [placeId]. */
    fun addGuestReport(placeId: String, uid: String) = change(placeId) { it.copy(guests = it.guests + uid) }

    /** Giả lập người khác xác nhận quán [placeId] vẫn mở. */
    fun addOpenConfirm(placeId: String, uid: String) = change(placeId) { it.copy(openers = it.openers + uid) }

    /** Giả lập một tài khoản thật khác mở xem quán [placeId]. */
    fun addViewer(placeId: String, uid: String) = change(placeId) { it.copy(viewers = it.viewers + uid) }

    override suspend fun recordView(place: Place) {
        if (guest || place.id !in tallies.value) return
        change(place.id) { it.copy(viewers = it.viewers + myUid) }
    }

    override suspend fun reportClosed(place: Place): Result<Int> = Result.success(
        change(place.id) {
            if (guest) it.copy(guests = it.guests + myUid) else it.copy(reporters = it.reporters + myUid)
        }.reporters.size,
    )

    override suspend fun confirmOpen(place: Place): Result<Unit> {
        if (guest) return Result.failure(ReportClosedException(ClosedReportRepository.GUEST_CANNOT_CONFIRM))
        change(place.id) { it.copy(openers = it.openers + myUid) }
        return Result.success(Unit)
    }

    private fun change(placeId: String, edit: (ReportTally) -> ReportTally): ReportTally {
        val next = edit(tallies.value[placeId] ?: ReportTally()).let {
            ReportTally(it.reporters.distinct(), it.guests.distinct(), it.openers.distinct(), it.viewers.distinct())
        }
        tallies.value = tallies.value + (placeId to next)
        hidden.value = ClosedReportRepository.hiddenPlaceIds(tallies.value, myUid)
        flagged.value = ClosedReportRepository.flaggedPlaceIds(tallies.value)
        return next
    }
}
