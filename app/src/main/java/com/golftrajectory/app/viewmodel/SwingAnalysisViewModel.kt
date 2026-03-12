package com.swingtrace.aicoaching.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swingtrace.aicoaching.domain.usecase.AnalyzeSwingUseCase
import com.swingtrace.aicoaching.domain.usecase.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * スイング分析のViewModel
 */
class SwingAnalysisViewModel(
    private val analyzeSwingUseCase: AnalyzeSwingUseCase
) : ViewModel() {
    
    // UI状態
    private val _uiState = MutableStateFlow<SwingAnalysisUiState>(SwingAnalysisUiState.Idle)
    val uiState: StateFlow<SwingAnalysisUiState> = _uiState.asStateFlow()
    
    /**
     * スイング分析を実行
     */
    fun analyzeSwing(
        videoUri: Uri,
        userId: String,
        isPremium: Boolean,
        targetProName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = SwingAnalysisUiState.Loading
            
            val result = analyzeSwingUseCase.execute(
                videoUri = videoUri,
                userId = userId,
                isPremium = isPremium,
                targetProName = targetProName
            )
            
            _uiState.value = if (result.isSuccess) {
                SwingAnalysisUiState.Success(result.getOrNull()!!)
            } else {
                SwingAnalysisUiState.Error(
                    result.exceptionOrNull()?.message ?: "分析に失敗しました"
                )
            }
        }
    }
    
    /**
     * 状態をリセット
     */
    fun resetState() {
        _uiState.value = SwingAnalysisUiState.Idle
    }
}

/**
 * UI状態
 */
sealed class SwingAnalysisUiState {
    object Idle : SwingAnalysisUiState()
    object Loading : SwingAnalysisUiState()
    data class Success(val result: AnalysisResult) : SwingAnalysisUiState()
    data class Error(val message: String) : SwingAnalysisUiState()
}
