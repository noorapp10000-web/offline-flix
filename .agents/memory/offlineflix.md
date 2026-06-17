---
name: OfflineFlix Android Project
description: Native Android Kotlin app with all stubs resolved — architecture decisions, known quirks, and completion status.
---

# OfflineFlix Android Project

## Project Path & Structure
- Root: `/home/runner/workspace/OfflineFlix/`
- Package: `com.offlineflix.player`
- Build: `libs.versions.toml` (ffmpeg-kit-full 6.0-2, media3 1.4.1, room 2.6.1, hilt 2.51.1, AGP 8.5.2, Kotlin 2.0.21)

## Key Architectural Decisions

### TrashEntity Fields (important!)
TrashEntity has: `id, originalPath, name, size, type (extension), deletedAt, expiresAt, thumbnailPath`
**No `mediaId` or `mediaType` field** — type detection must use `item.type.lowercase()` (file extension).

### AudioEntity vs VideoEntity
- AudioEntity has `isDeleted: Boolean` but **no `deletedAt` field** (VideoEntity has both)
- AudioDao `restoreFromTrash` only sets `isDeleted = 0` (no deletedAt reset)
- AudioDao `moveToTrash` only sets `isDeleted = 1`

### AudioRepository — Flow-returning methods
- `getTracksByAlbum/Artist/Folder()` return `Flow<List<AudioEntity>>` not `List`
- Must use `.first()` to get snapshot in ViewModel: `audioRepository.getTracksByAlbum(x).first()`
- `getPlaylistWithTracks(id)` returns `Flow<PlaylistWithAudio?>` — use `.first()?.tracks`

### VideoEditorUiState — All Dialog Flags Present
All 11 dialog flags in `VideoEditorUiState`: showTrimDialog, showMergeDialog, showAppendDialog, showMusicPickerDialog, showBitrateDialog, showResolutionDialog, showTextDialog, showStickerDialog, showMemeMakerDialog, showFpsDialog, showCodecDialog

## Completed Fixes (all stubs resolved)
1. VideoRepository.findDuplicates() — real logic by size+filename
2. ConversionService — FFmpegKit.executeAsync() with Statistics progress callback
3. VideoEditorViewModel — all 11+ stub operations implemented with FFmpeg commands
4. TrashViewModel.restoreItem() — type-detection from extension, videoDao/audioDao restore
5. PdfReaderScreen thumbnails — LazyRow with page chips, auto-scroll to current page
6. AudioDao — added restoreFromTrash(), moveToTrash()
7. MusicViewModel — openAlbum/Artist/Folder/Playlist() using Flow.first(), addToPlaylist(id)

**Why:** Ensures complete, production-ready codebase with no placeholder methods.
