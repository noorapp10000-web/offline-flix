package com.offlineflix.player.ui.screens.music

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.offlineflix.player.data.models.AudioEntity
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatDuration

/**
 * شاشة الموسيقى الكاملة بتصميم Spotify
 * شريط سفلي دائم + شاشة تشغيل كاملة
 */
@Composable
fun MusicScreen(viewModel: MusicViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    val addAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAudioManually(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        // ==================== المحتوى الرئيسي ====================
        Column(modifier = Modifier.fillMaxSize().padding(bottom = if (uiState.currentTrack != null) 72.dp else 0.dp)) {
            // تبويبات
            MusicTabBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            // المحتوى حسب التبويب
            when (uiState.selectedTab) {
                MusicTab.SONGS -> SongsTab(
                    tracks = uiState.tracks,
                    currentTrack = uiState.currentTrack,
                    onTrackClick = viewModel::playTrack,
                    onShuffle = viewModel::shuffleAll
                )
                MusicTab.ALBUMS -> AlbumsTab(
                    albums = uiState.albums,
                    onAlbumClick = viewModel::openAlbum
                )
                MusicTab.ARTISTS -> ArtistsTab(
                    artists = uiState.artists,
                    onArtistClick = viewModel::openArtist
                )
                MusicTab.PLAYLISTS -> PlaylistsTab(
                    playlists = uiState.playlists,
                    onPlaylistClick = viewModel::openPlaylist,
                    onCreatePlaylist = viewModel::createPlaylist
                )
                MusicTab.FOLDERS -> FoldersTab(
                    folders = uiState.folders,
                    onFolderClick = viewModel::openFolder
                )
            }
        }

        // ==================== شريط التشغيل السفلي (Spotify Style) ====================
        if (uiState.currentTrack != null) {
            MiniPlayer(
                track = uiState.currentTrack!!,
                isPlaying = uiState.isPlaying,
                progress = uiState.progress,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::playNext,
                onClick = { showFullPlayer = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // ==================== مشغل كامل (Full Screen) ====================
        AnimatedVisibility(
            visible = showFullPlayer && uiState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (uiState.currentTrack != null) {
                FullMusicPlayer(
                    uiState = uiState,
                    onClose = { showFullPlayer = false },
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::playNext,
                    onPrevious = viewModel::playPrevious,
                    onSeek = viewModel::seekTo,
                    onShuffle = viewModel::toggleShuffle,
                    onRepeat = viewModel::toggleRepeat,
                    onFavorite = { viewModel.toggleFavorite(uiState.currentTrack!!.id) },
                    onEqualizer = viewModel::openEqualizer,
                    onAddToPlaylist = viewModel::addToPlaylist
                )
            }
        }

        // ==================== زر إضافة صوت ====================
        if (!showFullPlayer) {
            FloatingActionButton(
                onClick = { addAudioLauncher.launch("audio/*") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        bottom = if (uiState.currentTrack != null) 88.dp else 16.dp,
                        end = 16.dp
                    ),
                containerColor = SpotifyGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صوت")
            }
        }

        // ==================== Equalizer ====================
        if (uiState.showEqualizer) {
            EqualizerSheet(
                viewModel = viewModel,
                onDismiss = viewModel::closeEqualizer
            )
        }
    }
}

// ==================== تبويبات الموسيقى ====================
enum class MusicTab(val label: String) {
    SONGS("الأغاني"),
    ALBUMS("الألبومات"),
    ARTISTS("الفنانين"),
    PLAYLISTS("قوائم التشغيل"),
    FOLDERS("المجلدات")
}

@Composable
fun MusicTabBar(selectedTab: MusicTab, onTabSelected: (MusicTab) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = MusicTab.values().indexOf(selectedTab),
        containerColor = SpotifyBlack,
        contentColor = SpotifyGreen,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[MusicTab.values().indexOf(selectedTab)]),
                color = SpotifyGreen
            )
        }
    ) {
        MusicTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        tab.label,
                        color = if (selectedTab == tab) SpotifyGreen else SpotifyLightGray,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

// ==================== تبويب الأغاني ====================
@Composable
fun SongsTab(
    tracks: List<AudioEntity>,
    currentTrack: AudioEntity?,
    onTrackClick: (AudioEntity) -> Unit,
    onShuffle: () -> Unit
) {
    Column {
        // زر عشوائي
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onShuffle,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("تشغيل عشوائي", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            OutlinedButton(
                onClick = { onTrackClick(tracks.firstOrNull() ?: return@OutlinedButton) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("تشغيل الكل")
            }
        }

        LazyColumn {
            items(tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    isPlaying = currentTrack?.id == track.id,
                    onClick = { onTrackClick(track) }
                )
            }
        }
    }
}

// ==================== عنصر الأغنية ====================
@Composable
fun TrackItem(track: AudioEntity, isPlaying: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val barHeight by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "bar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isPlaying) SpotifyGreen.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // غلاف الألبوم
        Box(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = track.albumArtPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
                error = null
            )
            if (track.albumArtPath.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                        .background(SpotifyDarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = SpotifyLightGray, modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) SpotifyGreen else Color.White,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.album}",
                color = SpotifyLightGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // مؤشر التشغيل
        if (isPlaying) {
            SpotifyEqualizerIcon(modifier = Modifier.size(24.dp))
        } else {
            Text(formatDuration(track.duration), color = SpotifyLightGray, fontSize = 11.sp)
        }
    }
}

// ==================== أيقونة الإيكولايزر المتحركة ====================
@Composable
fun SpotifyEqualizerIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b1")
    val bar2 by infiniteTransition.animateFloat(0.7f, 1f, infiniteRepeatable(tween(300, 100), RepeatMode.Reverse), label = "b2")
    val bar3 by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(500, 200), RepeatMode.Reverse), label = "b3")

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        listOf(bar1, bar2, bar3).forEach { h ->
            Box(modifier = Modifier.width(3.dp).fillMaxHeight(h).clip(RoundedCornerShape(1.dp)).background(SpotifyGreen))
        }
    }
}

// ==================== شريط التشغيل المصغر ====================
@Composable
fun MiniPlayer(
    track: AudioEntity,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpotifyDarkGray),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            // شريط تقدم
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = SpotifyGreen,
                trackColor = SpotifyMediumGray
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // غلاف الألبوم الدوار
                val rotation by rememberInfiniteTransition(label = "vinyl").animateFloat(
                    0f, 360f,
                    infiniteRepeatable(tween(8000, easing = LinearEasing)),
                    label = "rot"
                )
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .rotate(if (isPlaying) rotation else 0f)
                ) {
                    AsyncImage(
                        model = track.albumArtPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (track.albumArtPath.isEmpty()) {
                        Box(Modifier.fillMaxSize().background(SpotifyMediumGray), Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = SpotifyLightGray, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = SpotifyLightGray, fontSize = 11.sp, maxLines = 1)
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White)
                }
            }
        }
    }
}

// ==================== مشغل كامل (Spotify Full Player) ====================
@Composable
fun FullMusicPlayer(
    uiState: MusicUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onEqualizer: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val track = uiState.currentTrack ?: return
    val albumColor = remember(track.albumArtPath) {
        Color(0xFF1DB954).copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF121212), albumColor, Color(0xFF191414)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // رأس الصفحة
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("يُشغل الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))

            // غلاف الألبوم الكبير
            val infiniteTransition = rememberInfiniteTransition(label = "vinyl_full")
            val rotation by infiniteTransition.animateFloat(
                0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "rot_full"
            )

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .rotate(if (uiState.isPlaying) rotation else 0f)
                    .border(4.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                AsyncImage(
                    model = track.albumArtPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (track.albumArtPath.isEmpty()) {
                    Box(Modifier.fillMaxSize().background(SpotifyDarkGray), Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = SpotifyGreen, modifier = Modifier.size(100.dp))
                    }
                }
                // دائرة وسط
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF121212)).align(Alignment.Center))
            }

            Spacer(Modifier.height(40.dp))

            // اسم الأغنية والفنان
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = SpotifyLightGray, fontSize = 15.sp)
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null, tint = if (track.isFavorite) SpotifyGreen else Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // شريط التقدم
            Column {
                Slider(
                    value = uiState.progress,
                    onValueChange = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = SpotifyLightGray.copy(alpha = 0.3f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(uiState.currentPosition), color = SpotifyLightGray, fontSize = 12.sp)
                    Text(formatDuration(track.duration), color = SpotifyLightGray, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // أزرار التحكم
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Default.Shuffle, null,
                        tint = if (uiState.shuffleEnabled) SpotifyGreen else Color.White, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White).clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null, tint = Color.Black, modifier = Modifier.size(44.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        if (uiState.repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        null, tint = if (uiState.repeatMode > 0) SpotifyGreen else Color.White, modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // الصف السفلي
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = onEqualizer) {
                    Icon(Icons.Default.Equalizer, "إيكولايزر", tint = SpotifyLightGray)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.QueueMusic, "قائمة التشغيل", tint = SpotifyLightGray)
                }
            }

            // كلمات الأغنية LRC
            if (uiState.currentLyricLine.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = uiState.currentLyricLine,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ==================== تبويبات الألبومات والفنانين ====================
@Composable
fun AlbumsTab(albums: List<String>, onAlbumClick: (String) -> Unit) {
    LazyColumn {
        items(albums) { album ->
            ListItem(
                headlineContent = { Text(album, color = Color.White) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyDarkGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Album, null, tint = SpotifyLightGray)
                    }
                },
                modifier = Modifier.clickable { onAlbumClick(album) }.background(SpotifyBlack),
                colors = ListItemDefaults.colors(containerColor = SpotifyBlack)
            )
        }
    }
}

@Composable
fun ArtistsTab(artists: List<String>, onArtistClick: (String) -> Unit) {
    LazyColumn {
        items(artists) { artist ->
            ListItem(
                headlineContent = { Text(artist, color = Color.White) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SpotifyMediumGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = SpotifyLightGray)
                    }
                },
                modifier = Modifier.clickable { onArtistClick(artist) }.background(SpotifyBlack),
                colors = ListItemDefaults.colors(containerColor = SpotifyBlack)
            )
        }
    }
}

@Composable
fun PlaylistsTab(playlists: List<com.offlineflix.player.data.models.PlaylistEntity>, onPlaylistClick: (Long) -> Unit, onCreatePlaylist: () -> Unit) {
    LazyColumn {
        item {
            ListItem(
                headlineContent = { Text("إنشاء قائمة تشغيل جديدة", color = SpotifyGreen, fontWeight = FontWeight.Bold) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyDarkGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = SpotifyGreen)
                    }
                },
                modifier = Modifier.clickable(onClick = onCreatePlaylist).background(SpotifyBlack),
                colors = ListItemDefaults.colors(containerColor = SpotifyBlack)
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            ListItem(
                headlineContent = { Text(playlist.name, color = Color.White) },
                supportingContent = { Text("${playlist.trackCount} أغنية", color = SpotifyLightGray, fontSize = 12.sp) },
                leadingContent = {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(SpotifyDarkGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QueueMusic, null, tint = SpotifyGreen)
                    }
                },
                modifier = Modifier.clickable { onPlaylistClick(playlist.id) }.background(SpotifyBlack),
                colors = ListItemDefaults.colors(containerColor = SpotifyBlack)
            )
        }
    }
}

@Composable
fun FoldersTab(folders: List<String>, onFolderClick: (String) -> Unit) {
    LazyColumn {
        items(folders) { folder ->
            ListItem(
                headlineContent = { Text(folder.substringAfterLast("/"), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(folder, color = SpotifyLightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = {
                    Icon(Icons.Default.Folder, null, tint = SpotifyGreen, modifier = Modifier.size(36.dp))
                },
                modifier = Modifier.clickable { onFolderClick(folder) }.background(SpotifyBlack),
                colors = ListItemDefaults.colors(containerColor = SpotifyBlack)
            )
        }
    }
}

// ==================== Equalizer Sheet ====================
@Composable
fun EqualizerSheet(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpotifyDarkGray
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("إيكولايزر", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            // Presets
            Text("Presets", color = SpotifyLightGray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MusicViewModel.EQ_PRESETS.keys.toList()) { preset ->
                    FilterChip(
                        selected = uiState.eqPreset == preset,
                        onClick = { viewModel.applyEqPreset(preset) },
                        label = { Text(preset, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = SpotifyMediumGray,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 10-Band EQ Sliders
            val bands = listOf("31Hz", "63Hz", "125Hz", "250Hz", "500Hz", "1K", "2K", "4K", "8K", "16K")
            Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                bands.forEachIndexed { index, band ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${if ((uiState.eqBands.getOrNull(index) ?: 0) >= 0) "+" else ""}${uiState.eqBands.getOrNull(index) ?: 0}dB",
                            color = SpotifyGreen, fontSize = 9.sp
                        )
                        Slider(
                            value = uiState.eqBands.getOrNull(index)?.toFloat() ?: 0f,
                            onValueChange = { viewModel.setEqBand(index, it.toInt()) },
                            valueRange = -15f..15f,
                            modifier = Modifier.weight(1f).height(120.dp),
                            colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
                        )
                        Text(band, color = SpotifyLightGray, fontSize = 8.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bass Boost
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Bass Boost", color = Color.White, fontWeight = FontWeight.Medium)
                Text("${uiState.bassBoost}%", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = uiState.bassBoost.toFloat(),
                onValueChange = { viewModel.setBassBoost(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
            )

            // Virtualizer
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Virtualizer 3D", color = Color.White, fontWeight = FontWeight.Medium)
                Text("${uiState.virtualizer}%", color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = uiState.virtualizer.toFloat(),
                onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
