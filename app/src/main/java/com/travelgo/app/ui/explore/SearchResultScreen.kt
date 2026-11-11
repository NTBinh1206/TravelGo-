package com.travelgo.app.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelgo.app.ui.common.LoadStatus
import com.travelgo.app.ui.common.dataOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelgo.app.components.BackTitleHeader
import com.travelgo.app.components.FavoriteButton
import com.travelgo.app.components.PillChip
import com.travelgo.app.components.PlaceRowCard
import com.travelgo.app.components.PlaceTileCard
import com.travelgo.app.components.softShadow
import com.travelgo.app.ui.theme.Blue
import com.travelgo.app.ui.theme.Border
import com.travelgo.app.ui.theme.Muted
import com.travelgo.app.ui.theme.Navy

/** 06 · Kết quả tìm kiếm (Người 2). Tìm, lọc, sắp xếp nằm trong [SearchViewModel]. */
@Composable
fun SearchResultScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel { SearchViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query = viewModel.query
    val me = state.me
    val filter = state.options.filter
    val gridMode = state.options.gridMode
    val results = state.results.dataOrNull.orEmpty()
    // Bảng lọc đang mở hay đóng chỉ là trạng thái hiển thị, để ở màn hình.
    var showFilter by remember { mutableStateOf(false) }

    if (showFilter) {
        FilterSheet(
            current = filter,
            onApply = { viewModel.applyFilter(it); showFilter = false },
            onDismiss = { showFilter = false },
        )
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackTitleHeader(
            title = "Kết quả tìm kiếm",
            onBack = onBack,
            actionIcon = Icons.Outlined.Map,
            modifier = Modifier.padding(top = 8.dp),
        )
        SearchInput(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("${results.size} kết quả", color = Navy, fontSize = 19.sp)
                Text("Toàn Việt Nam · ưu tiên gần bạn", color = Muted, fontSize = 13.sp)
            }
            ViewToggle(gridMode = gridMode, onChange = viewModel::setGridMode)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(top = 14.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            PillChip("Liên quan", selected = !state.options.nearestFirst, onClick = { viewModel.setNearestFirst(false) })
            PillChip(
                label = if (filter.activeCount > 0) "Bộ lọc · ${filter.activeCount}" else "Bộ lọc",
                icon = Icons.Outlined.Tune,
                selected = filter.activeCount > 0,
                onClick = { showFilter = true },
            )
            PillChip("Gần nhất", selected = state.options.nearestFirst, onClick = { viewModel.setNearestFirst(true) })
        }
        if (query.isBlank()) {
            Text(
                "Gõ tên địa điểm hoặc thành phố, ví dụ \"da lat\", \"chùa\", \"biển\".",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        } else {
            LoadStatus(
                state = state.results,
                onRetry = viewModel::retry,
                emptyText = if (filter.activeCount > 0) {
                    "Không tìm thấy \"$query\" với bộ lọc hiện tại. Thử bỏ bớt bộ lọc."
                } else {
                    "Không tìm thấy \"$query\". Thử từ khoá khác."
                },
            )
        }
        if (gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(results, key = { it.id }) { place ->
                    PlaceTileCard(place = place, onClick = {}, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { place ->
                    PlaceRowCard(
                        place = place,
                        distanceKm = me.distanceKmTo(place.location),
                        onClick = { /* TODO(Người 2): mở chi tiết */ },
                        trailing = { FavoriteButton(size = 44.dp) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .softShadow(shape)
            .background(Color.White, shape)
            .border(1.dp, Border, shape)
            .padding(start = 16.dp, end = 4.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("biển đẹp gần Đà Nẵng", color = Muted.copy(alpha = 0.8f), fontSize = 16.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Navy, fontSize = 16.sp),
                cursorBrush = SolidColor(Blue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Xóa", tint = Muted)
            }
        }
    }
}

@Composable
private fun ViewToggle(gridMode: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .border(1.dp, Border, shape)
            .background(Color.White, shape)
            .padding(4.dp),
    ) {
        ToggleIcon(Icons.AutoMirrored.Outlined.FormatListBulleted, selected = !gridMode) { onChange(false) }
        ToggleIcon(Icons.Outlined.GridView, selected = gridMode) { onChange(true) }
    }
}

@Composable
private fun ToggleIcon(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 34.dp, height = 32.dp)
            .clip(shape)
            .background(if (selected) Blue else Color.White)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else Navy, modifier = Modifier.size(20.dp))
    }
}
