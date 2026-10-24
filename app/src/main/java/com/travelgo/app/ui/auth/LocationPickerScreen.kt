package com.travelgo.app.ui.auth

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.CenterHeader
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.location.noAccent
import com.travelgo.app.data.location.vietnamProvinces
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.theme.Background
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Danger
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/**
 * Chọn vị trí (Người 1): dùng GPS hoặc tự chọn tỉnh thành.
 * Mở từ màn 02 (khi từ chối quyền / "Để sau") và khi bấm vào vị trí ở Trang chủ.
 */
@Composable
fun LocationPickerScreen(onBack: () -> Unit, onPicked: () -> Unit) {
    val provider = AppContainer.locationProvider
    val currentName by provider.placeName.collectAsState()
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val locator = rememberGpsLocator { result ->
        message = when (result) {
            GpsResult.Success -> null
            GpsResult.PermissionDenied -> "Bạn chưa cho phép truy cập vị trí. Hãy chọn tỉnh thành bên dưới."
            GpsResult.GpsOff -> "Vị trí (GPS) đang tắt. Hãy bật lên hoặc chọn tỉnh thành bên dưới."
            is GpsResult.Failed -> result.message
        }
        if (result == GpsResult.Success) onPicked()
    }

    val list = remember(query) {
        val q = query.trim().noAccent()
        if (q.isEmpty()) vietnamProvinces else vietnamProvinces.filter { it.name.noAccent().contains(q) }
    }

    Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
        CenterHeader(title = "Chọn vị trí", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            // Thẻ "Dùng vị trí hiện tại"
            val cardShape = RoundedCornerShape(20.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(cardShape)
                    .background(Color.White, cardShape)
                    .clip(cardShape)
                    .clickable(enabled = !locator.isLoading) {
                        message = null
                        locator.start()
                    }
                    .padding(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).background(BlueLight, RoundedCornerShape(14.dp)),
                ) {
                    if (locator.isLoading) {
                        CircularProgressIndicator(color = Blue, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = Blue)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dùng vị trí hiện tại", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (locator.isLoading) "Đang lấy vị trí GPS…" else "Tự động định vị bằng GPS",
                        color = Muted,
                        fontSize = 13.sp,
                    )
                }
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Danger, fontSize = 13.sp, lineHeight = 18.sp)
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Tìm tỉnh, thành phố", color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Blue,
                    unfocusedBorderColor = Border,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Text("Tỉnh, thành phố", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
        }

        val listShape = RoundedCornerShape(20.dp)
        LazyColumn(
            contentPadding = PaddingValues(bottom = 12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .clip(listShape)
                .background(Color.White, listShape)
                .navigationBarsPadding(),
        ) {
            if (list.isEmpty()) {
                item {
                    Text("Không tìm thấy tỉnh thành phù hợp", color = Muted, modifier = Modifier.padding(16.dp))
                }
            }
            items(list, key = { it.name }) { province ->
                val selected = province.name == currentName
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            provider.updateLocation(province.center, province.name)
                            onPicked()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = if (selected) Blue else Muted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        province.name,
                        color = if (selected) Blue else Navy,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Icon(Icons.Outlined.Check, contentDescription = "Đang chọn", tint = Blue)
                }
                HorizontalDivider(color = Border.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
