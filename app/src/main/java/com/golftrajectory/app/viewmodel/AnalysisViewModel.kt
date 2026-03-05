package com.golftrajectory.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 動画解析用 ViewModel
 * Activity 再生成時も Uri を永続化する
 */
class AnalysisViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val KEY_VIDEO_URI = "video_uri"
        private const val KEY_ANALYSIS_MODE = "analysis_mode"
        private const val KEY_AUTO_START = "auto_start"
    }
    
    // 動画 URI（永続化）
    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()
    
    // 解析モード
    private val _analysisMode = MutableStateFlow("rear")
    val analysisMode: StateFlow<String> = _analysisMode.asStateFlow()
    
    // 自動開始フラグ
    private val _autoStart = MutableStateFlow(false)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()
    
    init {
        // SavedStateHandle から状態を復元
        savedStateHandle.get<String>(KEY_VIDEO_URI)?.let { uriString ->
            _videoUri.value = Uri.parse(uriString)
        }
        savedStateHandle.get<String>(KEY_ANALYSIS_MODE)?.let { mode ->
            _analysisMode.value = mode
        }
        savedStateHandle.get<Boolean>(KEY_AUTO_START)?.let { auto ->
            _autoStart.value = auto
        }
    }
    
    /**
     * 動画 URI を設定（永続化）
     */
    fun setVideoUri(uri: Uri?) {
        _videoUri.value = uri
        uri?.let { 
            savedStateHandle.set(KEY_VIDEO_URI, it.toString())
        } ?: run {
            savedStateHandle.remove<String>(KEY_VIDEO_URI)
        }
    }
    
    /**
     * 解析モードを設定
     */
    fun setAnalysisMode(mode: String) {
        _analysisMode.value = mode
        savedStateHandle.set(KEY_ANALYSIS_MODE, mode)
    }
    
    /**
     * 自動開始フラグを設定
     */
    fun setAutoStart(auto: Boolean) {
        _autoStart.value = auto
        savedStateHandle.set(KEY_AUTO_START, auto)
    }
    
    /**
     * 全状態をクリア
     */
    fun clear() {
        _videoUri.value = null
        _analysisMode.value = "rear"
        _autoStart.value = false
        
        savedStateHandle.remove<String>(KEY_VIDEO_URI)
        savedStateHandle.remove<String>(KEY_ANALYSIS_MODE)
        savedStateHandle.remove<Boolean>(KEY_AUTO_START)
    }
}
