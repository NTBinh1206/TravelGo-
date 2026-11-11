package com.travelgo.app.ui.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.common.LoadState
import com.travelgo.app.ui.common.loadFlow
import com.travelgo.app.ui.common.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Ngừng gõ một chút mới tìm. Mỗi lần gõ thêm chữ, lần chờ cũ bị huỷ và đếm lại từ đầu. */
private const val SEARCH_DEBOUNCE_MS = 400L

/** Các tuỳ chọn hiển thị kết quả, người dùng chỉnh trên màn hình. */
data class SearchOptions(
    val filter: SearchFilter = SearchFilter(),
    val nearestFirst: Boolean = false, // false = "Liên quan"
    val gridMode: Boolean = false,
)

data class SearchUiState(
    val me: GeoPoint = GeoPoint.DEFAULT,
    val options: SearchOptions = SearchOptions(),
    val results: LoadState<List<Place>> = LoadState.Ready(emptyList()), // đã lọc + sắp xếp
)

/** ViewModel màn 06 · Kết quả tìm kiếm: chờ gõ xong, tìm, lọc, sắp xếp. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    /**
     * Chữ đang gõ. Dùng state của Compose (không phải Flow) để ô nhập cập nhật ngay lập tức,
     * gõ tiếng Việt (Telex/VNI) không bị nhảy con trỏ. Cách Google khuyên dùng cho TextField.
     */
    var query by mutableStateOf("")
        private set

    private val options = MutableStateFlow(SearchOptions())
    private val retry = MutableStateFlow(0)

    // Tìm trong bộ dữ liệu có sẵn nên rất nhanh; nếu phải hỏi thêm Nominatim, repository tự giãn cách 1 giây/lần.
    private val found: Flow<LoadState<List<Place>>> = combine(snapshotFlow { query }, retry) { q, _ -> q }
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(LoadState.Ready(emptyList<Place>()))
            } else {
                loadFlow {
                    delay(SEARCH_DEBOUNCE_MS)
                    placeRepository.searchPlaces(q)
                }
            }
        }

    val uiState: StateFlow<SearchUiState> =
        combine(location.currentLocation, options, found) { current, opts, state ->
            val me = current ?: GeoPoint.DEFAULT
            SearchUiState(
                me = me,
                options = opts,
                results = state.map { list ->
                    val filtered = list.applyFilter(opts.filter, me)
                    if (opts.nearestFirst) filtered.sortedBy { me.distanceKmTo(it.location) } else filtered
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(text: String) {
        query = text
    }

    fun applyFilter(filter: SearchFilter) = options.update { it.copy(filter = filter) }

    fun setNearestFirst(nearest: Boolean) = options.update { it.copy(nearestFirst = nearest) }

    fun setGridMode(grid: Boolean) = options.update { it.copy(gridMode = grid) }

    fun retry() {
        retry.value++
    }
}
