package com.travelgo.app.ui.explore

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.route.chosenServer
import com.travelgo.app.ui.route.circleIcon
import com.travelgo.app.ui.route.pickTileServer
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Green
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * Bản đồ các điểm ở màn Khám phá (nút "Bản đồ", Người 2).
 * OpenStreetMap qua osmdroid như bản đồ Lộ trình, nhưng kéo / chụm hai ngón để phóng to được.
 * Mỗi điểm là một chấm đánh số theo thứ tự trong danh sách; chấm xanh lá là vị trí của bạn.
 * Chạm chấm thì [onSelect] nhận id điểm đó, chạm chỗ trống thì nhận null.
 */
@Composable
fun PlacesMap(
    me: GeoPoint,
    places: List<Place>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val select by rememberUpdatedState(onSelect)
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
        }
    }

    var server by remember { mutableStateOf(chosenServer) }
    LaunchedEffect(Unit) {
        if (server == null) server = pickTileServer()
    }

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

    // Chỉ thu phóng lại khi danh sách điểm đổi (đổi loại, đổi vị trí), không phải mỗi lần chọn một chấm.
    val fitted = remember { FitMemory() }

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { mapView },
            update = { map ->
                server?.let { if (map.tileProvider.tileSource != it.source) map.setTileSource(it.source) }
                map.showPlaces(me, places, selectedId) { select(it) }
                val ids = places.map { it.id }
                if (ids != fitted.ids) {
                    fitted.ids = ids
                    map.fitTo(listOf(me) + places.map { it.location })
                }
            },
            modifier = Modifier.matchParentSize(),
        )
        // Giấy phép dữ liệu OpenStreetMap và máy chủ ô bản đồ yêu cầu ghi nguồn trên bản đồ.
        Text(
            server?.attribution ?: "Đang tải bản đồ…",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Nhớ danh sách đã thu phóng theo (không phải state: đổi nó không cần vẽ lại màn hình). */
private class FitMemory {
    var ids: List<String>? = null
}

/** Vẽ lại các chấm: điểm đang chọn to hơn, màu đậm hơn và nằm trên cùng. */
@SuppressLint("ClickableViewAccessibility")
private fun MapView.showPlaces(me: GeoPoint, places: List<Place>, selectedId: String?, onSelect: (String?) -> Unit) {
    overlays.clear()
    // Chạm chỗ trống trên bản đồ: bỏ chọn.
    overlays += MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: OsmPoint?): Boolean {
            onSelect(null)
            return true
        }

        override fun longPressHelper(p: OsmPoint?): Boolean = false
    })
    overlays += marker(OsmPoint(me.lat, me.lng), circleIcon(context, "Bạn", Green, 34)) { }
    val ordered = places.withIndex().sortedBy { it.value.id == selectedId } // điểm đang chọn vẽ sau cùng
    ordered.forEach { (i, place) ->
        val selected = place.id == selectedId
        val icon = circleIcon(context, "${i + 1}", if (selected) Navy else Blue, if (selected) 36 else 28)
        overlays += marker(OsmPoint(place.lat, place.lng), icon) { onSelect(place.id) }
    }
    invalidate()
}

private fun MapView.marker(point: OsmPoint, icon: android.graphics.drawable.Drawable, onClick: () -> Unit) =
    Marker(this).apply {
        position = point
        setIcon(icon)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ ->
            onClick()
            true
        }
    }

/** Thu phóng cho vừa mọi điểm; chỉ một điểm thì phóng cỡ đường phố. */
private fun MapView.fitTo(points: List<GeoPoint>) {
    val osm = points.map { OsmPoint(it.lat, it.lng) }
    val fit = {
        val box = BoundingBox.fromGeoPointsSafe(osm)
        if (box.latitudeSpan < 0.002 && box.longitudeSpanWithDateLine < 0.002) {
            controller.setZoom(16.0)
            controller.setCenter(box.centerWithDateLine)
        } else {
            zoomToBoundingBox(box, false, (40 * context.resources.displayMetrics.density).toInt())
        }
    }
    // Lúc mới tạo, bản đồ chưa có kích thước: chờ lần bố cục đầu tiên rồi mới thu phóng.
    if (width > 0 && height > 0) fit() else addOnFirstLayoutListener { _, _, _, _, _ -> fit() }
}
