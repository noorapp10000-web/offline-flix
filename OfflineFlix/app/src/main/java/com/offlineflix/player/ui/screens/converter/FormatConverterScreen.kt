package com.offlineflix.player.ui.screens.converter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*

/**
 * محول الصيغ الشامل - أي صيغة لأي صيغة
 * يدعم الفيديو والصوت والصور و PDF وأكثر من 400 صيغة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatConverterScreen(
    onBack: () -> Unit,
    viewModel: FormatConverterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // منتقي الملف
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setInputFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محول الصيغ الشامل", color = Color.White) },
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // ==================== اختيار الملف ====================
            Card(
                modifier = Modifier.fillMaxWidth().clickable { filePicker.launch("*/*") },
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.inputFile.isNotEmpty()) NetflixDarkGray else NetflixDarkGray
                ),
                border = BorderStroke(
                    2.dp,
                    if (uiState.inputFile.isNotEmpty()) NetflixRed else NetflixMediumGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (uiState.inputFile.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.FileUpload,
                        null,
                        tint = if (uiState.inputFile.isNotEmpty()) NetflixRed else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (uiState.inputFile.isNotEmpty()) uiState.inputFileName else "اضغط لاختيار ملف",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    if (uiState.inputFile.isNotEmpty()) {
                        Text("النوع: ${uiState.detectedType} • الصيغة: ${uiState.detectedFormat}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    } else {
                        Text("أي صيغة - فيديو، صوت، صورة، PDF...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ==================== اختيار الصيغة المستهدفة ====================
            Text("تحويل إلى:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            // تبويبات الفئات
            val categories = listOf("فيديو", "صوت", "صورة", "مستندات", "أخرى")
            var selectedCategory by remember { mutableStateOf("فيديو") }

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = NetflixDarkGray,
                contentColor = NetflixRed,
                edgePadding = 0.dp
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = {
                            Text(cat, color = if (selectedCategory == cat) NetflixRed else Color.White.copy(alpha = 0.6f))
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // صيغ حسب الفئة
            val formats = when (selectedCategory) {
                "فيديو" -> listOf("MP4", "MKV", "AVI", "MOV", "WMV", "FLV", "WebM", "TS", "M4V", "3GP", "OGV", "MPEG", "VOB")
                "صوت" -> listOf("MP3", "FLAC", "WAV", "AAC", "M4A", "OGG", "WMA", "OPUS", "APE", "AC3", "DTS", "AIFF")
                "صورة" -> listOf("JPG", "PNG", "WebP", "BMP", "TIFF", "ICO", "GIF")
                "مستندات" -> listOf("PDF")
                else -> listOf("GIF", "SRT", "VTT", "M3U")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(formats) { format ->
                    FilterChip(
                        selected = uiState.outputFormat == format,
                        onClick = { viewModel.setOutputFormat(format) },
                        label = { Text(format, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NetflixRed,
                            selectedLabelColor = Color.White,
                            containerColor = NetflixDarkGray,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ==================== خيارات متقدمة ====================
            if (uiState.outputFormat.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("خيارات متقدمة", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))

                        // Codec
                        if (uiState.availableCodecs.isNotEmpty()) {
                            Text("Codec:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(uiState.availableCodecs) { codec ->
                                    FilterChip(
                                        selected = uiState.selectedCodec == codec,
                                        onClick = { viewModel.setCodec(codec) },
                                        label = { Text(codec, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NetflixRed.copy(alpha = 0.7f),
                                            selectedLabelColor = Color.White,
                                            containerColor = NetflixMediumGray,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // Bitrate
                        Text("Bitrate: ${uiState.selectedBitrate}", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                        Slider(
                            value = uiState.bitrateIndex.toFloat(),
                            onValueChange = { viewModel.setBitrateIndex(it.toInt()) },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = NetflixRed, activeTrackColor = NetflixRed)
                        )

                        // Resolution
                        if (uiState.isVideoFormat) {
                            Text("الدقة:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("نفس الأصلي", "480p", "720p", "1080p", "4K")) { res ->
                                    FilterChip(
                                        selected = uiState.selectedResolution == res,
                                        onClick = { viewModel.setResolution(res) },
                                        label = { Text(res, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NetflixRed.copy(alpha = 0.7f),
                                            selectedLabelColor = Color.White,
                                            containerColor = NetflixMediumGray,
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // تقدير الحجم
                        if (uiState.estimatedSize > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Calculate, null, tint = NetflixRed, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("الحجم المتوقع:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text(com.offlineflix.player.utils.formatSize(uiState.estimatedSize), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ==================== شريط التقدم ====================
            if (uiState.isConverting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NetflixDarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("جاري التحويل...", color = Color.White)
                            Text("${uiState.progress}%", color = NetflixRed, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = NetflixRed,
                            trackColor = NetflixMediumGray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("السرعة: ${uiState.conversionSpeed} • الوقت المتبقي: ${uiState.remainingTime}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = viewModel::cancelConversion,
                            border = BorderStroke(1.dp, Color(0xFFCF6679)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCF6679))
                        ) { Text("إيقاف") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ==================== زر التحويل ====================
            Button(
                onClick = viewModel::startConversion,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.inputFile.isNotEmpty() && uiState.outputFormat.isNotEmpty() && !uiState.isConverting,
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Transform, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "ابدأ التحويل ${if (uiState.inputFile.isNotEmpty() && uiState.outputFormat.isNotEmpty()) "(${uiState.detectedFormat} → ${uiState.outputFormat})" else ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // رسالة النجاح
            if (uiState.isComplete) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SpotifyGreen)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("تم التحويل بنجاح! ✅", color = SpotifyGreen, fontWeight = FontWeight.Bold)
                            Text(uiState.outputPath, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
