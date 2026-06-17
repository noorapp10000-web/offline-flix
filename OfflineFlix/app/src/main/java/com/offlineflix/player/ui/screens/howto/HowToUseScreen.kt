package com.offlineflix.player.ui.screens.howto

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.theme.*

/**
 * شاشة "طريقة التعامل مع التطبيق" - شرح كامل بالعربي
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طريقة استخدام التطبيق", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NetflixDarkGray)
            )
        },
        containerColor = NetflixBlack
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            // ترحيب
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NetflixRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NetflixRed.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(NetflixRed), Alignment.Center) {
                        Text("▶", fontSize = 24.sp, color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("مرحباً بك في OfflineFlix!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("التطبيق الأقوى للوسائط - 100% أوفلاين", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ==================== أقسام الشرح ====================
            HowToSection(
                icon = Icons.Default.Security,
                title = "الصلاحيات والمسح",
                color = SpotifyGreen,
                steps = listOf(
                    "عند أول تشغيل، سيطلب التطبيق صلاحية الوصول للتخزين الكامل",
                    "اضغط 'منح الصلاحيات' ووافق على الإذن",
                    "سيبدأ التطبيق تلقائياً بمسح كل جهازك: WhatsApp، Telegram، Downloads، وكل المجلدات",
                    "ستجد كل الفيديوهات والأغاني وملفات PDF في مكانها تلقائياً",
                    "يمكنك أيضاً إضافة ملفات يدوياً بالضغط على زر '+'"
                )
            )

            HowToSection(
                icon = Icons.Default.TouchApp,
                title = "إيماءات مشغل الفيديو",
                color = NetflixRed,
                steps = listOf(
                    "📱 اسحب يميناً ↕ لضبط الصوت",
                    "📱 اسحب يساراً ↕ لضبط السطوع",
                    "📱 اسحب الوسط ↔ للتقديم/التأخير",
                    "👆👆 Double Tap يمين = تقديم 10 ثواني",
                    "👆👆 Double Tap يسار = تأخير 10 ثواني",
                    "👆👆👆 Triple Tap = تخطي 30 ثانية",
                    "✋ Long Press = تشغيل بسرعة x2",
                    "✋✋ Double Long Press = سرعة x4",
                    "3 أصابع للسحب = لقطة شاشة",
                    "4 أصابع = قفل/فتح الشاشة"
                )
            )

            HowToSection(
                icon = Icons.Default.MusicNote,
                title = "مشغل الموسيقى (Spotify Style)",
                color = SpotifyGreen,
                steps = listOf(
                    "اضغط على أي أغنية للتشغيل",
                    "الشريط السفلي يظهر دائماً أثناء التشغيل",
                    "اضغط على الشريط السفلي لفتح المشغل الكامل",
                    "يمكنك رؤية كلمات الأغنية إذا كان ملف .lrc موجوداً",
                    "الإيكولايزر: فتح المشغل الكامل ← أيقونة Equalizer",
                    "إنشاء قوائم تشغيل: تبويب 'قوائم التشغيل' ← '+'"
                )
            )

            HowToSection(
                icon = Icons.Default.Transform,
                title = "محول الصيغ",
                color = NetflixYellow,
                steps = listOf(
                    "اختر الملف المراد تحويله (أي نوع)",
                    "اختر الصيغة المستهدفة من القائمة",
                    "اضبط الإعدادات المتقدمة: Codec، Bitrate، الدقة",
                    "ستجد تقديراً لحجم الملف قبل التحويل",
                    "اضغط 'ابدأ التحويل' وسيعمل في الخلفية",
                    "يمكنك الخروج والعودة، التحويل سيستمر"
                )
            )

            HowToSection(
                icon = Icons.Default.ContentCut,
                title = "محرر الفيديو",
                color = Color(0xFFAA44FF),
                steps = listOf(
                    "من مكتبة الفيديو: اضغط على ⋮ ← 'تعديل'",
                    "قص: حدد نقطة البداية والنهاية",
                    "دمج: اختر فيديوهات متعددة وادمجها",
                    "ضغط ذكي: يقلل الحجم 70-80% بدون فقد ملحوظ",
                    "الفلاتر: أبيض وأسود، Sepia، وأكثر من 12 فلتر",
                    "عند الانتهاء ستُسأل: تعدل الأصلي أم تحفظ نسخة جديدة"
                )
            )

            HowToSection(
                icon = Icons.Default.PictureAsPdf,
                title = "قارئ PDF",
                color = Color(0xFFFF6633),
                steps = listOf(
                    "يجلب كل ملفات PDF من WhatsApp وTelegram والتنزيلات تلقائياً",
                    "Pinch للتكبير/التصغير",
                    "اضغط ★ لإضافة علامة مرجعية للصفحة الحالية",
                    "اضغط 🔍 للبحث في نص الـ PDF",
                    "يتذكر التطبيق آخر صفحة وصلتها تلقائياً",
                    "أيقونة القمر 🌙 لتفعيل الوضع الليلي"
                )
            )

            HowToSection(
                icon = Icons.Default.Folder,
                title = "المجلدات الذكية",
                color = NetflixYellow,
                steps = listOf(
                    "'متابعة المشاهدة': فيديوهات لم تكملها",
                    "'لم تشاهد': فيديوهات لم تفتحها أبداً",
                    "'شاهدت 90%+': قرب الانتهاء",
                    "'فيديوهات 4K': كل فيديوهاتك عالية الجودة",
                    "'أكبر من 2GB': لإدارة المساحة الكبيرة",
                    "يمكنك إنشاء مجلدات مخصصة وإضافة فيديوهات لها"
                )
            )

            HowToSection(
                icon = Icons.Default.Delete,
                title = "سلة المحذوفات",
                color = Color(0xFFCF6679),
                steps = listOf(
                    "الملفات المحذوفة لا تُحذف فوراً، تذهب للسلة",
                    "في السلة لمدة 30 يوم قبل الحذف النهائي",
                    "يمكنك استعادة أي ملف من السلة في أي وقت",
                    "اضغط 🗑 في أعلى اليمين لحذف الكل نهائياً",
                    "بعد 30 يوم يُحذف الملف تلقائياً بدون رجعة"
                )
            )

            Spacer(Modifier.height(32.dp))

            // تذكير الأمان
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = SpotifyGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("100% أوفلاين وآمن 🔒", color = SpotifyGreen, fontWeight = FontWeight.ExtraBold)
                        Text("جميع بياناتك تبقى على جهازك فقط. لا إنترنت، لا سيرفرات، لا Firebase، لا تتبع.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HowToSection(icon: ImageVector, title: String, color: Color, steps: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = NetflixDarkGray),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = Color.White.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    steps.forEachIndexed { index, step ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
