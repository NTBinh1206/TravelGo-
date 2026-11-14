package com.travelgo.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.NoteOutcome
import com.travelgo.app.data.place.OsmNoteSender
import com.travelgo.app.data.place.OsmNotes
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.ReportClosedException
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodSheetUiState(
    val sending: Boolean = false,
    val reported: Boolean = false,    // báo thành công: tấm thông tin quán tự đóng
    val message: String? = null,      // câu thông báo hiện một lần
    val shareToOsm: Boolean = false,  // người dùng đồng ý gửi kèm ghi chú công khai lên OpenStreetMap
)

/**
 * ViewModel của tấm thông tin quán: gửi báo "quán đã đóng cửa".
 * Nếu người dùng tick ô đồng ý và quán đã có từ [ClosedReportRepository.HIDE_FOR_EVERYONE] người báo,
 * gửi thêm một ghi chú lên OpenStreetMap để người vẽ bản đồ sửa dữ liệu gốc.
 */
class FoodSheetViewModel(
    private val reports: ClosedReportRepository = AppContainer.closedReports,
    private val notes: OsmNoteSender = AppContainer.osmNotes,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodSheetUiState())
    val uiState: StateFlow<FoodSheetUiState> = _uiState.asStateFlow()

    fun setShareToOsm(share: Boolean) = _uiState.update { it.copy(shareToOsm = share) }

    fun reportClosed(place: Place) {
        if (_uiState.value.sending) return
        val share = _uiState.value.shareToOsm && OsmNotes.canReport(place)
        _uiState.update { it.copy(sending = true, reported = false, message = null) }
        viewModelScope.launch {
            val count = reports.reportClosed(place).getOrElse { e ->
                _uiState.update {
                    it.copy(sending = false, message = (e as? ReportClosedException)?.message ?: FAILED)
                }
                return@launch
            }
            val note = if (share && count >= ClosedReportRepository.HIDE_FOR_EVERYONE) {
                notes.reportClosed(place, count)
            } else {
                null
            }
            _uiState.update {
                it.copy(sending = false, reported = true, message = thanks(count) + noteLine(share, count, note))
            }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null, reported = false) }

    companion object {
        const val FAILED = "Chưa gửi được, kiểm tra mạng rồi thử lại."

        fun thanks(count: Int): String =
            if (count >= ClosedReportRepository.HIDE_FOR_EVERYONE) {
                "Cảm ơn bạn! Đã có $count người báo nên quán được ẩn với mọi người."
            } else {
                "Cảm ơn bạn! Quán đã ẩn khỏi danh sách của bạn."
            }

        /** Câu nói thêm về ghi chú OSM; rỗng nếu người dùng không chọn gửi. */
        fun noteLine(share: Boolean, count: Int, note: Result<NoteOutcome>?): String = when {
            !share -> ""
            count < ClosedReportRepository.HIDE_FOR_EVERYONE ->
                " Chưa gửi ghi chú lên OpenStreetMap vì quán mới có $count người báo."
            note == null -> ""
            note.isFailure -> " Chưa gửi được ghi chú lên OpenStreetMap."
            note.getOrNull() is NoteOutcome.AlreadyOpen -> " OpenStreetMap đã có ghi chú về quán này, không gửi thêm."
            else -> " Đã gửi ghi chú lên OpenStreetMap để người vẽ bản đồ kiểm tra."
        }
    }
}
