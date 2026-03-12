package com.golftrajectory.app.performance

import android.util.Log
import com.swingtrace.aicoaching.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 実測値ログ収集ツール
 * 録画開始後10秒間の実測データを収集
 */
object PerformanceLogger {
    
    private const val TAG = "SWING_TRACE"
    private var measurementJob: Job? = null
    private val frameTimestamps = mutableListOf<Long>()
    private val inferenceTimes = mutableListOf<Long>()
    private var measurementStartTime = 0L
    
    fun startMeasurement() {
        measurementJob?.cancel()
        frameTimestamps.clear()
        inferenceTimes.clear()
        measurementStartTime = System.currentTimeMillis()
        
        measurementJob = CoroutineScope(Dispatchers.IO).launch {
            var frameCount = 0
            var lastLogTime = 0L
            
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - measurementStartTime
                
                // 10秒で測定終了
                if (elapsed >= 10000) {
                    generateReport()
                    stopMeasurement()
                    break
                }
                
                // 1秒ごとにログ出力
                if (currentTime - lastLogTime >= 1000) {
                    val fps = if (elapsed > 0) (frameCount * 1000.0 / elapsed).toFloat() else 0f
                    val avgInference = if (inferenceTimes.isNotEmpty()) {
                        inferenceTimes.average()
                    } else 0.0
                    
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "CameraFrame: 1280x720 fps≈${String.format("%.1f", fps)}")
                        if (avgInference > 0) {
                            Log.i(TAG, "PoseInferenceAvg=${String.format("%.1f", avgInference)}ms")
                        }
                    }
                    
                    lastLogTime = currentTime
                }
                
                delay(100)
            }
        }
    }
    
    fun stopMeasurement() {
        measurementJob?.cancel()
        measurementJob = null
    }
    
    fun recordFrame() {
        if (measurementJob?.isActive == true) {
            frameTimestamps.add(System.currentTimeMillis())
        }
    }
    
    fun recordInference(timeMs: Long) {
        if (measurementJob?.isActive == true) {
            inferenceTimes.add(timeMs)
        }
    }
    
    private fun generateReport() {
        val totalTime = System.currentTimeMillis() - measurementStartTime
        val totalFrames = frameTimestamps.size
        
        // 実効FPS計算
        val effectiveFps = if (totalTime > 0) (totalFrames * 1000.0 / totalTime).toFloat() else 0f
        
        // 推論時間統計
        val avgInference = if (inferenceTimes.isNotEmpty()) inferenceTimes.average() else 0.0
        val maxInference = inferenceTimes.maxOrNull() ?: 0L
        val minInference = inferenceTimes.minOrNull() ?: 0L
        
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "=== 10秒間実測結果 ===")
            Log.i(TAG, "総フレーム数: $totalFrames")
            Log.i(TAG, "実効FPS: ${String.format("%.2f", effectiveFps)}")
            Log.i(TAG, "推論時間平均: ${String.format("%.2f", avgInference)}ms")
            Log.i(TAG, "推論時間最大: ${maxInference}ms")
            Log.i(TAG, "推論時間最小: ${minInference}ms")
            Log.i(TAG, "==================")
            
            // システム構成自己申告
            Log.i(TAG, "=== システム構成 ===")
            Log.i(TAG, "PoseModel: heavy/full/lite")
            Log.i(TAG, "RunningMode: LIVE_STREAM")
            Log.i(TAG, "Delegate: GPU")
            Log.i(TAG, "==============")
        }
    }
}
