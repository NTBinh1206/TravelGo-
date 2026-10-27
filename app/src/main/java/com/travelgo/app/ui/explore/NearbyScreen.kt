package com.travelgo.app.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.CenterHeader
import com.travelgo.app.components.PlaceRowCard
import com.travelgo.app.components.PrimaryButton
import com.travelgo.app.data.model.GeoPoint
import com.travelgo.app.data.place.Place
import com.travelgo.app.di.AppContainer
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.BlueLight
import com.travelgo.app.ui.theme.Muted

private const val MAX_STOPS = 5

/** 07 · Địa điểm gần bạn (Người 2). Lấy 5 điểm gần nhất rồi sang màn Lộ trình. */
@Composable
fun NearbyScreen(onBack: () -> Unit, onSmartRoute: (List<String>) -> Unit) {
    val userLocation by AppContainer.locationProvider.currentLocation.collectAsState()
    val me = userLocation ?: GeoPoint.DEFAULT // TODO(Người 2): vị trí thật
    val places by produceState(initialValue = emptyList<Place>(), me) {
        value = AppContainer.placeRepository.getNearbyPlaces(me)
            .sortedBy { me.distanceKmTo(it.location) }
            .take(MAX_STOPS)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        CenterHeader(
            title = "Địa điểm gần bạn",
            onBack = onBack,
            actionIcon = Icons.Outlined.Map,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
        ) {
            Text("${places.size} gợi ý dành cho bạn", color = Muted, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(BlueLight, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = Blue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gần nhất", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(places, key = { it.id }) { place ->
                PlaceRowCard(
                    place = place,
                    subtitle = place.tagline,
                    showPin = false,
                    arrowDistance = true,
                    distanceKm = me.distanceKmTo(place.location),
                    onClick = { /* TODO(Người 2): mở chi tiết */ },
                    trailing = {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Muted)
                    },
                )
            }
        }
        PrimaryButton(
            text = "Lên lộ trình thông minh",
            icon = Icons.Outlined.NearMe,
            enabled = places.isNotEmpty(),
            onClick = { onSmartRoute(places.map { it.id }) },
            modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        )
    }
}
