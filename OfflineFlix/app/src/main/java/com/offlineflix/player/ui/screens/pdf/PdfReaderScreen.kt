package com.offlineflix.player.ui.screens.pdf

import android.graphics.Color as AColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.offlineflix.player.ui.theme.*
import java.io.File

/**
 * شاشة قارئ PDF الكامل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    pdfId: Long,
    onBack: () -> Unit,
    viewModel: PdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(false) }
    var pdfViewRef by remember { mutableStateOf<PDFView?>(null) }

    LaunchedEffect(pdfId) { viewModel.loadPdf(pdfId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.currentPdf?.name ?: "", color = Color.White, fontSize = 14.sp, maxLines = 1)
                        if (uiState.pageCount > 0) {
                            Text("صفحة ${uiState.currentPage + 1} / ${uiState.pageCount}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = { isDarkMode = !isDarkMode }) {
                        Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = Color.White)
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
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
            if (uiState.currentPdf != null) {
                val pdfPath = uiState.currentPdf!!.path

                AndroidView(
                    factory = { ctx ->
                        PDFView(ctx, null).also { pdfView ->
                            pdfViewRef = pdfView
                            pdfView.fromFile(File(pdfPath))
                                .defaultPage(uiState.currentPdf!!.lastOpenedPage)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .enableAntialiasing(true)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .nightMode(isDarkMode)
                                .scrollHandle(DefaultScrollHandle(ctx))
                                .onLoad(OnLoadCompleteListener { nbPages ->
                                    viewModel.onPdfLoaded(nbPages)
                                })
                                .onPageChange(OnPageChangeListener { page, _ ->
                                    viewModel.onPageChanged(page)
                                })
                                .onPageError(OnPageErrorListener { _, _ -> })
                                .load()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { pdfView ->
                        pdfView.isNightMode = isDarkMode
                    }
                )

                // شريط تصغير الصفحات (Thumbnails)
                if (uiState.showThumbnails) {
                    Text("Thumbnails", color = Color.White) // سيُستبدل بشريط فعلي
                }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = NetflixRed)
                }
            }
        }
    }

    // حوار البحث
    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("بحث في الـ PDF", color = Color.White) },
            containerColor = NetflixDarkGray,
            text = {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::searchInPdf,
                    label = { Text("اكتب للبحث...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NetflixRed, unfocusedBorderColor = NetflixMediumGray)
                )
            },
            confirmButton = {
                TextButton(onClick = { showSearch = false }) { Text("إغلاق", color = NetflixRed) }
            }
        )
    }

    // حوار العلامات المرجعية
    if (showBookmarks) {
        AlertDialog(
            onDismissRequest = { showBookmarks = false },
            title = { Text("العلامات المرجعية", color = Color.White) },
            containerColor = NetflixDarkGray,
            text = {
                if (uiState.bookmarks.isEmpty()) {
                    Text("لا توجد علامات مرجعية بعد\nاضغط ➕ لإضافة علامة للصفحة الحالية", color = Color.White.copy(alpha = 0.6f))
                } else {
                    Column {
                        uiState.bookmarks.forEach { bookmark ->
                            TextButton(
                                onClick = {
                                    pdfViewRef?.jumpTo(bookmark.pageNumber)
                                    showBookmarks = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Bookmark, null, tint = NetflixRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("صفحة ${bookmark.pageNumber}: ${bookmark.title}", color = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarks = false }) { Text("إغلاق", color = NetflixRed) }
            }
        )
    }
}
