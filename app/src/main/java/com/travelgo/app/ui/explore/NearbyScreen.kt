package com.travelgo.app.ui.explore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelgo.app.components.CenterHeader
import com.travelgo.app.ui.common.EmptyActionButton
import com.travelgo.app.ui.common.LoadStatus
import com.travelgo.app.ui.common.NO_NEARBY_TEXT
import com.travelgo.app.ui.common.dataOrNull
import com.travelgo.app.components.PlaceRowCard
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Muted

/**
 * 07 · Địa điểm gần bạn (Người 2, mục 21).
 * Khoảng 20 điểm gần nhất; bấm thẻ để chọn / bỏ chọn (tối đa 5, mở màn đã chọn sẵn 5 điểm gần nhất),
 * nút (i) trên thẻ mở Chi tiết địa điểm. Nút dưới cùng gửi id các điểm đã chọn sang màn Lộ trình (Tín).
 * Dữ liệu và lựa chọn nằm trong [NearbyViewModel]; màn hình chỉ hiển thị.
 */
@Composable
fun NearbyScreen(
    onBack: () -> Unit,
    onSmartRoute: (List<String>) -> Unit,
    onChangeLocation: () -> Unit,
    onOpenPlace: (String) -> Unit,
    viewModel: NearbyViewModel = viewModel { NearbyViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val me = state.me
    val places = state.places.dataOrNull.orEmpty()
    val selectedCount = state.selectedIds.size
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        CenterHeader(
            title = "Địa điểm gần bạn",
            onBack = onBack,
            actionIcon = Icons.Outlined.Map,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("${places.size} gợi ý dành cho bạn", color = Muted, fontSize = 15.sp)
                if (places.isNotEmpty()) {
                    Text(
                        "Đã chọn $selectedCount/$MAX_STOPS điểm cho lộ trình",
                        color = Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(BlueLight, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gần nhất", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LoadStatus(
                    state = state.places,
                    onRetry = viewModel::retry,
                    emptyText = NO_NEARBY_TEXT,
                    emptyAction = { EmptyActionButton("Chọn vị trí khác", onChangeLocation) },
                )
            }
            items(places, key = { it.id }) { place ->
                SelectablePlaceCard(
                    place = place,
                    distanceKm = me.distanceKmTo(place.location),
                    selected = place.id in state.selectedIds,
                    onToggle = {
                        if (!viewModel.toggle(place.id)) {
                            Toast.makeText(context, "Chỉ chọn tối đa $MAX_STOPS điểm", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenDetail = { onOpenPlace(place.id) },
                )
            }
        }
        PrimaryButton(
            text = when {
                places.isEmpty() -> "Lên lộ trình thông minh"
                state.canRoute -> "Lên lộ trình thông minh ($selectedCount điểm)"
                else -> "Chọn ít nhất $MIN_STOPS điểm"
            },
            icon = Icons.Outlined.NearMe,
            enabled = state.canRoute,
            onClick = { onSmartRoute(state.selectedInOrder) },
            modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        )
    }
}

/**
 * Thẻ địa điểm có ô chọn: bấm thẻ để chọn / bỏ chọn, thẻ đang chọn có viền xanh và dấu tick ở góc ảnh.
 * Nút (i) bên phải mở màn Chi tiết địa điểm.
 */
@Composable
private fun SelectablePlaceCard(
    place: Place,
    distanceKm: Double,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box {
        PlaceRowCard(
            place = place,
            subtitle = place.tagline,
            showPin = false,
            arrowDistance = true,
            distanceKm = distanceKm,
            onClick = onToggle,
            modifier = if (selected) Modifier.border(2.dp, Blue, shape) else Modifier,
            trailing = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onOpenDetail),
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = "Xem chi tiết", tint = Muted, modifier = Modifier.size(22.dp))
                }
            },
        )
        // Dấu tick đè lên góc trên bên trái ảnh.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 15.dp, top = 15.dp)
                .size(26.dp)
                .background(if (selected) Blue else Color.White.copy(alpha = 0.9f), CircleShape)
                .border(2.dp, if (selected) Blue else Border, CircleShape),
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Đã chọn", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
