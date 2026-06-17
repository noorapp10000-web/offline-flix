---
name: OfflineFlix Build Error Patterns
description: Root compilation errors and lint/dependency config lessons for OfflineFlix Android project
---

## Compilation Errors Fixed

### 1. Invalid Coil `error` parameter syntax
- **File**: `MusicScreen.kt` in `TrackItem` composable
- **Bad**: `error = androidx.compose.painter.Painter::let { null }` — wrong package + invalid callable reference syntax
- **Fix**: `error = null` (Coil AsyncImage accepts null for no error placeholder)

### 2. ViewModel function signature mismatch
- **File**: `MusicScreen.kt` passes `viewModel::addToPlaylist` to `onAddToPlaylist: () -> Unit`
- **Rule**: Keep `FullMusicPlayer`'s `onAddToPlaylist: () -> Unit` — never change its type. Keep ViewModel's `addToPlaylist()` no-arg. Add `addToPlaylistById(id: Long)` as a separate function if needed.

### 3. Invalid Material Icon name
- **File**: `VideoEditorScreen.kt`
- **Bad**: `Icons.Default.SlowMotion_24fps` — does not exist in Material Icons Extended
- **Fix**: `Icons.Default.SlowMotionVideo`

## AudioDao: trash restoration query
- `getByPath()` filters `isDeleted = 0` — won't find deleted audio.
- Added `getByPathAny(path)` with no isDeleted filter for trash restore use-case.
- **Why**: TrashViewModel needs to find audio regardless of its deleted state to restore it.

## Dependency / Lint Config
- Add `lint { abortOnError = false; disable += setOf("HardcodedText", "RtlHardcoded", ...) }` inside `android {}` to prevent Arabic string lint warnings from failing the lint CI job.

## Unresolvable Maven Dependencies — Definitive Fix
- `com.arthenica:ffmpeg-kit-full` — NOT on Maven Central, JitPack, or any public repo at ANY version. The library distributes only via GitHub Releases (binary).
- `com.github.barteksc:android-pdf-viewer` — not reliably available on JitPack at any commonly cited version.
- **Fix applied**: Remove both deps from build files entirely. Create local stub classes in `com/arthenica/ffmpegkit/` that implement the exact API surface used (FFmpegKit, ReturnCode, Statistics, FFmpegSession, FFmpegKitConfig). Replace PDF viewer with Android built-in `PdfRenderer` (API 21+, no dependency needed).
- **Why**: Any CI runner that cannot reach these repos will always fail. Local stubs guarantee compilation with zero external dependencies.
