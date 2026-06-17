package com.offlineflix.player.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.ui.PlayerView
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatDuration
import kotlinx.coroutines.delay

/**
 * شاشة مشغل الفيديو الكامل مع كل الإيماءات المتقدمة
 */
@Composable
fun VideoPlayerScreen(
    videoId: Long,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()

    // تهيئة
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
        // شاشة أفقية
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // إبقاء الشاشة مضاءة
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // إعادة التوجه عند الخروج
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.saveProgress()
        }
    }

    // إخفاء عناصر التحكم تلقائياً
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls) {
        if (showControls && uiState.isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ==================== مشغل ExoPlayer ====================
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false // تحكم مخصص
                    player = viewModel.player
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = viewModel.player
                // تطبيق وضع الزوم
                playerView.resizeMode = when (uiState.zoomMode) {
                    ZoomMode.FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ZoomMode.FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    ZoomMode.ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ZoomMode.FIXED_WIDTH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
            }
        )

        // ==================== منطقة الإيماءات ====================
        GestureOverlay(
            modifier = Modifier.fillMaxSize(),
            onTap = { showControls = !showControls },
            onDoubleTapLeft = { viewModel.seekBackward(10000) },
            onDoubleTapRight = { viewModel.seekForward(10000) },
            onTripleTapLeft = { viewModel.seekBackward(30000) },
            onTripleTapRight = { viewModel.seekForward(30000) },
            onLongPress = { viewModel.setSpeed(2f) },
            onLongPressEnd = { viewModel.resetSpeed() },
            onSwipeRight = { delta -> viewModel.adjustVolume(delta) },
            onSwipeLeft = { delta -> viewModel.adjustBrightness(delta) },
            onSwipeCenter = { delta -> viewModel.seekBySwipe(delta) },
            onThreeFingerSwipe = { viewModel.takeScreenshot(context) },
            onFourFingerTap = { viewModel.toggleLock() }
        )

        // ==================== عناصر التحكم ====================
        AnimatedVisibility(
            visible = showControls && !uiState.isLocked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            PlayerControls(
                uiState = uiState,
                onBack = onBack,
                onPlayPause = viewModel::togglePlayPause,
                onSeek = viewModel::seekTo,
                onNextVideo = viewModel::playNext,
                onPrevVideo = viewModel::playPrevious,
                onSpeedChange = viewModel::setSpeed,
                onZoomChange = viewModel::setZoomMode,
                onABRepeat = viewModel::toggleABRepeat,
                onSleepTimer = viewModel::setSleepTimer,
                onAudioOnly = viewModel::toggleAudioOnly,
                onPiP = { activity?.enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder().build()
                )},
                onSubtitles = viewModel::toggleSubtitles,
                onRotate = viewModel::rotateScreen,
                onOpenEditor = onOpenEditor
            )
        }

        // ==================== مؤشر القفل ====================
        if (uiState.isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.toggleLock() },
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("مقفل", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // ==================== مؤشر الحجم/السطوع ====================
        if (uiState.showVolumeIndicator) {
            VolumeIndicator(volume = uiState.volume, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp))
        }
        if (uiState.showBrightnessIndicator) {
            BrightnessIndicator(brightness = uiState.brightness, modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp))
        }

        // ==================== مؤشر الإيماءة ====================
        if (uiState.seekIndicator != 0L) {
            SeekIndicator(
                seekMs = uiState.seekIndicator,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ==================== مؤشر السرعة ====================
        if (uiState.speed != 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "سرعة: ${uiState.speed}x",
                    color = NetflixRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // ==================== الترجمة ====================
        if (uiState.subtitleText.isNotEmpty()) {
            SubtitleOverlay(
                text = uiState.subtitleText,
                secondaryText = uiState.secondarySubtitleText,
                style = uiState.subtitleStyle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 100.dp else 32.dp)
            )
        }

        // ==================== مؤقت النوم ====================
        if (uiState.sleepTimerRemaining > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatDuration(uiState.sleepTimerRemaining),
                        color = SpotifyGreen,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ==================== عناصر التحكم ====================
@Composable
fun PlayerControls(
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextVideo: () -> Unit,
    onPrevVideo: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onZoomChange: (ZoomMode) -> Unit,
    onABRepeat: () -> Unit,
    onSleepTimer: () -> Unit,
    onAudioOnly: () -> Unit,
    onPiP: () -> Unit,
    onSubtitles: () -> Unit,
    onRotate: () -> Unit,
    onOpenEditor: () -> Unit
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // تدرج علوي
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        // تدرج سفلي
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        // ==================== الشريط العلوي ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "رجوع", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = uiState.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = "${formatDuration(uiState.currentPosition)} / ${formatDuration(uiState.duration)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // أزرار إضافية
            IconButton(onClick = onSubtitles) {
                Icon(Icons.Default.ClosedCaption, "ترجمة",
                    tint = if (uiState.subtitlesEnabled) NetflixRed else Color.White)
            }
            IconButton(onClick = { showSpeedMenu = true }) {
                Text(
                    text = "${uiState.speed}x",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, "المزيد", tint = Color.White)
            }
        }

        // ==================== أزرار التحكم الوسطى ====================
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // تخطي 30 ثانية للخلف
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { onSeek(uiState.currentPosition - 30000) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Replay30, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            // السابق
            IconButton(onClick = onPrevVideo) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // تشغيل/إيقاف
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "إيقاف" else "تشغيل",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            // التالي
            IconButton(onClick = onNextVideo) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // تخطي 30 ثانية للأمام
            IconButton(
                onClick = { onSeek(uiState.currentPosition + 30000) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Forward30, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // ==================== الشريط السفلي ====================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // مؤشر AB Repeat
            if (uiState.abRepeatA > 0) {
                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "A: ${formatDuration(uiState.abRepeatA)}${if (uiState.abRepeatB > 0) " → B: ${formatDuration(uiState.abRepeatB)}" else ""}",
                        color = SpotifyGreen,
                        fontSize = 11.sp
                    )
                }
            }

            // شريط التقدم
            Slider(
                value = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f,
                onValueChange = { onSeek((it * uiState.duration).toLong()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NetflixRed,
                    activeTrackColor = NetflixRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            // صف الأزرار السفلية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    PlayerIconButton(icon = Icons.Default.AudioFile, label = "صوت فقط", onClick = onAudioOnly,
                        active = uiState.audioOnlyMode)
                    PlayerIconButton(icon = Icons.Default.PictureInPicture, label = "PiP", onClick = onPiP)
                    PlayerIconButton(icon = Icons.Default.Repeat, label = "AB Repeat", onClick = onABRepeat,
                        active = uiState.abRepeatA > 0)
                    PlayerIconButton(icon = Icons.Default.Bedtime, label = "مؤقت النوم", onClick = onSleepTimer)
                }
                Row {
                    PlayerIconButton(icon = Icons.Default.ScreenRotation, label = "تدوير", onClick = onRotate)
                    PlayerIconButton(icon = Icons.Default.ContentCut, label = "محرر", onClick = onOpenEditor)
                }
            }
        }

        // ==================== قائمة السرعة ====================
        if (showSpeedMenu) {
            SpeedMenu(
                currentSpeed = uiState.speed,
                onSpeedSelected = {
                    onSpeedChange(it)
                    showSpeedMenu = false
                },
                onDismiss = { showSpeedMenu = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp)
            )
        }

        // ==================== قائمة المزيد ====================
        if (showMoreMenu) {
            MoreMenu(
                onDismiss = { showMoreMenu = false },
                onZoomChange = { onZoomChange(it); showMoreMenu = false },
                currentZoom = uiState.zoomMode,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(onClick = onClick) {
        Icon(
            icon, label,
            tint = if (active) NetflixRed else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ==================== قائمة السرعة ====================
@Composable
fun SpeedMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speeds = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f, 6f, 8f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("سرعة التشغيل", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), fontSize = 14.sp)
            speeds.forEach { speed ->
                TextButton(
                    onClick = { onSpeedSelected(speed) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (speed == 1f) "عادي (1x)" else "${speed}x",
                        color = if (speed == currentSpeed) NetflixRed else Color.White,
                        fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ==================== قائمة الزوم ====================
@Composable
fun MoreMenu(
    onDismiss: () -> Unit,
    onZoomChange: (ZoomMode) -> Unit,
    currentZoom: ZoomMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp).width(180.dp)) {
            Text("وضع العرض", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), fontSize = 14.sp)
            ZoomMode.values().forEach { mode ->
                TextButton(onClick = { onZoomChange(mode) }) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = mode.label,
                            color = if (mode == currentZoom) NetflixRed else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==================== مؤشرات الإيماءات ====================
@Composable
fun VolumeIndicator(volume: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Slider(
            value = volume,
            onValueChange = {},
            modifier = Modifier.height(100.dp).width(30.dp),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
        )
        Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun BrightnessIndicator(brightness: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Brightness6, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun SeekIndicator(seekMs: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (seekMs > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${if (seekMs > 0) "+" else ""}${seekMs / 1000}s",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

// ==================== الترجمة ====================
@Composable
fun SubtitleOverlay(
    text: String,
    secondaryText: String,
    style: SubtitleStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (secondaryText.isNotEmpty()) {
            Text(
                text = secondaryText,
                color = style.color.copy(alpha = 0.85f),
                fontSize = style.fontSize.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = style.backgroundAlpha))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = text,
            color = style.color,
            fontSize = style.fontSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = style.backgroundAlpha))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ==================== الإيماءات ====================
@Composable
fun GestureOverlay(
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onTripleTapLeft: () -> Unit,
    onTripleTapRight: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSwipeRight: (Float) -> Unit,
    onSwipeLeft: (Float) -> Unit,
    onSwipeCenter: (Float) -> Unit,
    onThreeFingerSwipe: () -> Unit,
    onFourFingerTap: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val now = System.currentTimeMillis()
                        val screenWidth = size.width
                        val isLeft = offset.x < screenWidth / 3f
                        val isRight = offset.x > screenWidth * 2 / 3f

                        if (now - lastTapTime < 400 && kotlin.math.abs(offset.x - lastTapX) < 100) {
                            tapCount++
                            when (tapCount) {
                                2 -> if (isLeft) onDoubleTapLeft() else if (isRight) onDoubleTapRight()
                                3 -> if (isLeft) onTripleTapLeft() else if (isRight) onTripleTapRight()
                            }
                        } else {
                            tapCount = 1
                            onTap()
                        }
                        lastTapTime = now
                        lastTapX = offset.x
                    },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onLongPressEnd() }
                ) { change, dragAmount ->
                    val screenWidth = size.width.toFloat()
                    val isRight = change.position.x > screenWidth * 2 / 3
                    val isLeft = change.position.x < screenWidth / 3
                    when {
                        isRight -> onSwipeRight(-dragAmount.y / size.height)
                        isLeft -> onSwipeLeft(-dragAmount.y / size.height)
                        else -> onSwipeCenter(dragAmount.x)
                    }
                }
            }
    )
}
