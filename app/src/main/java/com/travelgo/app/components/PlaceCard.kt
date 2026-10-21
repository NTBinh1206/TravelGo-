package com.travelgo.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy
import com.travelgo.app.ui.theme.StarYellow

/** Ảnh địa điểm (link Google Places hoặc ảnh mẫu trong app). */
@Composable
fun PlaceImage(place: Place, modifier: Modifier = Modifier) {
    AsyncImage(
        model = place.imageUrl.ifBlank { null },
        contentDescription = place.name,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(BlueLight),
    )
}

@Composable
fun RatingText(rating: Double, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = StarYellow, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text("$rating", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Card ngang: ảnh bên trái, tên, mô tả, sao, khoảng cách (màn 04, 06, 07).
 * [showPin]: hiện icon ghim trước mô tả. [arrowDistance]: hiện icon mũi tên trước km.
 */
@Composable
fun PlaceRowCard(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = place.address,
    distanceKm: Double? = null,
    showPin: Boolean = true,
    arrowDistance: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(shape)
            .background(Color.White, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(9.dp),
    ) {
        PlaceImage(place, Modifier.size(width = 94.dp, height = 86.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).height(86.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    Text(
                        place.name,
                        color = Navy,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showPin) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(subtitle, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                trailing?.invoke()
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 12.dp)) {
                RatingText(place.rating)
                Spacer(Modifier.weight(1f))
                if (distanceKm != null) {
                    if (arrowDistance) {
                        Icon(Icons.Outlined.NearMe, contentDescription = null, tint = Blue, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(formatKm(distanceKm), color = Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Card dọc: ảnh lớn phía trên, nút tim, tên và sao bên dưới (Trang chủ, dạng lưới). */
@Composable
fun PlaceTileCard(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = place.address,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .width(160.dp)
            .softShadow(shape)
            .background(Color.White, shape)
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        Box {
            PlaceImage(place, Modifier.fillMaxWidth().height(138.dp))
            FavoriteButton(
                bordered = false,
                size = 40.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            )
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name,
                    color = Navy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RatingText(place.rating)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(subtitle, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
