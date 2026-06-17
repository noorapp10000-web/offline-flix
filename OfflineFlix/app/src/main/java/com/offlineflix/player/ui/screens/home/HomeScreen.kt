package com.offlineflix.player.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
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
 * الشاشة الرئيسية - تصميم Netflix بالكامل
 */
@Composable
fun HomeScreen(
    onVideoClick: (Long) -> Unit,
    onSeeAllVideos: () -> Unit,
    onSeeAllMusic: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenHowTo: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ==================== Hero Banner ====================
        item {
            if (uiState.heroVideo != null) {
                NetflixHeroBanner(
                    video = uiState.heroVideo!!,
                    onPlay = { onVideoClick(uiState.heroVideo!!.id) }
                )
            } else {
                EmptyHeroBanner()
            }
        }

        // ==================== أدوات سريعة ====================
        item {
            QuickActionsRow(
                onOpenEditor = onOpenEditor,
                onOpenHowTo = onOpenHowTo,
                onSeeAllVideos = onSeeAllVideos,
                onSeeAllMusic = onSeeAllMusic
            )
        }

        // ==================== مكملتش ====================
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                VideoSection(
                    title = "متابعة المشاهدة",
                    videos = uiState.continueWatching,
                    onVideoClick = onVideoClick,
                    onSeeAll = onSeeAllVideos,
                    showProgress = true
                )
            }
        }

        // ==================== أحدث الإضافات ====================
        if (uiState.recentVideos.isNotEmpty()) {
            item {
                VideoSection(
                    title = "أحدث الإضافات",
                    videos = uiState.recentVideos,
                    onVideoClick = onVideoClick,
                    onSeeAll = onSeeAllVideos
                )
            }
        }

        // ==================== فيديوهات 4K ====================
        if (uiState.videos4K.isNotEmpty()) {
            item {
                VideoSection(
                    title = "فيديوهات 4K",
                    videos = uiState.videos4K,
                    onVideoClick = onVideoClick,
                    onSeeAll = onSeeAllVideos,
                    badgeText = "4K"
                )
            }
        }

        // ==================== إحصاءات ====================
        item {
            StatsRow(
                videoCount = uiState.videoCount,
                audioCount = uiState.audioCount,
                totalSize = uiState.totalSize
            )
        }

        // ==================== المفضلة ====================
        if (uiState.favoriteVideos.isNotEmpty()) {
            item {
                VideoSection(
                    title = "المفضلة",
                    videos = uiState.favoriteVideos,
                    onVideoClick = onVideoClick,
                    onSeeAll = onSeeAllVideos
                )
            }
        }
    }
}

// ==================== Hero Banner Netflix Style ====================
@Composable
fun NetflixHeroBanner(video: VideoEntity, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        // صورة مصغرة الفيديو
        AsyncImage(
            model = video.thumbnailPath.ifEmpty { video.path },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // تدرج من الأسفل
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.9f),
                            NetflixBlack
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // معلومات الفيديو
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            if (video.is4K) {
                Badge4K()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = video.displayName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(" • ", color = Color.White.copy(alpha = 0.5f))
                Text(
                    text = formatSize(video.size),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تشغيل", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {},
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("معلومات")
                }
            }
        }
    }
}

@Composable
fun EmptyHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.verticalGradient(
                    listOf(NetflixDarkGray, NetflixBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VideoLibrary, null, tint = NetflixRed, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("لا توجد فيديوهات بعد", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("امنح الصلاحيات وسيجلب التطبيق كل فيديوهاتك", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

@Composable
fun Badge4K() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NetflixBlue)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("4K", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ==================== أدوات سريعة ====================
@Composable
fun QuickActionsRow(
    onOpenEditor: () -> Unit,
    onOpenHowTo: () -> Unit,
    onSeeAllVideos: () -> Unit,
    onSeeAllMusic: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { QuickActionChip(Icons.Default.VideoLibrary, "الكل", onClick = onSeeAllVideos) }
        item { QuickActionChip(Icons.Default.MusicNote, "موسيقى", onClick = onSeeAllMusic) }
        item { QuickActionChip(Icons.Default.Transform, "تحويل", onClick = onOpenEditor) }
        item { QuickActionChip(Icons.Default.Help, "طريقة الاستخدام", onClick = onOpenHowTo) }
    }
}

@Composable
fun QuickActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NetflixDarkGray,
            labelColor = Color.White,
            iconColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = NetflixMediumGray
        )
    )
}

// ==================== قسم الفيديوهات ====================
@Composable
fun VideoSection(
    title: String,
    videos: List<VideoEntity>,
    onVideoClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
    showProgress: Boolean = false,
    badgeText: String? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (badgeText != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0055AA))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(badgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            TextButton(onClick = onSeeAll) {
                Text("عرض الكل", color = NetflixRed, fontSize = 13.sp)
                Icon(Icons.Default.ChevronRight, null, tint = NetflixRed, modifier = Modifier.size(16.dp))
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(videos.take(15), key = { it.id }) { video ->
                VideoThumbnailCard(
                    video = video,
                    onClick = { onVideoClick(video.id) },
                    showProgress = showProgress
                )
            }
        }
    }
}

// ==================== بطاقة الفيديو ====================
@Composable
fun VideoThumbnailCard(
    video: VideoEntity,
    onClick: () -> Unit,
    showProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray)
    ) {
        Box {
            // صورة مصغرة
            AsyncImage(
                model = video.thumbnailPath.ifEmpty { video.path },
                contentDescription = video.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentScale = ContentScale.Crop
            )

            // شارة 4K
            if (video.is4K) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0055AA))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("4K", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // شريط التقدم
            if (showProgress && video.watchProgress > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDuration(video.duration),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

// ==================== إحصاءات ====================
@Composable
fun StatsRow(videoCount: Int, audioCount: Int, totalSize: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.VideoLibrary, value = videoCount.toString(), label = "فيديو")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.MusicNote, value = audioCount.toString(), label = "أغنية")
        StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Storage, value = formatSize(totalSize), label = "مساحة")
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

private val NetflixBlue = Color(0xFF0055AA)
