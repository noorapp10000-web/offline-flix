# ==================== ProGuard Rules لـ OfflineFlix ====================

# قواعد عامة
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ==================== Hilt ====================
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class **_HiltModules* { *; }
-keep class **_Factory* { *; }

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ==================== ExoPlayer / Media3 ====================
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn androidx.media3.**

# ==================== FFmpegKit ====================
-keep class com.arthenica.ffmpegkit.** { *; }
-keep class com.arthenica.smartexception.** { *; }
-dontwarn com.arthenica.**

# ==================== PDF Viewer ====================
-keep class com.github.barteksc.pdfviewer.** { *; }
-dontwarn com.github.barteksc.**
-keep class com.shockwave.** { *; }

# ==================== Coil ====================
-keep class coil.** { *; }
-dontwarn coil.**

# ==================== Kotlin Coroutines ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ==================== WorkManager ====================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==================== Gson ====================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ==================== Compose ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== Apache Commons ====================
-dontwarn org.apache.commons.compress.**
-keep class org.apache.commons.compress.** { *; }

# ==================== Data Models ====================
-keep class com.offlineflix.player.data.models.** { *; }

# ==================== Native Libraries ====================
-keepclasseswithmembernames class * {
    native <methods>;
}
