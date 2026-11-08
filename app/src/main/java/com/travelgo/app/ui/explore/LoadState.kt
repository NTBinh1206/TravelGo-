package com.travelgo.app.ui.explore

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** 3 trạng thái khi tải dữ liệu qua mạng. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Error : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
}

/** Kết quả tải + hàm "Thử lại". */
class Loader<T>(val state: LoadState<T>, val retry: () -> Unit) {
    /** Dữ liệu nếu đã tải xong, ngược lại null. */
    val dataOrNull: T? get() = (state as? LoadState.Ready<T>)?.data
}

/** Giữ nguyên trạng thái (đang tải / lỗi) nhưng thay dữ liệu bằng [data], ví dụ danh sách sau khi lọc. */
fun <R> Loader<*>.withData(data: R): Loader<R> = Loader(
    when (state) {
        is LoadState.Ready<*> -> LoadState.Ready(data)
        LoadState.Loading -> LoadState.Loading
        LoadState.Error -> LoadState.Error
    },
    retry,
)

/**
 * Tải dữ liệu bằng [load], tự tải lại khi một trong [keys] đổi hoặc khi gọi retry().
 * Lỗi mạng không làm app văng mà chuyển sang LoadState.Error.
 */
@Composable
fun <T> rememberLoader(vararg keys: Any?, load: suspend () -> T): Loader<T> {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<LoadState<T>>(LoadState.Loading, *keys, attempt) {
        value = LoadState.Loading
        value = try {
            LoadState.Ready(load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TravelGo", "Tải dữ liệu lỗi", e)
            LoadState.Error
        }
    }
    return Loader(state) { attempt++ }
}

/**
 * Hiện vòng xoay khi đang tải, thông báo + nút "Thử lại" khi lỗi,
 * [emptyText] khi tải xong mà danh sách rỗng. Có dữ liệu thì không hiện gì.
 */
@Composable
fun <T> LoadStatus(
    loader: Loader<List<T>>,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    val box = modifier.fillMaxWidth().padding(vertical = 24.dp)
    when (val s = loader.state) {
        LoadState.Loading -> Column(box, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Blue, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        }
        LoadState.Error -> Message(
            icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Muted, modifier = Modifier.size(36.dp)) },
            title = "Không tải được dữ liệu",
            text = "Kiểm tra kết nối mạng rồi thử lại.",
            action = { TextButton(onClick = loader.retry) { Text("Thử lại", color = Blue, fontWeight = FontWeight.SemiBold) } },
            modifier = box,
        )
        is LoadState.Ready -> if (s.data.isEmpty()) {
            Message(
                icon = { Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = Muted, modifier = Modifier.size(36.dp)) },
                title = null,
                text = emptyText,
                action = null,
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
