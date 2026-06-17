package com.offlineflix.player.ui.screens.scanning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*

/**
 * شاشة المسح - تعرض تقدم فهرسة الوسائط
 */
@Composable
fun ScanningScreen(
    onScanComplete: () -> Unit,
    viewModel: ScanningViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            kotlinx.coroutines.delay(1500)
            onScanComplete()
        }
    }

    // تحريك الدوران
    val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // تحريك النبض
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NetflixBlack, Color(0xFF0D0D0D)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // أيقونة المسح الدوارة
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // حلقة خارجية دوارة
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(rotation),
                    color = NetflixRed,
                    strokeWidth = 4.dp,
                    progress = { uiState.progress / 100f }
                )
                // أيقونة وسط
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(NetflixDarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NetflixRed,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (uiState.isComplete) "اكتمل المسح! 🎉" else "جاري مسح جهازك...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.currentFolder,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(32.dp))

            // إحصاءات المسح
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScanStatItem(icon = Icons.Default.VideoLibrary, count = uiState.videosFound, label = "فيديو")
                ScanStatItem(icon = Icons.Default.MusicNote, count = uiState.audiosFound, label = "أغنية")
                ScanStatItem(icon = Icons.Default.PictureAsPdf, count = uiState.pdfsFound, label = "PDF")
                ScanStatItem(icon = Icons.Default.Folder, count = uiState.foldersScanned, label = "مجلد")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // شريط التقدم
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.phase,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${uiState.progress}%",
                        color = NetflixRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NetflixRed,
                    trackColor = NetflixMediumGray
                )
            }

            if (uiState.isComplete) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "🎬 جاهز! سيبدأ التطبيق الآن...",
                    color = SpotifyGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ScanStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}
