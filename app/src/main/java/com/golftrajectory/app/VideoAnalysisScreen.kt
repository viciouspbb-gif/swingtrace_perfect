package com.golftrajectory.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golftrajectory.app.usecase.AnalyzeVideoUseCase
import com.golftrajectory.app.usecase.DrawTrajectoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 動画分析ViewModel
 */
class VideoAnalysisViewModel(
    private val analyzeVideoUseCase: AnalyzeVideoUseCase,
    private val drawTrajectoryUseCase: DrawTrajectoryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _pathPoints = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Float, Float>>> = _pathPoints.asStateFlow()
    
    private val _phaseColors = MutableStateFlow<List<androidx.compose.ui.graphics.Color>>(emptyList())
    val phaseColors: StateFlow<List<androidx.compose.ui.graphics.Color>> = _phaseColors.asStateFlow()
    
    sealed class UiState {
        object Idle : UiState()
        data class Analyzing(
            val progress: Float,
            val currentFrame: Int,
            val totalFrames: Int
        ) : UiState()
        data class Completed(
            val pointCount: Int,
            val duration: Long,
            val fps: Float
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    /**
     * 動画を分析
     */
    fun analyzeVideo(videoUri: Uri, useGemini: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = UiState.Analyzing(0f, 0, 0)
            
            analyzeVideoUseCase.analyzeVideo(videoUri, useGemini).collect { result ->
                result.onSuccess { data ->
                    when (data) {
                        is AnalyzeVideoUseCase.AnalysisProgress -> {
                            _uiState.value = UiState.Analyzing(
                                progress = data.progress,
                                currentFrame = data.currentFrame,
                                totalFrames = data.totalFrames
                            )
                        }
                        
                        is AnalyzeVideoUseCase.VideoAnalysisResult -> {
                            // 描画データを準備
                            val drawablePoints = drawTrajectoryUseCase.prepareDrawData(
                                data.trajectory,
                                data.phases
                            )
                            
                            val (points, colors) = drawTrajectoryUseCase.getPathData(drawablePoints)
                            _pathPoints.value = points
                            _phaseColors.value = colors
                            
                            _uiState.value = UiState.Completed(
                                pointCount = data.trajectory.size,
                                duration = data.duration,
                                fps = data.fps
                            )
                        }
                    }
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unknown error")
                }
            }
        }
    }
    
    /**
     * リセット
     */
    fun reset() {
        _uiState.value = UiState.Idle
        _pathPoints.value = emptyList()
        _phaseColors.value = emptyList()
    }
}

/**
 * 動画分析画面
 */
@Composable
fun VideoAnalysisScreen(
    viewModel: VideoAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pathPoints by viewModel.pathPoints.collectAsState()
    val phaseColors by viewModel.phaseColors.collectAsState()
    
    var useGemini by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    // 動画選択ランチャー
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            viewModel.analyzeVideo(it, useGemini)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動画分析") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is VideoAnalysisViewModel.UiState.Idle -> {
                    IdleContent(
                        useGemini = useGemini,
                        onUseGeminiChange = { useGemini = it },
                        onSelectVideo = { videoPickerLauncher.launch("video/*") }
                    )
                }
                
                is VideoAnalysisViewModel.UiState.Analyzing -> {
                    AnalyzingContent(
                        progress = state.progress,
                        currentFrame = state.currentFrame,
                        totalFrames = state.totalFrames
                    )
                }
                
                is VideoAnalysisViewModel.UiState.Completed -> {
                    CompletedContent(
                        pathPoints = pathPoints,
                        phaseColors = phaseColors,
                        pointCount = state.pointCount,
                        duration = state.duration,
                        fps = state.fps,
                        onReset = { viewModel.reset() }
                    )
                }
                
                is VideoAnalysisViewModel.UiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun IdleContent(
    useGemini: Boolean,
    onUseGeminiChange: (Boolean) -> Unit,
    onSelectVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "スイング動画を選択",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "既存の動画からクラブヘッド軌道を分析します",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Gemini使用スイッチ
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gemini AI分類を使用")
            Switch(
                checked = useGemini,
                onCheckedChange = onUseGeminiChange
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 動画選択ボタン
        Button(
            onClick = onSelectVideo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.VideoLibrary,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("動画を選択")
        }
    }
}

@Composable
fun AnalyzingContent(
    progress: Float,
    currentFrame: Int,
    totalFrames: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(120.dp),
            strokeWidth = 8.dp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "分析中...",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Frame $currentFrame / $totalFrames",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompletedContent(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<androidx.compose.ui.graphics.Color>,
    pointCount: Int,
    duration: Long,
    fps: Float,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 軌道表示
        AnimatedSwingPathCanvas(
            pathPoints = pathPoints,
            phaseColors = phaseColors,
            showImpactMarker = true,
            modifier = Modifier.fillMaxSize()
        )
        
        // 情報カード
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✅ 分析完了",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("検出ポイント: $pointCount")
                Text("動画時間: ${duration / 1000f}秒")
                Text("FPS: ${fps.toInt()}")
            }
        }
        
        // リセットボタン
        FloatingActionButton(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                contentDescription = "リセット"
            )
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "エラー",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Text("もう一度試す")
        }
    }
}
