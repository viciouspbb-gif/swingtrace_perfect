package com.golftrajectory.app.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.swingtrace.aicoaching.BuildConfig

/**
 * デバイス適応型モデル選択器
 * デバイス性能に応じて最適なPoseモデルを自動選択
 */
object PoseModelSelector {
    private const val LOG_TAG = "SWING_TRACE"
    
    enum class PoseModel(val modelName: String, val assetPath: String) {
        HEAVY("heavy", "models/pose_landmarker_heavy.task"),
        FULL("full", "models/pose_landmarker_full.task"),
        LITE("lite", "models/pose_landmarker_lite.task")
    }
    
    /**
     * デバイス性能と実際のアセット存在状況を考慮して最適なモデルを選択
     */
    fun selectOptimalModel(
        context: Context,
        exclude: Set<PoseModel> = emptySet()
    ): PoseModel? {
        val availableModels = getAvailableModels(context).filterNot { exclude.contains(it) }.toSet()
        if (availableModels.isEmpty()) {
            Log.e(LOG_TAG, "利用可能なPose Landmarkerモデルファイルが見つかりませんでした")
            return null
        }
        
        val priorityList = buildModelPriority(context).filterNot { exclude.contains(it) }
        val selectedModel = priorityList.firstOrNull { availableModels.contains(it) }
            ?: availableModels.first()
        
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "=== モデル選択結果 ===")
            Log.i(LOG_TAG, "利用可能モデル: ${availableModels.joinToString { it.modelName }}")
            Log.i(LOG_TAG, "選択モデル: ${selectedModel.modelName}")
            Log.i(LOG_TAG, "==================")
        }
        
        return selectedModel
    }
    
    /**
     * ハイエンドSOC判定
     */
    private fun determinePreferredModel(context: Context): PoseModel {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        val isHighEndSOC = isHighEndSOC()
        
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "総メモリ: ${totalMemoryMB}MB, SOC: ${if (isHighEndSOC) "ハイエンド" else "その他"}")
        }
        
        return when {
            totalMemoryMB >= 8192 && isHighEndSOC -> PoseModel.HEAVY
            totalMemoryMB >= 4096 -> PoseModel.FULL
            else -> PoseModel.LITE
        }
    }
    
    private fun buildModelPriority(context: Context): List<PoseModel> {
        val preferred = determinePreferredModel(context)
        val remaining = PoseModel.values().filter { it != preferred }
        return listOf(preferred) + remaining
    }
    
    private fun isHighEndSOC(): Boolean {
        return when {
            // Snapdragon 8シリーズ
            Build.HARDWARE.contains("msmnile") || // Snapdragon 8 Gen 1
            Build.HARDWARE.contains("lahaina") || // Snapdragon 888
            Build.HARDWARE.contains("kona") || // Snapdragon 865/865+
            Build.HARDWARE.contains("msm8998") || // Snapdragon 835
            Build.HARDWARE.contains("msm8996") -> true // Snapdragon 820
            
            // MediaTek Dimensity 1000+
            Build.HARDWARE.contains("mt6889") || // Dimensity 1000
            Build.HARDWARE.contains("mt6893") -> true // Dimensity 1200
            
            // Exynos 2100+
            Build.HARDWARE.contains("exynos2100") ||
            Build.HARDWARE.contains("exynos2200") -> true
            
            // Apple Silicon (iOS互換)
            Build.MANUFACTURER.equals("Apple", ignoreCase = true) -> true
            
            else -> false
        }
    }
    
    /**
     * モデル情報をログ出力
     */
    fun logSelectedModel(model: PoseModel) {
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, "=== MediaPipe 設定 ===")
            Log.i(LOG_TAG, "PoseModel: ${model.modelName}")
            Log.i(LOG_TAG, "RunningMode: LIVE_STREAM")
            Log.i(LOG_TAG, "Delegate: GPU")
            Log.i(LOG_TAG, "検出人数: 1")
            Log.i(LOG_TAG, "検出信頼度: 0.5")
            Log.i(LOG_TAG, "存在信頼度: 0.5")
            Log.i(LOG_TAG, "追跡信頼度: 0.5")
            Log.i(LOG_TAG, "==================")
        }
    }
    
    fun getAvailableModels(context: Context): Set<PoseModel> {
        return PoseModel.values().filter { assetExists(context, it.assetPath) }.toSet()
    }
    
    fun isModelAssetAvailable(context: Context, model: PoseModel): Boolean {
        return assetExists(context, model.assetPath)
    }
    
    private fun assetExists(context: Context, assetPath: String): Boolean {
        val normalizedPath = assetPath.removePrefix("/")
        val directory = normalizedPath.substringBeforeLast('/', "")
        val fileName = normalizedPath.substringAfterLast('/')
        val assets = context.assets.list(directory) ?: return false
        return assets.contains(fileName)
    }
}
