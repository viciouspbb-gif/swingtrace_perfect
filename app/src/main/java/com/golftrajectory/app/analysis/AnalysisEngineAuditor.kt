package com.golftrajectory.app.analysis

import android.os.SystemClock
import android.util.Log
import com.swingtrace.aicoaching.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 解析エンジン性能実測ログ実装
 */
class AnalysisEngineAuditor {
    
    companion object {
        private const val TAG = "SWING_TRACE"
        private var auditJob: Job? = null
        private val frameCount = AtomicInteger(0)
        private val analysisStartTime = AtomicLong(0)
        private var lastFrameTimestamp = AtomicLong(0)
        private var lastFrameTime = AtomicLong(0)
        private var lastProcessTime = AtomicLong(0)
        private val frameTimestamps = mutableListOf<Long>()
        private val processTimestamps = mutableListOf<Long>()
        private val inferenceTimes = mutableListOf<Long>()
        private val inputFrameCount = AtomicInteger(0)
        private val processFrameCount = AtomicInteger(0)
        
        fun startAudit() {
            auditJob?.cancel()
            frameCount.set(0)
            analysisStartTime.set(System.currentTimeMillis())
            lastFrameTimestamp.set(0)
            lastFrameTime.set(0)
            lastProcessTime.set(0)
            frameTimestamps.clear()
            processTimestamps.clear()
            inferenceTimes.clear()
            inputFrameCount.set(0)
            processFrameCount.set(0)
            
            auditJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(1000) // 1秒ごと
                    val elapsedSec = (System.currentTimeMillis() - analysisStartTime.get()) / 1000
                    val fps = if (elapsedSec > 0) frameCount.get().toFloat() / elapsedSec else 0f
                    
                    // 実効FPS計測（直近フレーム間隔から）
                    val inputFps = calculateInputFps()
                    val processFps = calculateProcessFps()
                    val avgInference = if (inferenceTimes.isNotEmpty()) {
                        inferenceTimes.average()
                    } else 0.0
                    
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "FPS_STAT: Input=${String.format("%.1f", inputFps)} / Process=${String.format("%.1f", processFps)}")
                        if (avgInference > 0) {
                            Log.i(TAG, "PoseInferenceAvg=${String.format("%.1f", avgInference)}ms")
                        }
                        
                        // ボトルネック検知
                        if (inputFps > processFps + 5) {
                            Log.w(TAG, "⚠️ ボトルネック検知: 入力FPSが出力FPSを${String.format("%.1f", inputFps - processFps)}上回る")
                        }
                    }
                }
            }
        }
        
        fun stopAudit() {
            auditJob?.cancel()
            val elapsedSec = (System.currentTimeMillis() - analysisStartTime.get()) / 1000
            val totalFrames = frameCount.get()
            val avgFps = if (elapsedSec > 0) totalFrames.toFloat() / elapsedSec else 0f
            
            Log.i(TAG, "=== 解析完了 ===")
            Log.i(TAG, "総フレーム数: $totalFrames")
            Log.i(TAG, "総経過時間: ${elapsedSec}秒")
            Log.i(TAG, "平均FPS: ${String.format("%.2f", avgFps)}")
            
            if (inferenceTimes.isNotEmpty()) {
                Log.i(TAG, "推論時間平均: ${String.format("%.2f", inferenceTimes.average())}ms")
                Log.i(TAG, "推論時間最大: ${inferenceTimes.max()}ms")
                Log.i(TAG, "推論時間最小: ${inferenceTimes.min()}ms")
            }
            Log.i(TAG, "==============")
        }
        
        fun recordFrame(width: Int, height: Int, timestamp: Long) {
            frameCount.incrementAndGet()
            inputFrameCount.incrementAndGet()
            
            // 実効FPS計測用にタイムスタンプを記録
            val currentTime = SystemClock.elapsedRealtime()
            if (lastFrameTime.get() > 0) {
                frameTimestamps.add(currentTime - lastFrameTime.get())
                // 直近10フレーム分だけ保持
                if (frameTimestamps.size > 10) {
                    frameTimestamps.removeAt(0)
                }
            }
            lastFrameTime.set(currentTime)
            lastFrameTimestamp.set(timestamp)
        }
        
        fun recordProcessedFrame() {
            processFrameCount.incrementAndGet()
            
            val currentTime = SystemClock.elapsedRealtime()
            if (lastProcessTime.get() > 0) {
                processTimestamps.add(currentTime - lastProcessTime.get())
                // 直近10フレーム分だけ保持
                if (processTimestamps.size > 10) {
                    processTimestamps.removeAt(0)
                }
            }
            lastProcessTime.set(currentTime)
        }
        
        fun recordInferenceTime(timeMs: Long) {
            inferenceTimes.add(timeMs)
            // 直近100回分だけ保持
            if (inferenceTimes.size > 100) {
                inferenceTimes.removeAt(0)
            }
        }
        
        private fun calculateRecentFps(): Float {
            if (frameTimestamps.size < 2) return 0f
            
            val avgInterval = frameTimestamps.average()
            return if (avgInterval > 0) (1000.0 / avgInterval).toFloat() else 0f
        }
        
        private fun calculateInputFps(): Float {
            if (frameTimestamps.size < 2) return 0f
            
            val avgInterval = frameTimestamps.average()
            return if (avgInterval > 0) (1000.0 / avgInterval).toFloat() else 0f
        }
        
        private fun calculateProcessFps(): Float {
            if (processTimestamps.size < 2) return 0f
            
            val avgInterval = processTimestamps.average()
            return if (avgInterval > 0) (1000.0 / avgInterval).toFloat() else 0f
        }
        
        fun logMediaPipeConfig() {
            Log.i(TAG, "=== MediaPipe 設定 ===")
            Log.i(TAG, "PoseModel: heavy")
            Log.i(TAG, "RunningMode: LIVE_STREAM")
            Log.i(TAG, "Delegate: GPU")
            Log.i(TAG, "検出人数: 1")
            Log.i(TAG, "検出信頼度: 0.5")
            Log.i(TAG, "存在信頼度: 0.5")
            Log.i(TAG, "追跡信頼度: 0.5")
            Log.i(TAG, "==================")
        }
        
        fun logCameraXConfig() {
            Log.i(TAG, "=== CameraX 設定 ===")
            Log.i(TAG, "BackpressureStrategy: STRATEGY_KEEP_ONLY_LATEST")
            Log.i(TAG, "Analyzer: SingleThreadExecutor (非メインスレッド)")
            Log.i(TAG, "変換: ImageProxy → 直接処理 (ゼロコピー)")
            Log.i(TAG, "==============")
        }
    }
}
