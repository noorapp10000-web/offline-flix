package com.offlineflix.player.ui.components

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.theme.NetflixRed
import com.offlineflix.player.ui.theme.NetflixDarkGray
import com.offlineflix.player.ui.theme.NetflixMediumGray
import kotlinx.coroutines.delay

/**
 * شريط التخزين العلوي - يعرض المساحة المستخدمة/الكلية بشكل لحظي
 */
@Composable
fun StorageBar() {
    var totalSpace by remember { mutableLongStateOf(0L) }
    var usedSpace by remember { mutableLongStateOf(0L) }

    // تحديث لحظي كل 5 ثواني
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val stat = StatFs(Environment.getExternalStorageDirectory().path)
                totalSpace = stat.totalBytes
                usedSpace = totalSpace - stat.availableBytes
            } catch (e: Exception) { }
            delay(5000)
        }
    }

    val usagePercent = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace) else 0f
    val usedGB = usedSpace / (1024f * 1024 * 1024)
    val totalGB = totalSpace / (1024f * 1024 * 1024)

    // لون الشريط بناءً على النسبة
    val barColor = when {
        usagePercent > 0.9f -> Color(0xFFCF6679)
        usagePercent > 0.75f -> Color(0xFFF5A623)
        else -> NetflixRed
    }

    // تحريك شريط التقدم
    val animatedProgress by animateFloatAsState(
        targetValue = usagePercent,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "storage_progress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NetflixDarkGray,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Storage,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "التخزين",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "%.1f GB / %.1f GB".format(usedGB, totalGB),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                // تحذير إذا التخزين ممتلئ
                if (usagePercent > 0.9f) {
                    Text(
                        text = "⚠ التخزين ممتلئ!",
                        color = Color(0xFFCF6679),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // شريط التقدم
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NetflixMediumGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(barColor.copy(alpha = 0.8f), barColor)
                            )
                        )
                )
            }
        }
    }
}
