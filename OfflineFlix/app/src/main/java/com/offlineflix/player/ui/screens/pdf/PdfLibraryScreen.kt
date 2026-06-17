package com.offlineflix.player.ui.screens.pdf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.data.models.PdfEntity
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfLibraryScreen(
    onPdfClick: (Long) -> Unit,
    viewModel: PdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val addPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPdfManually(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(NetflixBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.search(it) },
                placeholder = { Text("ابحث في ملفات PDF...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = NetflixRed, unfocusedBorderColor = NetflixMediumGray,
                    cursorColor = NetflixRed, focusedContainerColor = NetflixDarkGray, unfocusedContainerColor = NetflixDarkGray
                ),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            if (uiState.pdfs.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = NetflixRed, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("لا توجد ملفات PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اضغط + لإضافة PDF أو افتح شاشة المسح",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 88.dp)
                ) {
                    items(uiState.pdfs, key = { it.id }) { pdf ->
                        PdfListItem(
                            pdf = pdf,
                            onClick = { onPdfClick(pdf.id) },
                            onDelete = { viewModel.moveToTrash(pdf.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { addPdfLauncher.launch("application/pdf") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            containerColor = NetflixRed,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة PDF")
        }
    }
}

@Composable
fun PdfListItem(pdf: PdfEntity, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(NetflixRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = NetflixRed, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pdf.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pdf.pageCount > 0) Text("${pdf.pageCount} صفحة", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text(formatSize(pdf.size), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    if (pdf.lastOpenedPage > 0) Text("آخر صفحة: ${pdf.lastOpenedPage}", color = NetflixRed, fontSize = 11.sp)
                }
            }
            if (pdf.isFavorite) Icon(Icons.Default.Star, null, tint = NetflixYellow, modifier = Modifier.size(20.dp))
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = NetflixRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
