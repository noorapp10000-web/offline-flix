package com.offlineflix.player.ui.screens.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.theme.*

/**
 * شاشة طلب الصلاحيات مع شرح بالعربي
 */
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var allGranted by remember { mutableStateOf(false) }

    // قائمة الصلاحيات المطلوبة
    data class PermissionItem(
        val icon: ImageVector,
        val title: String,
        val desc: String,
        val isGranted: Boolean
    )

    fun checkAllPermissions(): Boolean {
        val storageOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        val mediaOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        return storageOk && mediaOk
    }

    LaunchedEffect(Unit) {
        allGranted = checkAllPermissions()
        if (allGranted) onPermissionsGranted()
    }

    // مشغل الصلاحية الكاملة (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        allGranted = checkAllPermissions()
        if (allGranted) onPermissionsGranted()
    }

    // مشغل الصلاحيات العادية
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allGranted = checkAllPermissions()
        if (allGranted) onPermissionsGranted()
    }

    // تحريك اللوجو
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NetflixBlack, Color(0xFF1A0000), NetflixBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // لوجو
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NetflixRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    fontSize = 48.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OfflineFlix",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NetflixRed
            )

            Text(
                text = "مشغل الوسائط الأوفلاين الكامل",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // بطاقة الشرح
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NetflixDarkGray)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "لماذا نحتاج هذه الصلاحيات؟",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionRow(
                        icon = Icons.Default.FolderOpen,
                        title = "الوصول الكامل للتخزين",
                        desc = "لمسح كل الفيديوهات والأغاني وملفات PDF من جهازك تلقائياً بما فيها WhatsApp وTelegram"
                    )
                    Divider(color = NetflixMediumGray, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow(
                        icon = Icons.Default.VideoLibrary,
                        title = "قراءة ملفات الفيديو",
                        desc = "للوصول لكل الفيديوهات الموجودة على الجهاز وعرضها في المكتبة"
                    )
                    Divider(color = NetflixMediumGray, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow(
                        icon = Icons.Default.AudioFile,
                        title = "قراءة ملفات الصوت",
                        desc = "للوصول لكل الأغاني وعرضها في مشغل الموسيقى"
                    )
                    Divider(color = NetflixMediumGray, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "إشعارات التشغيل",
                        desc = "للتحكم في التشغيل من شريط الإشعارات وشاشة القفل"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ملاحظة الأمان
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A3A1A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔒 كل البيانات تبقى على جهازك فقط. لا إنترنت، لا سيرفرات، لا Firebase. 100% أوفلاين.",
                            color = SpotifyGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // زر منح الصلاحيات
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ - طلب MANAGE_EXTERNAL_STORAGE
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        manageStorageLauncher.launch(intent)
                    } else {
                        val perms = buildList {
                            add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.READ_MEDIA_VIDEO)
                                add(Manifest.permission.READ_MEDIA_AUDIO)
                                add(Manifest.permission.READ_MEDIA_IMAGES)
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        permissionsLauncher.launch(perms.toTypedArray())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "منح الصلاحيات والبدء",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "بعد منح الصلاحيات سيقوم التطبيق تلقائياً بمسح جهازك وجلب كل الوسائط",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NetflixRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
            Text(text = desc, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
