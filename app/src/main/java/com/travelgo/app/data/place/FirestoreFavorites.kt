package com.travelgo.app.data.place

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Yêu thích lưu trên Cloud Firestore, mỗi tài khoản một danh sách riêng:
 *   users/{uid}/favorites/{id địa điểm}
 *     name      tên địa điểm (để xem trên Firebase Console)
 *     addedAt   lúc thả tim (giờ máy chủ)
 * Luật ở firestore.rules: mỗi người chỉ đọc / ghi được danh sách của chính mình.
 *
 * Firestore có bộ nhớ đệm trên máy: thả tim thì listener báo ngay (kể cả khi mất mạng),
 * có mạng lại thì tự gửi lên. Đăng nhập trên máy khác vẫn thấy đúng danh sách.
 */
class FirestoreFavorites(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : FavoriteRepository {

    private val ids = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIds: StateFlow<Set<String>> = ids.asStateFlow()

    private var listeningUid: String? = null
    private var registration: ListenerRegistration? = null

    init {
        // Đăng nhập / đăng xuất / đổi tài khoản thì nghe lại đúng danh sách của người đó.
        auth.addAuthStateListener { listen(it.currentUser?.uid) }
    }

    @Synchronized
    private fun listen(uid: String?) {
        if (uid == listeningUid && (uid == null || registration != null)) return
        registration?.remove()
        registration = null
        listeningUid = uid
        ids.value = emptySet()
        if (uid == null) return
        registration = collection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Không đọc được danh sách yêu thích", error)
                return@addSnapshotListener
            }
            if (listeningUid == uid) ids.value = snapshot?.documents.orEmpty().map { it.id }.toSet()
        }
    }

    override fun setFavorite(place: Place, liked: Boolean, onError: (Throwable) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onError(NotSignedInException())
        val ref = collection(uid).document(place.id)
        val task = if (liked) {
            ref.set(mapOf(NAME to place.name.take(MAX_NAME), ADDED_AT to FieldValue.serverTimestamp()))
        } else {
            ref.delete()
        }
        task.addOnFailureListener { e ->
            Log.w(TAG, "Không lưu được yêu thích ${place.id}", e)
            onError(e)
        }
    }

    private fun collection(uid: String) = db.collection(USERS).document(uid).collection(FAVORITES)

    private companion object {
        const val TAG = "TravelGo"
        const val USERS = "users"
        const val FAVORITES = "favorites"
        const val NAME = "name"
        const val ADDED_AT = "addedAt"
        const val MAX_NAME = 200
    }
}
