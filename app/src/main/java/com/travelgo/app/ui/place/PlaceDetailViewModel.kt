package com.travelgo.app.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.common.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Trạng thái màn Chi tiết địa điểm. */
data class PlaceDetailUiState(
    /** Đang tải / lỗi / đã có điểm. Không tìm thấy điểm theo id cũng tính là lỗi. */
    val place: LoadState<Place> = LoadState.Loading,
    /** Khoảng cách từ vị trí hiện tại (null nếu chưa biết vị trí). */
    val distanceKm: Double? = null,
    /** Vị trí app đang dùng, làm điểm xuất phát khi bấm "Chỉ đường". */
    val myLocation: GeoPoint? = null,
    /** Đoạn giới thiệu Wikipedia; null khi chưa tải xong hoặc điểm không có bài Wikipedia. */
    val description: String? = null,
    /** Đang hỏi Wikipedia (hiện dòng "Đang tải giới thiệu…"). */
    val loadingDescription: Boolean = false,
)

/**
 * ViewModel màn Chi tiết địa điểm (Người 2).
 * Lấy điểm theo [placeId] từ repository (điểm đã hiện ở danh sách nên có sẵn, không cần mạng),
 * rồi hỏi thêm Wikipedia ảnh + đoạn giới thiệu (cần mạng, lỗi thì vẫn hiện phần còn lại).
 */
class PlaceDetailViewModel(
    private val placeId: String,
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val state = MutableStateFlow(PlaceDetailUiState())

    val uiState: StateFlow<PlaceDetailUiState> =
        combine(state, location.currentLocation) { s, me ->
            val place = (s.place as? LoadState.Ready)?.data
            s.copy(
                distanceKm = if (place != null && me != null) me.distanceKmTo(place.location) else null,
                myLocation = me,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaceDetailUiState())

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            state.update { it.copy(place = LoadState.Loading) }
            val place = try {
                placeRepository.getPlacesByIds(listOf(placeId)).firstOrNull()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (place == null) {
                state.update { it.copy(place = LoadState.Error) }
                return@launch
            }
            state.update { it.copy(place = LoadState.Ready(place), loadingDescription = true) }

            val wiki = try {
                placeRepository.getWikiInfo(placeId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            val wikiImage = wiki?.imageUrl.orEmpty()
            state.update { current ->
                val shown = (current.place as? LoadState.Ready)?.data ?: place
                current.copy(
                    // Thẻ ở danh sách chưa kịp có ảnh thì lấy ảnh Wikipedia vừa tải.
                    place = LoadState.Ready(
                        if (shown.imageUrl.isBlank() && wikiImage.isNotBlank()) shown.copy(imageUrl = wikiImage) else shown,
                    ),
                    description = wiki?.description?.takeIf { it.isNotBlank() },
                    loadingDescription = false,
                )
            }
        }
    }
}
