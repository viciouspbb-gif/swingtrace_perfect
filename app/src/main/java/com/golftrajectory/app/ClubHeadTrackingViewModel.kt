package com.golftrajectory.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * クラブヘッド追跡ViewModel
 */
class ClubHeadTrackingViewModel(
    private val recorder: TrajectoryRecorder,
    private val phaseDetector: SwingPhaseDetector,
    private val repository: TrajectoryRepository,
    private val geminiClassifier: GeminiPhaseClassifier? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Idle)
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()
    
    private val _currentTrajectory = MutableStateFlow<List<ClubHeadPoint>>(emptyList())
    val currentTrajectory: StateFlow<List<ClubHeadPoint>> = _currentTrajectory.asStateFlow()
    
    private val _savedTrajectories = MutableStateFlow<List<SwingTrajectory>>(emptyList())
    val savedTrajectories: StateFlow<List<SwingTrajectory>> = _savedTrajectories.asStateFlow()
    
    sealed class TrackingUiState {
        object Idle : TrackingUiState()
        object Recording : TrackingUiState()
        object Processing : TrackingUiState()
        data class Completed(val trajectory: SwingTrajectory) : TrackingUiState()
        data class Error(val message: String) : TrackingUiState()
    }
    
    init {
        loadSavedTrajectories()
    }
    
    /**
     * 記録開始
     */
    fun startRecording() {
        recorder.startRecording()
        phaseDetector.reset()
        _uiState.value = TrackingUiState.Recording
        _currentTrajectory.value = emptyList()
    }
    
    /**
     * フレームを処理
     */
    fun processFrame(detection: ClubHeadDetection) {
        if (_uiState.value !is TrackingUiState.Recording) return
        
        // フェーズを検出
        val point = ClubHeadPoint(
            x = detection.position.x,
            y = detection.position.y,
            timestamp = detection.timestamp,
            confidence = detection.confidence,
            phase = SwingPhase.SETUP  // 仮の値
        )
        
        val detectedPhase = phaseDetector.detectPhase(point)
        val updatedPoint = point.copy(phase = detectedPhase)
        
        // 記録
        val recordedDetection = detection.copy()
        if (recorder.recordFrame(recordedDetection)) {
            _currentTrajectory.value = recorder.currentTrajectory.value
        }
    }
    
    /**
     * 記録停止
     */
    fun stopRecording(useGeminiClassification: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = TrackingUiState.Processing
            
            try {
                var trajectory = recorder.stopRecording()
                
                if (trajectory == null) {
                    _uiState.value = TrackingUiState.Error("No trajectory data recorded")
                    return@launch
                }
                
                // Gemini APIでフェーズ分類を補助
                if (useGeminiClassification && geminiClassifier != null) {
                    val classification = geminiClassifier.classifyPhases(trajectory.points)
                    val updatedPoints = geminiClassifier.applyClassification(
                        trajectory.points,
                        classification
                    )
                    trajectory = trajectory.copy(points = updatedPoints)
                }
                
                // 保存
                repository.saveTrajectory(trajectory)
                
                _uiState.value = TrackingUiState.Completed(trajectory)
                loadSavedTrajectories()
                
            } catch (e: Exception) {
                _uiState.value = TrackingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 保存済み軌道を読み込み
     */
    private fun loadSavedTrajectories() {
        viewModelScope.launch {
            repository.allTrajectories.collect { trajectories ->
                _savedTrajectories.value = trajectories
            }
        }
    }
    
    /**
     * 軌道を削除
     */
    fun deleteTrajectory(trajectory: SwingTrajectory) {
        viewModelScope.launch {
            repository.deleteTrajectory(trajectory)
        }
    }
    
    /**
     * 古い軌道を削除
     */
    fun deleteOldTrajectories(daysOld: Int = 30) {
        viewModelScope.launch {
            repository.deleteOldTrajectories(daysOld)
        }
    }
    
    /**
     * リセット
     */
    fun reset() {
        recorder.clear()
        phaseDetector.reset()
        _uiState.value = TrackingUiState.Idle
        _currentTrajectory.value = emptyList()
    }
}
