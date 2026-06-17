package com.offlineflix.player.ui.screens.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.theme.*

/**
 * شاشة الإعدادات الكاملة
 */
@Composable
fun SettingsScreen(
    onOpenTrash: () -> Unit,
    onOpenHowTo: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenDeviceTools: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(NetflixBlack).verticalScroll(rememberScrollState())
    ) {
        // رأس الصفحة
        Box(
            modifier = Modifier.fillMaxWidth().background(NetflixDarkGray).padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(NetflixRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", fontSize = 28.sp, color = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("OfflineFlix", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("v1.0.0 • 100% Offline", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ==================== قسم الملفات ====================
        SettingsSection(title = "إدارة الملفات") {
            SettingsItem(Icons.Default.Refresh, "إعادة مسح الملفات", "مسح الجهاز بحثاً عن ملفات جديدة") {}
            SettingsItem(Icons.Default.Delete, "سلة المحذوفات", "استعادة أو حذف الملفات المحذوفة", onClick = onOpenTrash)
            SettingsItem(Icons.Default.FolderOpen, "مدير الملفات", onClick = onOpenFileManager)
            SettingsItem(Icons.Default.FindReplace, "كاشف التكرار", "اعثر على الفيديوهات المكررة") {}
            SettingsItem(Icons.Default.Schedule, "جدولة الحذف", "احذف ملفات تلقائياً بعد 30 يوم") {}
        }

        // ==================== قسم التحويل ====================
        SettingsSection(title = "الأدوات") {
            SettingsItem(Icons.Default.Transform, "محول الصيغ الشامل", "+400 صيغة", onClick = onOpenConverter)
            SettingsItem(Icons.Default.Speed, "اختبار أداء الجهاز", "تحقق من قدرة جهازك على تشغيل 4K", onClick = onOpenDeviceTools)
            SettingsItem(Icons.Default.Photo, "مشغل الصور والـ GIF", "عرض الصور والـ GIF المتحركة") {}
        }

        // ==================== قسم الواجهة ====================
        SettingsSection(title = "الواجهة والمظهر") {
            var darkMode by remember { mutableStateOf(true) }
            SettingsSwitchItem(Icons.Default.DarkMode, "الوضع الليلي", "يوفر راحة للعيون", darkMode) { darkMode = it }

            var autoPlay by remember { mutableStateOf(true) }
            SettingsSwitchItem(Icons.Default.PlayArrow, "تشغيل تلقائي للحلقة التالية", checked = autoPlay) { autoPlay = it }

            var rememberPosition by remember { mutableStateOf(true) }
            SettingsSwitchItem(Icons.Default.History, "تذكر موضع التشغيل", checked = rememberPosition) { rememberPosition = it }
        }

        // ==================== قسم المشغل ====================
        SettingsSection(title = "إعدادات المشغل") {
            var doubleTapSeek by remember { mutableStateOf("10 ثواني") }
            SettingsItemWithValue(Icons.Default.TouchApp, "Double Tap للتخطي", doubleTapSeek) {
                doubleTapSeek = when (doubleTapSeek) {
                    "5 ثواني" -> "10 ثواني"
                    "10 ثواني" -> "15 ثواني"
                    "15 ثواني" -> "30 ثواني"
                    else -> "5 ثواني"
                }
            }

            var defaultZoom by remember { mutableStateOf("ملاءمة الشاشة") }
            SettingsItemWithValue(Icons.Default.AspectRatio, "وضع الزوم الافتراضي", defaultZoom) {}

            var maxVolume by remember { mutableStateOf("600%") }
            SettingsItemWithValue(Icons.Default.VolumeUp, "أقصى رفع صوت", maxVolume) {
                maxVolume = when (maxVolume) { "300%" -> "600%"; "600%" -> "1000%"; else -> "300%" }
            }
        }

        // ==================== قسم الترجمة ====================
        SettingsSection(title = "الترجمة") {
            var subtitleSize by remember { mutableStateOf("متوسط") }
            SettingsItemWithValue(Icons.Default.TextFields, "حجم الترجمة", subtitleSize) {
                subtitleSize = when (subtitleSize) { "صغير" -> "متوسط"; "متوسط" -> "كبير"; else -> "صغير" }
            }

            var subtitleColor by remember { mutableStateOf("أبيض") }
            SettingsItemWithValue(Icons.Default.Palette, "لون الترجمة", subtitleColor) {}

            var dualSubtitles by remember { mutableStateOf(false) }
            SettingsSwitchItem(Icons.Default.Subtitles, "ترجمة مزدوجة (عربي + إنجليزي)", checked = dualSubtitles) { dualSubtitles = it }
        }

        // ==================== قسم التخزين ====================
        SettingsSection(title = "التخزين") {
            var autoDeleteTrash by remember { mutableStateOf(true) }
            SettingsSwitchItem(Icons.Default.AutoDelete, "حذف سلة المهملات بعد 30 يوم", checked = autoDeleteTrash) { autoDeleteTrash = it }

            SettingsItem(Icons.Default.CleaningServices, "تنظيف الملفات المؤقتة") {}
            SettingsItem(Icons.Default.Storage, "إحصاءات التخزين") {}
        }

        // ==================== قسم المساعدة ====================
        SettingsSection(title = "المساعدة") {
            SettingsItem(Icons.Default.Help, "طريقة استخدام التطبيق", "شرح كامل بالعربي", onClick = onOpenHowTo)
            SettingsItem(Icons.Default.Info, "عن التطبيق", "OfflineFlix v1.0.0 - 100% أوفلاين") {}
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, color = NetflixRed, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) { content() }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String = "", onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 46.dp))
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, subtitle: String = "", checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NetflixRed, uncheckedTrackColor = NetflixMediumGray)
        )
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 46.dp))
}

@Composable
fun SettingsItemWithValue(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NetflixRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = NetflixRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = NetflixMediumGray.copy(alpha = 0.3f), modifier = Modifier.padding(start = 46.dp))
}
