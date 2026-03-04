package com.swingtrace.aicoaching.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.golftrajectory.app.PoseDetector
import com.golftrajectory.app.UsageManager
import com.golftrajectory.app.UsageLimitDialog
import com.golftrajectory.app.plan.LitePlanAdBanner
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.logic.BiomechanicsFrame
import com.golftrajectory.app.ui.BiomechanicsHud
import com.golftrajectory.app.ui.KinematicsGraph
import com.golftrajectory.app.SwingTraceViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.golftrajectory.app.plan.Plan
import android.media.MediaMetadataRetriever

/**
 * スイング姿勢分析画面（MediaPipe使用）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwingPoseAnalysisScreen(
    videoUri: Uri,
    analysisMode: String = "rear", // "rear" or "front"
    planTier: Plan,
    onBack: () -> Unit,
    onAICoachClick: (com.golftrajectory.app.SwingAnalysisResult) -> Unit = {}
) {
    var showDetailScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val usageManager = remember { UsageManager(context) }
    val viewModel: SwingTraceViewModel = viewModel()
    
    var showUsageLimitDialog by remember { mutableStateOf(false) }
    var remainingCount by remember { mutableStateOf(usageManager.getRemainingCount()) }
    
    // Biomechanics state for real-time display
    var biomechanicsData by remember { mutableStateOf<BiomechanicsFrame?>(null) }
    val biomechanicsHistory by viewModel.biomechanicsHistory.collectAsState()
    
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
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
            // Update ViewModel with new biomechanics data
            viewModel.updateBiomechanics(biomechanicsFrame)
        }
    }
    
    // 分析開始
    fun startAnalysis() {
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
                    
                    withContext(Dispatchers.Main) {
                        videoWidth = width
                        videoHeight = height
                    }
                    
                    // フレームを解析（100msごと）
                    val frameInterval = 100_000L // マイクロ秒
                    var currentTime = 0L
                    val detectedPoses = mutableListOf<List<Offset>>()
                    
                    while (currentTime < duration * 1000) {
                        // フレームを取得
                        val frame = retriever.getFrameAtTime(
                            currentTime,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        
                        frame?.let { bitmap ->
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
                                    detectedPoses.add(points)
                                }
                            }
                        }
                        
                        currentTime += frameInterval
                        
                        // 進捗更新
                        withContext(Dispatchers.Main) {
                            analysisProgress = currentTime.toFloat() / (duration * 1000)
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
                                    
                                    // プロ類似度を計算
                                    val similarities = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.calculateSimilarities(
                                        backswingAngle = result.backswingAngle.toDouble(),
                                        downswingSpeed = result.downswingSpeed.toDouble(),
                                        hipRotation = result.hipRotation.toDouble(),
                                        shoulderRotation = result.shoulderRotation.toDouble(),
                                        headStability = result.headStability.toDouble(),
                                        weightTransfer = result.weightTransfer.toDouble()
                                    )
                                    val topPro = similarities.firstOrNull()
                                    
                                    val historyEntity = com.swingtrace.aicoaching.database.AnalysisHistoryEntity(
                                        userId = userId,
                                        timestamp = System.currentTimeMillis(),
                                        videoUri = videoUri.toString(),
                                        ballDetected = false,
                                        carryDistance = 0.0,
                                        maxHeight = 0.0,
                                        flightTime = 0.0,
                                        confidence = 0.0,
                                        aiAdvice = null,
                                        aiScore = result.score,
                                        swingSpeed = null,
                                        backswingTime = null,
                                        downswingTime = null,
                                        impactSpeed = null,
                                        tempo = null,
                                        totalScore = result.score,
                                        backswingAngle = result.backswingAngle.toDouble(),
                                        downswingSpeed = result.downswingSpeed.toDouble(),
                                        hipRotation = result.hipRotation.toDouble(),
                                        shoulderRotation = result.shoulderRotation.toDouble(),
                                        headStability = result.headStability.toDouble(),
                                        weightTransfer = result.weightTransfer.toDouble(),
                                        swingPlane = "正常",
                                        topProName = topPro?.pro?.name,
                                        topProSimilarity = topPro?.similarity
                                    )
                                    
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        database.analysisHistoryDao().insert(historyEntity)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            isAnalyzing = false
                        } else {
                            errorMessage = "姿勢を検出できませんでした\n\n人物が画面に映っているか確認してください"
                            isAnalyzing = false
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMessage = "エラー: ${e.message}\n\nMediaPipeモデルが見つからない可能性があります。"
                    isAnalyzing = false
                }
            }
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
            
            // 骨格表示（オーバーレイ）
            if (posePoints.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 正規化座標を画面座標に変換
                    val scaleX = size.width
                    val scaleY = size.height
                    val scaledPoints = posePoints.map { point ->
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
                        // バイオメカニクスデータに基づく動的色決定
                        biomechanicsData?.let { frame ->
                            return when (partType) {
                                "hip" -> if (frame.isStable) Color.Green else Color.Red
                                "spine" -> if (frame.spineAngleDegrees in 25f..50f) Color.Cyan else Color.Yellow
                                "shoulder" -> if (frame.xFactorDegrees in 30f..60f) Color.Green else Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        }
                        return Color(0xFF4CAF50)
                    }
                    
                    // MediaPipe Poseの骨格接続（部位別）
                    val headConnections = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 7, 0 to 4, 4 to 5, 5 to 6, 6 to 8, 9 to 10)
                    val shoulderConnections = listOf(11 to 12)
                    val hipConnections = listOf(23 to 24)
                    val armConnections = listOf(11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
                                                12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20)
                    val legConnections = listOf(23 to 25, 25 to 27, 27 to 29, 27 to 31, 29 to 31,
                                               24 to 26, 26 to 28, 28 to 30, 28 to 32, 30 to 32)
                    val bodyConnections = listOf(11 to 23, 12 to 24)
                    
                    // 頭部（緑/赤）
                    headConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("head"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 6f
                            )
                        }
                    }
                    
                    // 肩（緑/黄/赤）
                    shoulderConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("shoulder"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 8f
                            )
                        }
                    }
                    
                    // 腰（緑/黄/赤）
                    hipConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("hip"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 8f
                            )
                        }
                    }
                    
                    // 腕（緑/黄/赤）
                    armConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("arm"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 6f
                            )
                        }
                    }
                    
                    // 脚（緑/黄/赤）
                    legConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("leg"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 6f
                            )
                        }
                    }
                    
                    // 背骨（Spine Angleに基づく色）
                    bodyConnections.forEach { (start, end) ->
                        if (start < scaledPoints.size && end < scaledPoints.size) {
                            drawLine(
                                color = getPartColor("spine"),
                                start = scaledPoints[start],
                                end = scaledPoints[end],
                                strokeWidth = 8f
                            )
                        }
                    }
                    
                    // 骨格の点を描画
                    scaledPoints.forEach { point ->
                        drawCircle(
                            color = Color.Yellow,
                            radius = 10f,
                            center = point
                        )
                    }
                }
            }
            
            // Biomechanics HUD overlay
            BiomechanicsHud(
                frame = biomechanicsData,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // Kinematics Graph
            KinematicsGraph(
                history = biomechanicsHistory,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            
            // 分析結果表示（簡易版）
            analysisResult?.let { result ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .width(160.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "スコア",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${result.score}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                result.score >= 80 -> Color(0xFF4CAF50)
                                result.score >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 推定飛距離表示
                        if (result.estimatedDistance > 0) {
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "推定飛距離",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            // 選択したクラブに応じた飛距離を計算
                            val clubRatio = com.swingtrace.aicoaching.utils.DistanceEstimator.getClubRatio(selectedClub)
                            val adjustedDistance = (result.estimatedDistance * clubRatio).toInt()
                            Text(
                                text = "${adjustedDistance}yd",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = getClubName(selectedClub),
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showDetailScreen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("詳細", fontSize = 16.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                analysisResult?.let { onAICoachClick(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            enabled = analysisResult != null
                        ) {
                            Text("AIコーチ", fontSize = 14.sp)
                        }

                        if (isLitePlan) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "詳細な時系列データはPROプラン専用です。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LitePlanAdBanner()
                        }
                    }
                }
            }
            
            // アドバイス表示
            analysisResult?.let { result ->
                val adviceList = swingAnalyzer.getAdvice(result)
                
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .width(280.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💬 アドバイス",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        adviceList.forEach { advice ->
                            Text(
                                text = advice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // 分析ボタン
            if (!isAnalyzing && analysisResult == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 残り回数表示
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "残り回数: $remainingCount/2",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // クラブ選択ボタン
                    OutlinedButton(
                        onClick = { showClubSelector = !showClubSelector },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🏌️ クラブ: ${getClubName(selectedClub)}")
                    }
                    
                    // クラブ選択ドロップダウン
                    if (showClubSelector) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.values().forEach { club ->
                                    TextButton(
                                        onClick = {
                                            selectedClub = club
                                            showClubSelector = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = getClubName(club),
                                            modifier = Modifier.fillMaxWidth(),
                                            fontWeight = if (club == selectedClub) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { startAnalysis() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("分析開始")
                    }
                }
            }
            
            // 分析中の表示
            if (isAnalyzing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("姿勢を分析中...")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = analysisProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // エラーメッセージ
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
                onDismiss = {
                    showUsageLimitDialog = false
                },
                onUpgrade = {
                    // TODO: プレミアムプランへ
                    showUsageLimitDialog = false
                },
                onWatchAd = {
                    // TODO: 広告表示
                    usageManager.reviveWithAd()
                    remainingCount = usageManager.getRemainingCount()
                    showUsageLimitDialog = false
                    startAnalysis()
                }
            )
        }
    }
    
    // 詳細画面を表示
    if (showDetailScreen && analysisResult != null) {
        SwingResultDetailScreen(
            result = analysisResult!!,
            adviceList = swingAnalyzer.getAdvice(analysisResult!!),
            onBack = { showDetailScreen = false },
            isPremium = true  // テスト用：常にプレミアム
        )
    }
}

@Composable
fun ResultItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * クラブタイプの日本語名を取得
 */
fun getClubName(clubType: com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType): String {
    return when (clubType) {
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.DRIVER -> "ドライバー"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.WOOD_3 -> "3番ウッド"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.WOOD_5 -> "5番ウッド"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.UT_3 -> "3番ユーティリティ"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.UT_4 -> "4番ユーティリティ"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_4 -> "4番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_5 -> "5番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_6 -> "6番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_7 -> "7番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_8 -> "8番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.IRON_9 -> "9番アイアン"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.WEDGE_PW -> "ピッチングウェッジ"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.WEDGE_AW -> "アプローチウェッジ"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.WEDGE_SW -> "サンドウェッジ"
        com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.CUSTOM -> "カスタム"
    }
}
