package com.swingtrace.aicoaching.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.golftrajectory.app.AutoTrajectoryAnalyzer
import com.golftrajectory.app.UnitConverter
import com.golftrajectory.app.UsageManager
import com.golftrajectory.app.UsageLimitDialog
import com.golftrajectory.app.YOLOv8BallDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * YOLOv8自動ボール検出 + 弾道表示画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTrajectoryScreen(
    videoUri: Uri,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 使用回数管理
    val usageManager = remember { UsageManager(context) }
    var showUsageLimitDialog by remember { mutableStateOf(false) }
    var remainingCount by remember { mutableStateOf(usageManager.getRemainingCount()) }
    
    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }
    
    // YOLOv8検出器
    val detector = remember { YOLOv8BallDetector(context) }
    val analyzer = remember { AutoTrajectoryAnalyzer() }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var trajectoryResult by remember { mutableStateOf<AutoTrajectoryAnalyzer.TrajectoryResult?>(null) }
    // ヤード固定
    val distanceUnit = UnitConverter.DistanceUnit.YARDS
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            detector.close()
        }
    }
    
    /**
     * 動画を解析してボールを自動検出
     */
    fun analyzeVideo() {
        // 使用回数チェック
        if (!usageManager.canUse()) {
            showUsageLimitDialog = true
            return
        }
        
        scope.launch {
            // 使用回数をカウント
            usageManager.incrementUsage()
            remainingCount = usageManager.getRemainingCount()
            
            isAnalyzing = true
            analysisProgress = 0f
            errorMessage = null
            analyzer.clear()
            
            try {
                withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, videoUri)
                    
                    // 動画情報を取得
                    val duration = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L
                    
                    val width = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 1920
                    
                    val height = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 1080
                    
                    // フレームレート取得（デフォルト30fps）
                    val fps = 30f
                    
                    // 100msごとにフレームを解析（高速化のため）
                    val frameInterval = 100_000L // マイクロ秒
                    var currentTime = 0L
                    var frameIndex = 0
                    
                    while (currentTime < duration * 1000) {
                        // フレームを取得
                        val frame = retriever.getFrameAtTime(
                            currentTime,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        
                        frame?.let { bitmap ->
                            // YOLOv8でボール検出
                            val detection = detector.detectBall(bitmap)
                            
                            detection?.let {
                                // 検出されたボール位置を記録
                                analyzer.addPoint(
                                    it.position,
                                    currentTime / 1000,
                                    frameIndex
                                )
                            }
                        }
                        
                        currentTime += frameInterval
                        frameIndex++
                        
                        // 進捗更新
                        analysisProgress = currentTime.toFloat() / (duration * 1000)
                    }
                    
                    retriever.release()
                    
                    // 弾道を計算
                    val result = analyzer.analyzeTrajectory(width, height, fps)
                    
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            trajectoryResult = result
                        } else {
                            errorMessage = "ボールを検出できませんでした"
                        }
                        isAnalyzing = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "エラー: ${e.message}\n\nYOLOv8モデルが見つからない可能性があります。\nassetsフォルダにyolov8n.tfliteを配置してください。"
                    isAnalyzing = false
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自動弾道表示") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
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
            // 動画プレーヤー
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 弾道オーバーレイ
            trajectoryResult?.let { result ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 1920f // 動画サイズに合わせて調整
                    val scaleY = size.height / 1080f
                    
                    // 弾道線を描画（回数で色分け）
                    val path = Path()
                    result.points.forEachIndexed { index, point ->
                        val scaledPoint = Offset(
                            point.x * scaleX,
                            point.y * scaleY
                        )
                        
                        if (index == 0) {
                            path.moveTo(scaledPoint.x, scaledPoint.y)
                        } else {
                            path.lineTo(scaledPoint.x, scaledPoint.y)
                        }
                    }
                    
                    // 回数で色を変更（1回目=青、2回目=緑、3回目=赤）
                    val trajectoryColor = when (usageManager.getCurrentColor()) {
                        UsageManager.TrajectoryColor.BLUE -> Color(0xFF2196F3)  // 青
                        UsageManager.TrajectoryColor.GREEN -> Color(0xFF4CAF50) // 緑
                        UsageManager.TrajectoryColor.RED -> Color(0xFFF44336)   // 赤
                    }
                    
                    drawPath(
                        path = path,
                        color = trajectoryColor,
                        style = Stroke(width = 8f)
                    )
                    
                    // 発射点マーカー
                    drawCircle(
                        color = Color.Green,
                        radius = 15f,
                        center = Offset(
                            result.launchPoint.x * scaleX,
                            result.launchPoint.y * scaleY
                        )
                    )
                    
                    // 着地点マーカー
                    drawCircle(
                        color = Color.Red,
                        radius = 15f,
                        center = Offset(
                            result.landingPoint.x * scaleX,
                            result.landingPoint.y * scaleY
                        )
                    )
                }
            }
            
            // 分析ボタン
            if (!isAnalyzing && trajectoryResult == null) {
                FloatingActionButton(
                    onClick = { analyzeVideo() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, "自動分析")
                }
            }
            
            // 分析中の進捗表示
            if (isAnalyzing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ボールを検出中...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = analysisProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(analysisProgress * 100).toInt()}%",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 結果表示
            trajectoryResult?.let { result ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "弾道データ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DataItem(
                                label = "飛距離",
                                value = UnitConverter.formatDistance(result.distance.toDouble(), distanceUnit)
                            )
                            DataItem(
                                label = "最高到達点",
                                value = UnitConverter.formatDistance(result.maxHeight.toDouble(), distanceUnit)
                            )
                            DataItem(
                                label = "滞空時間",
                                value = String.format("%.1fs", result.flightTime)
                            )
                        }
                    }
                }
            }
            
            // エラーメッセージ（長く表示）
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ エラー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { errorMessage = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("閉じる")
                        }
                    }
                }
            }
        }
        
        // 使用制限ダイアログ
        if (showUsageLimitDialog) {
            UsageLimitDialog(
                remainingCount = remainingCount,
                nextResetTime = usageManager.getNextResetTime(),
                canWatchAd = usageManager.canReviveWithAd(),
                onDismiss = { showUsageLimitDialog = false },
                onUpgrade = {
                    showUsageLimitDialog = false
                    // TODO: プレミアムプラン登録画面へ
                },
                onWatchAd = {
                    showUsageLimitDialog = false
                    // TODO: リワード広告を表示
                    // 広告視聴完了後に以下を実行
                    usageManager.reviveWithAd()
                    remainingCount = usageManager.getRemainingCount()
                }
            )
        }
    }
}

@Composable
private fun DataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
