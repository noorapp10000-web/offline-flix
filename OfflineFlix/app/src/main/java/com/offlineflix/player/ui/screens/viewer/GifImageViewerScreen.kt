package com.offlineflix.player.ui.screens.viewer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.offlineflix.player.ui.theme.*
import java.io.File

/**
 * شاشة عرض GIF و WebP المتحرك و صور عادية
 * تدعم: تكبير/تصغير، تمرير، عرض متعدد الملفات، عرض شرائح
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifImageViewerScreen(
    initialPath: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // قائمة الملفات المفتوحة في الجلسة الحالية
    var files by remember { mutableStateOf(if (initialPath.isNotEmpty()) listOf(initialPath) else emptyList<String>()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }
    var isSlideshow by remember { mutableStateOf(false) }
    var slideshowInterval by remember { mutableIntStateOf(3) }

    // Coil ImageLoader مع دعم GIF
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // منتقي الملف
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val paths = uris.mapNotNull { uri ->
            val path = uri.path
            if (path != null && File(path).exists()) path
            else uri.toString()
        }
        if (paths.isNotEmpty()) {
            files = files + paths
        }
    }

    // Slideshow تلقائي
    LaunchedEffect(isSlideshow, currentIndex) {
        if (isSlideshow && files.isNotEmpty()) {
            kotlinx.coroutines.delay(slideshowInterval * 1000L)
            currentIndex = (currentIndex + 1) % files.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val currentFile = files.getOrNull(currentIndex)
                        Text(
                            text = currentFile?.substringAfterLast("/") ?: "عارض الصور",
                            color = Color.White, fontSize = 14.sp, maxLines = 1
                        )
                        if (files.size > 1) {
                            Text("${currentIndex + 1} / ${files.size}",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    // زر Slideshow
                    IconButton(onClick = { isSlideshow = !isSlideshow }) {
                        Icon(
                            if (isSlideshow) Icons.Default.Pause else Icons.Default.PlayCircle,
                            "عرض شرائح",
                            tint = if (isSlideshow) SpotifyGreen else Color.White
                        )
                    }
                    // فتح ملف
                    IconButton(onClick = { filePicker.launch("image/*") }) {
                        Icon(Icons.Default.FolderOpen, "فتح", tint = Color.White)
                    }
                    // معلومات
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(Icons.Default.Info, "معلومات", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (files.isEmpty()) {
                // شاشة الترحيب
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎞️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("افتح ملف GIF أو WebP أو صورة",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("يدعم: GIF، WebP متحرك، PNG، JPG، BMP",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { filePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("اختر ملف صورة")
                        }
                    }
                }
            } else {
                // ==================== منطقة العرض الرئيسية ====================
                val currentPath = files.getOrElse(currentIndex) { "" }
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .pointerInput(currentPath) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                if (scale > 1f) offset += pan
                                else offset = Offset.Zero
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) { scale = 1f; offset = Offset.Zero }
                                    else scale = 2.5f
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val ext = currentPath.substringAfterLast(".").lowercase()
                    val isAnimated = ext in listOf("gif", "webp")

                    if (isAnimated) {
                        // GIF / WebP متحرك عبر Coil
                        val request = remember(currentPath) {
                            ImageRequest.Builder(context)
                                .data(if (currentPath.startsWith("/")) File(currentPath) else Uri.parse(currentPath))
                                .build()
                        }
                        AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = scale, scaleY = scale,
                                translationX = offset.x, translationY = offset.y
                            ),
                            update = { view ->
                                imageLoader.enqueue(
                                    ImageRequest.Builder(context)
                                        .data(if (currentPath.startsWith("/")) File(currentPath) else Uri.parse(currentPath))
                                        .target(view)
                                        .build()
                                )
                            }
                        )
                        // شارة GIF
                        Box(
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(NetflixRed).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(ext.uppercase(), color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    } else {
                        // صورة عادية
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (currentPath.startsWith("/")) File(currentPath) else Uri.parse(currentPath))
                                .crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer(
                                scaleX = scale, scaleY = scale,
                                translationX = offset.x, translationY = offset.y
                            )
                        )
                    }

                    // بانل المعلومات
                    if (showInfo) {
                        val file = try { File(currentPath) } catch (e: Exception) { null }
                        Card(
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("اسم الملف: ${currentPath.substringAfterLast("/")}",
                                    color = Color.White, fontSize = 11.sp)
                                if (file?.exists() == true) {
                                    Text("الحجم: ${String.format("%.1f", file.length() / 1024f)} KB",
                                        color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                }
                                Text("النوع: ${ext.uppercase()}",
                                    color = if (isAnimated) NetflixRed else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp)
                                Text("تكبير: ${String.format("%.1f", scale)}x",
                                    color = SpotifyGreen, fontSize = 11.sp)
                            }
                        }
                    }

                    // أزرار التنقل
                    if (files.size > 1) {
                        Row(modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            if (currentIndex > 0) {
                                IconButton(
                                    onClick = { currentIndex--; scale = 1f; offset = Offset.Zero },
                                    modifier = Modifier.clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) { Icon(Icons.Default.ChevronLeft, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                            } else { Spacer(Modifier.size(48.dp)) }

                            if (currentIndex < files.size - 1) {
                                IconButton(
                                    onClick = { currentIndex++; scale = 1f; offset = Offset.Zero },
                                    modifier = Modifier.clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) { Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
                            } else { Spacer(Modifier.size(48.dp)) }
                        }
                    }
                }

                // ==================== شريط الصور المصغرة ====================
                if (files.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                            .background(Color.Black).padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(files) { index, path ->
                            Box(
                                modifier = Modifier.size(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        2.dp,
                                        if (index == currentIndex) NetflixRed else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { currentIndex = index; scale = 1f; offset = Offset.Zero }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(if (path.startsWith("/")) File(path) else Uri.parse(path))
                                        .size(128, 128).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Slideshow settings
                if (isSlideshow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircle, null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("عرض شرائح — كل $slideshowInterval ثانية", color = SpotifyGreen, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { if (slideshowInterval > 1) slideshowInterval-- }) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { if (slideshowInterval < 30) slideshowInterval++ }) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { isSlideshow = false }) {
                            Text("إيقاف", color = NetflixRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

