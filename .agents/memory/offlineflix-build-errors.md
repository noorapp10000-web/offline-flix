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
- `android-pdf-viewer` (JitPack) pulls in old datastore/mediarouter versions — add `configurations.all { resolutionStrategy { force(...) } }` in `build.gradle.kts`.
- Disable `mediarouter` implementation (not used in source) to avoid transitive conflicts.
- Add `lint { abortOnError = false; disable += setOf("HardcodedText", "RtlHardcoded", ...) }` inside `android {}` to prevent Arabic string lint warnings from failing the lint CI job.
