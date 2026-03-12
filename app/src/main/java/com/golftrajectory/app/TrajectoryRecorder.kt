package com.golftrajectory.app

import android.graphics.PointF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.sqrt

/**
 * クラブヘッド軌道記録マネージャー
 */
class TrajectoryRecorder(
    private val minConfidence: Float = 0.6f  // 最小信頼度閾値
) {
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _currentTrajectory = MutableStateFlow<List<ClubHeadPoint>>(emptyList())
    val currentTrajectory: StateFlow<List<ClubHeadPoint>> = _currentTrajectory.asStateFlow()
    
    private var recordingStartTime: Long = 0
    private var previousPoint: ClubHeadPoint? = null
    private val points = mutableListOf<ClubHeadPoint>()
    
    /**
     * 記録開始
     */
    fun startRecording() {
        _isRecording.value = true
        recordingStartTime = System.currentTimeMillis()
        points.clear()
        previousPoint = null
    }
    
    /**
     * 記録停止
     */
    fun stopRecording(): SwingTrajectory? {
        _isRecording.value = false
        
        if (points.isEmpty()) return null
        
        val metadata = calculateMetadata()
        
        return SwingTrajectory(
            id = UUID.randomUUID().toString(),
            recordedAt = recordingStartTime,
            points = points.toList(),
            metadata = metadata
        )
    }
    
    /**
     * フレームを記録
     * @param detection YOLOv8検出結果
     * @return 記録されたかどうか
     */
    fun recordFrame(detection: ClubHeadDetection): Boolean {
        if (!_isRecording.value) return false
        
        // 信頼度チェック
        if (detection.confidence < minConfidence) return false
        
        // スイングフェーズを判定
        val phase = determinePhase(detection.position)
        
        val point = ClubHeadPoint(
            x = detection.position.x,
            y = detection.position.y,
            timestamp = detection.timestamp,
            confidence = detection.confidence,
            phase = phase
        )
        
        points.add(point)
        previousPoint = point
        
        // UIに通知
        _currentTrajectory.value = points.toList()
        
        return true
    }
    
    /**
     * スイングフェーズを判定
     */
    private fun determinePhase(currentPos: PointF): SwingPhase {
        val prev = previousPoint ?: return SwingPhase.SETUP
        
        // Y座標の変化でフェーズを判定
        val deltaY = currentPos.y - prev.y
        val deltaX = currentPos.x - prev.x
        
        return when {
            points.size < 5 -> SwingPhase.SETUP
            deltaY < -5 && deltaX < 0 -> SwingPhase.BACKSWING  // 上に移動＋左
            deltaY > -2 && deltaY < 2 && deltaX < 0 -> SwingPhase.TOP  // 頂点
            deltaY > 5 && deltaX > 0 -> SwingPhase.DOWNSWING  // 下に移動＋右
            deltaY > -2 && deltaX > 10 -> SwingPhase.FOLLOW_THROUGH  // 右に移動
            else -> prev.phase  // 前のフェーズを維持
        }
    }
    
    /**
     * メタデータを計算
     */
    private fun calculateMetadata(): SwingMetadata {
        if (points.isEmpty()) {
            return SwingMetadata(
                duration = 0,
                maxSpeed = 0f
            )
        }
        
        val duration = points.last().timestamp - points.first().timestamp
        
        // ヘッドスピードを計算
        var maxSpeed = 0f
        for (i in 1 until points.size) {
            val p1 = points[i - 1]
            val p2 = points[i]
            
            val distance = sqrt(
                (p2.x - p1.x) * (p2.x - p1.x) + 
                (p2.y - p1.y) * (p2.y - p1.y)
            )
            
            val timeDiff = (p2.timestamp - p1.timestamp) / 1000f  // 秒
            if (timeDiff > 0) {
                val speed = distance / timeDiff  // ピクセル/秒
                // 実際のm/sに変換（仮定：画面幅1080px = 2m）
                val speedMps = speed * 2f / 1080f
                maxSpeed = maxOf(maxSpeed, speedMps)
            }
        }
        
        // インパクト時のスピードを検出
        val impactPoint = points.firstOrNull { it.phase == SwingPhase.IMPACT }
        val impactSpeed = impactPoint?.let { 
            calculateSpeedAtPoint(points.indexOf(it))
        }
        
        return SwingMetadata(
            duration = duration,
            maxSpeed = maxSpeed,
            impactSpeed = impactSpeed
        )
    }
    
    /**
     * 特定ポイントでのスピードを計算
     */
    private fun calculateSpeedAtPoint(index: Int): Float {
        if (index < 1 || index >= points.size) return 0f
        
        val p1 = points[index - 1]
        val p2 = points[index]
        
        val distance = sqrt(
            (p2.x - p1.x) * (p2.x - p1.x) + 
            (p2.y - p1.y) * (p2.y - p1.y)
        )
        
        val timeDiff = (p2.timestamp - p1.timestamp) / 1000f
        return if (timeDiff > 0) {
            val speed = distance / timeDiff
            speed * 2f / 1080f  // m/sに変換
        } else 0f
    }
    
    /**
     * 現在の記録をクリア
     */
    fun clear() {
        points.clear()
        previousPoint = null
        _currentTrajectory.value = emptyList()
    }
}
