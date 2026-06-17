package com.offlineflix.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineflix.player.ui.navigation.Routes
import com.offlineflix.player.ui.theme.NetflixRed
import com.offlineflix.player.ui.theme.NetflixDarkGray

/** عناصر الشريط السفلي */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.VIDEO_LIBRARY, "الفيديوهات", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    BottomNavItem(Routes.MUSIC, "الموسيقى", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    BottomNavItem(Routes.PDF_LIBRARY, "PDF", Icons.Filled.PictureAsPdf, Icons.Outlined.PictureAsPdf),
    BottomNavItem(Routes.SETTINGS, "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * شريط التنقل السفلي بتصميم Netflix
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = NetflixDarkGray,
        contentColor = NetflixRed,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) NetflixRed else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "nav_icon_color"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        color = iconColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NetflixRed,
                    selectedTextColor = NetflixRed,
                    indicatorColor = NetflixRed.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
