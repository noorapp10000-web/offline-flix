package com.offlineflix.player.ui.screens.filemanager

import android.content.Intent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offlineflix.player.ui.theme.*
import com.offlineflix.player.utils.formatSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * مدير الملفات المصغر
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(onBack: () -> Unit, viewModel: FileManagerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFile by remember { mutableStateOf<File?>(null) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentPath.substringAfterLast("/").ifEmpty { "التخزين الداخلي" },
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // مسار التنقل
            Text(
                text = uiState.currentPath,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LazyColumn {
                items(uiState.files) { file ->
                    FileItem(
                        file = file,
                        onClick = {
                            if (file.isDirectory) viewModel.openDirectory(file.absolutePath)
                        },
                        onRename = {
                            renameFile = file
                            newName = file.nameWithoutExtension
                            showRenameDialog = true
                        },
                        onDelete = { viewModel.deleteFile(file) },
                        onCopy = { viewModel.copyFile(file) },
                        onMove = { viewModel.moveFile(file) }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("إعادة تسمية", color = Color.White) },
            containerColor = NetflixDarkGray,
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم الجديد") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NetflixRed, unfocusedBorderColor = NetflixMediumGray)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameFile?.let { viewModel.renameFile(it, newName) }
                    showRenameDialog = false
                }) { Text("تأكيد", color = NetflixRed) }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("إلغاء", color = Color.White) } }
        )
    }
}

@Composable
fun FileItem(file: File, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onCopy: () -> Unit, onMove: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                file.isDirectory -> Icons.Default.Folder
                file.extension.lowercase() in listOf("mp4", "mkv", "avi") -> Icons.Default.VideoFile
                file.extension.lowercase() in listOf("mp3", "flac", "wav") -> Icons.Default.AudioFile
                file.extension.lowercase() == "pdf" -> Icons.Default.PictureAsPdf
                file.extension.lowercase() in listOf("jpg", "jpeg", "png") -> Icons.Default.Image
                else -> Icons.Default.InsertDriveFile
            },
            contentDescription = null,
            tint = when {
                file.isDirectory -> NetflixYellow
                file.extension.lowercase() in listOf("mp4", "mkv", "avi") -> NetflixRed
                file.extension.lowercase() == "pdf" -> Color(0xFFFF6633)
                else -> Color.White.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(32.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(file.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (file.isDirectory) "مجلد" else "${formatSize(file.length())} • ${dateFormat.format(Date(file.lastModified()))}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(alpha = 0.5f))
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = NetflixDarkGray
            ) {
                DropdownMenuItem(text = { Text("إعادة تسمية", color = Color.White) }, onClick = { onRename(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = NetflixRed) })
                DropdownMenuItem(text = { Text("نسخ", color = Color.White) }, onClick = { onCopy(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color.White) })
                DropdownMenuItem(text = { Text("نقل", color = Color.White) }, onClick = { onMove(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.DriveFileMove, null, tint = Color.White) })
                DropdownMenuItem(text = { Text("حذف", color = Color(0xFFCF6679)) }, onClick = { onDelete(); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFCF6679)) })
            }
        }
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.2f))
}
