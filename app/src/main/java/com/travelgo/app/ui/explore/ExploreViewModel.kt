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
import com.travelgo.app.ui.common.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class SortOption(val label: String) { NEAREST("Gần nhất"), TOP_SCORE("Nổi bật") }

data class ExploreUiState(
    val me: GeoPoint = GeoPoint.DEFAULT,
    val category: String? = null,         // null = tất cả
    val sort: SortOption = SortOption.NEAREST,
    val places: LoadState<List<Place>> = LoadState.Loading, // đã lọc + sắp xếp
)

/** ViewModel màn 04 · Khám phá: lọc theo loại, sắp xếp gần nhất / nổi bật. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val category = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(SortOption.NEAREST)
    private val retry = MutableStateFlow(0)

    private val nearby = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center -> loadFlow { placeRepository.getNearbyPlaces(center) } }
        // Giữ kết quả suốt đời ViewModel: rời màn rồi quay lại không phải tải lại.
        .stateIn(viewModelScope, SharingStarted.Lazily, LoadState.Loading)

    val uiState: StateFlow<ExploreUiState> =
        combine(location.currentLocation, category, sort, nearby) { current, cat, order, state ->
            val me = current ?: GeoPoint.DEFAULT
            ExploreUiState(
                me = me,
                category = cat,
                sort = order,
                places = state.map { list ->
                    list.filter { cat == null || it.category == cat }.let { filtered ->
                        when (order) {
                            SortOption.NEAREST -> filtered.sortedBy { me.distanceKmTo(it.location) }
                            SortOption.TOP_SCORE -> filtered.sortedByDescending { it.score }
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

    fun retry() {
        retry.value++
    }
}
