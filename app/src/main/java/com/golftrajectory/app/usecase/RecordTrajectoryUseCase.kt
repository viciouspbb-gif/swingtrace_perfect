package com.golftrajectory.app.usecase

import com.golftrajectory.app.ClubHeadDetection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 軌道記録UseCase
 */
class RecordTrajectoryUseCase {
    
    data class FrameData(
        val timeMs: Long,
        val x: Float,
        val y: Float,
        val confidence: Float
    )
    
    private val _trajectory = MutableStateFlow<List<FrameData>>(emptyList())
    val trajectory: StateFlow<List<FrameData>> = _trajectory.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private var startTime: Long = 0
    
    /**
     * 記録開始
     */
    fun startRecording() {
        _isRecording.value = true
        startTime = System.currentTimeMillis()
        _trajectory.value = emptyList()
    }
    
    /**
     * フレームを記録
     */
    fun recordFrame(detection: ClubHeadDetection) {
        if (!_isRecording.value) return
        
        val frameData = FrameData(
            timeMs = detection.timestamp - startTime,
            x = detection.position.x,
            y = detection.position.y,
            confidence = detection.confidence
        )
        
        _trajectory.value = _trajectory.value + frameData
    }
    
    /**
     * 記録停止
     */
    fun stopRecording(): List<FrameData> {
        _isRecording.value = false
        return _trajectory.value
    }
    
    /**
     * リセット
     */
    fun reset() {
        _isRecording.value = false
        _trajectory.value = emptyList()
    }
}
