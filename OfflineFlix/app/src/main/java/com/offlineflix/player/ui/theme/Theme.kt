package com.offlineflix.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ==================== ألوان Netflix ====================
val NetflixRed = Color(0xFFE50914)
val NetflixRedDark = Color(0xFFB20710)
val NetflixBlack = Color(0xFF141414)
val NetflixDarkGray = Color(0xFF181818)
val NetflixMediumGray = Color(0xFF2F2F2F)
val NetflixLightGray = Color(0xFF757575)
val NetflixWhite = Color(0xFFFFFFFF)
val NetflixYellow = Color(0xFFF5A623)
val NetflixGold = Color(0xFFFFD700)

// ==================== ألوان Spotify ====================
val SpotifyGreen = Color(0xFF1DB954)
val SpotifyGreenDark = Color(0xFF1AA34A)
val SpotifyBlack = Color(0xFF191414)
val SpotifyDarkGray = Color(0xFF282828)
val SpotifyMediumGray = Color(0xFF333333)
val SpotifyLightGray = Color(0xFFB3B3B3)
val SpotifyWhite = Color(0xFFFFFFFF)

// ==================== نظام الألوان الداكن (Netflix) ====================
private val DarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    primaryContainer = NetflixRedDark,
    onPrimaryContainer = NetflixWhite,
    secondary = SpotifyGreen,
    onSecondary = NetflixBlack,
    secondaryContainer = SpotifyGreenDark,
    onSecondaryContainer = NetflixWhite,
    tertiary = NetflixYellow,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixDarkGray,
    onSurface = NetflixWhite,
    surfaceVariant = NetflixMediumGray,
    onSurfaceVariant = NetflixLightGray,
    error = Color(0xFFCF6679),
    outline = NetflixMediumGray,
    outlineVariant = NetflixLightGray,
    scrim = Color(0x80000000),
    inverseSurface = NetflixWhite,
    inverseOnSurface = NetflixBlack,
    inversePrimary = NetflixRedDark
)

private val LightColorScheme = lightColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    primaryContainer = Color(0xFFFFDAD6),
    secondary = SpotifyGreen,
    background = Color(0xFFF5F5F5),
    surface = NetflixWhite,
    onBackground = NetflixBlack,
    onSurface = NetflixBlack
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

data class Spacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp,
    val xxl: androidx.compose.ui.unit.Dp = 48.dp
)

/**
 * ثيم OfflineFlix الرئيسي
 */
@Composable
fun OfflineFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OfflineFlixTypography,
            content = content
        )
    }
}
