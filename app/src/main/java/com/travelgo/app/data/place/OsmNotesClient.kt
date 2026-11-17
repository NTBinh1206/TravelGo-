package com.travelgo.app.data.place

import android.util.Log
import com.travelgo.app.data.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody

/**
 * Gửi ghi chú lên API ghi chú của OpenStreetMap (ẩn danh, không cần tài khoản).
 *
 * - Trước khi gửi, tìm trong ô khoảng 110 m quanh quán xem đã có ghi chú đang mở nhắc tới quán chưa,
 *   để nhiều người báo cùng một quán không tạo nhiều ghi chú trùng nhau.
 * - OSM giới hạn số ghi chú ẩn danh và có thể tạm chặn ghi chú ẩn danh ở một số vùng; khi bị từ chối,
 *   quán vẫn được ẩn trong app như bình thường, chỉ có ghi chú là không gửi được.
 */
class OsmNotesClient : OsmNoteSender {

    override suspend fun reportClosed(place: Place, reporters: Int): Result<NoteOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(OsmNotes.canReport(place)) { "Quán không có mã OpenStreetMap" }
                val existing = OsmNotes.findExisting(Http.get(OsmNotes.searchUrl(place)), place)
                if (existing != null) {
                    NoteOutcome.AlreadyOpen(existing)
                } else {
                    val form = FormBody.Builder().apply {
                        OsmNotes.createForm(place, reporters).forEach { (key, value) -> add(key, value) }
                    }.build()
                    val id = OsmNotes.parseCreatedId(Http.post(OsmNotes.API, form)) ?: error("Không đọc được id ghi chú")
                    NoteOutcome.Created(id)
                }
            }.onSuccess { Log.i(TAG, "Ghi chú OSM cho ${place.id}: $it") }
                .onFailure { Log.w(TAG, "Không gửi được ghi chú OSM cho ${place.id}: ${it.message}") }
        }

    private companion object {
        const val TAG = "OsmNotes"
    }
}
