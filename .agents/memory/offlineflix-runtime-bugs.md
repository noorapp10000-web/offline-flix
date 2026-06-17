---
name: OfflineFlix Runtime Bugs Fixed
description: Critical runtime bugs causing app to be non-functional — permission loop, WorkManager crash, edge-to-edge overflow
---

## Bug 1: Permission screen infinite loop (Android 13+)
**The rule:** On Android 11+ (API 30+), checking `READ_MEDIA_VIDEO`/`READ_MEDIA_AUDIO` alongside `MANAGE_EXTERNAL_STORAGE` in `checkAllPermissions()` always returns false on Android 13 because those permissions were never requested — the button flow only requests `MANAGE_EXTERNAL_STORAGE`.
**Why:** `MANAGE_EXTERNAL_STORAGE` already covers ALL file access. Checking granular media permissions is redundant and breaks the flow.
**Fix:** `checkAllPermissions()` now only checks `Environment.isExternalStorageManager()` on API 30+.

## Bug 2: WorkManager double-initialization crash
**The rule:** When the Application class implements `Configuration.Provider`, the manifest `WorkManagerInitializer` meta-data must be removed with `tools:node="remove"`. Having both causes WorkManager to initialize twice.
**Fix:** Added `tools:node="remove"` to the `WorkManagerInitializer` meta-data in `AndroidManifest.xml`.

## Bug 3: enableEdgeToEdge() layout overflow
**The rule:** `enableEdgeToEdge()` makes the app draw behind system bars. Without proper `contentWindowInsets` handling in all Scaffold/TopAppBar composables, content bleeds under the status bar.
**Fix:** Replaced `enableEdgeToEdge()` with `WindowCompat.setDecorFitsSystemWindows(window, true)` in `MainActivity.kt`.

## Bug 4: No visible rescan button
**Fix:** Added `rescan()` to `VideoLibraryViewModel` (calls `videoRepository.scanAllMedia()` with `isScanning` state), and added a red FAB with Refresh icon at `Alignment.BottomEnd` in `VideoLibraryScreen`. Wrapped the screen Column in a Box to support the FAB overlay.
