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
    private var poseLandmarker: PoseLandmarker? = null
    private var selectedModel: PoseModel? = null
    private var useGpuDelegate = true
    
    // LIVE_STREAM 用のコールバックフロー
    private val _poseResultFlow = MutableSharedFlow<PoseLandmarkerResult>(extraBufferCapacity = 10)
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
                poseLandmarker = null
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
                
            val options = PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
                
            poseLandmarker?.close()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            
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
     * 画像から姿勢を検出
     */
    fun detectPose(bitmap: Bitmap): PoseLandmarkerResult? {
        return try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            poseLandmarker?.detect(mpImage)
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
            poseLandmarker?.detectAsync(mpImage, timestamp)
            
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
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
