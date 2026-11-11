package com.travelgo.app.ui.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.data.place.PlaceRepository
import com.travelgo.app.di.AppContainer
import com.travelgo.app.domain.route.RouteMode
import com.travelgo.app.domain.route.RouteResult
import com.travelgo.app.domain.route.evaluateRoute
import com.travelgo.app.domain.route.findBestRoute
import com.travelgo.app.ui.common.LoadState
import com.travelgo.app.ui.common.dataOrNull
import com.travelgo.app.ui.common.loadFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** findBestRoute chỉ nhận tối đa 5 điểm (vét cạn 5! = 120 thứ tự), nhiều hơn sẽ báo lỗi. */
private const val MAX_ROUTE_PLACES = 5

data class SmartRouteUiState(
    val start: GeoPoint = GeoPoint.DEFAULT,
    val mode: RouteMode = RouteMode.SHORTEST,
    /** Lộ trình đã tính; Loading khi đang lấy thông tin các điểm, Error khi lỗi. */
    val route: LoadState<RouteResult> = LoadState.Loading,
    /** true khi người dùng đã tự đổi thứ tự bằng nút mũi tên (không còn là thứ tự tối ưu). */
    val customOrder: Boolean = false,
)

/**
 * ViewModel màn 08 · Lộ trình thông minh (Người 3).
 * Giữ chế độ lộ trình và thứ tự người dùng tự xếp, nên quay màn hình hay quay lại từ Google Maps vẫn còn.
 *
 * @param placeIds id các điểm do màn Gần bạn gửi sang (qua tham số điều hướng).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SmartRouteViewModel(
    private val placeIds: List<String>,
    private val placeRepository: PlaceRepository = AppContainer.placeRepository,
    location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    /** Điểm xuất phát lấy một lần lúc mở màn, để lộ trình không nhảy lung tung khi GPS xê dịch. */
    private val start: GeoPoint = location.currentLocation.value ?: GeoPoint.DEFAULT

    private val mode = MutableStateFlow(RouteMode.SHORTEST)
    /** Thứ tự người dùng tự xếp (danh sách id); null = dùng thứ tự tối ưu của thuật toán. */
    private val customOrder = MutableStateFlow<List<String>?>(null)
    private val retry = MutableStateFlow(0)

    private val places = retry.flatMapLatest {
        loadFlow { placeRepository.getPlacesByIds(placeIds).take(MAX_ROUTE_PLACES) }
    }

    val uiState: StateFlow<SmartRouteUiState> =
        combine(places, mode, customOrder) { placesState, currentMode, order ->
            SmartRouteUiState(
                start = start,
                mode = currentMode,
                route = when (placesState) {
                    is LoadState.Ready -> LoadState.Ready(buildRoute(placesState.data, currentMode, order))
                    LoadState.Loading -> LoadState.Loading
                    LoadState.Error -> LoadState.Error
                },
                customOrder = order != null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartRouteUiState(start = start))

    private fun buildRoute(list: List<Place>, currentMode: RouteMode, order: List<String>?): RouteResult {
        if (order == null) return findBestRoute(start, list, currentMode)
        val byId = list.associateBy { it.id }
        return evaluateRoute(start, order.mapNotNull { byId[it] }, currentMode)
    }

    /** Đổi kiểu lộ trình: bỏ thứ tự tự xếp, tính lại thứ tự tối ưu. */
    fun selectMode(newMode: RouteMode) {
        mode.value = newMode
        customOrder.value = null
    }

    fun moveUp(index: Int) = move(index, index - 1)

    fun moveDown(index: Int) = move(index, index + 1)

    /** Đưa điểm ở vị trí [from] sang vị trí [to] rồi tính lại km, phút (Task 30). */
    private fun move(from: Int, to: Int) {
        val route = uiState.value.route.dataOrNull ?: return
        val ids = route.order.map { it.id }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        customOrder.update { ids }
    }

    fun retry() {
        retry.value++
    }
}
