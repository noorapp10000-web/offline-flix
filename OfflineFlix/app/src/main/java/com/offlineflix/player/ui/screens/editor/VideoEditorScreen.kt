package com.offlineflix.player.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatDuration

/**
 * شاشة محرر الفيديو المتكامل باستخدام FFmpegKit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    videoId: Long,
    onBack: () -> Unit,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(videoId) { viewModel.loadVideo(videoId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محرر الفيديو", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            // معلومات الفيديو
            if (uiState.videoName.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NetflixDarkGray)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoFile, null, tint = NetflixRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(uiState.videoName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(uiState.videoInfo, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // شريط التقدم
            if (uiState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NetflixDarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(uiState.processingStatus, color = Color.White, fontSize = 13.sp)
                            Text("${uiState.processingProgress}%", color = NetflixRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.processingProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = NetflixRed,
                            trackColor = NetflixMediumGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("الوقت المتبقي: ${uiState.remainingTime}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::cancelProcessing,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCF6679)),
                            border = BorderStroke(1.dp, Color(0xFFCF6679))
                        ) {
                            Text("إيقاف العملية")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // أقسام الأدوات
            EditorSection(title = "القص والتقطيع") {
                EditorToolButton(Icons.Default.ContentCut, "قص الفيديو", "حدد نقطة البداية والنهاية") { viewModel.openTrimmer() }
                EditorToolButton(Icons.Default.BlurLinear, "قص بدون إعادة ترميز", "Lossless - سريع جداً") { viewModel.losslessCut() }
            }
            EditorSection(title = "الدمج والتجميع") {
                EditorToolButton(Icons.Default.MergeType, "دمج فيديوهات", "ادمج أكثر من فيديو معاً") { viewModel.openMerger() }
                EditorToolButton(Icons.Default.Queue, "إضافة فيديو للنهاية") { viewModel.appendVideo() }
            }
            EditorSection(title = "الصوت") {
                EditorToolButton(Icons.Default.AudioFile, "استخراج الصوت", "MP3/FLAC/WAV/AAC") { viewModel.extractAudio() }
                EditorToolButton(Icons.Default.MusicNote, "إضافة موسيقى خلفية") { viewModel.addBackgroundMusic() }
                EditorToolButton(Icons.Default.VolumeUp, "رفع الصوت 600%") { viewModel.boostVolume(600) }
                EditorToolButton(Icons.Default.VolumeMute, "إزالة الصوت الأصلي") { viewModel.muteOriginalAudio() }
            }
            EditorSection(title = "الضغط والتحسين") {
                EditorToolButton(Icons.Default.Compress, "ضغط ذكي 70-80%", "بدون فقد ملحوظ بالجودة") { viewModel.smartCompress() }
                EditorToolButton(Icons.Default.HighQuality, "تغيير الجودة/Bitrate") { viewModel.changeBitrate() }
                EditorToolButton(Icons.Default.AspectRatio, "تغيير الدقة") { viewModel.changeResolution() }
            }
            EditorSection(title = "الحركة والتأثيرات") {
                EditorToolButton(Icons.Default.SlowMotion_24fps, "Slow Motion") { viewModel.applySlowMotion() }
                EditorToolButton(Icons.Default.FastForward, "Fast Motion") { viewModel.applyFastMotion() }
                EditorToolButton(Icons.Default.Replay, "عكس الفيديو (Reverse)") { viewModel.reverseVideo() }
                EditorToolButton(Icons.Default.RotateRight, "تدوير 90/180/270") { viewModel.rotateVideo() }
                EditorToolButton(Icons.Default.Flip, "قلب أفقي/رأسي") { viewModel.flipVideo() }
            }
            EditorSection(title = "الفلاتر البصرية") {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("أبيض وأسود", "Sepia", "تباين عالي", "تشبع", "Blur", "تمييل", "صورة نيغاتيف", "دفء", "برودة", "Pixelate", "Vignette", "HDR وهمي")
                    items(filters) { filter ->
                        FilterChip(
                            selected = uiState.selectedFilter == filter,
                            onClick = { viewModel.applyFilter(filter) },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NetflixRed,
                                selectedLabelColor = Color.White,
                                containerColor = NetflixDarkGray,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
            EditorSection(title = "الإضافات النصية") {
                EditorToolButton(Icons.Default.TextFields, "إضافة نص متحرك") { viewModel.addText() }
                EditorToolButton(Icons.Default.EmojiEmotions, "إضافة ستيكر/إيموجي") { viewModel.addSticker() }
                EditorToolButton(Icons.Default.Gif, "تصدير كـ GIF") { viewModel.exportAsGif() }
                EditorToolButton(Icons.Default.Photo, "مولد الميمز") { viewModel.openMemeMaker() }
                EditorToolButton(Icons.Default.PhotoLibrary, "استخراج كل الإطارات") { viewModel.extractFrames() }
            }
            EditorSection(title = "معلومات الفيديو") {
                EditorToolButton(Icons.Default.Timer, "تغيير FPS") { viewModel.changeFps() }
                EditorToolButton(Icons.Default.Videocam, "تغيير Codec") { viewModel.changeCodec() }
                EditorToolButton(Icons.Default.BrokenImage, "إصلاح ملف تالف") { viewModel.repairVideo() }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, color = NetflixRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun EditorToolButton(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(NetflixRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.3f))
}
