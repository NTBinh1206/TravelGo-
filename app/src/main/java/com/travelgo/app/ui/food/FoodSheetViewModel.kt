package com.travelgo.app.ui.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodSheetUiState(
    val sending: Boolean = false,
    val reported: Boolean = false,   // báo thành công: tấm thông tin quán tự đóng
    val message: String? = null,     // câu thông báo hiện một lần
)

/** ViewModel của tấm thông tin quán: gửi báo "quán đã đóng cửa". */
class FoodSheetViewModel(
    private val reports: ClosedReportRepository = AppContainer.closedReports,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodSheetUiState())
    val uiState: StateFlow<FoodSheetUiState> = _uiState.asStateFlow()

    fun reportClosed(place: Place) {
        if (_uiState.value.sending) return
        _uiState.value = FoodSheetUiState(sending = true)
        viewModelScope.launch {
            val result = reports.reportClosed(place)
            _uiState.value = result.fold(
                onSuccess = { count -> FoodSheetUiState(reported = true, message = thanks(count)) },
                onFailure = { e -> FoodSheetUiState(message = e.message?.takeIf { it.startsWith("Bạn cần") } ?: FAILED) },
            )
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
    }
}
