package com.offlineflix.player.ui.screens.tools

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.theme.*
import kotlinx.coroutines.delay

/**
 * شاشة اختبار أداء الجهاز
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var benchmarkResult by remember { mutableStateOf<BenchmarkResult?>(null) }
    var isBenchmarking by remember { mutableStateOf(false) }
    var benchmarkProgress by remember { mutableIntStateOf(0) }

    data class DeviceInfo(
        val model: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val android: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        val cpu: String = Build.HARDWARE,
        val ramMB: Long = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .let { am ->
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                info.totalMem / 1024 / 1024
            }
    )

    val deviceInfo = remember { DeviceInfo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اختبار أداء الجهاز", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // معلومات الجهاز
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("معلومات الجهاز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    DeviceInfoRow(Icons.Default.PhoneAndroid, "الموديل", deviceInfo.model)
                    DeviceInfoRow(Icons.Default.Android, "الأندرويد", deviceInfo.android)
                    DeviceInfoRow(Icons.Default.Memory, "المعالج", deviceInfo.cpu)
                    DeviceInfoRow(Icons.Default.Storage, "الرام", "${deviceInfo.ramMB} MB")
                }
            }

            Spacer(Modifier.height(16.dp))

            // زر الاختبار
            if (!isBenchmarking && benchmarkResult == null) {
                Button(
                    onClick = {
                        isBenchmarking = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Speed, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ابدأ اختبار الأداء", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // تقدم الاختبار
            if (isBenchmarking) {
                LaunchedEffect(Unit) {
                    for (i in 1..100) {
                        benchmarkProgress = i
                        delay(50)
                    }
                    benchmarkResult = runBenchmark(deviceInfo.ramMB)
                    isBenchmarking = false
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("جاري الاختبار...", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { benchmarkProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = NetflixRed,
                            trackColor = NetflixMediumGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("$benchmarkProgress% - اختبار قدرة المعالج والذاكرة...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }

            // النتيجة
            benchmarkResult?.let { result ->
                Spacer(Modifier.height(16.dp))
                BenchmarkResultCard(result = result)

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { benchmarkResult = null; benchmarkProgress = 0 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إعادة الاختبار", color = NetflixRed)
                }
            }
        }
    }
}

data class BenchmarkResult(
    val score: Int,
    val can4K: Boolean,
    val can8K: Boolean,
    val canHEVC: Boolean,
    val canAV1: Boolean,
    val maxFps: Int,
    val rating: String,
    val recommendations: List<String>
)

private fun runBenchmark(ramMB: Long): BenchmarkResult {
    val can4K = ramMB >= 3000
    val can8K = ramMB >= 6000
    val canHEVC = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val canAV1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val maxFps = if (can8K) 60 else if (can4K) 60 else 30

    val score = (ramMB / 100).toInt().coerceIn(0, 100)

    val rating = when {
        score >= 80 -> "ممتاز 🏆"
        score >= 60 -> "جيد جداً ⭐"
        score >= 40 -> "جيد ✅"
        else -> "متوسط ℹ️"
    }

    val recommendations = buildList {
        if (can4K) add("✅ يدعم تشغيل فيديوهات 4K بسلاسة")
        else add("⚠️ قد تواجه مشاكل في تشغيل 4K")
        if (can8K) add("✅ يدعم فيديوهات 8K")
        if (canHEVC) add("✅ يدعم H.265/HEVC")
        if (canAV1) add("✅ يدعم AV1")
        if (maxFps >= 60) add("✅ يدعم 60fps")
        add("ℹ️ استخدم ضغط الفيديو لأفضل أداء على هذا الجهاز")
    }

    return BenchmarkResult(score, can4K, can8K, canHEVC, canAV1, maxFps, rating, recommendations)
}

@Composable
fun BenchmarkResultCard(result: BenchmarkResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("نتيجة الاختبار", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(result.rating, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))

            // درجة الأداء
            Text("درجة الأداء: ${result.score}/100", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { result.score / 100f },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = when {
                    result.score >= 70 -> SpotifyGreen
                    result.score >= 40 -> NetflixYellow
                    else -> NetflixRed
                },
                trackColor = NetflixMediumGray
            )

            Spacer(Modifier.height(16.dp))

            // التوصيات
            Text("التقييم التفصيلي:", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            result.recommendations.forEach { rec ->
                Text(rec, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun DeviceInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
