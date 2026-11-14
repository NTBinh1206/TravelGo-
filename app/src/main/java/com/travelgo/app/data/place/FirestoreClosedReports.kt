package com.travelgo.app.data.place

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Báo quán đã đóng cửa, lưu trên Cloud Firestore.
 *
 * Mỗi quán bị báo là một tài liệu `closed_reports/{id quán}`:
 *   name, lat, lng      tên và toạ độ quán (để xem lại trên Firebase Console)
 *   reporters           uid TÀI KHOẢN THẬT (Google, Facebook) đã báo đóng cửa
 *   guests              uid tài khoản Khách (ẩn danh) đã báo; không tính vào ngưỡng gắn nhãn
 *   openers             uid đã bấm "Quán vẫn mở cửa"
 *   updatedAt           lần ghi gần nhất (giờ máy chủ)
 * Mỗi uid chỉ được tính một lần; luật bảo vệ dữ liệu nằm ở file firestore.rules ở thư mục gốc project.
 *
 * App nghe cả bộ sưu tập này (số quán bị báo ít nên rất nhẹ, nằm trong mức miễn phí 50.000 lượt đọc mỗi ngày),
 * có người báo mới thì danh sách quán ẩn / gắn nhãn tự cập nhật, không phải tải lại màn hình.
 */
class FirestoreClosedReports(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ClosedReportRepository {

    private val tallies = MutableStateFlow<Map<String, ReportTally>>(emptyMap())
    private val myUid = MutableStateFlow(auth.currentUser?.uid)
    private var registration: ListenerRegistration? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val hiddenIds: StateFlow<Set<String>> =
        combine(tallies, myUid) { all, me -> ClosedReportRepository.hiddenPlaceIds(all, me) }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override val flaggedIds: StateFlow<Set<String>> =
        tallies.map { ClosedReportRepository.flaggedPlaceIds(it) }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    init {
        // Luật Firestore chỉ cho người đã đăng nhập (kể cả khách) đọc, nên đăng nhập / đăng xuất thì nghe lại.
        auth.addAuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != myUid.value || registration == null) listen(uid)
        }
    }

    @Synchronized
    private fun listen(uid: String?) {
        myUid.value = uid
        registration?.remove()
        registration = null
        if (uid == null) {
            tallies.value = emptyMap()
            return
        }
        registration = db.collection(COLLECTION).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Firestore dừng hẳn listener khi lỗi (mất mạng lâu, luật chưa cho đọc…), nên thử nghe lại sau ít giây.
                Log.w(TAG, "Không đọc được danh sách quán bị báo đóng cửa, thử lại sau", error)
                scope.launch {
                    delay(RETRY_MS)
                    if (myUid.value == uid) listen(uid)
                }
                return@addSnapshotListener
            }
            tallies.value = snapshot?.documents.orEmpty().associate { doc ->
                doc.id to ReportTally(
                    reporters = doc.uids(REPORTERS),
                    guests = doc.uids(GUESTS),
                    openers = doc.uids(OPENERS),
                )
            }
        }
    }

    override suspend fun reportClosed(place: Place): Result<Int> = write(place) { me, anonymous ->
        val field = if (anonymous) GUESTS else REPORTERS
        val ref = db.collection(COLLECTION).document(place.id)
        // Giao dịch: đọc rồi ghi cùng lúc, hai người báo cùng một quán một lúc vẫn đếm đúng.
        db.runTransaction { tx ->
            val doc = tx.get(ref)
            val already = (doc.get(field) as? List<*>).orEmpty().filterIsInstance<String>()
            val real = (doc.get(REPORTERS) as? List<*>).orEmpty().filterIsInstance<String>().size
            when {
                me in already -> real
                doc.exists() -> {
                    tx.update(ref, mapOf(field to FieldValue.arrayUnion(me), UPDATED_AT to FieldValue.serverTimestamp()))
                    if (anonymous) real else real + 1
                }
                else -> {
                    tx.set(
                        ref,
                        mapOf(
                            "name" to place.name.take(200),
                            "lat" to place.lat,
                            "lng" to place.lng,
                            REPORTERS to if (anonymous) emptyList() else listOf(me),
                            GUESTS to if (anonymous) listOf(me) else emptyList<String>(),
                            OPENERS to emptyList<String>(),
                            UPDATED_AT to FieldValue.serverTimestamp(),
                        ),
                    )
                    if (anonymous) 0 else 1
                }
            }
        }.awaitResult().also {
            // Ẩn quán ngay với mình, không chờ listener báo về.
            local(place.id) {
                if (anonymous) it.copy(guests = (it.guests + me).distinct())
                else it.copy(reporters = (it.reporters + me).distinct())
            }
        }
    }

    override suspend fun confirmOpen(place: Place): Result<Unit> = write(place) { me, _ ->
        val ref = db.collection(COLLECTION).document(place.id)
        ref.update(mapOf(OPENERS to FieldValue.arrayUnion(me), UPDATED_AT to FieldValue.serverTimestamp()))
            .awaitResult()
        local(place.id) { it.copy(openers = (it.openers + me).distinct()) }
    }

    /** Khung chung: lấy uid, chạy [block], đổi lỗi Firestore thành câu dễ hiểu. */
    private suspend fun <T> write(place: Place, block: suspend (String, Boolean) -> T): Result<T> = runCatching {
        val user = auth.currentUser ?: throw ReportClosedException("Bạn cần đăng nhập để báo quán đã đóng cửa.")
        block(user.uid, user.isAnonymous)
    }.recoverCatching { e ->
        Log.w(TAG, "Ghi báo cáo cho quán ${place.id} thất bại", e)
        throw when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                ReportClosedException("Máy chủ chưa cho phép báo quán (kiểm tra luật Firestore).", e)
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                ReportClosedException("Không kết nối được máy chủ, kiểm tra mạng rồi thử lại.", e)
            else -> e
        }
    }

    private fun local(placeId: String, edit: (ReportTally) -> ReportTally) {
        tallies.update { all -> all + (placeId to edit(all[placeId] ?: ReportTally())) }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.uids(field: String): List<String> =
        (get(field) as? List<*>).orEmpty().filterIsInstance<String>()

    private companion object {
        const val TAG = "ClosedReports"
        const val COLLECTION = "closed_reports"
        const val REPORTERS = "reporters"
        const val GUESTS = "guests"
        const val OPENERS = "openers"
        const val UPDATED_AT = "updatedAt"
        const val RETRY_MS = 15_000L
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
