---
name: OfflineFlix completed features
description: Full list of what was implemented vs what was stub, and current state after all 4 phases.
---

## What was stub → now real

### VideoPlayerViewModel.kt (606 lines)
- **Subtitle parsing**: parseSrt() + parseAss() + parseVtt() — حقيقي 100%
- **Dual subtitles**: selectSubtitle(primary) + selectSecondarySubtitle(secondary) — ترجمة مزدوجة
- **Subtitle settings**: setSubtitleFontSize, setSubtitleColor, setSubtitleDelay
- **Auto-scan**: scanSubtitleFiles() يبحث تلقائياً عن SRT/ASS/SSA/VTT بجوار الفيديو
- **Screenshot**: takeScreenshot() — PixelCopy على API 26+ ، FFmpeg fallback
- **Screen rotation**: rotateScreen(activity) — requestedOrientation حقيقي
- **Sleep timer**: SleepTimerMode enum: OFF/15/30/60/END_OF_EPISODE

### DeviceToolsScreen.kt + DeviceToolsViewModel.kt (new)
- **Duplicate Finder**: فحص DB بالحجم ± 50KB + المدة ± 3s + الاسم
- **Compression Calculator**: حاسبة حجم ما بعد الضغط
- **Benchmark**: حسابات حقيقية مع coroutine

### GifImageViewerScreen.kt (new)
- **GIF/WebP**: Coil + coil-gif library
- **Slideshow**: عرض تلقائي قابل للضبط
- **Zoom/Pan**: detectTransformGestures
- **Double-tap**: zoom in/out toggle
- **Thumbnails strip**: للتنقل بين الصور

## What was already real (confirmed)
- FFmpegKit commands in FormatConverterViewModel
- AB Repeat, Sleep Timer (basic), PiP
- MediaStyle via Media3 MediaSessionService
- EQ 10-band + Bass Boost + Virtualizer in MusicViewModel

## File counts
- Total Kotlin files: 57
- VideoPlayerViewModel: 606 lines
- VideoPlayerScreen: 753 lines
- DeviceToolsScreen: 362 lines
- DeviceToolsViewModel: 189 lines
- GifImageViewerScreen: 341 lines

## Dependencies added
- coil-gif = "io.coil-kt:coil-gif:2.7.0" — for GIF/WebP animated decoding
- VideoDao.getAllVideosOnce() — suspend fun for one-shot scan (Duplicate Finder)

**Why:** Subtitle tracking needs 100ms polling loop not ExoPlayer TextTrack (which needs HLS/DASH). PixelCopy needs Activity window reference passed from UI layer.
