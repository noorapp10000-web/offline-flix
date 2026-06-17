package com.offlineflix.player.ui.screens.tools

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*

/**
 * شاشة أدوات الجهاز:
 * - اختبار الأداء (4K / 8K / HEVC / AV1)
 * - كاشف التكرار Duplicate Finder
 * - حاسبة الضغط
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceToolsScreen(
    onBack: () -> Unit,
    viewModel: DeviceToolsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    data class DeviceInfo(
        val model:   String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val android: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        val cpu:     String = Build.HARDWARE,
        val ramMB:   Long   = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem / 1024 / 1024 }
    )
    val deviceInfo = remember { DeviceInfo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أدوات الجهاز", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {

            // ==================== معلومات الجهاز ====================
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("معلومات الجهاز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    DeviceInfoRow(Icons.Default.PhoneAndroid, "الموديل",  deviceInfo.model)
                    DeviceInfoRow(Icons.Default.Android,      "الأندرويد", deviceInfo.android)
                    DeviceInfoRow(Icons.Default.Memory,       "المعالج",  deviceInfo.cpu)
                    DeviceInfoRow(Icons.Default.Storage,      "الرام",    "${deviceInfo.ramMB} MB")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ==================== اختبار الأداء ====================
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("اختبار الأداء", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.Speed, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(12.dp))

                    if (!uiState.isBenchmarking && uiState.benchmarkResult == null) {
                        Button(
                            onClick = { viewModel.startBenchmark(deviceInfo.ramMB) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ابدأ الاختبار", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.isBenchmarking) {
                        Text("جاري الاختبار...", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.benchmarkProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = NetflixRed, trackColor = NetflixMediumGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${uiState.benchmarkProgress}%", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }

                    uiState.benchmarkResult?.let { result ->
                        BenchmarkResultCard(result = result)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = viewModel::resetBenchmark, modifier = Modifier.fillMaxWidth()) {
                            Text("إعادة الاختبار", color = NetflixRed)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ==================== كاشف التكرار ====================
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("كاشف الملفات المكررة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.FindReplace, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("يكشف الفيديوهات والملفات المكررة بناءً على الحجم والمدة",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    if (!uiState.isDuplicateScanning && uiState.duplicateGroups.isEmpty()) {
                        Button(
                            onClick = viewModel::startDuplicateScan,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ابحث عن المكررات", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.isDuplicateScanning) {
                        Text("جاري المسح...", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF2196F3), trackColor = NetflixMediumGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("فحص ${uiState.duplicateScanProgress} ملف...",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }

                    if (uiState.duplicateGroups.isNotEmpty()) {
                        val totalWaste = uiState.duplicateGroups.sumOf { group ->
                            group.drop(1).sumOf { it.second }
                        }
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
                            shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("وُجد ${uiState.duplicateGroups.size} مجموعة مكررة • يمكن توفير ${formatSizeMB(totalWaste)}",
                                    color = SpotifyGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        uiState.duplicateGroups.forEachIndexed { groupIdx, group ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                                shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("المجموعة ${groupIdx + 1} (${group.size} ملفات متطابقة)",
                                        color = NetflixRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(Modifier.height(6.dp))
                                    group.forEachIndexed { fileIdx, (path, size) ->
                                        val isOriginal = fileIdx == 0
                                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (isOriginal) Icons.Default.Star else Icons.Default.ContentCopy,
                                                null,
                                                tint = if (isOriginal) SpotifyGreen else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(path.substringAfterLast("/"),
                                                    color = if (isOriginal) Color.White else Color.White.copy(alpha = 0.6f),
                                                    fontSize = 12.sp, maxLines = 1)
                                                Text("${formatSizeMB(size)} • ${if (isOriginal) "الأصل (محفوظ)" else "نسخة مكررة"}",
                                                    color = if (isOriginal) SpotifyGreen.copy(alpha = 0.7f) else Color(0xFFCF6679),
                                                    fontSize = 10.sp)
                                            }
                                            if (!isOriginal) {
                                                TextButton(
                                                    onClick = { viewModel.deleteDuplicate(path, groupIdx, fileIdx) },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                                                ) { Text("حذف", fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = viewModel::resetDuplicateScan,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) { Text("إعادة المسح", color = Color.White, fontSize = 12.sp) }
                            Button(
                                onClick = viewModel::deleteAllDuplicates,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("حذف كل المكررات", fontSize = 12.sp) }
                        }
                    } else if (!uiState.isDuplicateScanning && uiState.duplicateScanDone) {
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
                            shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("لا توجد ملفات مكررة!", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = viewModel::resetDuplicateScan, modifier = Modifier.fillMaxWidth()) {
                            Text("إعادة المسح", color = NetflixRed)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ==================== حاسبة الضغط ====================
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("حاسبة الضغط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Icon(Icons.Default.Calculate, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("احسب حجم الفيديو بعد الضغط قبل ما تبدأ",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    Text("الحجم الأصلي (MB):", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = uiState.compressionInputMB,
                        onValueChange = viewModel::setCompressionInput,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثال: 500", color = Color.White.copy(alpha = 0.3f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = NetflixRed, unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("نسبة الضغط:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Slider(
                        value = uiState.compressionRatio,
                        onValueChange = viewModel::setCompressionRatio,
                        valueRange = 0.1f..0.9f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = NetflixRed, activeTrackColor = NetflixRed)
                    )
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("10%", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Text("ضغط ${((1 - uiState.compressionRatio) * 100).toInt()}%", color = NetflixRed, fontWeight = FontWeight.Bold)
                        Text("90%", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }

                    val inputMB = uiState.compressionInputMB.toFloatOrNull() ?: 0f
                    if (inputMB > 0) {
                        Spacer(Modifier.height(12.dp))
                        val outputMB = inputMB * uiState.compressionRatio
                        val savedMB = inputMB - outputMB
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2A)),
                            shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("الحجم بعد الضغط:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Text("${String.format("%.1f", outputMB)} MB", color = SpotifyGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("المساحة المُوفَّرة:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    Text("${String.format("%.1f", savedMB)} MB", color = NetflixRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun formatSizeMB(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / 1024f / 1024 / 1024)
    bytes >= 1024 * 1024         -> String.format("%.1f MB", bytes / 1024f / 1024)
    else                          -> "${bytes / 1024} KB"
}

@Composable
fun BenchmarkResultCard(result: BenchmarkResult) {
    Column {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("النتيجة: ${result.rating}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${result.score}/100", color = NetflixRed, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { result.score / 100f },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = when { result.score >= 70 -> SpotifyGreen; result.score >= 40 -> NetflixYellow; else -> NetflixRed },
            trackColor = NetflixMediumGray
        )
        Spacer(Modifier.height(12.dp))
        result.recommendations.forEach { rec ->
            Text(rec, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
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
