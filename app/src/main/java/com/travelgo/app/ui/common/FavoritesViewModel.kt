package com.travelgo.app.ui.common

import androidx.lifecycle.ViewModel
import com.travelgo.app.data.place.FavoriteRepository
import com.travelgo.app.data.place.NotSignedInException
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel cho nút tim (Người 2). Mỗi màn hình dùng chung một bản cho mọi nút tim trên màn đó.
 * Danh sách yêu thích nằm ở [FavoriteRepository] nên Trang chủ, Khám phá, Tìm kiếm, Chi tiết luôn khớp nhau.
 */
class FavoritesViewModel(
    private val favorites: FavoriteRepository = AppContainer.favorites,
) : ViewModel() {

    val favoriteIds: StateFlow<Set<String>> = favorites.favoriteIds

    /** Bật / tắt tim; lỗi thì [onError] nhận câu báo cho người dùng. */
    fun toggle(place: Place, onError: (String) -> Unit) {
        val liked = place.id !in favoriteIds.value
        favorites.setFavorite(place, liked) { e ->
            onError(
                if (e is NotSignedInException) "Đăng nhập để lưu địa điểm yêu thích"
                else "Chưa lưu được yêu thích, thử lại sau",
            )
        }
    }
}
