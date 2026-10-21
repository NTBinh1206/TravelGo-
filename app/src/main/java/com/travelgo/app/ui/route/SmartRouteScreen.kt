package com.travelgo.app.ui.route

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.R
import com.travelgo.app.components.CenterHeader
import com.travelgo.app.components.PlaceImage
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.components.RatingText
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import com.travelgo.app.domain.route.RouteMode
import com.travelgo.app.domain.route.RouteResult
import com.travelgo.app.domain.route.findBestRoute
import com.travelgo.app.domain.route.legMinutes
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Green
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy
import com.travelgo.app.ui.theme.SurfaceSoft

/**
 * 08 · Lộ trình thông minh (Người 3).
 * TODO(Người 3): thay ảnh bản đồ bằng Maps Compose, kéo thả đổi thứ tự,
 * mở Google Maps dẫn đường bằng Intent.
 */
@Composable
fun SmartRouteScreen(placeIds: List<String>, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(RouteMode.SHORTEST) }
    val places by produceState(initialValue = emptyList<Place>(), placeIds) {
        value = AppContainer.placeRepository.getPlacesByIds(placeIds)
    }
    val start = AppContainer.locationProvider.currentLocation.value ?: GeoPoint.DEFAULT
    val route = findBestRoute(start, places, mode)

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
                    Text("Gợi ý bởi TravelGo", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
            }
            item {
                RouteMapPreview(start = start, route = route, modifier = Modifier.fillMaxWidth().height(184.dp))
                Spacer(Modifier.height(14.dp))
            }
            item {
                StatsCard(route)
                Spacer(Modifier.height(14.dp))
            }
            item {
                ModeSelector(mode = mode, onChange = { mode = it })
                Spacer(Modifier.height(14.dp))
            }
            item {
                TipBox(mode)
                Spacer(Modifier.height(14.dp))
            }
            item { StartCard() }
            itemsIndexed(route.order, key = { _, p -> p.id }) { index, place ->
                val km = route.legKm.getOrNull(index) ?: 0.0
                LegConnector(km)
                TimelineStepCard(number = index + 1, place = place)
            }
        }
        Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
            HorizontalDivider(color = Border)
            PrimaryButton(
                text = "Bắt đầu dẫn đường",
                icon = Icons.Outlined.NearMe,
                onClick = { /* TODO(Người 3): mở Google Maps bằng Intent */ },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Chỉnh sửa điểm", color = Blue, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Ảnh bản đồ mẫu + đường đi và marker đánh số vẽ đè lên. */
@Composable
private fun RouteMapPreview(start: GeoPoint, route: RouteResult, modifier: Modifier = Modifier) {
    val points = listOf(start) + route.order.map { it.location }
    val minLat = points.minOf { it.lat }; val maxLat = points.maxOf { it.lat }
    val minLng = points.minOf { it.lng }; val maxLng = points.maxOf { it.lng }
    val pad = 0.14f
    fun nx(p: GeoPoint) = if (maxLng == minLng) 0.5f else pad + ((p.lng - minLng) / (maxLng - minLng)).toFloat() * (1 - 2 * pad)
    fun ny(p: GeoPoint) = if (maxLat == minLat) 0.5f else pad + ((maxLat - p.lat) / (maxLat - minLat)).toFloat() * (1 - 2 * pad)

    BoxWithConstraints(modifier.clip(RoundedCornerShape(20.dp))) {
        Image(
            painter = painterResource(R.drawable.img_route_map),
            contentDescription = "Bản đồ lộ trình",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = Offset(nx(p) * size.width, ny(p) * size.height)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(path, Blue, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        val w = maxWidth; val h = maxHeight
        points.forEachIndexed { i, p ->
            val isStart = i == 0
            val d = if (isStart) 38.dp else 30.dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = w * nx(p) - d / 2, y = h * ny(p) - d / 2)
                    .size(d)
                    .background(if (isStart) Green else Blue, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            ) {
                Text(
                    if (isStart) "Bạn" else "$i",
                    color = Color.White,
                    fontSize = if (isStart) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            "${route.totalMinutes} phút",
            color = Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
        StatBox("Di chuyển", "${route.totalMinutes} phút", Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        StatBox("Điểm dừng", "${route.order.size} điểm", Modifier.weight(1f))
    }
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
        RouteMode.BEST_RATED -> "Ưu tiên ghé các điểm được đánh giá cao trước, quãng đường dài hơn một chút."
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
private fun StartCard() {
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
            Text("Vị trí của bạn", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Bắt đầu hành trình", color = Muted, fontSize = 13.sp)
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
        Text("${formatKm(km)} · ${legMinutes(km)} phút", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimelineStepCard(number: Int, place: Place) {
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
                RatingText(place.rating)
            }
            Text(place.tagline, color = Muted, fontSize = 13.sp, maxLines = 1)
            Text("tham quan ~${place.stayMinutes} phút", color = Blue, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).background(SurfaceSoft, CircleShape)) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = "Kéo để đổi thứ tự", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}
