package com.golftrajectory.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * 処理状態
 */
sealed class ProcessingState {
    object Idle : ProcessingState()
    object Processing : ProcessingState()
    
    data class Success(
        val stats: VideoProcessor.DetectionStats
    ) : ProcessingState() {
        val qualityLevel: QualityLevel
            get() = when {
                stats.adoptionRate >= 0.7f -> QualityLevel.EXCELLENT
                stats.adoptionRate >= 0.4f -> QualityLevel.GOOD
                stats.adoptionRate >= 0.2f -> QualityLevel.POOR
                else -> QualityLevel.FAILED
            }
    }
    
    data class Failed(val message: String) : ProcessingState()
}

enum class QualityLevel {
    EXCELLENT,  // 70%以上
    GOOD,       // 40-70%
    POOR,       // 20-40%
    FAILED      // 20%未満
}

/**
 * クラブヘッド追跡画面
 */
@Composable
fun ClubHeadTrackingScreen(
    useCase: ClubHeadTrackingUseCase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val pathPoints by useCase.pathPoints.collectAsState()
    val phaseColors by useCase.phaseColors.collectAsState()
    
    var isRecording by remember { mutableStateOf(false) }
    var processingState by remember { mutableStateOf<ProcessingState>(ProcessingState.Idle) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scope = rememberCoroutineScope()
    
    // 動画選択ランチャー
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        android.util.Log.d("ClubHeadTracking", "動画選択コールバック: uri=$uri")
        
        if (uri != null) {
            processingState = ProcessingState.Processing
            android.util.Log.d("ClubHeadTracking", "処理開始")
            
            scope.launch(Dispatchers.IO) {
                try {
                    android.util.Log.d("ClubHeadTracking", "Detector作成中")
                    val detector = ClubHeadDetector(context)
                    android.util.Log.d("ClubHeadTracking", "Processor作成中")
                    
                    // UI表示用の信頼度閾値を設定（段階的に調整可能）
                    // テスト1: 0.25f (25%) - 推奨初期値
                    // テスト2: 0.1f (10%) - 緩い設定
                    // テスト3: 0.5f (50%) - 厳しい設定
                    val uiConfidenceThreshold = 0.25f
                    
                    val processor = VideoProcessor(context, detector, uiConfidenceThreshold)
                    android.util.Log.d("ClubHeadTracking", "動画処理開始 (UI閾値: $uiConfidenceThreshold)")
                    val result = processor.processVideo(uri)
                    
                    // 統計情報をログ出力
                    android.util.Log.d("ClubHeadTracking", "処理完了: ${result.trajectoryPoints.size}点")
                    android.util.Log.d("ClubHeadTracking", result.stats.toReadableString())
                    
                    // 成功状態に遷移
                    withContext(Dispatchers.Main) {
                        processingState = ProcessingState.Success(result.stats)
                    }
                    
                    detector.close()
                } catch (e: Exception) {
                    android.util.Log.e("ClubHeadTracking", "エラー", e)
                    withContext(Dispatchers.Main) {
                        processingState = ProcessingState.Failed(
                            e.message ?: "不明なエラーが発生しました"
                        )
                    }
                    android.util.Log.d("ClubHeadTracking", "処理終了")
                }
            }
        } else {
            android.util.Log.d("ClubHeadTracking", "動画選択キャンセル")
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("クラブヘッド軌道追跡") },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 動画選択ボタン
                    OutlinedButton(
                        onClick = { 
                            videoPickerLauncher.launch("video/*")
                        },
                        enabled = processingState !is ProcessingState.Processing && !isRecording,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (processingState is ProcessingState.Processing) "処理中..." else "📹 動画")
                    }
                    
                    // 記録開始/停止ボタン
                    Button(
                        onClick = {
                            if (isRecording) {
                                useCase.stopRecording()
                                isRecording = false
                            } else {
                                useCase.startRecording()
                                isRecording = true
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isRecording) "⏹ 停止" else "⏺ 記録")
                    }
                    
                    // リセットボタン
                    OutlinedButton(
                        onClick = { useCase.reset() },
                        enabled = !isRecording,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("🔄 リセット")
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
            // カメラプレビュー
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrameAvailable = { bitmap ->
                    if (isRecording) {
                        useCase.processFrame(bitmap)
                    }
                },
                cameraExecutor = cameraExecutor
            )
            
            // 軌道オーバーレイ
            SwingPathCanvas(
                pathPoints = pathPoints,
                phaseColors = phaseColors,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 6f
            )
            
            // 記録中インジケーター
            if (isRecording) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 録画アイコン（点滅）
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "記録中...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            
            // ポイント数表示
            if (pathPoints.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ポイント: ${pathPoints.size}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 処理状態オーバーレイ
            when (val state = processingState) {
                is ProcessingState.Processing -> {
                    ProcessingOverlay()
                }
                is ProcessingState.Success -> {
                    SuccessOverlay(
                        stats = state.stats,
                        qualityLevel = state.qualityLevel,
                        onDismiss = { processingState = ProcessingState.Idle }
                    )
                }
                is ProcessingState.Failed -> {
                    FailedOverlay(
                        message = state.message,
                        onDismiss = { processingState = ProcessingState.Idle }
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * カメラプレビュー
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameAvailable: (android.graphics.Bitmap) -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // プレビュー
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // 画像解析
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                        onFrameAvailable(bitmap)
                        imageProxy.close()
                    }
                }
            
            // カメラを選択
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
