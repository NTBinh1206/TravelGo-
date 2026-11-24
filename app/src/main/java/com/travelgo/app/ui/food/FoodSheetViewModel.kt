package com.travelgo.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.GpsFix
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.NoteOutcome
import com.travelgo.app.data.place.OsmNoteSender
import com.travelgo.app.data.place.OsmNotes
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.ReportClosedException
import com.travelgo.app.data.place.ReportGuards
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class FoodSheetUiState(
    val sending: Boolean = false,
    val reported: Boolean = false,    // báo thành công: tấm thông tin quán tự đóng
    val message: String? = null,      // câu thông báo hiện một lần
    val shareToOsm: Boolean = false,  // người dùng đồng ý gửi kèm ghi chú công khai lên OpenStreetMap
)

/**
 * ViewModel của tấm thông tin quán: báo "quán đã đóng cửa" hoặc xác nhận "quán vẫn mở cửa".
 * Nếu người dùng tick ô đồng ý và quán vừa bị gắn nhãn (đủ tài khoản thật báo, chưa ai xác nhận còn mở),
 * gửi thêm một ghi chú lên OpenStreetMap để người vẽ bản đồ sửa dữ liệu gốc.
 *
 * Tài khoản thật (phiếu được tính) phải đứng cách quán không quá [ReportGuards.NEAR_METERS] mét,
 * đọc bằng GPS ngay lúc bấm và không nhận vị trí giả. Khách báo chỉ ẩn quán với chính họ nên không cần kiểm tra.
 */
class FoodSheetViewModel(
    private val reports: ClosedReportRepository = AppContainer.closedReports,
    private val notes: OsmNoteSender = AppContainer.osmNotes,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodSheetUiState())
    val uiState: StateFlow<FoodSheetUiState> = _uiState.asStateFlow()

    /** Quán đang gắn nhãn "Có thể đã đóng cửa" (để tấm thông tin hiện nút "Quán vẫn mở cửa"). */
    val flaggedIds: StateFlow<Set<String>> = reports.flaggedIds

    fun setShareToOsm(share: Boolean) = _uiState.update { it.copy(shareToOsm = share) }

    /** Tấm thông tin quán vừa mở: ghi nhận lượt xem nếu quán đang bị báo. */
    fun onShown(place: Place) {
        viewModelScope.launch { reports.recordView(place) }
    }

    fun reportClosed(place: Place) {
        if (_uiState.value.sending) return
        val share = _uiState.value.shareToOsm && OsmNotes.canReport(place)
        _uiState.update { it.copy(sending = true, reported = false, message = null) }
        viewModelScope.launch {
            if (!reports.isGuest) {
                locationProblem(place)?.let { problem ->
                    _uiState.update { it.copy(sending = false, message = problem) }
                    return@launch
                }
            }
            val count = reports.reportClosed(place).getOrElse { e ->
                _uiState.update {
                    it.copy(sending = false, message = (e as? ReportClosedException)?.message ?: FAILED)
                }
                return@launch
            }
            // Nhãn tính lại ngay từ bản sao cục bộ, nên đọc được liền sau khi báo.
            val flaggedNow = place.id in reports.flaggedIds.value
            val note = if (share && flaggedNow) notes.reportClosed(place, count) else null
            _uiState.update {
                // Bỏ tick sau mỗi lần báo: đồng ý gửi ghi chú công khai chỉ áp dụng cho lần báo này.
                it.copy(
                    sending = false,
                    reported = true,
                    shareToOsm = false,
                    message = thanks(flaggedNow) + noteLine(share, flaggedNow, note),
                )
            }
        }
    }

    fun confirmOpen(place: Place) {
        if (_uiState.value.sending) return
        _uiState.update { it.copy(sending = true, reported = false, message = null) }
        viewModelScope.launch {
            if (!reports.isGuest) {
                locationProblem(place)?.let { problem ->
                    _uiState.update { it.copy(sending = false, message = problem) }
                    return@launch
                }
            }
            val result = reports.confirmOpen(place)
            _uiState.update {
                result.fold(
                    onSuccess = { _ -> it.copy(sending = false, reported = true, message = OPEN_THANKS) },
                    onFailure = { e -> it.copy(sending = false, message = (e as? ReportClosedException)?.message ?: FAILED) },
                )
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null, reported = false) }

    /** Đọc GPS lúc này; trả về câu báo lỗi nếu không được báo, null nếu đứng đủ gần quán. */
    private suspend fun locationProblem(place: Place): String? {
        val fix = location.freshFix().getOrElse { e ->
            return if (e is SecurityException) NEED_PERMISSION else NO_GPS
        }
        return locationProblem(fix, place)
    }

    companion object {
        const val FAILED = "Chưa gửi được, kiểm tra mạng rồi thử lại."
        const val OPEN_THANKS = "Cảm ơn bạn! Đã ghi nhận quán vẫn mở cửa."
        const val NEED_PERMISSION = "Cần cho TravelGo dùng vị trí để kiểm tra bạn đang ở gần quán."
        const val NO_GPS = "Không lấy được vị trí hiện tại. Bật GPS rồi thử lại nhé."
        const val MOCK_LOCATION = "Máy đang dùng vị trí giả lập, không báo hoặc xác nhận quán được."

        /** Kiểm tra một lần đọc GPS: vị trí giả hoặc đứng xa quán thì không được báo. */
        fun locationProblem(fix: GpsFix, place: Place): String? {
            if (fix.mock) return MOCK_LOCATION
            val km = fix.point.distanceKmTo(place.location)
            if (km * 1000 <= ReportGuards.NEAR_METERS) return null
            val far = if (km < 1) "${(km * 1000).roundToInt()} m" else formatKm(km)
            return "Bạn đang cách quán khoảng $far. Cần đứng trong vòng ${ReportGuards.NEAR_METERS.roundToInt()} m " +
                "mới báo hoặc xác nhận quán được."
        }

        fun thanks(flaggedNow: Boolean): String =
            if (flaggedNow) {
                "Cảm ơn bạn! Quán được gắn nhãn \"Có thể đã đóng cửa\" với mọi người."
            } else {
                "Cảm ơn bạn! Quán đã ẩn khỏi danh sách của bạn."
            }

        /** Câu nói thêm về ghi chú OSM; rỗng nếu người dùng không chọn gửi. */
        fun noteLine(share: Boolean, flaggedNow: Boolean, note: Result<NoteOutcome>?): String = when {
            !share -> ""
            !flaggedNow ->
                " Chưa gửi ghi chú lên OpenStreetMap vì chưa đủ tài khoản Google/Facebook cùng báo."
            note == null -> ""
            note.isFailure -> " Chưa gửi được ghi chú lên OpenStreetMap."
            note.getOrNull() is NoteOutcome.AlreadyOpen -> " OpenStreetMap đã có ghi chú về quán này, không gửi thêm."
            else -> " Đã gửi ghi chú lên OpenStreetMap để người vẽ bản đồ kiểm tra."
        }
    }
}
