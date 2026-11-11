package com.travelgo.app.ui.auth

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.travelgo.app.data.location.LocationProvider
import com.travelgo.app.data.location.Province
import com.travelgo.app.data.location.noAccent
import com.travelgo.app.data.location.vietnamProvinces
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel màn Chọn vị trí (Người 1): tìm tỉnh thành không dấu và lưu tỉnh được chọn.
 * Phần xin quyền + bật GPS dùng hộp thoại của hệ thống nên nằm ở màn hình (rememberGpsLocator).
 */
class LocationPickerViewModel(
    private val location: LocationProvider = AppContainer.locationProvider,
) : ViewModel() {

    /** Chữ trong ô tìm kiếm (state của Compose để gõ tiếng Việt không bị giật). */
    var query by mutableStateOf("")
        private set

    /** Danh sách 34 tỉnh thành đã lọc theo [query]. */
    val provinces: List<Province> by derivedStateOf {
        val q = query.trim().noAccent()
        if (q.isEmpty()) vietnamProvinces else vietnamProvinces.filter { it.name.noAccent().contains(q) }
    }

    /** Tên vị trí đang chọn, để đánh dấu trong danh sách. */
    val currentName: StateFlow<String?> = location.placeName

    fun onQueryChange(text: String) {
        query = text
    }

    fun pick(province: Province) {
        location.updateLocation(province.center, province.name)
    }
}
