package com.offlineflix.player.ui.screens.trash

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.data.models.TrashEntity
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatSize
import java.text.SimpleDateFormat
import java.util.*

/**
 * شاشة سلة المحذوفات (30 يوم)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit, viewModel: TrashViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سلة المحذوفات", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFCF6679))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        if (uiState.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, null, tint = NetflixMediumGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("سلة المحذوفات فارغة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("الملفات المحذوفة ستظهر هنا لمدة 30 يوم", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // معلومات السلة
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A1A1A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFCF6679), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${uiState.items.size} ملف • سيُحذف تلقائياً بعد 30 يوم", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }

                LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(uiState.items, key = { it.id }) { item ->
                        TrashItem(
                            item = item,
                            onRestore = { viewModel.restoreItem(item) },
                            onDelete = { viewModel.permanentlyDelete(item) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("حذف نهائي", color = Color.White) },
            text = { Text("سيتم حذف كل الملفات في السلة بشكل دائم ولا يمكن التراجع!", color = Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll(); showClearDialog = false }) {
                    Text("حذف الكل", color = Color(0xFFCF6679), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("إلغاء", color = Color.White) } },
            containerColor = NetflixDarkGray
        )
    }
}

@Composable
fun TrashItem(item: TrashEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    val daysLeft = ((item.expiresAt - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (item.type) {
                    "video" -> Icons.Default.VideoFile
                    "audio" -> Icons.Default.AudioFile
                    "pdf" -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.InsertDriveFile
                },
                null, tint = NetflixRed, modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(
                    "${formatSize(item.size)} • يُحذف بعد $daysLeft يوم",
                    color = if (daysLeft <= 3) Color(0xFFCF6679) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Column {
                TextButton(onClick = onRestore) { Text("استعادة", color = SpotifyGreen, fontSize = 12.sp) }
                TextButton(onClick = onDelete) { Text("حذف", color = Color(0xFFCF6679), fontSize = 12.sp) }
            }
        }
    }
}
