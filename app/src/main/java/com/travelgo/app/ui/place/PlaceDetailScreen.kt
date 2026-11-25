package com.travelgo.app.ui.place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelgo.app.components.CircleIconButton
import com.travelgo.app.components.FavoriteButton
import com.travelgo.app.components.PlaceImage
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.components.ScoreBadge
import com.travelgo.app.components.SecondaryButton
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.Place
import com.travelgo.app.domain.route.formatDuration
import com.travelgo.app.ui.common.LoadState
import com.travelgo.app.ui.common.openDirectionsInGoogleMaps
import com.travelgo.app.ui.common.openPlaceInGoogleMaps
import com.travelgo.app.ui.theme.Background
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/**
 * Màn Chi tiết địa điểm (Người 2): mở khi bấm vào thẻ điểm du lịch ở Trang chủ, Khám phá, Tìm kiếm, Gần bạn.
 * Ảnh lớn, tên, loại, khoảng cách, thời gian tham quan, giới thiệu Wikipedia, nút mở Google Maps / chỉ đường.
 */
@Composable
fun PlaceDetailScreen(
    viewModel: PlaceDetailViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = ui.place) {
        LoadState.Loading -> Box(Modifier.fillMaxSize().background(Background)) {
            CircularProgressIndicator(color = Blue, modifier = Modifier.align(Alignment.Center))
            BackButton(onBack, Modifier.statusBarsPadding().padding(16.dp))
        }
        LoadState.Error -> Box(Modifier.fillMaxSize().background(Background)) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Không mở được địa điểm này", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Có thể dữ liệu đã thay đổi hoặc mất mạng. Bạn thử lại nhé.",
                    color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TextButton(onClick = viewModel::retry) {
                    Text("Thử lại", color = Blue, fontWeight = FontWeight.SemiBold)
                }
            }
            BackButton(onBack, Modifier.statusBarsPadding().padding(16.dp))
        }
        is LoadState.Ready -> PlaceDetailContent(state.data, ui, onBack)
    }
}

@Composable
private fun PlaceDetailContent(place: Place, ui: PlaceDetailUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Ảnh lớn phía trên, nút quay lại và nút tim nổi trên ảnh.
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            PlaceImage(place, Modifier.fillMaxSize())
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BackButton(onBack)
                FavoriteButton(size = 44.dp, bordered = false)
            }
        }

        // Phần nội dung kéo đè lên mép dưới ảnh, bo góc trên.
        Column(
            Modifier
                .offset(y = (-24).dp)
                .fillMaxWidth()
                .background(Background, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(place.name, color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (place.tagline.isNotBlank()) {
                    Text(
                        place.tagline,
                        color = Blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(BlueLight, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                ScoreBadge(place.score)
            }

            if (place.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(place.address, color = Muted, fontSize = 14.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoBox(Icons.Outlined.NearMe, "Cách bạn", ui.distanceKm?.let { formatKm(it) } ?: "—", Modifier.weight(1f))
                InfoBox(
                    Icons.Outlined.Schedule, "Tham quan",
                    if (place.stayMinutes > 0) "~" + formatDuration(place.stayMinutes) else "—",
                    Modifier.weight(1f),
                )
                InfoBox(Icons.Outlined.Category, "Loại", categoryLabel(place.category), Modifier.weight(1f))
            }

            Text("Giới thiệu", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            when {
                ui.loadingDescription -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Blue, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Đang tải giới thiệu từ Wikipedia…", color = Muted, fontSize = 14.sp)
                }
                ui.description != null -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ui.description, color = Navy, fontSize = 15.sp, lineHeight = 22.sp)
                    Text("Nguồn: Wikipedia", color = Muted, fontSize = 12.sp)
                }
                else -> Text(
                    "Chưa có phần giới thiệu cho địa điểm này. Bạn có thể xem thêm ảnh và đánh giá trên Google Maps.",
                    color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }

            // Nói rõ "độ nổi bật" là gì để người xem không tưởng là điểm đánh giá của người dùng.
            Text(
                "Độ nổi bật ${"%.1f".format(place.score)}/5 do TravelGo tính từ dữ liệu OpenStreetMap " +
                    "(có bài Wikipedia, là di tích, cảnh quan…), không phải đánh giá của người dùng.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            )

            Spacer(Modifier.height(2.dp))
            PrimaryButton(
                text = "Xem trên Google Maps",
                icon = Icons.Outlined.Map,
                onClick = { openPlaceInGoogleMaps(context, place) },
            )
            SecondaryButton(text = "Chỉ đường", onClick = { openDirectionsInGoogleMaps(context, place, from = ui.myLocation) })
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Quay lại", onClick = onBack, modifier = modifier)
}

/** Ô nhỏ: biểu tượng + nhãn + giá trị (Cách bạn / Tham quan / Loại). */
@Composable
private fun InfoBox(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .softShadow(shape, elevation = 6.dp)
            .background(Color.White, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(20.dp))
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

/** Tên loại theo tiếng Việt, giống các chip ở màn Khám phá. */
private fun categoryLabel(category: String): String = when (category) {
    "bien" -> "Biển"
    "nui" -> "Núi"
    "van_hoa" -> "Văn hóa"
    "thanh_pho" -> "Thành phố"
    "thien_nhien" -> "Thiên nhiên"
    else -> "Khác"
}
