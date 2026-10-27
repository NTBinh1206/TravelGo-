package com.travelgo.app.ui.explore

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.FavoriteButton
import com.travelgo.app.components.PillChip
import com.travelgo.app.components.PlaceRowCard
import com.travelgo.app.components.SearchBarButton
import com.travelgo.app.components.SquareIconButton
import com.travelgo.app.components.softShadow
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

private data class Category(val key: String?, val label: String, val icon: ImageVector)

private val categories = listOf(
    Category(null, "Tất cả", Icons.Outlined.Waves),
    Category("bien", "Biển", Icons.Outlined.Waves),
    Category("nui", "Núi", Icons.Outlined.Terrain),
    Category("thanh_pho", "Thành phố", Icons.Outlined.Apartment),
    Category("van_hoa", "Văn hóa", Icons.Outlined.AccountBalance),
    Category("thien_nhien", "Thiên nhiên", Icons.Outlined.Park),
)

enum class SortOption(val label: String) { NEAREST("Gần nhất"), TOP_RATED("Đánh giá cao") }

/** 04 · Khám phá (Người 2). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(onOpenSearch: () -> Unit) {
    var category by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(SortOption.NEAREST) }
    val location by AppContainer.locationProvider.currentLocation.collectAsState()
    val me = location ?: GeoPoint.DEFAULT
    val places by produceState(initialValue = emptyList<Place>(),me) {
        value = AppContainer.placeRepository.getNearbyPlaces(me)
    }
    val shown = places
        .filter { category == null || it.category == category }
        .let { list ->
            when (sort) {
                SortOption.NEAREST -> list.sortedBy { me.distanceKmTo(it.location) }
                SortOption.TOP_RATED -> list.sortedByDescending { it.rating }
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Khám phá", color = Navy, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                SquareIconButton(Icons.Outlined.Map, contentDescription = "Bản đồ", onClick = { /* TODO */ })
            }
        }
        item {
            SearchBarButton(
                placeholder = "Tìm địa điểm, thành phố",
                onClick = onOpenSearch,
                trailing = { SquareIconButton(Icons.Outlined.Tune, contentDescription = "Bộ lọc", onClick = onOpenSearch, size = 40.dp) },
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                categories.forEach { c ->
                    PillChip(label = c.label, icon = c.icon, selected = category == c.key, onClick = { category = c.key })
                }
            }
        }
        item { SortBar(sort = sort, onChange = { sort = it }) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Địa điểm phổ biến", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${shown.size} địa điểm", color = Muted, fontSize = 14.sp)
            }
        }
        items(shown, key = { it.id }) { place ->
            PlaceRowCard(
                place = place,
                distanceKm = me.distanceKmTo(place.location),
                onClick = { /* TODO(Người 2): mở chi tiết */ },
                trailing = { FavoriteButton() },
            )
        }
    }
}

@Composable
private fun SortBar(sort: SortOption, onChange: (SortOption) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .softShadow(CircleShape)
            .background(Color.White, CircleShape)
            .padding(3.dp),
    ) {
        Text(
            "Sắp xếp theo",
            color = Navy,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        SortOption.entries.forEach { option ->
            val selected = option == sort
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (selected) Blue else Color.White)
                    .clickable { onChange(option) }
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    option.label,
                    color = if (selected) Color.White else Muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
