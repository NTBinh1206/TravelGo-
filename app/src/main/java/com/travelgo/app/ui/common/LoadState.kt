package com.travelgo.app.ui.common

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 3 trạng thái khi tải dữ liệu. ViewModel giữ trạng thái này, màn hình chỉ việc hiển thị. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Error : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
}

/** Dữ liệu nếu đã tải xong, ngược lại null. */
val <T> LoadState<T>.dataOrNull: T? get() = (this as? LoadState.Ready<T>)?.data

/** Giữ nguyên trạng thái (đang tải / lỗi) nhưng đổi dữ liệu, ví dụ danh sách sau khi lọc. */
inline fun <T, R> LoadState<T>.map(transform: (T) -> R): LoadState<R> = when (this) {
    is LoadState.Ready -> LoadState.Ready(transform(data))
    LoadState.Loading -> LoadState.Loading
    LoadState.Error -> LoadState.Error
}

/**
 * Chạy [load] thành một luồng: phát Loading, rồi Ready(kết quả) hoặc Error nếu lỗi.
 * Lỗi mạng không làm app văng mà chuyển sang LoadState.Error để hiện nút "Thử lại".
 */
fun <T> loadFlow(load: suspend () -> T): Flow<LoadState<T>> = flow {
    emit(LoadState.Loading)
    val result = try {
        LoadState.Ready(load())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("TravelGo", "Tải dữ liệu lỗi", e)
        LoadState.Error
    }
    emit(result)
}

/**
 * Hiện vòng xoay khi đang tải, thông báo + nút "Thử lại" khi lỗi,
 * [emptyText] (kèm [emptyAction] nếu có) khi tải xong mà danh sách rỗng. Có dữ liệu thì không hiện gì.
 */
@Composable
fun <T> LoadStatus(
    state: LoadState<List<T>>,
    onRetry: () -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    emptyAction: (@Composable () -> Unit)? = null,
) {
    val box = modifier.fillMaxWidth().padding(vertical = 24.dp)
    when (state) {
        LoadState.Loading -> Column(box, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Blue, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        }
        LoadState.Error -> Message(
            icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Muted, modifier = Modifier.size(36.dp)) },
            title = "Không tải được dữ liệu",
            text = "Kiểm tra kết nối mạng rồi thử lại.",
            action = { TextButton(onClick = onRetry) { Text("Thử lại", color = Blue, fontWeight = FontWeight.SemiBold) } },
            modifier = box,
        )
        is LoadState.Ready -> if (state.data.isEmpty()) {
            Message(
                icon = { Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = Muted, modifier = Modifier.size(36.dp)) },
                title = null,
                text = emptyText,
                action = emptyAction,
                modifier = box,
            )
        }
    }
}

@Composable
private fun Message(
    icon: @Composable () -> Unit,
    title: String?,
    text: String,
    action: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        if (title != null) Text(title, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** Nút chữ dùng cho [LoadStatus] khi danh sách trống, ví dụ "Chọn vị trí khác". */
@Composable
fun EmptyActionButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, color = Blue, fontWeight = FontWeight.SemiBold) }
}
