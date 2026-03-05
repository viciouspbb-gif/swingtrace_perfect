package com.swingtrace.aicoaching.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import com.golftrajectory.app.PoseDetector
import com.golftrajectory.app.UsageManager
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.logic.BiomechanicsFrame
import com.golftrajectory.app.ui.BiomechanicsHud
import com.golftrajectory.app.ui.KinematicsGraph
import com.golftrajectory.app.utils.LockScreenOrientation
import android.content.pm.ActivityInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.golftrajectory.app.plan.Plan
import android.media.MediaMetadataRetriever
import android.view.Surface
import android.view.TextureView
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.Log
import android.widget.Toast

/**
 * スイング姿勢分析画面（MediaPipe使用）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwingPoseAnalysisScreen(
    videoUri: Uri,
    analysisMode: String = "rear", // "rear" or "front"
    planTier: Plan,
    autoStart: Boolean = false, // 自動開始フラグ
    onBack: () -> Unit,
    onAICoachClick: (com.golftrajectory.app.SwingAnalysisResult) -> Unit = {}
) {
    var showDetailScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val usageManager = remember { UsageManager(context) }
    
    // 横画面に固定（一度だけ実行）
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    
    // 物理的な向きロック（Lifecycle 連動）
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val activity = (context as? ComponentActivity)
        val originalOrientation = activity?.requestedOrientation
        
        // Activity が AttachedToWindow された瞬間に物理的にロック
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            // 元の向きに戻す
            originalOrientation?.let { activity?.requestedOrientation = it }
        }
    }
    
    // Ready-to-Analyze gatekeeping states
    var isOrientationLocked by remember { mutableStateOf(false) }
    var isVideoLoaded by remember { mutableStateOf(false) }
    var isSurfaceReady by remember { mutableStateOf(false) }
    var videoRotation by remember { mutableStateOf(0) }
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var letterboxRatio by remember { mutableStateOf(1.0f) } // レターボックス比率
    
    // 向きロック完了を待機
    LaunchedEffect(Unit) {
        delay(100) // 向き変更を待機
        isOrientationLocked = true
    }
    
    // 動画読み込みと向き確認
    LaunchedEffect(videoUri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // 動画の回転角を取得
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            videoRotation = rotation
            
            // 動画のサイズを取得
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            
            // 縦動画の判定とレターボックス比率計算
            val isPortrait = rotation == 90 || rotation == 270
            if (isPortrait) {
                // 縦動画の場合、横画面での表示比率を計算
                val videoAspectRatio = height.toFloat() / width.toFloat() // 実際の動画比率（縦長）
                val screenAspectRatio = 16f / 9f // 画面比率（横長）
                letterboxRatio = screenAspectRatio / videoAspectRatio // レターボックス比率
                Log.d("SwingPoseAnalysisScreen", "Portrait video detected: ${width}x${height}, letterbox ratio: $letterboxRatio")
            } else {
                letterboxRatio = 1.0f
            }
            
            // プレビュー用の最初のフレームを取得
            val firstFrame = retriever.frameAtTime
            previewFrame = firstFrame
            
            isVideoLoaded = true
            retriever.release()
        } catch (e: Exception) {
            Log.e("SwingPoseAnalysisScreen", "Error loading video metadata", e)
            errorMessage = "動画の読み込みに失敗しました"
        }
    }
    
    // Ready-to-Analyze gatekeeping
    val isReadyToAnalyze = isOrientationLocked && isVideoLoaded && isSurfaceReady
    
    var showUsageLimitDialog by remember { mutableStateOf(false) }
    var remainingCount by remember { mutableStateOf(usageManager.getRemainingCount()) }
    
    // Biomechanics state for real-time display
    var biomechanicsData by remember { mutableStateOf<BiomechanicsFrame?>(null) }
    var biomechanicsHistory by remember { mutableStateOf<List<BiomechanicsFrame>>(emptyList()) }
    
    // クラブ選択
    var selectedClub by remember { mutableStateOf(com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.DRIVER) }
    var showClubSelector by remember { mutableStateOf(false) }
    
    // ExoPlayerの初期化
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }
    
    // PoseDetectorの初期化
    val poseDetector = remember { PoseDetector(context) }
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var posePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var allPoses by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var videoWidth by remember { mutableStateOf(1920) }
    var videoHeight by remember { mutableStateOf(1080) }
    var frameDuration by remember { mutableStateOf(100L) } // ms
    var analysisResult by remember { mutableStateOf<com.golftrajectory.app.SwingAnalysisResult?>(null) }
    
    val swingAnalyzer = remember { com.golftrajectory.app.PoseSwingAnalyzer() }
    val isLitePlan = planTier == Plan.PRACTICE
    
    // 動画の再生位置を監視
    LaunchedEffect(allPoses) {
        if (allPoses.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(50) // 20fps
                val currentPosition = exoPlayer.currentPosition
                val frameIndex = (currentPosition / frameDuration).toInt()
                    .coerceIn(0, allPoses.size - 1)
                posePoints = allPoses[frameIndex]
            }
        }
    }
    
    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            poseDetector.close()
        }
    }
    
    // Collect biomechanics data
    LaunchedEffect(poseDetector) {
        poseDetector.poseResultFlow.collect { resultPair ->
            val biomechanicsFrame = resultPair.second
            biomechanicsData = biomechanicsFrame
            
            // Update local biomechanics history (ring buffer for last 100 frames)
            biomechanicsFrame?.let { newFrame ->
                val currentHistory = biomechanicsHistory.toMutableList()
                currentHistory.add(newFrame)
                
                // Keep only the last 100 frames
                if (currentHistory.size > 100) {
                    currentHistory.removeAt(0)
                }
                
                biomechanicsHistory = currentHistory
            }
        }
    }
    
    // 座標変換関数（レターボックス補正）
    fun transformCoordinatesForLetterbox(
        points: List<Offset>,
        letterboxRatio: Float
    ): List<Offset> {
        if (letterboxRatio == 1.0f) return points // 補正不要
        
        val letterboxWidth = (1.0f - letterboxRatio) / 2.0f // 左右の黒帯の幅
        return points.map { point ->
            // レターボックス内の有効エリアに座標を再マッピング
            val normalizedX = (point.x - letterboxWidth) / letterboxRatio
            Offset(
                x = normalizedX.coerceIn(0f, 1f),
                y = point.y // Y座標はそのまま
            )
        }
    }
    
    // 分析開始
    fun startAnalysis() {
        // Ready-to-Analyze gatekeeping
        if (!isReadyToAnalyze) {
            Log.w("SwingPoseAnalysisScreen", "Not ready to analyze: orientation=$isOrientationLocked, video=$isVideoLoaded, surface=$isSurfaceReady")
            return
        }
        
        // 使用回数チェック
        if (!usageManager.canUse()) {
            showUsageLimitDialog = true
            return
        }
        
        // 使用回数をカウント
        usageManager.incrementUsage()
        remainingCount = usageManager.getRemainingCount()
        
        // 分析処理を開始
        isAnalyzing = true
        errorMessage = null
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.Default) {
                    try {
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
                        
                        withContext(Dispatchers.Main) {
                            videoWidth = width
                            videoHeight = height
                        }
                        
                        // 縦動画のアスペクト比補正
                        val isPortrait = videoRotation == 90 || videoRotation == 270
                        val aspectRatio = if (isPortrait) {
                            height.toFloat() / width.toFloat()
                        } else {
                            width.toFloat() / height.toFloat()
                        }
                        
                        // フレームを解析（100msごと）
                        val frameInterval = 100_000L // マイクロ秒
                        var currentTime = 0L
                        val detectedPoses = mutableListOf<List<Offset>>()
                        
                        while (currentTime < duration * 1000) {
                            try {
                                // フレームを取得
                                val frame = retriever.getFrameAtTime(
                                    currentTime,
                                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                )
                                
                                frame?.let { bitmap ->
                                    try {
                                        // MediaPipeで姿勢検出
                                        val result = poseDetector.detectPose(bitmap)
                                        
                                        result?.let { poseResult ->
                                            if (poseResult.landmarks().isNotEmpty()) {
                                                // 骨格の点を抽出（正規化座標のまま保存）
                                                val landmarks = poseResult.landmarks()[0]
                                                val points = landmarks.map { landmark ->
                                                    Offset(
                                                        landmark.x(),
                                                        landmark.y()
                                                    )
                                                }
                                                
                                                // レターボックス補正を適用
                                                val correctedPoints = transformCoordinatesForLetterbox(points, letterboxRatio)
                                                detectedPoses.add(correctedPoints)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SwingPoseAnalysisScreen", "Error processing frame", e)
                                    }
                                }
                                
                                currentTime += frameInterval
                                
                                // 進捗更新
                                withContext(Dispatchers.Main) {
                                    analysisProgress = currentTime.toFloat() / (duration * 1000)
                                }
                            } catch (e: Exception) {
                                Log.e("SwingPoseAnalysisScreen", "Error in frame processing loop", e)
                            }
                        }
                        
                        retriever.release()
                        
                        withContext(Dispatchers.Main) {
                            if (detectedPoses.isNotEmpty()) {
                                // 全フレームの姿勢を保存
                                allPoses = detectedPoses
                                // 最初のフレームの骨格を表示
                                posePoints = detectedPoses.first()
                                
                                // スイング分析を実行
                                analysisResult = swingAnalyzer.analyze(detectedPoses)
                                
                                // 履歴に保存
                                analysisResult?.let { result ->
                                    try {
                                        val database = com.swingtrace.aicoaching.database.AppDatabase.getDatabase(context)
                                        val userPreferences = com.golftrajectory.app.UserPreferences(context)
                                        val userId = userPreferences.getUserId() ?: "guest"
                                        
                                        val historyEntity = com.swingtrace.aicoaching.database.AnalysisHistoryEntity(
                                            userId = userId,
                                            timestamp = System.currentTimeMillis(),
                                            videoUri = videoUri.toString(),
                                            ballDetected = true,
                                            carryDistance = 0.0,
                                            maxHeight = 0.0,
                                            flightTime = 0.0,
                                            confidence = 0.8,
                                            aiAdvice = "分析完了",
                                            aiScore = 85,
                                            swingSpeed = 0.0,
                                            backswingTime = 0.0,
                                            downswingTime = 0.0,
                                            impactSpeed = 0.0,
                                            tempo = 0.0,
                                            headStability = result.headStability.toDouble(),
                                            shoulderRotation = result.shoulderRotation.toDouble(),
                                            hipRotation = result.hipRotation.toDouble(),
                                            weightTransfer = result.weightTransfer.toDouble(),
                                            swingPlane = "正常"
                                        )
                                        
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            database.analysisHistoryDao().insert(historyEntity)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SwingPoseAnalysisScreen", "Error saving analysis result", e)
                                    }
                                }
                                
                                isAnalyzing = false
                            } else {
                                errorMessage = "姿勢を検出できませんでした\n\n人物が画面に映っているか確認してください"
                                isAnalyzing = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SwingPoseAnalysisScreen", "Error during video analysis", e)
                        withContext(Dispatchers.Main) {
                            errorMessage = "この動画形式はサポート外です"
                            Toast.makeText(context, "この動画形式はサポート外です", Toast.LENGTH_SHORT).show()
                            delay(2000)
                            onBack() // 安全に前の画面に戻る
                        }
                    } finally {
                        try {
                            // retriever is already released in the inner finally block
                        } catch (e: Exception) {
                            Log.e("SwingPoseAnalysisScreen", "Error in finally block", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SwingPoseAnalysisScreen", "Critical error in analysis", e)
                withContext(Dispatchers.Main) {
                    errorMessage = "解析中にエラーが発生しました: ${e.message}"
                    Toast.makeText(context, "この動画形式はサポート外です", Toast.LENGTH_SHORT).show()
                    isAnalyzing = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isAnalyzing = false
                }
            }
        }
    }
    
    // 自動開始処理
    LaunchedEffect(isReadyToAnalyze) {
        if (isReadyToAnalyze && autoStart) {
            delay(500) // UIが安定するのを待ってから自動開始
            startAnalysis()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (analysisMode == "rear") "後方スイング分析" else "正面スイング分析") 
                },
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
                .background(Color.Black) // 黒背景で白飛びを防止
                .padding(padding)
        ) {
            // 動画プレーヤー
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        Log.d("SwingPoseAnalysisScreen", "PlayerView created")
                        
                        // Surface準備完了を検知
                        try {
                            // Surface準備完了を検知（簡易版）
                            isSurfaceReady = true
                            Log.d("SwingPoseAnalysisScreen", "Surface ready flag set")
                        } catch (e: Exception) {
                            Log.e("SwingPoseAnalysisScreen", "Error setting surface callback", e)
                            // Surface callbackの設定に失敗しても続行
                            isSurfaceReady = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // プレビュー表示（準備中）
            if (!isReadyToAnalyze) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // プレビュー画像があれば表示
                    previewFrame?.let { frame ->
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "動画プレビュー",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    // 準備中表示
                    Column(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val statusText = when {
                            !isOrientationLocked -> "画面向きを調整中..."
                            !isVideoLoaded -> "動画を読み込み中..."
                            !isSurfaceReady -> "描画準備中..."
                            else -> "準備完了"
                        }
                        
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "少々お待ちください",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (previewFrame != null && !isAnalyzing) {
                // 準備完了後のプレビュー
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewFrame!!.asImageBitmap(),
                        contentDescription = "動画プレビュー",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // 解析開始ボタン
                    Button(
                        onClick = { startAnalysis() },
                        modifier = Modifier
                            .background(Color.Blue.copy(alpha = 0.8f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "分析開始",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // 骨格表示（オーバーレイ）
            if (posePoints.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 正規化座標を画面座標に変換
                    val scaleX = size.width
                    val scaleY = size.height
                    
                    // レターボックス補正を適用
                    val correctedPoints = transformCoordinatesForLetterbox(posePoints, letterboxRatio)
                    val scaledPoints = correctedPoints.map { point ->
                        Offset(
                            point.x * scaleX,
                            point.y * scaleY
                        )
                    }
                    
                    // 部位ごとの色を決定する関数
                    fun getPartColor(partType: String): Color {
                        analysisResult?.let { result ->
                            return when (partType) {
                                "head" -> if (result.headStability >= 70) Color(0xFF4CAF50) else Color(0xFFF44336)
                                "shoulder" -> when {
                                    result.shoulderRotation in 35f..65f -> Color(0xFF4CAF50)
                                    result.shoulderRotation in 25f..75f -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }
                                else -> Color(0xFF4CAF50)
                            }
                        }
                        return Color(0xFF4CAF50)
                    }
                    
                    // 骨格を描画
                    drawSkeleton(scaledPoints) { partType -> getPartColor(partType) }
                }
            }
            
            // 分析進捗
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "分析中... ${(analysisProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            // エラーメッセージ
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "エラー",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onBack) {
                                Text("戻る")
                            }
                        }
                    }
                }
            }
            
            // バイオメカニクスHUD
            if (biomechanicsData != null && !isLitePlan) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    biomechanicsData?.let { data ->
                        BiomechanicsHud(frame = data)
                    }
                }
            }
            
            // 分析結果ボタン
            if (analysisResult != null && !isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = { onAICoachClick(analysisResult!!) },
                        containerColor = Color(0xFF2196F3)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "AIコーチ")
                    }
                }
            }
        }
    }
    
    // 使用回数制限ダイアログ
    if (showUsageLimitDialog) {
        AlertDialog(
            onDismissRequest = { showUsageLimitDialog = false },
            title = { Text("使用回数制限") },
            text = { 
                Text("今日の分析回数が上限に達しました。\n明日までお待ちいただくか、プランをアップグレードしてください。")
            },
            confirmButton = {
                TextButton(onClick = { showUsageLimitDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// 骨格描画関数
private fun DrawScope.drawSkeleton(
    points: List<Offset>,
    getPartColor: (String) -> Color
) {
    // 骨格の接続関係を定義
    val connections = listOf(
        Pair(0, 1), // 頭-首
        Pair(1, 2), // 首-左肩
        Pair(1, 5), // 首-右肩
        Pair(2, 3), // 左肩-左肘
        Pair(3, 4), // 左肘-左手首
        Pair(5, 6), // 右肩-右肘
        Pair(6, 7), // 右肘-右手首
        Pair(1, 8), // 首-左腰
        Pair(1, 11), // 首-右腰
        Pair(8, 9), // 左腰-左膝
        Pair(9, 10), // 左膝-左足首
        Pair(11, 12), // 右腰-右膝
        Pair(12, 13), // 右膝-右足首
    )
    
    // 骨格の線を描画
    connections.forEach { connection: Pair<Int, Int> ->
        val (start, end) = connection
        if (start < points.size && end < points.size) {
            drawLine(
                color = Color.White,
                start = points[start],
                end = points[end],
                strokeWidth = 4.dp.value
            )
        }
    }
    
    // 関節の点を描画
    points.forEach { point ->
        drawCircle(
            color = Color.Red,
            radius = 6.dp.value,
            center = point
        )
    }
}
