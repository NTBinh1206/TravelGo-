package com.travelgo.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.ui.theme.Navy

/** Header tiêu đề giữa, nút tròn quay lại bên trái, nút tuỳ chọn bên phải (màn 07, 08). */
@Composable
fun CenterHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp)) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            contentDescription = "Quay lại",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            title,
            color = Navy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        if (actionIcon != null) {
            CircleIconButton(
                icon = actionIcon,
                contentDescription = null,
                onClick = onAction,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/** Header tiêu đề bên trái, nút vuông quay lại (màn 06). */
@Composable
fun BackTitleHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
    ) {
        SquareIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            onClick = onBack,
            container = Color.White,
            tint = Navy,
            bordered = true,
        )
        Spacer(Modifier.width(14.dp))
        Text(title, color = Navy, fontSize = 23.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (actionIcon != null) {
            SquareIconButton(icon = actionIcon, contentDescription = null, onClick = onAction)
        }
    }
}
