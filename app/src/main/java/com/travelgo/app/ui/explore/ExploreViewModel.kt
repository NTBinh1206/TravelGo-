package com.travelgo.app.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.FoodPlaces
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) { NEAREST("Gần nhất"), TOP_SCORE("Nổi bật") }

/** Chip "Ăn uống" ở Khám phá: gồm cả quán ăn và quán nước. */
const val FOOD_GROUP = "an_uong"

/**
 * Màn khác muốn mở Khám phá ở sẵn một nhóm, ví dụ Trang chủ bấm "Xem tất cả" ở mục Ăn uống.
 * Khám phá là một tab nên không truyền tham số điều hướng được; ViewModel đọc yêu cầu này rồi xoá đi.
 */
object ExploreRequest {
    internal val category = MutableStateFlow<String?>(null)

    fun open(category: String) {
        this.category.value = category
    }
}

/** Lọc thêm khi đang xem Ăn uống. */
enum class FoodKind(val label: String, val category: String?) {
    ALL("Tất cả", null),
    QUAN_AN("Quán ăn", FoodPlaces.QUAN_AN),
    QUAN_NUOC("Cà phê, trà", FoodPlaces.QUAN_NUOC),
}

data class ExploreUiState(
    val me: GeoPoint = GeoPoint.DEFAULT,
    val category: String? = null,         // null = tất cả, FOOD_GROUP = ăn uống
    val sort: SortOption = SortOption.NEAREST,
    val foodKind: FoodKind = FoodKind.ALL,
    val places: LoadState<List<Place>> = LoadState.Loading, // đã lọc + sắp xếp
) {
    val isFood: Boolean get() = category == FOOD_GROUP
}

/** ViewModel màn 04 · Khám phá: lọc theo loại, sắp xếp gần nhất / nổi bật; riêng Ăn uống lấy quán quanh bạn. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val category = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(SortOption.NEAREST)
    private val foodKind = MutableStateFlow(FoodKind.ALL)
    private val retry = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            ExploreRequest.category.filterNotNull().collect { requested ->
                category.value = requested
                ExploreRequest.category.value = null
            }
        }
    }

    private val nearby = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center -> loadFlow { placeRepository.getNearbyPlaces(center) } }
        // Giữ kết quả suốt đời ViewModel: rời màn rồi quay lại không phải tải lại.
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    // Quán ăn uống cần mạng nên chỉ tải khi người dùng chọn chip "Ăn uống".
    private val food = combine(
        location.currentLocation.anchored(),
        retry,
        category.map { it == FOOD_GROUP }.distinctUntilChanged(),
    ) { center, _, wanted -> center to wanted }
        .flatMapLatest { (center, wanted) ->
            if (wanted) loadFlow { placeRepository.getFoodPlaces(center) } else flowOf(LoadState.Loading)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    private val source = combine(category, nearby, food) { cat, nearbyState, foodState ->
        if (cat == FOOD_GROUP) foodState else nearbyState
    }

    val uiState: StateFlow<ExploreUiState> =
        combine(location.currentLocation, category, sort, foodKind, source) { current, cat, order, kind, state ->
            val me = current ?: GeoPoint.DEFAULT
            ExploreUiState(
                me = me,
                category = cat,
                sort = order,
                foodKind = kind,
                places = state.map { list ->
                    if (cat == FOOD_GROUP) {
                        // Quán không có điểm đánh giá nên chỉ xếp theo khoảng cách.
                        list.filter { kind.category == null || it.category == kind.category }
                            .sortedBy { me.distanceKmTo(it.location) }
                    } else {
                        list.filter { cat == null || it.category == cat }.let { filtered ->
                            when (order) {
                                SortOption.NEAREST -> filtered.sortedBy { me.distanceKmTo(it.location) }
                                SortOption.TOP_SCORE -> filtered.sortedByDescending { it.score }
                            }
                        }
                    }
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())

    fun selectCategory(key: String?) {
        category.value = key
    }

    fun selectSort(option: SortOption) {
        sort.value = option
    }

    fun selectFoodKind(kind: FoodKind) {
        foodKind.value = kind
    }

    fun retry() {
        retry.value++
    }
}
