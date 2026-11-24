package com.travelgo.app.data.place

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
 *   reporters           uid TÀI KHOẢN THẬT (Google) đã báo đóng cửa
 *   guests              uid tài khoản Khách (ẩn danh) đã báo; không tính vào ngưỡng gắn nhãn
 *   openers             uid đã bấm "Quán vẫn mở cửa"
 *   viewers             uid tài khoản thật đã mở xem quán sau khi quán bị báo (quán đông người xem cần nhiều người báo hơn)
 *   updatedAt           lần ghi gần nhất (giờ máy chủ)
 * Mỗi uid chỉ được tính một lần; luật bảo vệ dữ liệu nằm ở file firestore.rules ở thư mục gốc project.
 *
 * Chống một người dùng nhiều tài khoản để phá, mỗi người dùng có một tài liệu `users/{uid}`:
 *   createdAt           lần đầu dùng TravelGo (giờ máy chủ); tài khoản thật phải đủ 1 ngày mới được báo, xác nhận
 *   windowStart, actions  khung 24 giờ hiện tại và số lần đã báo/xác nhận trong khung (tối đa 5)
 *   lastActionAt        lần báo/xác nhận gần nhất; luật Firestore bắt mỗi lần báo phải cập nhật trường này cùng lúc,
 *                       nên không ai báo được mà không bị trừ lượt
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

    override val isGuest: Boolean get() = auth.currentUser?.isAnonymous ?: true

    init {
        // Luật Firestore chỉ cho người đã đăng nhập (kể cả khách) đọc, nên đăng nhập / đăng xuất thì nghe lại.
        auth.addAuthStateListener { a ->
            val uid = a.currentUser?.uid
            if (uid != myUid.value || registration == null) listen(uid)
            // Tạo hồ sơ ngay khi đăng nhập: tuổi tài khoản tính từ lần đầu mở app, không phải từ lúc bấm báo.
            if (uid != null) scope.launch { runCatching { ensureProfile(uid) } }
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
                    viewers = doc.uids(VIEWERS),
                )
            }
        }
    }

    override suspend fun reportClosed(place: Place): Result<Int> = write(place) { me, anonymous ->
        val field = if (anonymous) GUESTS else REPORTERS
        val ref = db.collection(COLLECTION).document(place.id)
        val userRef = db.collection(USERS).document(me)
        // Giao dịch: đọc rồi ghi cùng lúc, hai người báo cùng một quán một lúc vẫn đếm đúng.
        // Ghi luôn cả hồ sơ người báo (trừ một lượt), luật Firestore kiểm tra hai việc này đi cùng nhau.
        db.runTransaction { tx ->
            val profile = tx.get(userRef) // Firestore bắt đọc hết rồi mới ghi
            val doc = tx.get(ref)
            val real = doc.uids(REPORTERS).size
            if (me in doc.uids(field)) return@runTransaction real // đã báo rồi: không ghi gì, không trừ lượt
            val spend = spendOneAction(profile, countsForFlag = !anonymous)
            if (doc.exists()) {
                tx.update(ref, mapOf(field to FieldValue.arrayUnion(me), UPDATED_AT to FieldValue.serverTimestamp()))
            } else {
                tx.set(
                    ref,
                    mapOf(
                        "name" to place.name.take(200),
                        "lat" to place.lat,
                        "lng" to place.lng,
                        REPORTERS to if (anonymous) emptyList() else listOf(me),
                        GUESTS to if (anonymous) listOf(me) else emptyList<String>(),
                        OPENERS to emptyList<String>(),
                        VIEWERS to emptyList<String>(),
                        UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
            }
            tx.update(userRef, spend)
            if (anonymous) real else real + 1
        }.awaitResult().also {
            // Ẩn quán ngay với mình, không chờ listener báo về.
            local(place.id) {
                if (anonymous) it.copy(guests = (it.guests + me).distinct())
                else it.copy(reporters = (it.reporters + me).distinct())
            }
        }
    }

    override suspend fun confirmOpen(place: Place): Result<Unit> = write(place) { me, anonymous ->
        // Khách tạo tài khoản mới mỗi lần đăng nhập, nên cũng không được tính khi gỡ nhãn.
        if (anonymous) throw ReportClosedException(ClosedReportRepository.GUEST_CANNOT_CONFIRM)
        val ref = db.collection(COLLECTION).document(place.id)
        val userRef = db.collection(USERS).document(me)
        db.runTransaction { tx ->
            val profile = tx.get(userRef)
            val doc = tx.get(ref)
            if (!doc.exists()) throw ReportClosedException(NOT_REPORTED)
            if (me in doc.uids(OPENERS)) return@runTransaction Unit // đã xác nhận rồi
            val spend = spendOneAction(profile, countsForFlag = true)
            tx.update(ref, mapOf(OPENERS to FieldValue.arrayUnion(me), UPDATED_AT to FieldValue.serverTimestamp()))
            tx.update(userRef, spend)
            Unit
        }.awaitResult()
        local(place.id) { it.copy(openers = (it.openers + me).distinct()) }
    }

    override suspend fun recordView(place: Place) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return // Khách đăng nhập lại là có tài khoản mới, đếm vào thì ai cũng tăng được
        val tally = tallies.value[place.id] ?: return // quán chưa ai báo: không lưu gì
        if (user.uid in tally.viewers) return
        runCatching {
            db.collection(COLLECTION).document(place.id).update(VIEWERS, FieldValue.arrayUnion(user.uid)).awaitResult()
            local(place.id) { it.copy(viewers = (it.viewers + user.uid).distinct()) }
        }.onFailure { Log.w(TAG, "Không ghi được lượt xem quán ${place.id}", it) }
    }

    /**
     * Kiểm tra hồ sơ trước khi báo/xác nhận và trả về phần cần ghi vào hồ sơ (trừ một lượt).
     * Đây là kiểm tra trước để báo lỗi dễ hiểu; luật Firestore kiểm tra lại trên máy chủ.
     */
    private fun spendOneAction(profile: DocumentSnapshot, countsForFlag: Boolean): Map<String, Any> {
        if (!profile.exists()) throw ReportClosedException(NO_PROFILE)
        val now = System.currentTimeMillis()
        val createdAt = profile.getTimestamp(CREATED_AT)?.toDate()?.time ?: now
        if (countsForFlag && !ReportGuards.oldEnough(createdAt, now)) throw ReportClosedException(ReportGuards.TOO_NEW)
        val windowStart = profile.getTimestamp(WINDOW_START)?.toDate()?.time ?: 0L
        val used = profile.getLong(ACTIONS)?.toInt() ?: 0
        val next = ReportGuards.nextCount(windowStart, used, now)
            ?: throw ReportClosedException(ReportGuards.LIMIT_REACHED)
        val change = mutableMapOf<String, Any>(ACTIONS to next, LAST_ACTION_AT to FieldValue.serverTimestamp())
        if (!ReportGuards.inWindow(windowStart, now)) change[WINDOW_START] = FieldValue.serverTimestamp()
        return change
    }

    /** Tạo hồ sơ `users/{uid}` nếu chưa có. Giờ tạo do máy chủ ghi, người dùng không sửa được. */
    private suspend fun ensureProfile(uid: String) {
        val ref = db.collection(USERS).document(uid)
        if (ref.get().awaitResult().exists()) return
        ref.set(
            mapOf(
                CREATED_AT to FieldValue.serverTimestamp(),
                WINDOW_START to FieldValue.serverTimestamp(),
                ACTIONS to 0,
            ),
        ).awaitResult()
    }

    /** Khung chung: lấy uid, chạy [block], đổi lỗi Firestore thành câu dễ hiểu. */
    private suspend fun <T> write(place: Place, block: suspend (String, Boolean) -> T): Result<T> = runCatching {
        val user = auth.currentUser ?: throw ReportClosedException("Bạn cần đăng nhập để báo quán đã đóng cửa.")
        // Lỗi ở đây (mạng, đã có hồ sơ…) bỏ qua: giao dịch bên dưới sẽ báo nếu thật sự chưa có hồ sơ.
        runCatching { ensureProfile(user.uid) }.onFailure { Log.w(TAG, "Chưa tạo được hồ sơ ${user.uid}", it) }
        block(user.uid, user.isAnonymous)
    }.recoverCatching { e ->
        Log.w(TAG, "Ghi báo cáo cho quán ${place.id} thất bại", e)
        // Lỗi do mình ném trong giao dịch có thể bị Firestore bọc lại: lấy câu giải thích bên trong ra.
        generateSequence(e) { it.cause }.filterIsInstance<ReportClosedException>().firstOrNull()?.let { throw it }
        throw when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                ReportClosedException(
                    "Máy chủ chưa cho phép: tài khoản cần dùng TravelGo từ 1 ngày và chưa hết lượt báo hôm nay.",
                    e,
                )
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                ReportClosedException("Không kết nối được máy chủ, kiểm tra mạng rồi thử lại.", e)
            else -> e
        }
    }

    private fun local(placeId: String, edit: (ReportTally) -> ReportTally) {
        tallies.update { all -> all + (placeId to edit(all[placeId] ?: ReportTally())) }
    }

    private fun DocumentSnapshot.uids(field: String): List<String> =
        (get(field) as? List<*>).orEmpty().filterIsInstance<String>()

    private companion object {
        const val TAG = "ClosedReports"
        const val COLLECTION = "closed_reports"
        const val REPORTERS = "reporters"
        const val GUESTS = "guests"
        const val OPENERS = "openers"
        const val VIEWERS = "viewers"
        const val UPDATED_AT = "updatedAt"
        const val USERS = "users"
        const val CREATED_AT = "createdAt"
        const val WINDOW_START = "windowStart"
        const val ACTIONS = "actions"
        const val LAST_ACTION_AT = "lastActionAt"
        const val RETRY_MS = 15_000L
        const val NO_PROFILE = "Chưa tạo được hồ sơ tài khoản trên máy chủ, kiểm tra mạng rồi thử lại."
        const val NOT_REPORTED = "Quán này chưa có ai báo đóng cửa."
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
