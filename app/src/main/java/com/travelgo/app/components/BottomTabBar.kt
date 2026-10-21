package com.travelgo.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.navigation.Routes
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Muted

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Trang chủ", Icons.Outlined.Home),
    TabItem(Routes.EXPLORE, "Khám phá", Icons.Outlined.Explore),
    TabItem(Routes.PROFILE, "Cá nhân", Icons.Outlined.Person),
)

/** Thanh tab dưới: vạch xanh phía trên mục đang chọn, như Figma. */
@Composable
fun BottomTabBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
        HorizontalDivider(color = Border)
        Row(Modifier.fillMaxWidth().height(70.dp)) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                val color = if (selected) Blue else Muted.copy(alpha = 0.75f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab.route) },
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(if (selected) Blue else Color.Transparent, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.height(9.dp))
                    Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        color = color,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
