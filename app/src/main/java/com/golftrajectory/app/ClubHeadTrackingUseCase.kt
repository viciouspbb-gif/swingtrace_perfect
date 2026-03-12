package com.golftrajectory.app

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * クラブヘッド追跡UseCase
 */
class ClubHeadTrackingUseCase(
    private val detector: ClubHeadDetector,
    private val phaseDetector: SimpleSwingPhaseDetector
) {
    
    private val _pathPoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Float, Float>>> = _pathPoints.asStateFlow()
    
    private val _phaseColors = MutableStateFlow<List<androidx.compose.ui.graphics.Color>>(emptyList())
    val phaseColors: StateFlow<List<androidx.compose.ui.graphics.Color>> = _phaseColors.asStateFlow()
    
    private val rawPoints = mutableListOf<SimpleSwingPhaseDetector.TrajectoryPoint>()
    private var isRecording = false
    
    /**
     * 記録開始
     */
    fun startRecording() {
        isRecording = true
        rawPoints.clear()
        _pathPoints.value = emptyList()
        _phaseColors.value = emptyList()
    }
    
    /**
     * フレームを処理
     */
    fun processFrame(bitmap: Bitmap) {
        if (!isRecording) return
        
        // YOLOv8でクラブヘッドを検出
        val detection = detector.detect(bitmap) ?: return
        
        // 信頼度チェック
        if (detection.confidence < 0.6f) return
        
        // 軌道ポイントを追加
        val point = SimpleSwingPhaseDetector.TrajectoryPoint(
            timestamp = detection.timestamp,
            x = detection.position.x,
            y = detection.position.y
        )
        rawPoints.add(point)
        
        // フェーズを検出
        val phases = phaseDetector.detectSwingPhases(rawPoints)
        
        // 色に変換
        val colors = phases.map { phaseDetector.phaseToColor(it) }
        
        // UIを更新
        _pathPoints.value = rawPoints.map { Pair(it.x, it.y) }
        _phaseColors.value = colors
    }
    
    /**
     * 記録停止
     */
    fun stopRecording(): SwingTrajectoryData {
        isRecording = false
        
        return SwingTrajectoryData(
            points = rawPoints.toList(),
            phases = phaseDetector.detectSwingPhases(rawPoints)
        )
    }
    
    /**
     * リセット
     */
    fun reset() {
        isRecording = false
        rawPoints.clear()
        _pathPoints.value = emptyList()
        _phaseColors.value = emptyList()
    }
    
    data class SwingTrajectoryData(
        val points: List<SimpleSwingPhaseDetector.TrajectoryPoint>,
        val phases: List<SimpleSwingPhaseDetector.SwingPhase>
    )
}
