package com.travelgo.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.common.LoadState
import com.travelgo.app.ui.common.anchored
import com.travelgo.app.ui.common.loadFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Số điểm tối đa đưa sang màn Lộ trình (thuật toán vét cạn 5! = 120 thứ tự). */
const val MAX_STOPS = 5

/** Cần ít nhất ngần này điểm mới có "lộ trình" để xếp. */
const val MIN_STOPS = 2

/** Số điểm hiện ở màn Gần bạn để người dùng chọn. */
const val NEARBY_COUNT = 20

data class NearbyUiState(
    val me: GeoPoint = GeoPoint.DEFAULT,
    /** Khoảng [NEARBY_COUNT] điểm gần nhất, xếp từ gần đến xa. */
    val places: LoadState<List<Place>> = LoadState.Loading,
    /** Id các điểm đang chọn (tối đa [MAX_STOPS]). */
    val selectedIds: Set<String> = emptySet(),
) {
    /** Id đã chọn theo thứ tự trên danh sách (gần trước, xa sau) để gửi sang màn Lộ trình. */
    val selectedInOrder: List<String>
        get() = places.let { if (it is LoadState.Ready) it.data else emptyList() }
            .map { it.id }
            .filter { it in selectedIds }

    val canRoute: Boolean get() = selectedIds.size >= MIN_STOPS
}

/**
 * ViewModel màn 07 · Địa điểm gần bạn (mục 21).
 * Hiện khoảng 20 điểm gần nhất, mở màn thì chọn sẵn 5 điểm gần nhất;
 * người dùng bấm thẻ để chọn / bỏ chọn, tối đa [MAX_STOPS] điểm, rồi gửi id sang màn Lộ trình.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    /** Lựa chọn của người dùng; null = chưa đụng tới, dùng mặc định 5 điểm gần nhất. */
    private val userSelection = MutableStateFlow<Set<String>?>(null)

    private val nearby = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center -> loadFlow { placeRepository.getNearbyPlaces(center) } }
        // Đổi vị trí -> danh sách mới -> chọn lại 5 điểm gần nhất của nơi mới.
        .onEach { if (it is LoadState.Ready) userSelection.value = null }
        // Giữ kết quả suốt đời ViewModel: sang màn Lộ trình rồi quay lại vẫn còn danh sách và lựa chọn.
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    val uiState: StateFlow<NearbyUiState> =
        combine(location.currentLocation, nearby, userSelection) { current, state, picked ->
            val me = current ?: GeoPoint.DEFAULT
            val places = when (state) {
                is LoadState.Ready -> LoadState.Ready(
                    state.data.sortedBy { me.distanceKmTo(it.location) }.take(NEARBY_COUNT),
                )
                else -> state
            }
            val list = (places as? LoadState.Ready)?.data.orEmpty()
            val ids = list.map { it.id }.toSet()
            NearbyUiState(
                me = me,
                places = places,
                selectedIds = (picked ?: list.take(MAX_STOPS).map { it.id }.toSet()).filter { it in ids }.toSet(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NearbyUiState())

    /**
     * Chọn hoặc bỏ chọn một điểm.
     * @return false nếu đã đủ [MAX_STOPS] điểm mà vẫn bấm chọn thêm (màn hình báo cho người dùng).
     */
    fun toggle(placeId: String): Boolean {
        val current = uiState.value.selectedIds
        val next = when {
            placeId in current -> current - placeId
            current.size >= MAX_STOPS -> return false
            else -> current + placeId
        }
        userSelection.value = next
        return true
    }

    fun retry() {
        retry.value++
    }
}
