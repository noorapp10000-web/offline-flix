package com.offlineflix.player.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.offlineflix.player.data.models.VideoEntity
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatDuration
import com.offlineflix.player.utils.formatSize

/**
 * شاشة مكتبة الفيديوهات الكاملة مع الفلاتر الذكية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    onVideoClick: (Long) -> Unit,
    onEditVideo: (Long) -> Unit,
    viewModel: VideoLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(NetflixBlack)
    ) {
        // ==================== شريط البحث ====================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.search(it)
            },
            placeholder = { Text("ابحث عن فيديو...", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                        Icon(Icons.Default.Clear, null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NetflixRed,
                unfocusedBorderColor = NetflixMediumGray,
                cursorColor = NetflixRed,
                focusedContainerColor = NetflixDarkGray,
                unfocusedContainerColor = NetflixDarkGray
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // ==================== شرائط الفلاتر الذكية ====================
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SmartFilter.values().toList()) { filter ->
                FilterChip(
                    selected = uiState.activeFilter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.label, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(filter.icon, null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetflixRed,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = NetflixDarkGray,
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.activeFilter == filter,
                        borderColor = NetflixMediumGray,
                        selectedBorderColor = NetflixRed
                    )
                )
            }
        }

        // ==================== شريط التحكم ====================
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.videos.size} فيديو",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            Row {
                // فرز
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.Sort, "فرز", tint = Color.White)
                }
                // طريقة العرض
                IconButton(onClick = viewModel::toggleViewMode) {
                    Icon(
                        if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        "طريقة العرض",
                        tint = Color.White
                    )
                }
                // تحديد متعدد
                IconButton(onClick = viewModel::toggleMultiSelect) {
                    Icon(
                        Icons.Default.CheckBox,
                        "تحديد متعدد",
                        tint = if (uiState.isMultiSelectMode) NetflixRed else Color.White
                    )
                }
            }
        }

        // ==================== شريط التحديد المتعدد ====================
        if (uiState.isMultiSelectMode && uiState.selectedIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(NetflixRed.copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${uiState.selectedIds.size} محدد", color = Color.White, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = viewModel::deleteSelected) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("حذف", color = Color(0xFFCF6679))
                    }
                    TextButton(onClick = viewModel::toggleMultiSelect) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            }
        }

        // ==================== قائمة / شبكة الفيديوهات ====================
        if (uiState.isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.videos, key = { it.id }) { video ->
                    VideoGridItem(
                        video = video,
                        isSelected = uiState.selectedIds.contains(video.id),
                        isMultiSelect = uiState.isMultiSelectMode,
                        onClick = {
                            if (uiState.isMultiSelectMode) viewModel.toggleSelect(video.id)
                            else onVideoClick(video.id)
                        },
                        onLongClick = {
                            if (!uiState.isMultiSelectMode) viewModel.toggleMultiSelect()
                            viewModel.toggleSelect(video.id)
                        },
                        onEdit = { onEditVideo(video.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.videos, key = { it.id }) { video ->
                    VideoListItem(
                        video = video,
                        isSelected = uiState.selectedIds.contains(video.id),
                        isMultiSelect = uiState.isMultiSelectMode,
                        onClick = {
                            if (uiState.isMultiSelectMode) viewModel.toggleSelect(video.id)
                            else onVideoClick(video.id)
                        },
                        onLongClick = {
                            if (!uiState.isMultiSelectMode) viewModel.toggleMultiSelect()
                            viewModel.toggleSelect(video.id)
                        },
                        onEdit = { onEditVideo(video.id) }
                    )
                }
            }
        }
    }

    // قائمة الفرز
    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("فرز الفيديوهات", color = Color.White) },
            containerColor = NetflixDarkGray,
            text = {
                Column {
                    SortOption.values().forEach { sort ->
                        TextButton(
                            onClick = { viewModel.setSort(sort); showSortMenu = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sort.label, color = if (uiState.sortOption == sort) NetflixRed else Color.White)
                                if (uiState.sortOption == sort) {
                                    Icon(Icons.Default.Check, null, tint = NetflixRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// ==================== عنصر الشبكة ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridItem(
    video: VideoEntity,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NetflixRed.copy(alpha = 0.2f) else NetflixDarkGray
        ),
        border = if (isSelected) BorderStroke(2.dp, NetflixRed) else null
    ) {
        Box {
            AsyncImage(
                model = video.thumbnailPath.ifEmpty { video.path },
                contentDescription = video.displayName,
                modifier = Modifier.fillMaxWidth().height(90.dp),
                contentScale = ContentScale.Crop
            )
            // مدة الفيديو
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(formatDuration(video.duration), color = Color.White, fontSize = 9.sp)
            }
            // شارة 4K
            if (video.is4K) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0055AA))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("4K", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            // تحديد
            if (isMultiSelect) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            // شريط التقدم
            if (video.watchProgress > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NetflixMediumGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(video.watchProgress / 100f)
                            .fillMaxHeight()
                            .background(NetflixRed)
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = video.displayName,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatSize(video.size), color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ==================== عنصر القائمة ====================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    video: VideoEntity,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isSelected) NetflixRed.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelect) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                null,
                tint = if (isSelected) NetflixRed else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
        }

        Box(modifier = Modifier.size(80.dp, 50.dp)) {
            AsyncImage(
                model = video.thumbnailPath.ifEmpty { video.path },
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(formatDuration(video.duration), color = Color.White, fontSize = 8.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(video.displayName, color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatSize(video.size), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                if (video.is4K) Text("4K", color = Color(0xFF4488FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (video.isHdr) Text("HDR", color = Color(0xFFFFAA00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (video.watchProgress > 0 && video.watchProgress < 100) {
                LinearProgressIndicator(
                    progress = { video.watchProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp),
                    color = NetflixRed,
                    trackColor = NetflixMediumGray
                )
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(alpha = 0.5f))
        }
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 108.dp))
}

// ==================== الفلاتر الذكية ====================
enum class SmartFilter(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("الكل", Icons.Default.VideoLibrary),
    CONTINUE("متابعة", Icons.Default.PlayCircle),
    UNWATCHED("لم تشاهد", Icons.Default.FiberNew),
    ALMOST_DONE("90%+", Icons.Default.DoneAll),
    WATCHED("شاهدت", Icons.Default.CheckCircle),
    FAVORITES("المفضلة", Icons.Default.Favorite),
    FOUR_K("4K", Icons.Default.HighQuality),
    LARGE("أكبر 2GB", Icons.Default.Storage),
    OLDEST("الأقدم", Icons.Default.History)
}

enum class SortOption(val label: String) {
    DATE_DESC("الأحدث أولاً"),
    DATE_ASC("الأقدم أولاً"),
    NAME_ASC("الاسم أ-ي"),
    NAME_DESC("الاسم ي-أ"),
    SIZE_DESC("الأكبر أولاً"),
    DURATION_DESC("الأطول أولاً"),
    LAST_WATCHED("آخر مشاهدة")
}
