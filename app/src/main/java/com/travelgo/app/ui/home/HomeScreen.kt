package com.travelgo.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.R
import com.travelgo.app.components.PlaceTileCard
import com.travelgo.app.components.SearchBarButton
import com.travelgo.app.components.SectionHeader
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/** 03 · Trang chủ (Người 2). */
@Composable
fun HomeScreen(onOpenSearch: () -> Unit, onOpenExplore: () -> Unit, onOpenNearby: () -> Unit) {
    val featured by produceState(initialValue = emptyList<Place>()) {
        value = AppContainer.placeRepository.getFeaturedDestinations()
    }
    val nearby by produceState(initialValue = emptyList<Place>()) {
        // TODO(Người 2): dùng vị trí thật từ AppContainer.locationProvider
        value = AppContainer.placeRepository.getNearbyPlaces(GeoPoint.DEFAULT)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Vị trí hiện tại", color = Muted, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Blue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Đà Nẵng", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Muted)
                    }
                }
                val bellShape = RoundedCornerShape(16.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).softShadow(bellShape).background(Color.White, bellShape),
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Thông báo", tint = Navy)
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-9).dp, y = 9.dp)
                            .size(8.dp)
                            .background(Blue, CircleShape),
                    )
                }
            }
        }
        item {
            SearchBarButton(
                placeholder = "Bạn muốn đi đâu?",
                onClick = onOpenSearch,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
        }
        item {
            Banner(onClick = onOpenExplore, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp))
        }
        item {
            SectionHeader(
                "Điểm đến nổi bật", "Xem tất cả", onOpenExplore,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(featured, key = { it.id }) { place -> PlaceTileCard(place = place, onClick = onOpenExplore) }
            }
        }
        item {
            SectionHeader(
                "Gần bạn", "Xem tất cả", onOpenNearby,
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(nearby, key = { it.id }) { place ->
                    PlaceTileCard(place = place, onClick = { /* TODO(Người 2): mở chi tiết */ })
                }
            }
        }
    }
}

@Composable
private fun Banner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(20.dp))) {
        Image(
            painter = painterResource(R.drawable.img_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent)),
            ),
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("HÀNH TRÌNH MÙA HÈ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Khám phá Việt Nam",
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            Spacer(Modifier.height(2.dp))
            Text("Vẻ đẹp bất tận, ngay trong tầm tay.", color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Khám phá ngay",
                color = Blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
