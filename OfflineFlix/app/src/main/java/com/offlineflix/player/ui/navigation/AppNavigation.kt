package com.offlineflix.player.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.offlineflix.player.ui.screens.home.HomeScreen
import com.offlineflix.player.ui.screens.player.VideoPlayerScreen
import com.offlineflix.player.ui.screens.music.MusicScreen
import com.offlineflix.player.ui.screens.library.VideoLibraryScreen
import com.offlineflix.player.ui.screens.pdf.PdfLibraryScreen
import com.offlineflix.player.ui.screens.pdf.PdfReaderScreen
import com.offlineflix.player.ui.screens.editor.VideoEditorScreen
import com.offlineflix.player.ui.screens.converter.FormatConverterScreen
import com.offlineflix.player.ui.screens.settings.SettingsScreen
import com.offlineflix.player.ui.screens.permission.PermissionScreen
import com.offlineflix.player.ui.screens.scanning.ScanningScreen
import com.offlineflix.player.ui.screens.trash.TrashScreen
import com.offlineflix.player.ui.screens.howto.HowToUseScreen
import com.offlineflix.player.ui.screens.filemanager.FileManagerScreen
import com.offlineflix.player.ui.screens.tools.DeviceToolsScreen
import com.offlineflix.player.ui.screens.viewer.GifImageViewerScreen
import com.offlineflix.player.ui.components.BottomNavBar
import com.offlineflix.player.ui.components.StorageBar

/** وجهات التنقل */
object Routes {
    const val PERMISSION = "permission"
    const val SCANNING = "scanning"
    const val HOME = "home"
    const val VIDEO_LIBRARY = "video_library"
    const val VIDEO_PLAYER = "video_player/{videoId}"
    const val MUSIC = "music"
    const val PDF_LIBRARY = "pdf_library"
    const val PDF_READER = "pdf_reader/{pdfId}"
    const val VIDEO_EDITOR = "video_editor/{videoId}"
    const val FORMAT_CONVERTER = "format_converter"
    const val FILE_MANAGER = "file_manager"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val HOW_TO_USE = "how_to_use"
    const val DEVICE_TOOLS = "device_tools"
    const val GIF_VIEWER = "gif_viewer"

    fun videoPlayer(videoId: Long) = "video_player/$videoId"
    fun pdfReader(pdfId: Long) = "pdf_reader/$pdfId"
    fun videoEditor(videoId: Long) = "video_editor/$videoId"
}

/** شاشات الـ Bottom Navigation */
val bottomNavScreens = setOf(Routes.HOME, Routes.VIDEO_LIBRARY, Routes.MUSIC, Routes.PDF_LIBRARY, Routes.SETTINGS)

/**
 * نظام التنقل الرئيسي للتطبيق
 */
@Composable
fun AppNavigation(intentData: Uri?) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in bottomNavScreens
    val showStorageBar = currentRoute != null &&
        currentRoute != Routes.PERMISSION &&
        currentRoute != Routes.SCANNING &&
        !currentRoute.startsWith("video_player")

    Scaffold(
        topBar = {
            if (showStorageBar) {
                StorageBar()
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.PERMISSION,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(tween(300))
            }
        ) {
            composable(Routes.PERMISSION) {
                PermissionScreen(
                    onPermissionsGranted = {
                        navController.navigate(Routes.SCANNING) {
                            popUpTo(Routes.PERMISSION) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SCANNING) {
                ScanningScreen(
                    onScanComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SCANNING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onVideoClick = { videoId -> navController.navigate(Routes.videoPlayer(videoId)) },
                    onSeeAllVideos = { navController.navigate(Routes.VIDEO_LIBRARY) },
                    onSeeAllMusic = { navController.navigate(Routes.MUSIC) },
                    onOpenEditor = { navController.navigate(Routes.FORMAT_CONVERTER) },
                    onOpenHowTo = { navController.navigate(Routes.HOW_TO_USE) }
                )
            }

            composable(Routes.VIDEO_LIBRARY) {
                VideoLibraryScreen(
                    onVideoClick = { videoId -> navController.navigate(Routes.videoPlayer(videoId)) },
                    onEditVideo = { videoId -> navController.navigate(Routes.videoEditor(videoId)) }
                )
            }

            composable(
                route = Routes.VIDEO_PLAYER,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(200)) }
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                VideoPlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onOpenEditor = { navController.navigate(Routes.videoEditor(videoId)) }
                )
            }

            composable(Routes.MUSIC) {
                MusicScreen()
            }

            composable(Routes.PDF_LIBRARY) {
                PdfLibraryScreen(
                    onPdfClick = { pdfId -> navController.navigate(Routes.pdfReader(pdfId)) }
                )
            }

            composable(
                route = Routes.PDF_READER,
                arguments = listOf(navArgument("pdfId") { type = NavType.LongType })
            ) { backStackEntry ->
                val pdfId = backStackEntry.arguments?.getLong("pdfId") ?: 0L
                PdfReaderScreen(
                    pdfId = pdfId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.VIDEO_EDITOR,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                VideoEditorScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.FORMAT_CONVERTER) {
                FormatConverterScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.FILE_MANAGER) {
                FileManagerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                    onOpenHowTo = { navController.navigate(Routes.HOW_TO_USE) },
                    onOpenConverter = { navController.navigate(Routes.FORMAT_CONVERTER) },
                    onOpenFileManager = { navController.navigate(Routes.FILE_MANAGER) },
                    onOpenDeviceTools = { navController.navigate(Routes.DEVICE_TOOLS) },
                    onOpenGifViewer = { navController.navigate(Routes.GIF_VIEWER) }
                )
            }

            composable(Routes.TRASH) {
                TrashScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.HOW_TO_USE) {
                HowToUseScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.DEVICE_TOOLS) {
                DeviceToolsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.GIF_VIEWER) {
                GifImageViewerScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
