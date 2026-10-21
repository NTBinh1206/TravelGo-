package com.travelgo.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/** Bóng mờ nhẹ như các card trong Figma. */
fun Modifier.softShadow(shape: Shape, elevation: Dp = 10.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = Navy.copy(alpha = 0.05f),
    spotColor = Navy.copy(alpha = 0.10f),
)

/** Nút icon tròn nền trắng có bóng (nút quay lại, nút bản đồ ở header). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .softShadow(CircleShape)
            .background(Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Navy, modifier = Modifier.size(22.dp))
    }
}

/** Nút icon vuông bo góc (nút bản đồ nền xanh nhạt, nút lọc...). */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = BlueLight,
    tint: Color = Blue,
    bordered: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (bordered) Modifier.border(1.dp, Border, shape) else Modifier)
            .background(container, shape)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Ô tìm kiếm dạng nút (bấm để mở màn tìm kiếm). */
@Composable
fun SearchBarButton(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .softShadow(shape)
            .background(Color.White, shape)
            .border(1.dp, Border, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 7.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(placeholder, color = Muted.copy(alpha = 0.8f), fontSize = 16.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Chip bo tròn: chọn thì nền xanh chữ trắng, không chọn thì nền trắng có viền. */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val content = if (selected) Color.White else Navy
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .then(if (selected) Modifier else Modifier.border(1.dp, Border, CircleShape))
            .background(if (selected) Blue else Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Nút tim tròn trên card. Bấm để bật/tắt yêu thích (TODO Người 2: lưu theo tài khoản). */
@Composable
fun FavoriteButton(modifier: Modifier = Modifier, size: Dp = 36.dp, bordered: Boolean = true) {
    var liked by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (bordered) Modifier.border(1.dp, Border, CircleShape) else Modifier)
            .background(Color.White, CircleShape)
            .clip(CircleShape)
            .clickable { liked = !liked },
    ) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Yêu thích",
            tint = if (bordered) Blue else Navy,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Tiêu đề mục kèm link "Xem tất cả". */
@Composable
fun SectionHeader(title: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Text(title, color = Navy, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = Blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}
