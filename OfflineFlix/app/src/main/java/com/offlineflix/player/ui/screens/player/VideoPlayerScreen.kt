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
 * يدعم: ترجمة SRT/ASS/VTT مزدوجة، لقطة شاشة حقيقية، تدوير، AB Repeat، Sleep Timer
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

    // مرجع PlayerView للقطة الشاشة
    var playerViewRef by remember { mutableStateOf<android.view.View?>(null) }

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            viewModel.saveProgress()
        }
    }

    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(showControls, uiState.isPlaying) {
        if (showControls && uiState.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ==================== مشغل ExoPlayer ====================
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.player
                    setBackgroundColor(android.graphics.Color.BLACK)
                }.also { playerViewRef = it }
            },
            modifier = Modifier.fillMaxSize(),
            update = { pv ->
                pv.player = viewModel.player
                pv.resizeMode = when (uiState.zoomMode) {
                    ZoomMode.FIT         -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ZoomMode.FILL        -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                    ZoomMode.ZOOM        -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ZoomMode.FIXED_WIDTH -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
                playerViewRef = pv
            }
        )

        // ==================== منطقة الإيماءات ====================
        GestureOverlay(
            modifier = Modifier.fillMaxSize(),
            isLocked = uiState.isLocked,
            onTap = { showControls = !showControls },
            onDoubleTapLeft  = { viewModel.seekBackward(10000) },
            onDoubleTapRight = { viewModel.seekForward(10000) },
            onTripleTapLeft  = { viewModel.seekBackward(30000) },
            onTripleTapRight = { viewModel.seekForward(30000) },
            onLongPressStart = { viewModel.setSpeed(2f) },
            onLongPressEnd   = { viewModel.resetSpeed() },
            onSwipeRight     = { delta -> viewModel.adjustVolume(delta) },
            onSwipeLeft      = { delta -> viewModel.adjustBrightness(delta, activity) },
            onSwipeCenter    = { delta -> viewModel.seekBySwipe(delta) },
            onThreeFingerTap = { viewModel.takeScreenshot(playerViewRef) },
            onFourFingerTap  = { viewModel.toggleLock() }
        )

        // ==================== عناصر التحكم الرئيسية ====================
        AnimatedVisibility(
            visible = showControls && !uiState.isLocked,
            enter = fadeIn(tween(200)),
            exit  = fadeOut(tween(200))
        ) {
            PlayerControls(
                uiState       = uiState,
                onBack        = onBack,
                onPlayPause   = viewModel::togglePlayPause,
                onSeek        = viewModel::seekTo,
                onNextVideo   = viewModel::playNext,
                onPrevVideo   = viewModel::playPrevious,
                onSpeedChange = viewModel::setSpeed,
                onZoomChange  = viewModel::setZoomMode,
                onABRepeat    = viewModel::toggleABRepeat,
                onSleepTimer  = { mode -> viewModel.setSleepTimer(mode) },
                onAudioOnly   = viewModel::toggleAudioOnly,
                onPiP         = { activity?.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) },
                onSubtitles   = viewModel::toggleSubtitleSettings,
                onRotate      = { viewModel.rotateScreen(activity) },
                onOpenEditor  = onOpenEditor,
                onSubtitleSelect          = { i -> viewModel.selectSubtitle(i) },
                onSecondarySubtitleSelect = { i -> viewModel.selectSecondarySubtitle(i) },
                onSubtitleFontSize        = { s -> viewModel.setSubtitleFontSize(s) },
                onSubtitleDelay           = { d -> viewModel.setSubtitleDelay(d) },
                onSubtitleColor           = { c -> viewModel.setSubtitleColor(c) }
            )
        }

        // ==================== مؤشر القفل ====================
        if (uiState.isLocked) {
            Box(
                modifier = Modifier.fillMaxSize().clickable { viewModel.toggleLock() },
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f)).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("مقفل", color = Color.White, fontSize = 12.sp)
                    Text("اضغط للفتح", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }

        // ==================== مؤشرات الإيماءات ====================
        if (uiState.showVolumeIndicator) {
            VolumeIndicator(volume = uiState.volume,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp))
        }
        if (uiState.showBrightnessIndicator) {
            BrightnessIndicator(brightness = uiState.brightness,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp))
        }
        if (uiState.seekIndicator != 0L) {
            SeekIndicator(seekMs = uiState.seekIndicator, modifier = Modifier.align(Alignment.Center))
        }

        // ==================== مؤشر السرعة ====================
        if (uiState.speed != 1f) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("سرعة: ${uiState.speed}x", color = NetflixRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // ==================== الترجمة (مزدوجة) ====================
        if (uiState.subtitlesEnabled && (uiState.subtitleText.isNotEmpty() || uiState.secondarySubtitleText.isNotEmpty())) {
            SubtitleOverlay(
                text          = uiState.subtitleText,
                secondaryText = uiState.secondarySubtitleText,
                style         = uiState.subtitleStyle,
                modifier      = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls) 110.dp else 40.dp)
            )
        }

        // ==================== مؤقت النوم ====================
        if (uiState.sleepTimerRemaining > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bedtime, null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDuration(uiState.sleepTimerRemaining), color = SpotifyGreen, fontSize = 12.sp)
                }
            }
        }

        // ==================== إشعار لقطة الشاشة ====================
        AnimatedVisibility(
            visible = uiState.showScreenshotToast,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit  = slideOutVertically(targetOffsetY  = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SpotifyGreen.copy(alpha = 0.9f)),
                shape  = RoundedCornerShape(24.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("تم حفظ لقطة الشاشة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==================== مؤشر التخزين المؤقت ====================
        if (uiState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = NetflixRed,
                strokeWidth = 3.dp
            )
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
    onSleepTimer: (SleepTimerMode) -> Unit,
    onAudioOnly: () -> Unit,
    onPiP: () -> Unit,
    onSubtitles: () -> Unit,
    onRotate: () -> Unit,
    onOpenEditor: () -> Unit,
    onSubtitleSelect: (Int) -> Unit,
    onSecondarySubtitleSelect: (Int) -> Unit,
    onSubtitleFontSize: (Int) -> Unit,
    onSubtitleDelay: (Long) -> Unit,
    onSubtitleColor: (androidx.compose.ui.graphics.Color) -> Unit
) {
    var showSpeedMenu    by remember { mutableStateOf(false) }
    var showMoreMenu     by remember { mutableStateOf(false) }
    var showSleepMenu    by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // تدرج علوي
        Box(modifier = Modifier.fillMaxWidth().height(130.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))))

        // تدرج سفلي
        Box(modifier = Modifier.fillMaxWidth().height(170.dp).align(Alignment.BottomCenter)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))))

        // ==================== الشريط العلوي ====================
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "رجوع", tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(uiState.title, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, maxLines = 1)
                Text("${formatDuration(uiState.currentPosition)} / ${formatDuration(uiState.duration)}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            // ترجمة
            IconButton(onClick = { showSubtitleMenu = true }) {
                Icon(Icons.Default.ClosedCaption, "ترجمة",
                    tint = if (uiState.subtitlesEnabled) NetflixRed else Color.White)
            }
            // سرعة
            IconButton(onClick = { showSpeedMenu = true }) {
                Text("${uiState.speed}x", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            // المزيد
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Default.MoreVert, "المزيد", tint = Color.White)
            }
        }

        // ==================== أزرار التحكم الوسطى ====================
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSeek(uiState.currentPosition - 30000) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Replay30, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onPrevVideo) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp)
                )
            }
            IconButton(onClick = onNextVideo) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { onSeek(uiState.currentPosition + 30000) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Forward30, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // ==================== الشريط السفلي ====================
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // مؤشر AB Repeat
            if (uiState.abRepeatA > 0) {
                Text(
                    text = "A: ${formatDuration(uiState.abRepeatA)}${if (uiState.abRepeatB > 0) " → B: ${formatDuration(uiState.abRepeatB)}" else " (اضغط مجددًا لتحديد B)"}",
                    color = SpotifyGreen, fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // شريط التقدم مع Buffer
            Box(modifier = Modifier.fillMaxWidth()) {
                // Buffer
                LinearProgressIndicator(
                    progress = { if (uiState.duration > 0) uiState.buffered.toFloat() / uiState.duration else 0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.CenterStart),
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Slider(
                    value = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration else 0f,
                    onValueChange = { onSeek((it * uiState.duration).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = NetflixRed,
                        activeTrackColor = NetflixRed,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            // صف الأزرار السفلية
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row {
                    PlayerIconButton(Icons.Default.AudioFile,       "صوت فقط",    onAudioOnly,   uiState.audioOnlyMode)
                    PlayerIconButton(Icons.Default.PictureInPicture, "PiP",        onPiP)
                    PlayerIconButton(Icons.Default.Repeat,          "AB",          onABRepeat,    uiState.abRepeatA > 0)
                    PlayerIconButton(Icons.Default.Bedtime,         "مؤقت النوم", { showSleepMenu = true }, uiState.sleepTimerRemaining > 0)
                }
                Row {
                    PlayerIconButton(Icons.Default.ScreenRotation,  "تدوير",   onRotate)
                    PlayerIconButton(Icons.Default.ContentCut,       "محرر",    onOpenEditor)
                }
            }
        }

        // ==================== قوائم منبثقة ====================
        if (showSpeedMenu) {
            SpeedMenu(uiState.speed, { onSpeedChange(it); showSpeedMenu = false }, { showSpeedMenu = false },
                Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 12.dp))
        }
        if (showMoreMenu) {
            MoreMenu({ showMoreMenu = false }, { onZoomChange(it); showMoreMenu = false }, uiState.zoomMode,
                Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 12.dp))
        }
        if (showSleepMenu) {
            SleepTimerMenu(
                onModeSelected = { mode -> onSleepTimer(mode); showSleepMenu = false },
                onDismiss = { showSleepMenu = false },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 12.dp)
            )
        }
        if (uiState.showSubtitleSettings || showSubtitleMenu) {
            SubtitleSettingsSheet(
                uiState = uiState,
                onDismiss = { showSubtitleMenu = false },
                onSubtitleSelect = onSubtitleSelect,
                onSecondarySubtitleSelect = onSecondarySubtitleSelect,
                onFontSize = onSubtitleFontSize,
                onDelay   = onSubtitleDelay,
                onColor   = onSubtitleColor,
                modifier  = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 12.dp)
            )
        }
    }
}

// ==================== زر تحكم صغير ====================
@Composable
fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(onClick = onClick) {
        Icon(icon, label,
            tint = if (active) NetflixRed else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp))
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
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("سرعة التشغيل", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), fontSize = 14.sp)
            speeds.forEach { speed ->
                TextButton(onClick = { onSpeedSelected(speed) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (speed == 1f) "عادي (1x)" else "${speed}x",
                        color = if (speed == currentSpeed) NetflixRed else Color.White,
                        fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

// ==================== قائمة وضع العرض ====================
@Composable
fun MoreMenu(onDismiss: () -> Unit, onZoomChange: (ZoomMode) -> Unit, currentZoom: ZoomMode, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(8.dp).width(180.dp)) {
            Text("وضع العرض", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), fontSize = 14.sp)
            ZoomMode.values().forEach { mode ->
                TextButton(onClick = { onZoomChange(mode) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (mode == currentZoom) Icon(Icons.Default.Check, null, tint = NetflixRed, modifier = Modifier.size(16.dp))
                        else Spacer(Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, color = if (mode == currentZoom) NetflixRed else Color.White)
                    }
                }
            }
        }
    }
}

// ==================== قائمة مؤقت النوم ====================
@Composable
fun SleepTimerMenu(onModeSelected: (SleepTimerMode) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(8.dp).width(200.dp)) {
            Text("مؤقت النوم", color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp), fontSize = 14.sp)
            SleepTimerMode.values().forEach { mode ->
                TextButton(onClick = { onModeSelected(mode) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (mode == SleepTimerMode.OFF) Icons.Default.Close else Icons.Default.Bedtime,
                            null, tint = if (mode == SleepTimerMode.OFF) Color.Gray else SpotifyGreen,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==================== إعدادات الترجمة ====================
@Composable
fun SubtitleSettingsSheet(
    uiState: PlayerUiState,
    onDismiss: () -> Unit,
    onSubtitleSelect: (Int) -> Unit,
    onSecondarySubtitleSelect: (Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onDelay: (Long) -> Unit,
    onColor: (androidx.compose.ui.graphics.Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitleColors = listOf(
        "أبيض" to Color.White,
        "أصفر" to Color.Yellow,
        "أخضر" to SpotifyGreen,
        "أزرق" to Color.Cyan,
        "برتقالي" to Color(0xFFFF8C00)
    )

    Card(modifier = modifier.width(260.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("إعدادات الترجمة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

            // اختيار الترجمة الرئيسية
            Text("الترجمة الرئيسية", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            if (uiState.availableSubtitleFiles.isEmpty()) {
                Text("لا توجد ملفات ترجمة", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            } else {
                TextButton(onClick = { onSubtitleSelect(-1) }, modifier = Modifier.fillMaxWidth()) {
                    Text("إيقاف", color = if (uiState.selectedSubtitleIndex == -1) NetflixRed else Color.White)
                }
                uiState.availableSubtitleFiles.forEachIndexed { index, file ->
                    TextButton(onClick = { onSubtitleSelect(index) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            if (index == uiState.selectedSubtitleIndex)
                                Icon(Icons.Default.Check, null, tint = NetflixRed, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${file.language} - ${file.name.take(20)}",
                                color = if (index == uiState.selectedSubtitleIndex) NetflixRed else Color.White,
                                fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // اختيار الترجمة الثانوية (مزدوجة)
            if (uiState.availableSubtitleFiles.size >= 2) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                Text("ترجمة ثانوية (مزدوجة)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                uiState.availableSubtitleFiles.forEachIndexed { index, file ->
                    TextButton(onClick = { onSecondarySubtitleSelect(index) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth()) {
                            if (index == uiState.secondarySubtitleIndex)
                                Icon(Icons.Default.Check, null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${file.language}", color = if (index == uiState.secondarySubtitleIndex) SpotifyGreen else Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // حجم الخط
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("حجم الخط", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFontSize(uiState.subtitleStyle.fontSize - 2) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("${uiState.subtitleStyle.fontSize}sp", color = Color.White, fontSize = 12.sp)
                    IconButton(onClick = { onFontSize(uiState.subtitleStyle.fontSize + 2) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // تأخير الترجمة
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("تأخير (ms)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDelay(uiState.subtitleStyle.delayMs - 500) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("${uiState.subtitleStyle.delayMs}", color = Color.White, fontSize = 12.sp)
                    IconButton(onClick = { onDelay(uiState.subtitleStyle.delayMs + 500) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // لون الترجمة
            Text("لون الترجمة", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                subtitleColors.forEach { (name, color) ->
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(color)
                            .border(2.dp, if (uiState.subtitleStyle.color == color) Color.White else Color.Transparent, CircleShape)
                            .clickable { onColor(color) }
                    )
                }
            }
        }
    }
}

// ==================== مؤشرات الصوت والسطوع ====================
@Composable
fun VolumeIndicator(volume: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.VolumeUp, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BrightnessIndicator(brightness: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Brightness6, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SeekIndicator(seekMs: Long, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.7f))
        .padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (seekMs > 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("${if (seekMs > 0) "+" else ""}${seekMs / 1000}s",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

// ==================== عرض الترجمة (مزدوجة) ====================
@Composable
fun SubtitleOverlay(text: String, secondaryText: String, style: SubtitleStyle, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (secondaryText.isNotEmpty()) {
            Text(secondaryText, color = style.color.copy(alpha = 0.9f),
                fontSize = style.fontSize.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = style.backgroundAlpha))
                    .padding(horizontal = 10.dp, vertical = 4.dp))
            Spacer(Modifier.height(4.dp))
        }
        if (text.isNotEmpty()) {
            Text(text, color = style.color, fontSize = style.fontSize.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = style.backgroundAlpha))
                    .padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

// ==================== منطقة الإيماءات المتقدمة ====================
@Composable
fun GestureOverlay(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    onTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onTripleTapLeft: () -> Unit,
    onTripleTapRight: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSwipeRight: (Float) -> Unit,
    onSwipeLeft: (Float) -> Unit,
    onSwipeCenter: (Float) -> Unit,
    onThreeFingerTap: () -> Unit,
    onFourFingerTap: () -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectTapGestures(
                    onTap = { offset ->
                        val now = System.currentTimeMillis()
                        val screenWidth = size.width.toFloat()
                        val isLeft  = offset.x < screenWidth / 3f
                        val isRight = offset.x > screenWidth * 2f / 3f

                        if (now - lastTapTime < 400 && kotlin.math.abs(offset.x - lastTapX) < 150) {
                            tapCount++
                            when (tapCount) {
                                2 -> if (isLeft) onDoubleTapLeft() else if (isRight) onDoubleTapRight() else onTap()
                                3 -> { if (isLeft) onTripleTapLeft() else if (isRight) onTripleTapRight(); tapCount = 0 }
                            }
                        } else {
                            tapCount = 1
                            onTap()
                        }
                        lastTapTime = now; lastTapX = offset.x
                    },
                    onLongPress = { onLongPressStart() }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDragEnd = { onLongPressEnd() }
                ) { change, dragAmount ->
                    val sw = size.width.toFloat()
                    val isRight  = change.position.x > sw * 2f / 3f
                    val isLeft   = change.position.x < sw / 3f
                    when {
                        isRight  -> onSwipeRight(-dragAmount.y / size.height)
                        isLeft   -> onSwipeLeft(-dragAmount.y / size.height)
                        else     -> onSwipeCenter(dragAmount.x)
                    }
                }
            }
    )
}
