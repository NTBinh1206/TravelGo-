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
import kotlinx.coroutines.flow.stateIn

/** Số điểm tối đa đưa sang màn Lộ trình (thuật toán vét cạn 5! = 120 thứ tự). */
const val MAX_STOPS = 5

data class NearbyUiState(
    val me: GeoPoint = GeoPoint.DEFAULT,
    val places: LoadState<List<Place>> = LoadState.Loading,
)

/** ViewModel màn 07 · Địa điểm gần bạn: 5 điểm gần nhất để lên lộ trình. */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyViewModel(
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    private val nearby = combine(location.currentLocation.anchored(), retry) { center, _ -> center }
        .flatMapLatest { center -> loadFlow { placeRepository.getNearbyPlaces(center) } }

    val uiState: StateFlow<NearbyUiState> =
        combine(location.currentLocation, nearby) { current, state ->
            val me = current ?: GeoPoint.DEFAULT
            NearbyUiState(
                me = me,
                places = when (state) {
                    is LoadState.Ready -> LoadState.Ready(
                        state.data.sortedBy { me.distanceKmTo(it.location) }.take(MAX_STOPS),
                    )
                    else -> state
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NearbyUiState())

    fun retry() {
        retry.value++
    }
}
