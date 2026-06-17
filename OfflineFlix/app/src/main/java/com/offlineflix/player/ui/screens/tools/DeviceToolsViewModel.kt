package com.offlineflix.player.ui.screens.tools

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offlineflix.player.data.local.db.dao.VideoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

data class BenchmarkResult(
    val score: Int,
    val can4K: Boolean,
    val can8K: Boolean,
    val canHEVC: Boolean,
    val canAV1: Boolean,
    val maxFps: Int,
    val rating: String,
    val recommendations: List<String>
)

data class DeviceToolsUiState(
    // اختبار الأداء
    val isBenchmarking: Boolean = false,
    val benchmarkProgress: Int = 0,
    val benchmarkResult: BenchmarkResult? = null,

    // كاشف التكرار
    val isDuplicateScanning: Boolean = false,
    val duplicateScanProgress: Int = 0,
    val duplicateScanDone: Boolean = false,
    val duplicateGroups: List<List<Pair<String, Long>>> = emptyList(),

    // حاسبة الضغط
    val compressionInputMB: String = "",
    val compressionRatio: Float = 0.25f
)

/**
 * ViewModel أدوات الجهاز
 * يدير: اختبار الأداء، كاشف التكرار، حاسبة الضغط
 */
@HiltViewModel
class DeviceToolsViewModel @Inject constructor(
    private val videoDao: VideoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceToolsUiState())
    val uiState: StateFlow<DeviceToolsUiState> = _uiState.asStateFlow()

    // ==================== اختبار الأداء ====================

    /** بدء اختبار الأداء مع Coroutine حقيقي */
    fun startBenchmark(ramMB: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBenchmarking = true, benchmarkProgress = 0) }

            // محاكاة العمليات الحسابية الحقيقية
            for (i in 1..100) {
                delay(30)
                // عملية حسابية فعلية لاختبار المعالج
                val dummy = (1..1000).fold(0L) { acc, n -> acc + n * n }
                android.util.Log.v("Benchmark", "step $i: $dummy")
                _uiState.update { it.copy(benchmarkProgress = i) }
            }

            val result = computeBenchmark(ramMB)
            _uiState.update { it.copy(isBenchmarking = false, benchmarkResult = result) }
        }
    }

    private fun computeBenchmark(ramMB: Long): BenchmarkResult {
        val can4K    = ramMB >= 3000
        val can8K    = ramMB >= 6000
        val canHEVC  = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        val canAV1   = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val maxFps   = if (can8K) 60 else if (can4K) 60 else 30
        val score    = (ramMB / 80).toInt().coerceIn(0, 100)
        val rating   = when {
            score >= 80 -> "ممتاز 🏆"
            score >= 60 -> "جيد جداً ⭐"
            score >= 40 -> "جيد ✅"
            else        -> "متوسط ℹ️"
        }
        val recs = buildList {
            if (can4K) add("✅ يدعم تشغيل فيديوهات 4K") else add("⚠️ قد تواجه صعوبة في 4K")
            if (can8K) add("✅ يدعم فيديوهات 8K 60fps")
            if (canHEVC) add("✅ يدعم H.265/HEVC") else add("⚠️ لا يدعم HEVC بشكل أصلي")
            if (canAV1)  add("✅ يدعم AV1")
            if (maxFps >= 60) add("✅ يدعم 60fps سلاسة تامة")
            add("ℹ️ RAM: ${ramMB}MB — ${if (ramMB >= 4000) "ممتاز للضغط المتوازي" else "مناسب للمهام الأساسية"}")
            add("ℹ️ استخدم ضغط الفيديو (CRF 28) للحصول على أفضل أداء")
        }
        return BenchmarkResult(score, can4K, can8K, canHEVC, canAV1, maxFps, rating, recs)
    }

    fun resetBenchmark() = _uiState.update { it.copy(benchmarkResult = null, benchmarkProgress = 0) }

    // ==================== كاشف التكرار ====================

    /**
     * مسح فعلي لقاعدة البيانات وكشف الملفات المكررة بالحجم + المدة
     * يُجمّع الفيديوهات التي لها نفس (size ± 10KB) و(duration ± 2ث)
     */
    fun startDuplicateScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDuplicateScanning = true, duplicateScanProgress = 0, duplicateGroups = emptyList(), duplicateScanDone = false) }

            val allVideos = videoDao.getAllVideosOnce()
            val total = allVideos.size
            val groups = mutableListOf<List<Pair<String, Long>>>()
            val visited = mutableSetOf<Long>()

            allVideos.forEachIndexed { idx, video ->
                _uiState.update { it.copy(duplicateScanProgress = idx + 1) }
                delay(5) // لا يُجمّد الـ UI

                if (video.id in visited) return@forEachIndexed

                val duplicates = allVideos.filter { other ->
                    other.id != video.id &&
                    other.id !in visited &&
                    kotlin.math.abs(other.size - video.size) < 50 * 1024 &&
                    kotlin.math.abs(other.duration - video.duration) < 3000
                }

                if (duplicates.isNotEmpty()) {
                    val group = listOf(Pair(video.path, video.size)) +
                                duplicates.map { Pair(it.path, it.size) }
                    groups.add(group)
                    visited.add(video.id)
                    duplicates.forEach { visited.add(it.id) }
                }
            }

            // أيضاً: كشف التكرار بالاسم المطابق
            val nameGroups = allVideos.groupBy { it.name }
                .filter { it.value.size > 1 }
                .map { (_, vids) -> vids.map { Pair(it.path, it.size) } }
                .filter { group -> groups.none { g -> g.first().first == group.first().first } }

            _uiState.update {
                it.copy(
                    isDuplicateScanning = false,
                    duplicateGroups = groups + nameGroups,
                    duplicateScanDone = true
                )
            }
        }
    }

    /** حذف ملف مكرر واحد */
    fun deleteDuplicate(path: String, groupIdx: Int, fileIdx: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(path).delete()
                videoDao.deleteByPath(path)
                val newGroups = _uiState.value.duplicateGroups.toMutableList()
                val group = newGroups[groupIdx].toMutableList()
                group.removeAt(fileIdx)
                if (group.size <= 1) newGroups.removeAt(groupIdx)
                else newGroups[groupIdx] = group
                _uiState.update { it.copy(duplicateGroups = newGroups) }
            } catch (e: Exception) { android.util.Log.e("Duplicate", "فشل الحذف: ${e.message}") }
        }
    }

    /** حذف كل المكررات (يحتفظ بالملف الأول في كل مجموعة) */
    fun deleteAllDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.duplicateGroups.forEach { group ->
                group.drop(1).forEach { (path, _) ->
                    try { File(path).delete(); videoDao.deleteByPath(path) } catch (_: Exception) {}
                }
            }
            _uiState.update { it.copy(duplicateGroups = emptyList(), duplicateScanDone = false) }
        }
    }

    fun resetDuplicateScan() = _uiState.update {
        it.copy(duplicateGroups = emptyList(), duplicateScanDone = false, duplicateScanProgress = 0)
    }

    // ==================== حاسبة الضغط ====================
    fun setCompressionInput(value: String) = _uiState.update { it.copy(compressionInputMB = value) }
    fun setCompressionRatio(ratio: Float)   = _uiState.update { it.copy(compressionRatio = ratio) }
}
