package com.travelgo.app.ui.route

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Green
import com.travelgo.app.ui.theme.Muted
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * Bản đồ OpenStreetMap thật (thư viện osmdroid, không cần khoá API) có đường đi và điểm đánh số.
 * Thay cho ảnh bản đồ Đà Nẵng chụp sẵn trước đây (vẽ điểm ở tỉnh khác lên ảnh Đà Nẵng là sai).
 *
 * Bản đồ chỉ để xem: chạm/kéo trên bản đồ sẽ cuộn danh sách như bình thường.
 * Muốn chỉ đường chi tiết thì bấm "Bắt đầu dẫn đường" để mở Google Maps.
 */
@Composable
fun RouteMap(
    start: GeoPoint,
    stops: List<GeoPoint>,
    roundTrip: Boolean,
    durationText: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 18.0
        }
    }

    // Bản đồ cần biết lúc màn hình tạm dừng / tiếp tục để dừng tải ô bản đồ, và dọn dẹp khi rời màn.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { mapView },
            update = { map -> map.showRoute(context, start, stops, roundTrip) },
            modifier = Modifier.matchParentSize(),
        )
        // Lớp phủ trong suốt: nhận chạm thay bản đồ để danh sách bên ngoài vẫn cuộn được.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent()
                    }
                },
        )
        Text(
            durationText,
            color = Blue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.White, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        // Giấy phép dữ liệu OpenStreetMap (ODbL) yêu cầu ghi nguồn trên bản đồ.
        Text(
            "© OpenStreetMap contributors",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Vẽ lại đường đi + các điểm, rồi thu phóng cho vừa khung. */
private fun MapView.showRoute(context: Context, start: GeoPoint, stops: List<GeoPoint>, roundTrip: Boolean) {
    overlays.clear()
    val startPoint = OsmPoint(start.lat, start.lng)
    val stopPoints = stops.map { OsmPoint(it.lat, it.lng) }
    val path = listOf(startPoint) + stopPoints + if (roundTrip && stopPoints.isNotEmpty()) listOf(startPoint) else emptyList()

    if (path.size > 1) {
        overlays += Polyline(this).apply {
            setPoints(path)
            outlinePaint.color = Blue.toArgb()
            outlinePaint.strokeWidth = 5f * context.resources.displayMetrics.density
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            infoWindow = null
        }
    }
    stopPoints.forEachIndexed { i, p -> overlays += pointMarker(context, p, "${i + 1}", Blue, 30) }
    overlays += pointMarker(context, startPoint, "Bạn", Green, 38)

    val all = listOf(startPoint) + stopPoints
    val fit = {
        val box = BoundingBox.fromGeoPointsSafe(all)
        if (box.latitudeSpan < 0.002 && box.longitudeSpan < 0.002) {
            controller.setZoom(17.0)
            controller.setCenter(box.centerWithDateLine)
        } else {
            val margin = (36 * context.resources.displayMetrics.density).toInt()
            zoomToBoundingBox(box, false, margin)
        }
    }
    // Lúc mới tạo, bản đồ chưa có kích thước nên phải chờ lần bố cục đầu tiên rồi mới thu phóng.
    if (width > 0 && height > 0) fit() else addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
    invalidate()
}

private fun MapView.pointMarker(context: Context, point: OsmPoint, label: String, color: Color, sizeDp: Int) =
    Marker(this).apply {
        position = point
        setIcon(circleIcon(context, label, color, sizeDp))
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setInfoWindow(null)
    }

/** Vòng tròn màu viền trắng có chữ/số ở giữa, giống thiết kế Figma. */
private fun circleIcon(context: Context, label: String, color: Color, sizeDp: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val r = size / 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(r, r, r, paint)
    paint.color = color.toArgb()
    canvas.drawCircle(r, r, r - 2 * density, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textSize = if (label.length > 1) size * 0.32f else size * 0.45f
    canvas.drawText(label, r, r - (paint.descent() + paint.ascent()) / 2, paint)
    return BitmapDrawable(context.resources, bitmap)
}
