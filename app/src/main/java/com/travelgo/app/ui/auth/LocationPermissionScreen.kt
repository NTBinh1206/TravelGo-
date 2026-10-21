package com.travelgo.app.ui.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.PersonPinCircle
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.R
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.components.SecondaryButton
import com.travelgo.app.components.softShadow
import com.travelgo.app.ui.theme.Background
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/**
 * 02 · Cho phép truy cập vị trí (Người 1).
 * TODO(Người 1):
 *  - Khi được cấp quyền: lấy GPS bằng FusedLocationProviderClient,
 *    gọi AppContainer.locationProvider.updateLocation(...)
 *  - Khi bị từ chối: cho người dùng tự chọn tỉnh thành hoặc nhập địa chỉ
 */
@Composable
fun LocationPermissionScreen(onDone: (granted: Boolean) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        onDone(result.values.any { it })
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        Box(Modifier.fillMaxWidth().height(290.dp)) {
            Image(
                painter = painterResource(R.drawable.img_permission_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(Color.White, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Khám phá quanh bạn", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mỗi hành trình bắt đầu\ntừ nơi bạn đứng",
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                )
            }
        }

        Column(Modifier.weight(1f).padding(horizontal = 20.dp).padding(top = 22.dp)) {
            Text("Cho phép truy cập vị trí", color = Navy, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "TravelGo dùng vị trí để cá nhân hóa gợi ý. Bạn có thể thay đổi quyền này bất cứ lúc nào.",
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(18.dp))
            val cardShape = RoundedCornerShape(24.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cardShape)
                    .background(Color.White, cardShape)
                    .padding(16.dp),
            ) {
                Benefit(Icons.Outlined.NearMe, "Gợi ý gần bạn", "Khám phá địa điểm phù hợp ngay quanh vị trí hiện tại.")
                Benefit(Icons.Outlined.Route, "Khoảng cách chính xác", "Xem quãng đường và thời gian di chuyển thực tế.")
                Benefit(Icons.Outlined.PersonPinCircle, "Dẫn đường thuận tiện", "Bắt đầu lộ trình nhanh hơn khi bạn sẵn sàng.")
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "Cho phép",
                icon = Icons.Outlined.NearMe,
                onClick = {
                    launcher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Để sau", onClick = { onDone(false) })
            Spacer(Modifier.navigationBarsPadding().height(14.dp))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(BlueLight, RoundedCornerShape(14.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
