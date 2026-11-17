package com.travelgo.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.OutdoorGrill
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.travelgo.app.data.model.formatKm
import com.travelgo.app.data.place.FoodPlaces
import com.travelgo.app.data.place.Place
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Danger
import com.travelgo.app.ui.theme.DangerLight
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/** Từ độ nổi bật này trở lên thì hiện nhãn "Nổi bật" (khoảng 9% điểm trong bộ dữ liệu). */
const val FEATURED_SCORE = 4.0

/** Biểu tượng theo loại địa điểm, dùng khi chưa có ảnh. */
fun categoryIcon(category: String): ImageVector = when (category) {
    "bien" -> Icons.Outlined.Waves
    "nui" -> Icons.Outlined.Terrain
    "van_hoa" -> Icons.Outlined.AccountBalance
    "thanh_pho" -> Icons.Outlined.Apartment
    "thien_nhien" -> Icons.Outlined.Park
    FoodPlaces.QUAN_AN -> Icons.Outlined.Restaurant
    FoodPlaces.QUAN_NUOC -> Icons.Outlined.LocalCafe
    else -> Icons.Outlined.Place
}

/**
 * Ảnh địa điểm (Wikipedia/Wikimedia hoặc ảnh có sẵn trong app).
 * Chưa có ảnh hoặc tải ảnh lỗi thì vẫn thấy biểu tượng theo loại, không còn ô trống trơn.
 */
@Composable
fun PlaceImage(place: Place, modifier: Modifier = Modifier) {
    if (FoodPlaces.isFood(place.category) && place.imageUrl.isBlank()) {
        DishArt(place, modifier)
        return
    }
    Box(modifier.background(BlueLight), contentAlignment = Alignment.Center) {
        Icon(
            categoryIcon(place.category),
            contentDescription = null,
            tint = Blue.copy(alpha = 0.55f),
            modifier = Modifier.size(30.dp),
        )
        if (place.imageUrl.isNotBlank()) {
            AsyncImage(
                model = place.imageUrl,
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * Hình minh hoạ cho quán ăn uống: OpenStreetMap không có ảnh quán, nên vẽ biểu tượng món đoán từ tên quán
 * (trà sữa, phở, lẩu nướng…) trên nền màu riêng của món. Không lấy ảnh từ Google vì điều khoản không cho phép.
 */
@Composable
private fun DishArt(place: Place, modifier: Modifier) {
    val dish = FoodPlaces.dishOf(place.name, place.tagline, place.category)
    val (icon, color) = dishStyle(dish)
    BoxWithConstraints(
        modifier.background(Brush.linearGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.38f)))),
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(side * 0.46f))
    }
}

private fun dishStyle(dish: FoodPlaces.Dish): Pair<ImageVector, Color> = when (dish) {
    FoodPlaces.Dish.TRA_SUA -> Icons.Outlined.EmojiFoodBeverage to Color(0xFF8D5A97)
    FoodPlaces.Dish.CA_PHE -> Icons.Outlined.LocalCafe to Color(0xFF7B5236)
    FoodPlaces.Dish.BANH_NGOT -> Icons.Outlined.Cake to Color(0xFFC2457A)
    FoodPlaces.Dish.KEM -> Icons.Outlined.Icecream to Color(0xFFD9667E)
    FoodPlaces.Dish.PIZZA -> Icons.Outlined.LocalPizza to Color(0xFFD0571F)
    FoodPlaces.Dish.BURGER -> Icons.Outlined.LunchDining to Color(0xFFB7791F)
    FoodPlaces.Dish.GA_RAN -> Icons.Outlined.Fastfood to Color(0xFFC0392B)
    FoodPlaces.Dish.CHAY -> Icons.Outlined.Eco to Color(0xFF2E8B57)
    FoodPlaces.Dish.LAU_NUONG -> Icons.Outlined.OutdoorGrill to Color(0xFFB4441F)
    FoodPlaces.Dish.HAI_SAN -> Icons.Outlined.SetMeal to Color(0xFF1E7FA8)
    FoodPlaces.Dish.BANH_MI -> Icons.Outlined.BakeryDining to Color(0xFFA9711F)
    FoodPlaces.Dish.PHO_BUN -> Icons.Outlined.RamenDining to Color(0xFFC6612A)
    FoodPlaces.Dish.COM -> Icons.Outlined.RiceBowl to Color(0xFF7A8B2B)
    FoodPlaces.Dish.NUOC -> Icons.Outlined.LocalDrink to Color(0xFF2A8C9A)
    FoodPlaces.Dish.QUAN_AN -> Icons.Outlined.Restaurant to Color(0xFF3F6FB5)
}

/**
 * Nhãn "Nổi bật" cho điểm có độ nổi bật cao.
 * Thay cho ngôi sao + số trước đây: dữ liệu OpenStreetMap không có điểm đánh giá của người dùng,
 * hiện sao sẽ khiến người xem tưởng là đánh giá thật.
 */
@Composable
fun ScoreBadge(score: Double, modifier: Modifier = Modifier) {
    if (score < FEATURED_SCORE) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(BlueLight, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Blue, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Nổi bật", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Nhãn "Có thể đã đóng cửa": quán có đủ người báo (xem ClosedReportRepository) không bị xoá
 * mà chỉ mờ đi, xếp cuối và mang nhãn này, để báo nhầm thì người khác vẫn thấy và xác nhận lại được.
 */
@Composable
fun ClosedBadge(modifier: Modifier = Modifier) {
    Text(
        "Có thể đã đóng cửa",
        color = Danger,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(DangerLight, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * Card ngang: ảnh bên trái, tên, mô tả, nhãn "Nổi bật", khoảng cách (màn 04, 06, 07).
 * [showPin]: hiện icon ghim trước mô tả. [arrowDistance]: hiện icon mũi tên trước km.
 * [flagged]: quán bị báo "có thể đã đóng cửa" → ảnh mờ đi, hiện nhãn đỏ.
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
    flagged: Boolean = false,
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
        PlaceImage(
            place,
            Modifier.size(width = 94.dp, height = 86.dp).clip(RoundedCornerShape(14.dp)).alpha(if (flagged) 0.45f else 1f),
        )
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
                if (flagged) ClosedBadge() else ScoreBadge(place.score)
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

/** Card dọc: ảnh lớn phía trên (kèm nhãn "Nổi bật"), nút tim, tên bên dưới (Trang chủ, dạng lưới). */
@Composable
fun PlaceTileCard(
    place: Place,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = place.address,
    flagged: Boolean = false,
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
            PlaceImage(place, Modifier.fillMaxWidth().height(138.dp).alpha(if (flagged) 0.45f else 1f))
            if (flagged) {
                ClosedBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
            } else {
                ScoreBadge(place.score, Modifier.align(Alignment.TopStart).padding(10.dp))
            }
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
