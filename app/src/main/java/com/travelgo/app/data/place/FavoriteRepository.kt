package com.travelgo.app.data.place

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Chưa đăng nhập thì không lưu được yêu thích. */
class NotSignedInException : Exception("Chưa đăng nhập")

/**
 * Địa điểm yêu thích của tài khoản đang đăng nhập (Người 2).
 * Bản thật: [FirestoreFavorites]; chạy không cần Firebase (test, xem trước): [InMemoryFavorites].
 */
interface FavoriteRepository {
    /** Id các điểm đã thả tim; đổi tài khoản thì tự đổi theo, đăng xuất thì rỗng. */
    val favoriteIds: StateFlow<Set<String>>

    /**
     * Thả tim ([liked] = true) hoặc bỏ tim. [favoriteIds] đổi ngay, không chờ mạng;
     * ghi lên máy chủ thất bại (bị luật chặn, chưa đăng nhập…) thì gọi [onError] và trạng thái quay lại như cũ.
     */
    fun setFavorite(place: Place, liked: Boolean, onError: (Throwable) -> Unit = {})
}

/** Bản lưu trong bộ nhớ, mất khi tắt app. */
class InMemoryFavorites : FavoriteRepository {
    private val ids = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIds: StateFlow<Set<String>> = ids.asStateFlow()

    override fun setFavorite(place: Place, liked: Boolean, onError: (Throwable) -> Unit) {
        ids.update { if (liked) it + place.id else it - place.id }
    }
}
