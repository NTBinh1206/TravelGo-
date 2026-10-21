package com.travelgo.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.softShadow
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Danger
import com.travelgo.app.ui.theme.DangerLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/**
 * 05 · Cá nhân (Người 1).
 * TODO(Người 1): lấy tên, email, ảnh từ FirebaseAuth.currentUser;
 * đăng xuất bằng FirebaseAuth.signOut().
 */
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val name = "Nguyễn Minh Anh"
    val email = "minhanh@email.com"
    val initials = name.split(" ").takeLast(2).joinToString("") { it.take(1) }.uppercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Cá nhân", color = Navy, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(22.dp))

        val cardShape = RoundedCornerShape(24.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(cardShape)
                .background(Color.White, cardShape)
                .padding(vertical = 28.dp, horizontal = 16.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp).background(BlueLight, CircleShape)) {
                Text(initials, color = Blue, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(14.dp))
            Text(name, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(email, color = Muted, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))
        Text("TÀI KHOẢN & TÙY CHỌN", color = Muted, fontSize = 13.sp, letterSpacing = 0.3.sp)
        Spacer(Modifier.height(12.dp))

        val groupShape = RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().softShadow(groupShape).background(Color.White, groupShape).clip(groupShape)) {
            SettingRow(Icons.Outlined.Translate, "Ngôn ngữ", trailingText = "Tiếng Việt", onClick = {})
            HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(Icons.Outlined.VerifiedUser, "Chính sách", onClick = {})
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().softShadow(groupShape).background(Color.White, groupShape).clip(groupShape)) {
            SettingRow(Icons.AutoMirrored.Outlined.Logout, "Đăng xuất", danger = true, onClick = onLogout)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    danger: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(if (danger) DangerLight else BlueLight, RoundedCornerShape(12.dp)),
        ) {
            Icon(icon, contentDescription = null, tint = if (danger) Danger else Blue, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (danger) Danger else Navy,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(trailingText, color = Muted, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Muted)
    }
}
