package com.travelgo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.common.LoadState
import com.travelgo.app.ui.common.anchored
import com.travelgo.app.ui.common.loadFlow
import com.travelgo.app.ui.common.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

private const val HOME_NEARBY_COUNT = 10
private const val HOME_FOOD_COUNT = 10

/** Mọi thứ màn Trang chủ cần để vẽ. */
data class HomeUiState(
    val placeName: String? = null,
    val me: GeoPoint? = null,
    val featured: List<Place> = emptyList(),
    val nearby: LoadState<List<Place>> = LoadState.Loading,
    val food: LoadState<List<Place>> = LoadState.Loading,
    val flaggedFood: Set<String> = emptySet(), // quán gắn nhãn "có thể đã đóng cửa"
)

/** ViewModel màn 03 · Trang chủ: tên vị trí, điểm đến nổi bật, 10 điểm gần bạn, 10 quán ăn uống gần bạn. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val places: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
    private val closedReports: ClosedReportRepository = AppContainer.closedReports,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    private val featured = flow { emit(runCatching { places.getFeaturedDestinations() }.getOrDefault(emptyList())) }
        .onStart { emit(emptyList()) }

    // Vị trí đổi xa hơn 2 km (GPS mới / chọn tỉnh khác) hoặc bấm "Thử lại" thì tải lại.
    private val nearby = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center ->
            loadFlow {
                val me = location.currentLocation.value ?: GeoPoint.DEFAULT
                places.getNearbyPlaces(center)
                    .sortedBy { me.distanceKmTo(it.location) }
                    .take(HOME_NEARBY_COUNT)
            }
        }
        // Giữ kết quả suốt đời ViewModel: rời màn rồi quay lại không phải tải lại.
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    // Quán ăn uống. Lỗi thì chỉ mục này hiện "Thử lại", các mục khác vẫn chạy.
    // Bỏ quán bị báo đã đóng cửa trước rồi mới lấy 10 quán gần nhất.
    private val allFood = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center -> loadFlow { places.getFoodPlaces(center) } }
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    private val food = combine(allFood, closedReports.hiddenIds, closedReports.flaggedIds) { state, hidden, flagged ->
        // Bỏ quán mình đã báo; quán gắn nhãn "có thể đã đóng cửa" vẫn hiện nhưng mờ đi và xếp cuối.
        state.map { list ->
            list.filterNot { it.id in hidden }.sortedBy { it.id in flagged }.take(HOME_FOOD_COUNT)
        } to flagged
    }

    val uiState: StateFlow<HomeUiState> =
        combine(location.placeName, location.currentLocation, featured, nearby, food) { name, me, featuredList, nearbyState, (foodState, flagged) ->
            HomeUiState(placeName = name, me = me, featured = featuredList, nearby = nearbyState, food = foodState, flaggedFood = flagged)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun retry() {
        retry.value++
    }
}
