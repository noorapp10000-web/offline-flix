package com.offlineflix.player.ui.screens.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfId: Long,
    onBack: () -> Unit,
    viewModel: PdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDarkMode by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }

    LaunchedEffect(pdfId) { viewModel.loadPdf(pdfId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.currentPdf?.name ?: "",
                            color = Color.White, fontSize = 14.sp, maxLines = 1
                        )
                        if (uiState.pageCount > 0) {
                            Text(
                                "صفحة ${uiState.currentPage + 1} / ${uiState.pageCount}",
                                color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isDarkMode = !isDarkMode }) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            null, tint = Color.White
                        )
                    }
                    IconButton(onClick = { showBookmarks = !showBookmarks }) {
                        Icon(Icons.Default.Bookmark, null, tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.addBookmark(uiState.currentPage) }) {
                        Icon(Icons.Default.BookmarkAdd, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = if (isDarkMode) NetflixBlack else Color(0xFF2B2B2B)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val pdf = uiState.currentPdf
            if (pdf != null) {
                PdfPageView(
                    pdfPath      = pdf.path,
                    pageIndex    = uiState.currentPage,
                    pageCount    = uiState.pageCount,
                    isDark       = isDarkMode,
                    onPageLoaded  = { count -> viewModel.onPdfLoaded(count) },
                    onPageChanged = { page  -> viewModel.onPageChanged(page) }
                )
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed)
                }
            }
        }
    }

    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("العلامات المرجعية", color = Color.White) },
            containerColor = NetflixDarkGray,
            text = {
                if (uiState.bookmarks.isEmpty()) {
                    Text(
                        "لا توجد علامات مرجعية بعد\nاضغط ➕ لإضافة علامة للصفحة الحالية",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    Column {
                        uiState.bookmarks.forEach { bookmark ->
                            TextButton(
                                onClick = {
                                    viewModel.onPageChanged(bookmark.pageNumber)
                                    showBookmarks = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth()) {
                                    Icon(
                                        Icons.Default.Bookmark, null,
                                        tint = NetflixRed, modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "صفحة ${bookmark.pageNumber}: ${bookmark.title}",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) {
                    Text("إغلاق", color = NetflixRed)
                }
            }
        )
    }
}

@Composable
private fun PdfPageView(
    pdfPath: String,
    pageIndex: Int,
    pageCount: Int,
    isDark: Boolean,
    onPageLoaded: (Int) -> Unit,
    onPageChanged: (Int) -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var totalPages by remember { mutableIntStateOf(pageCount) }

    suspend fun renderPage(index: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(pdfPath)
            if (!file.exists()) return@withContext null
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (totalPages == 0) {
                totalPages = renderer.pageCount
                onPageLoaded(renderer.pageCount)
            }
            val page = renderer.openPage(index.coerceIn(0, renderer.pageCount - 1))
            val w = page.width * 2
            val h = page.height * 2
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(
                if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bmp
        } catch (_: Exception) { null }
    }

    LaunchedEffect(pdfPath) {
        bitmap = renderPage(pageIndex)
    }

    LaunchedEffect(pageIndex, isDark) {
        bitmap = renderPage(pageIndex)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "PDF صفحة ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (totalPages > 1) 72.dp else 0.dp)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(500.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NetflixRed)
            }
        }

        if (totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (pageIndex > 0) onPageChanged(pageIndex - 1) },
                    enabled = pageIndex > 0
                ) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                }
                Text("${pageIndex + 1} / $totalPages", color = Color.White, fontSize = 14.sp)
                IconButton(
                    onClick = { if (pageIndex < totalPages - 1) onPageChanged(pageIndex + 1) },
                    enabled = pageIndex < totalPages - 1
                ) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.White)
                }
            }

            // شريط الصفحات السريع
            if (totalPages > 1) {
                val listState = rememberLazyListState()
                LaunchedEffect(pageIndex) {
                    listState.animateScrollToItem(pageIndex.coerceIn(0, totalPages - 1))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(vertical = 8.dp)
                ) {
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(List(totalPages) { it }) { index, _ ->
                            val isActive = index == pageIndex
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isActive) NetflixRed else Color(0xFF3A3A3A))
                                    .border(
                                        width = if (isActive) 2.dp else 0.5.dp,
                                        color = if (isActive) NetflixRed else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onPageChanged(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = if (isActive) Color.White
                                    else Color.White.copy(alpha = 0.7f),
                                    fontSize = if (isActive) 13.sp else 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
