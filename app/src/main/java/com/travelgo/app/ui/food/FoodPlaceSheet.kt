package com.travelgo.app.ui.food

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.travelgo.app.components.PlaceImage
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.ClosedReportRepository
import com.travelgo.app.data.place.OsmNotes
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.common.openPlaceInGoogleMaps
import com.travelgo.app.ui.theme.Danger
import com.travelgo.app.ui.theme.DangerLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/**
 * Chạm vào một quán ở Trang chủ / Khám phá thì hiện tấm này:
 * xem ảnh và đánh giá trên Google Maps, hoặc báo quán đã đóng cửa.
 * [place] = null thì không hiện gì.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPlaceSheet(
    place: Place?,
    distanceKm: Double?,
    onDismiss: () -> Unit,
    viewModel: FoodSheetViewModel = viewModel { FoodSheetViewModel() },
) {
    val context = LocalContext.current
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    // Báo xong (hoặc lỗi) thì hiện một dòng thông báo ngắn.
    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.messageShown()
        if (ui.reported) onDismiss()
    }

    if (place == null) return
    var confirming by remember(place.id) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaceImage(place, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(place.name, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(place.tagline, color = Muted, fontSize = 14.sp)
                    val where = listOfNotNull(place.address.ifBlank { null }, distanceKm?.let { "cách bạn ${formatKm(it)}" })
                    if (where.isNotEmpty()) Text(where.joinToString(" · "), color = Muted, fontSize = 13.sp)
                }
            }
            Text(
                "Quán lấy từ OpenStreetMap, chưa có ảnh và đánh giá. Xem trên Google Maps để biết quán còn mở cửa không.",
                color = Muted, fontSize = 13.sp,
            )
            PrimaryButton(
                text = "Xem ảnh, đánh giá trên Google Maps",
                icon = Icons.Outlined.Map,
                onClick = { openPlaceInGoogleMaps(context, place) },
            )

            if (!confirming) {
                TextButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Báo quán đã đóng cửa", color = Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DangerLight)
                        .padding(start = 14.dp, end = 4.dp, top = 12.dp),
                ) {
                    Text(
                        "Bạn chắc quán này đã đóng cửa? Quán sẽ ẩn với bạn ngay, và ẩn với mọi người khi có " +
                            "${ClosedReportRepository.HIDE_FOR_EVERYONE} người báo.",
                        color = Navy, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp),
                    )
                    if (OsmNotes.canReport(place)) {
                        // Ghi chú trên OSM là công khai nên người dùng phải tự tick, mặc định không gửi.
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = ui.shareToOsm,
                                onCheckedChange = viewModel::setShareToOsm,
                                enabled = !ui.sending,
                                colors = CheckboxDefaults.colors(checkedColor = Danger),
                            )
                            Text(
                                "Gửi kèm ghi chú công khai lên OpenStreetMap khi quán có từ " +
                                    "${ClosedReportRepository.HIDE_FOR_EVERYONE} người báo, để người vẽ bản đồ kiểm tra lại",
                                color = Navy, fontSize = 13.sp,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirming = false }, enabled = !ui.sending) {
                            Text("Huỷ", color = Muted, fontSize = 15.sp)
                        }
                        TextButton(onClick = { viewModel.reportClosed(place) }, enabled = !ui.sending) {
                            Text(
                                if (ui.sending) "Đang gửi…" else "Đúng, đã đóng cửa",
                                color = Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
