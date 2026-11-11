package com.travelgo.app.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.PillChip
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.components.SecondaryButton
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.theme.Navy
import kotlinx.coroutines.launch

/** Điều kiện lọc ở màn Tìm kiếm. Rỗng/null = không lọc theo tiêu chí đó. */
data class SearchFilter(
    val categories: Set<String> = emptySet(), // "bien", "nui"...; rỗng = mọi loại
    val maxDistanceKm: Int? = null,           // null = không giới hạn
) {
    /** Số tiêu chí đang bật, hiện trên nút "Bộ lọc · n". */
    val activeCount: Int
        get() = listOf(categories.isNotEmpty(), maxDistanceKm != null).count { it }
}

/** Giữ lại các điểm thỏa mọi điều kiện trong [criteria]. */
fun List<Place>.applyFilter(criteria: SearchFilter, me: GeoPoint): List<Place> = filter { p ->
    (criteria.categories.isEmpty() || p.category in criteria.categories) &&
        (criteria.maxDistanceKm == null || me.distanceKmTo(p.location) <= criteria.maxDistanceKm)
}

private val filterCategories = listOf(
    "bien" to "Biển",
    "nui" to "Núi",
    "thanh_pho" to "Thành phố",
    "van_hoa" to "Văn hóa",
    "thien_nhien" to "Thiên nhiên",
)

private val distanceOptions = listOf<Int?>(null, 5, 10, 25, 50)

/**
 * Bảng lọc trượt lên từ đáy màn hình (màn 06).
 * Người dùng chỉnh trên bản nháp, bấm "Áp dụng" mới gửi về qua [onApply].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    current: SearchFilter,
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Bộ lọc", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            SectionTitle("Loại địa điểm")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                filterCategories.forEach { (key, label) ->
                    val selected = key in draft.categories
                    PillChip(
                        label = label,
                        selected = selected,
                        onClick = {
                            draft = draft.copy(
                                categories = if (selected) draft.categories - key else draft.categories + key,
                            )
                        },
                    )
                }
            }

            SectionTitle("Khoảng cách tối đa")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                distanceOptions.forEach { km ->
                    PillChip(
                        label = if (km == null) "Không giới hạn" else "$km km",
                        selected = draft.maxDistanceKm == km,
                        onClick = { draft = draft.copy(maxDistanceKm = km) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Đặt lại", onClick = { draft = SearchFilter() }, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Áp dụng",
                    onClick = {
                        // Cho bảng trượt xuống hết rồi mới gỡ khỏi màn hình,
                        // nếu gỡ ngay thì lần bấm "Bộ lọc" kế tiếp hay bị nuốt mất.
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onApply(draft) }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Navy,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
    )
}
