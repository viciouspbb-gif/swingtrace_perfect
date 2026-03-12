package com.golftrajectory.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golftrajectory.app.usecase.*
import com.golftrajectory.app.logic.BiomechanicsFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * スイング軌道追跡ViewModel
 */
class SwingTraceViewModel(
    private val detectClubHeadUseCase: DetectClubHeadUseCase,
    private val recordTrajectoryUseCase: RecordTrajectoryUseCase,
    private val classifySwingPhaseUseCase: ClassifySwingPhaseUseCase,
    private val drawTrajectoryUseCase: DrawTrajectoryUseCase,
    private val quotaManager: com.golftrajectory.app.billing.FreeQuotaManager,
    private val billingManager: com.golftrajectory.app.billing.BillingManager
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // 軌道データ
    private val _pathPoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Float, Float>>> = _pathPoints.asStateFlow()
    
    private val _phaseColors = MutableStateFlow<List<Color>>(emptyList())
    val phaseColors: StateFlow<List<Color>> = _phaseColors.asStateFlow()
    
    // 記録状態
    val isRecording = recordTrajectoryUseCase.isRecording
    val trajectory = recordTrajectoryUseCase.trajectory
    
    // 課金・枠管理
    val isPremium = billingManager.isPremium
    val remainingFreeAnalysis = quotaManager.remainingFreeAnalysis
    val remainingRewardedAds = quotaManager.remainingRewardedAds
    
    // ダイアログ表示
    private val _showQuotaDialog = MutableStateFlow(false)
    val showQuotaDialog: StateFlow<Boolean> = _showQuotaDialog.asStateFlow()
    
    private val _showRewardedAd = MutableStateFlow(false)
    val showRewardedAd: StateFlow<Boolean> = _showRewardedAd.asStateFlow()
    
    // Biomechanics state
    private val _biomechanicsState = MutableStateFlow<BiomechanicsFrame?>(null)
    val biomechanicsState: StateFlow<BiomechanicsFrame?> = _biomechanicsState.asStateFlow()
    
    // Biomechanics history (ring buffer for last 100 frames)
    private val _biomechanicsHistory = MutableStateFlow<List<BiomechanicsFrame>>(emptyList())
    val biomechanicsHistory: StateFlow<List<BiomechanicsFrame>> = _biomechanicsHistory.asStateFlow()
    
    companion object {
        private const val MAX_HISTORY_SIZE = 100
    }
    
    /**
     * Update biomechanics state and history
     */
    fun updateBiomechanics(frame: BiomechanicsFrame?) {
        _biomechanicsState.value = frame
        
        frame?.let { newFrame ->
            val currentHistory = _biomechanicsHistory.value.toMutableList()
            currentHistory.add(newFrame)
            
            // Keep only the last MAX_HISTORY_SIZE frames
            if (currentHistory.size > MAX_HISTORY_SIZE) {
                currentHistory.removeAt(0)
            }
            
            _biomechanicsHistory.value = currentHistory
        }
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Recording : UiState()
        object Processing : UiState()
        data class Completed(
            val pointCount: Int,
            val duration: Long
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    /**
     * スイング分析を開始（枠チェック付き）
     */
    fun onAnalyzeSwing() {
        viewModelScope.launch {
            val premium = isPremium.first()
            val adsRemaining = remainingRewardedAds.first()
            
            when {
                // プレミアムユーザーは無制限
                premium -> {
                    startAnalysis()
                }
                // 無料枠が残っている
                quotaManager.useFreeAnalysis() -> {
                    startAnalysis()
                }
                // リワード広告が残っている
                adsRemaining > 0 -> {
                    _showRewardedAd.value = true
                }
                // 枠なし
                else -> {
                    _showQuotaDialog.value = true
                }
            }
        }
    }
    
    /**
     * リワード広告視聴後の分析開始
     */
    fun onRewardedAdWatched() {
        viewModelScope.launch {
            if (quotaManager.useRewardedAd()) {
                _showRewardedAd.value = false
                startAnalysis()
            }
        }
    }
    
    /**
     * 分析を開始
     */
    private fun startAnalysis() {
        recordTrajectoryUseCase.startRecording()
        _uiState.value = UiState.Recording
        _pathPoints.value = emptyList()
        _phaseColors.value = emptyList()
    }
    
    /**
     * ダイアログを閉じる
     */
    fun dismissQuotaDialog() {
        _showQuotaDialog.value = false
    }
    
    fun dismissRewardedAdDialog() {
        _showRewardedAd.value = false
    }
    
    /**
     * フレームを処理
     */
    fun processFrame(bitmap: Bitmap) {
        if (!isRecording.value) return
        
        viewModelScope.launch {
            // 1. クラブヘッドを検出
            detectClubHeadUseCase.execute(bitmap)
                .onSuccess { detection ->
                    // 2. 軌道を記録
                    recordTrajectoryUseCase.recordFrame(detection)
                    
                    // 3. リアルタイムでフェーズ分類（ローカル）
                    val currentTrajectory = trajectory.value
                    if (currentTrajectory.isNotEmpty()) {
                        val phases = classifySwingPhaseUseCase.classifyLocally(currentTrajectory)
                        
                        // 4. 描画データを準備
                        val drawablePoints = drawTrajectoryUseCase.prepareDrawData(
                            currentTrajectory,
                            phases
                        )
                        
                        val (points, colors) = drawTrajectoryUseCase.getPathData(drawablePoints)
                        _pathPoints.value = points
                        _phaseColors.value = colors
                    }
                }
        }
    }
    
    /**
     * 記録停止（Gemini分類オプション）
     */
    fun stopRecording(useGemini: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = UiState.Processing
            
            try {
                // 軌道データを取得
                val trajectoryData = recordTrajectoryUseCase.stopRecording()
                
                if (trajectoryData.isEmpty()) {
                    _uiState.value = UiState.Error("軌道データがありません")
                    return@launch
                }
                
                // フェーズ分類
                val phases: List<ClassifySwingPhaseUseCase.SwingPhase> = if (useGemini) {
                    // Gemini APIで分類
                    val trajectoryString = trajectoryData.joinToString(",") { "${it.x},${it.y}" }
                    classifySwingPhaseUseCase.classify(trajectoryString)
                        .map { phase -> List(trajectoryData.size) { phase } }
                        .getOrElse {
                            // 失敗時はローカル判定
                            classifySwingPhaseUseCase.classifyLocally(trajectoryData)
                        }
                } else {
                    // ローカルで分類
                    classifySwingPhaseUseCase.classifyLocally(trajectoryData)
                }
                
                // 描画データを準備
                val drawablePoints = drawTrajectoryUseCase.prepareDrawData(
                    trajectoryData,
                    phases
                )
                
                val (points, colors) = drawTrajectoryUseCase.getPathData(drawablePoints)
                _pathPoints.value = points
                _phaseColors.value = colors
                
                // 完了
                val duration = trajectoryData.lastOrNull()?.timeMs ?: 0L
                _uiState.value = UiState.Completed(
                    pointCount = trajectoryData.size,
                    duration = duration
                )
                
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * リセット
     */
    fun reset() {
        recordTrajectoryUseCase.reset()
        _uiState.value = UiState.Idle
        _pathPoints.value = emptyList()
        _phaseColors.value = emptyList()
    }
}
