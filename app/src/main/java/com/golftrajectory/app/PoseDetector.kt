package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.golftrajectory.app.analysis.AnalysisEngineAuditor
import com.golftrajectory.app.performance.PerformanceLogger
import com.golftrajectory.app.performance.PoseModelSelector
import com.golftrajectory.app.performance.PoseModelSelector.PoseModel
import com.golftrajectory.app.logic.BiomechanicsEngine
import com.golftrajectory.app.logic.BiomechanicsFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.camera.core.ImageProxy
import android.os.SystemClock

/**
 * MediaPipe Pose検出器
 * ゴルフスイングの姿勢を検出
 */
class PoseDetector(private val context: Context) {
    
    private val TAG = "SWING_TRACE"
    private var liveLandmarker: PoseLandmarker? = null  // For LIVE_STREAM (real-time)
    private var staticLandmarker: PoseLandmarker? = null // For IMAGE/VIDEO (synchronous)
    private var selectedModel: PoseModel? = null
    private var useGpuDelegate = true
    
    // BiomechanicsEngine instance
    private val biomechanicsEngine = BiomechanicsEngine()
    
    // LIVE_STREAM 用のコールバックフロー（BiomechanicsFrameを含む）
    private val _poseResultFlow = MutableSharedFlow<Pair<PoseLandmarkerResult, BiomechanicsFrame?>>(extraBufferCapacity = 10)
    val poseResultFlow = _poseResultFlow.asSharedFlow()
    
    init {
        setupPoseLandmarker()
    }
    
    /**
     * MediaPipe Pose Landmarkerをセットアップ
     */
    private fun setupPoseLandmarker() {
        val attemptedModels = mutableSetOf<PoseModel>()
        while (true) {
            val candidate = PoseModelSelector.selectOptimalModel(context, attemptedModels)
            if (candidate == null) {
                Log.e(TAG, "Poseモデルファイルが見つからないため初期化を中止します")
                liveLandmarker = null
                staticLandmarker = null
                return
            }

            if (!PoseModelSelector.isModelAssetAvailable(context, candidate)) {
                Log.w(TAG, "モデルファイル未検出: ${candidate.assetPath}")
                attemptedModels += candidate
                continue
            }

            if (initializeWithModel(candidate)) {
                selectedModel = candidate
                return
            } else {
                attemptedModels += candidate
            }
        }
    }

    private fun initializeWithModel(model: PoseModel): Boolean {
        return try {
            useGpuDelegate = true
            setupPoseLandmarkerWithDelegate(model, true)
            true
        } catch (gpuError: Exception) {
            Log.w(TAG, "GPU Delegate初期化失敗(${model.modelName}): ${gpuError.message}")
            try {
                useGpuDelegate = false
                setupPoseLandmarkerWithDelegate(model, false)
                true
            } catch (cpuError: Exception) {
                Log.e(TAG, "CPU Delegateでも初期化失敗(${model.modelName}): ${cpuError.message}", cpuError)
                false
            }
        }
    }

    private fun setupPoseLandmarkerWithDelegate(model: PoseModel, useGpu: Boolean) {
        if (!PoseModelSelector.isModelAssetAvailable(context, model)) {
            throw IllegalStateException("モデルファイルが存在しません: ${model.assetPath}")
        }

        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(model.assetPath)
                .setDelegate(if (useGpu) Delegate.GPU else Delegate.CPU)
                .build()
                
            // LIVE_STREAM 用の設定（リアルタイムカメラ用）
            val liveOptions = PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, input ->
                    processResult(result, input)
                }
                .setErrorListener { error: RuntimeException ->
                    Log.e(TAG, "Pose detection error: $error")
                }
                .build()
                
            // IMAGE 用の設定（静止画/動画解析用）
            val staticOptions = PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
                
            // Close existing instances
            liveLandmarker?.close()
            staticLandmarker?.close()
            
            // Initialize new instances
            liveLandmarker = PoseLandmarker.createFromOptions(context, liveOptions)
            staticLandmarker = PoseLandmarker.createFromOptions(context, staticOptions)
            
            Log.i(TAG, "MediaPipe Pose Landmarker初期化成功: model=${model.modelName}, delegate=${if (useGpu) "GPU" else "CPU"}")
            Log.i(TAG, "★★★ ENGINE READY: Model=${model.modelName}, Delegate=${if (useGpu) "GPU" else "CPU"} ★★★")
            
            // フォールバックログ
            if (!useGpu) {
                Log.w(TAG, "PoseDelegateFallback: CPU")
            }
            
            // 監査ログ出力
            PoseModelSelector.logSelectedModel(model)
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe Pose Landmarker初期化失敗(${model.modelName}): ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Process pose detection results and compute biomechanics
     */
    private fun processResult(result: PoseLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        // 1. worldLandmarks の抽出とバイオメカニクス解析の実行
        var biomechanicsFrame: BiomechanicsFrame? = null
        if (result.worldLandmarks().isNotEmpty()) {
            // 最初の人物のランドマークを使用
            val landmarks = result.worldLandmarks()[0]
            biomechanicsFrame = biomechanicsEngine.analyzeFrame(landmarks, result.timestampMs())
            
            // デバッグログ: 解析結果の出力
            Log.i(TAG, "Biomechanics - X-Factor: ${"%.1f".format(biomechanicsFrame.xFactorDegrees)}°, SpineAngle: ${"%.1f".format(biomechanicsFrame.spineAngleDegrees)}°, Stable: ${biomechanicsFrame.isStable}")
        }

        // 2. フロー経由で上位層へ発火
        try {
            _poseResultFlow.tryEmit(Pair(result, biomechanicsFrame))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to emit pose result: ${e.message}")
        }
    }
    
    /**
     * 画像から姿勢を検出（同期処理用）
     */
    fun detectPose(bitmap: Bitmap): PoseLandmarkerResult? {
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            // IMAGEモードのstaticLandmarkerを使用
            staticLandmarker?.detect(mpImage)
        } catch (e: Exception) {
            Log.e(TAG, "姿勢検出失敗: ${e.message}", e)
            null
        }
    }
    
    /**
     * 非同期検出（LIVE_STREAM用）
     */
    fun detectAsync(mpImage: MPImage, timestamp: Long) {
        val startTime = SystemClock.elapsedRealtime()
        
        try {
            // LIVE_STREAMモードのliveLandmarkerを使用
            liveLandmarker?.detectAsync(mpImage, timestamp)
            
            // 推論時間を記録
            val inferenceTime = SystemClock.elapsedRealtime() - startTime
            AnalysisEngineAuditor.recordInferenceTime(inferenceTime)
            PerformanceLogger.recordInference(inferenceTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "非同期姿勢検出失敗: ${e.message}", e)
        }
    }
    
    /**
     * ImageProxyから直接MPImageを生成して非同期検出
     */
    fun detectFromImageProxy(imageProxy: ImageProxy) {
        try {
            // 現在はBitmap経由で処理（将来の改善余地あり）
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            detectAsync(mpImage, imageProxy.imageInfo.timestamp)
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "ImageProxyからの検出失敗: ${e.message}", e)
        }
    }
    
    /**
     * リソースを解放
     */
    fun close() {
        liveLandmarker?.close()
        staticLandmarker?.close()
        liveLandmarker = null
        staticLandmarker = null
    }
}
