package com.travelgo.app.ui.common

import com.travelgo.app.data.place.FavoriteRepository
import com.travelgo.app.data.place.InMemoryFavorites
import com.travelgo.app.data.place.NotSignedInException
import com.travelgo.app.data.place.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Nút tim: thả / bỏ tim, báo lỗi dễ hiểu khi không lưu được. */
class FavoritesViewModelTest {

    private val place = Place("p1", "Cầu Rồng", "thanh_pho", "Đà Nẵng", 16.0611, 108.2273, 4.7, 30)

    /** Kho luôn báo lỗi [error] khi ghi. */
    private class FailingFavorites(private val error: Throwable) : FavoriteRepository {
        override val favoriteIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override fun setFavorite(place: Place, liked: Boolean, onError: (Throwable) -> Unit) = onError(error)
    }

    @Test fun toggle_likesThenUnlikes() {
        val vm = FavoritesViewModel(InMemoryFavorites())
        var message: String? = null

        vm.toggle(place) { message = it }
        assertTrue(place.id in vm.favoriteIds.value)
        vm.toggle(place) { message = it }
        assertFalse(place.id in vm.favoriteIds.value)
        assertNull(message)
    }

    @Test fun notSignedIn_asksToSignIn() {
        var message: String? = null
        FavoritesViewModel(FailingFavorites(NotSignedInException())).toggle(place) { message = it }
        assertEquals("Đăng nhập để lưu địa điểm yêu thích", message)
    }

    @Test fun serverError_saysTryAgain() {
        var message: String? = null
        FavoritesViewModel(FailingFavorites(IllegalStateException("PERMISSION_DENIED"))).toggle(place) { message = it }
        assertEquals("Chưa lưu được yêu thích, thử lại sau", message)
    }
}
