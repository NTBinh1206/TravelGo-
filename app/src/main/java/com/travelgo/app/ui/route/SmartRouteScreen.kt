package com.travelgo.app.ui.route

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelgo.app.components.CenterHeader
import com.travelgo.app.components.PlaceImage
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.components.ScoreBadge
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.Place
import com.travelgo.app.domain.route.RouteMode
import com.travelgo.app.domain.route.RouteResult
import com.travelgo.app.domain.route.formatDuration
import com.travelgo.app.domain.route.legMinutes
import com.travelgo.app.ui.common.LoadStatus
import com.travelgo.app.ui.common.dataOrNull
import com.travelgo.app.ui.common.map
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Green
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy
import com.travelgo.app.ui.theme.SurfaceSoft

/**
 * 08 · Lộ trình thông minh (Người 3).
 * Task 30: đổi thứ tự điểm bằng nút mũi tên, tính lại km và phút (evaluateRoute).
 * Task 31: nút "Bắt đầu dẫn đường" mở Google Maps bằng Intent.
 * Bản đồ: OpenStreetMap (osmdroid), xem [RouteMap].
 * Tính toán và trạng thái nằm trong [SmartRouteViewModel]; màn hình chỉ hiển thị và báo sự kiện.
 */
@Composable
fun SmartRouteScreen(viewModel: SmartRouteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val route = state.route.dataOrNull
    val roundTrip = state.mode == RouteMode.ROUND_TRIP

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        CenterHeader(
            title = "Lộ trình thông minh",
            onBack = onBack,
            actionIcon = Icons.Outlined.MoreHoriz,
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(BlueLight, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.customOrder) "Thứ tự do bạn sắp xếp" else "Gợi ý bởi TravelGo",
                        color = Blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
            if (route == null) {
                item {
                    LoadStatus(
                        state = state.route.map { emptyList<Place>() },
                        onRetry = viewModel::retry,
                        emptyText = "",
                    )
                }
            } else {
                item {
                    RouteMap(
                        start = state.start,
                        stops = route.order.map { it.location },
                        roundTrip = roundTrip,
                        durationText = formatDuration(route.totalMinutes),
                        modifier = Modifier.fillMaxWidth().height(184.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                }
                item {
                    StatsCard(route)
                    Spacer(Modifier.height(14.dp))
                }
                item {
                    ModeSelector(mode = state.mode, onChange = viewModel::selectMode)
                    Spacer(Modifier.height(14.dp))
                }
                item {
                    TipBox(state.mode)
                    Spacer(Modifier.height(14.dp))
                }
                item { StartCard(title = "Vị trí của bạn", subtitle = "Bắt đầu hành trình") }
                itemsIndexed(route.order, key = { _, p -> p.id }) { index, place ->
                    LegConnector(route.legKm.getOrNull(index) ?: 0.0)
                    TimelineStepCard(
                        number = index + 1,
                        place = place,
                        onMoveUp = if (index > 0) ({ viewModel.moveUp(index) }) else null,
                        onMoveDown = if (index < route.order.lastIndex) ({ viewModel.moveDown(index) }) else null,
                    )
                }
                // Khứ hồi: hiện thêm chặng quay về (trước đây có tính vào tổng km nhưng không hiện trong danh sách).
                if (roundTrip && route.order.isNotEmpty()) {
                    item {
                        LegConnector(route.returnKm)
                        StartCard(title = "Về vị trí xuất phát", subtitle = "Kết thúc hành trình")
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
            HorizontalDivider(color = Border)
            PrimaryButton(
                text = "Bắt đầu dẫn đường",
                icon = Icons.Outlined.NearMe,
                enabled = !route?.order.isNullOrEmpty(),
                onClick = { route?.let { openGoogleMaps(context, state.start, it.order, roundTrip) } },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Chỉnh sửa điểm", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatsCard(route: RouteResult) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(shape)
            .background(Color.White, shape)
            .padding(12.dp),
    ) {
        StatBox("Quãng đường", formatKm(route.totalKm), Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        StatBox("Di chuyển", formatDuration(route.totalMinutes), Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        StatBox("Điểm dừng", "${route.order.size} điểm", Modifier.weight(1f))
    }
    // Cả chuyến = di chuyển + thời gian tham quan ước lượng ở từng điểm.
    Text(
        "Cả chuyến khoảng ${formatDuration(route.tripMinutes)} (gồm ${formatDuration(route.stayMinutes)} tham quan)",
        color = Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(SurfaceSoft, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Text(label, color = Muted, fontSize = 13.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ModeSelector(mode: RouteMode, onChange: (RouteMode) -> Unit) {
    Row(Modifier.fillMaxWidth().background(SurfaceSoft, RoundedCornerShape(16.dp)).padding(6.dp)) {
        RouteMode.entries.forEach { item ->
            val selected = item == mode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Blue else Color.Transparent)
                    .clickable { onChange(item) },
            ) {
                Text(
                    item.label,
                    color = if (selected) Color.White else Blue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TipBox(mode: RouteMode) {
    val tip = when (mode) {
        RouteMode.SHORTEST -> "Đi theo hướng từ gần đến xa, tránh quay đầu để tiết kiệm quãng đường."
        RouteMode.BEST_RATED -> "Ưu tiên ghé các điểm nổi bật trước, quãng đường có thể dài hơn một chút."
        RouteMode.ROUND_TRIP -> "Lộ trình quay về điểm xuất phát sau khi ghé điểm cuối."
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(SurfaceSoft, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(Color.White, RoundedCornerShape(12.dp)),
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = Blue)
        }
        Spacer(Modifier.width(12.dp))
        Text(tip, color = Navy, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun StartCard(title: String, subtitle: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().softShadow(shape).background(Color.White, shape).padding(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).background(Green, CircleShape)) {
            Text("Bạn", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
    }
}

/** Đường nét đứt giữa hai điểm, kèm km và phút của chặng. */
@Composable
private fun LegConnector(km: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 28.dp)) {
        Canvas(Modifier.width(2.dp).height(30.dp)) {
            drawLine(
                color = Blue.copy(alpha = 0.5f),
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = size.width,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text("${formatKm(km)} · ${formatDuration(legMinutes(km))}", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimelineStepCard(
    number: Int,
    place: Place,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier.fillMaxWidth().softShadow(shape).background(Color.White, shape).padding(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 4.dp).size(28.dp).background(Blue, CircleShape)) {
            Text("$number", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        PlaceImage(place, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name,
                    color = Navy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ScoreBadge(place.score)
            }
            Text(place.tagline, color = Muted, fontSize = 13.sp, maxLines = 1)
            Text("tham quan ~${formatDuration(place.stayMinutes)}", color = Blue, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MoveButton(Icons.Outlined.KeyboardArrowUp, "Đưa lên", onMoveUp)
            Spacer(Modifier.height(4.dp))
            MoveButton(Icons.Outlined.KeyboardArrowDown, "Đưa xuống", onMoveDown)
        }
    }
}

/** Nút mũi tên đổi thứ tự. onClick = null nghĩa là không đi được nữa (mờ đi). */
@Composable
private fun MoveButton(icon: ImageVector, description: String, onClick: (() -> Unit)?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SurfaceSoft)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (onClick != null) Blue else Border,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Mở Google Maps dẫn đường qua từng điểm theo thứ tự đang hiển thị.
 * Máy không có Google Maps lẫn trình duyệt thì báo bằng Toast thay vì để app văng.
 */
private fun openGoogleMaps(context: Context, start: GeoPoint, order: List<Place>, roundTrip: Boolean) {
    if (order.isEmpty()) return
    val stops = order.map { it.location }
    val destination = if (roundTrip) start else stops.last()
    val waypoints = (if (roundTrip) stops else stops.dropLast(1))
        .joinToString("%7C") { "${it.lat},${it.lng}" }
    val url = buildString {
        append("https://www.google.com/maps/dir/?api=1")
        append("&origin=${start.lat},${start.lng}")
        append("&destination=${destination.lat},${destination.lng}")
        if (waypoints.isNotEmpty()) append("&waypoints=$waypoints")
        append("&travelmode=driving")
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Máy chưa có Google Maps hoặc trình duyệt để dẫn đường.", Toast.LENGTH_LONG).show()
    }
}
