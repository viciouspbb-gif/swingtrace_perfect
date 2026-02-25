package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * MediaPipe Pose検出器
 * ゴルフスイングの姿勢を検出
 */
class PoseDetector(private val context: Context) {
    
    private val TAG = "PoseDetector"
    private var poseLandmarker: PoseLandmarker? = null
    
    init {
        setupPoseLandmarker()
    }
    
    /**
     * MediaPipe Pose Landmarkerをセットアップ
     */
    private fun setupPoseLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("pose_landmarker_heavy.task")
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1) // 1人のゴルファーのみ検出
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.d(TAG, "MediaPipe Pose Landmarker初期化成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe Pose Landmarker初期化失敗: ${e.message}", e)
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
            Log.e(TAG, "姿勢検出エラー: ${e.message}", e)
            null
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
