package com.golftrajectory.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * スイング軌道追跡画面
 */
@Composable
fun SwingTraceScreen(
    viewModel: SwingTraceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val pathPoints by viewModel.pathPoints.collectAsState()
    val phaseColors by viewModel.phaseColors.collectAsState()
    val trajectory by viewModel.trajectory.collectAsState()
    
    var useGemini by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スイング軌道追跡") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Gemini使用スイッチ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gemini AI分類を使用")
                        Switch(
                            checked = useGemini,
                            onCheckedChange = { useGemini = it },
                            enabled = !isRecording
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ボタン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 記録開始/停止
                        Button(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopRecording(useGemini)
                                } else {
                                    viewModel.onAnalyzeSwing()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRecording) "停止" else "記録開始")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // リセット
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            enabled = !isRecording,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("リセット")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // カメラプレビュー + 軌道オーバーレイ
            CameraWithTrajectory(
                viewModel = viewModel,
                pathPoints = pathPoints,
                phaseColors = phaseColors
            )
            
            // ステータス表示
            when (val state = uiState) {
                is SwingTraceViewModel.UiState.Recording -> {
                    RecordingIndicator(
                        pointCount = trajectory.size,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                
                is SwingTraceViewModel.UiState.Processing -> {
                    ProcessingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is SwingTraceViewModel.UiState.Completed -> {
                    CompletedCard(
                        pointCount = state.pointCount,
                        duration = state.duration,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                
                is SwingTraceViewModel.UiState.Error -> {
                    ErrorCard(
                        message = state.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {}
            }
        }
    }
}

@Composable
fun CameraWithTrajectory(
    viewModel: SwingTraceViewModel,
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<androidx.compose.ui.graphics.Color>
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // カメラプレビュー
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrameAvailable = { bitmap ->
                viewModel.processFrame(bitmap)
            },
            cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
        )
        
        // 軌道オーバーレイ
        SwingPathCanvas(
            pathPoints = pathPoints,
            phaseColors = phaseColors,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 6f
        )
    }
}

@Composable
fun RecordingIndicator(
    pointCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 録画アイコン
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("記録中... ($pointCount points)")
        }
    }
}

@Composable
fun ProcessingIndicator(modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("フェーズ分類中...")
        }
    }
}

@Composable
fun CompletedCard(
    pointCount: Int,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✅ 記録完了",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("ポイント数: $pointCount")
            Text("時間: ${duration}ms")
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "❌ エラー",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(message)
        }
    }
}
