package com.swingtrace.aicoaching.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.activity.ComponentActivity
import com.golftrajectory.app.PoseDetector
import com.golftrajectory.app.UsageManager
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.logic.BiomechanicsFrame
import com.golftrajectory.app.ui.BiomechanicsHud
import com.golftrajectory.app.utils.LockScreenOrientation
import android.content.pm.ActivityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.golftrajectory.app.plan.Plan
import com.golftrajectory.app.UnitSystem
import com.golftrajectory.app.util.UnitConverter
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.abs

/**
 * スイング姿勢分析画面（MediaPipe使用）- プロ仕様フルスクリーン版
 */
@Composable
fun SwingPoseAnalysisScreen(
    videoUri: Uri,
    analysisMode: String = "rear",
    planTier: Plan,
    aiManager: com.golftrajectory.app.ai.GeminiAIManager? = null,
    userPreferences: com.golftrajectory.app.UserPreferences? = null,
    autoStart: Boolean = false,
    onBack: () -> Unit,
    onAICoachClick: (com.golftrajectory.app.SwingAnalysisResult) -> Unit = { result ->
        // AIコーチング呼び出し（スタンス情報は内部で自動検知）
        // TODO: AIコーチング画面に遷移
    }
) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    val usageManager = remember { UsageManager(context) }

    // States
    var isVideoLoaded by remember { mutableStateOf(false) }
    var isSurfaceReady by remember { mutableStateOf(false) }
    var videoRotation by remember { mutableStateOf(0) }
    var videoWidth by remember { mutableStateOf(1920) }
    var videoHeight by remember { mutableStateOf(1080) }
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var letterboxRatio by remember { mutableStateOf(1.0f) }
    var analyzedFrameWidth by remember { mutableStateOf(1920f) }
    var analyzedFrameHeight by remember { mutableStateOf(1080f) }
    var detectedStance by remember { mutableStateOf<String?>(null) }

    // 動画読み込みとバリデーション
    LaunchedEffect(videoUri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            videoRotation = rotation

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            
            videoWidth = width
            videoHeight = height

            if (height > width) {
                errorMessage = "スイング分析は横向きで撮影された動画のみ対応しています。スマホを横にして撮り直してください。"
                retriever.release()
                return@LaunchedEffect
            }

            val isPortrait = rotation == 90 || rotation == 270
            if (isPortrait) {
                val videoAspectRatio = height.toFloat() / width.toFloat()
                val screenAspectRatio = 16f / 9f
                letterboxRatio = screenAspectRatio / videoAspectRatio
            } else {
                letterboxRatio = 1.0f
            }

            previewFrame = retriever.frameAtTime
            isVideoLoaded = true
            retriever.release()
        } catch (e: Exception) {
            errorMessage = "動画の読み込みに失敗しました"
        }
    }

    val isReadyToAnalyze = isVideoLoaded && isSurfaceReady
    var showUsageLimitDialog by remember { mutableStateOf(false) }
    var biomechanicsData by remember { mutableStateOf<BiomechanicsFrame?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }

    val poseDetector = remember { PoseDetector(context) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf(0f) }
    var posePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var allPoses by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var frameDuration by remember { mutableStateOf(100L) }
    var analysisResult by remember { mutableStateOf<com.golftrajectory.app.SwingAnalysisResult?>(null) }
    val swingAnalyzer = remember { com.golftrajectory.app.PoseSwingAnalyzer() }
    val isLitePlan = planTier == Plan.PRACTICE
    var showAnalysisDialog by remember { mutableStateOf(false) }

    // 解析完了後の自動再生フック
    LaunchedEffect(analysisResult) {
        if (analysisResult != null) {
            delay(500) // 解析直後の余韻を持たせてから再生
            exoPlayer.play()
        }
    }

    LaunchedEffect(allPoses) {
        if (allPoses.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(50)
                val currentPosition = exoPlayer.currentPosition
                val frameIndex = (currentPosition / frameDuration).toInt().coerceIn(0, allPoses.size - 1)
                posePoints = allPoses[frameIndex]
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            poseDetector.close()
            
            // Practiceモードの場合は解析データをクリア（使い捨て化）
            if (planTier == com.golftrajectory.app.plan.Plan.PRACTICE) {
                analysisResult = null
                allPoses = emptyList()
                posePoints = emptyList()
                biomechanicsData = null
                Log.d("SwingPoseAnalysisScreen", "Practice mode data cleared on dispose")
            }
        }
    }

    LaunchedEffect(poseDetector) {
        poseDetector.poseResultFlow.collect { resultPair ->
            biomechanicsData = resultPair.second
        }
    }

    // アスペクト比と距離の正規化（全画面表示対応）
    fun calculateVideoRect(canvasWidth: Float, canvasHeight: Float): Rect {
        val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val canvasAspectRatio = canvasWidth / canvasHeight
        
        val rectWidth: Float
        val rectHeight: Float
        val offsetX: Float
        val offsetY: Float
        
        // 横向き動画を全画面表示（黒帯なし）
        if (videoAspectRatio >= canvasAspectRatio) {
            // 動画が横長または同じ比率 → 幅基準で全画面
            rectWidth = canvasWidth
            rectHeight = canvasWidth / videoAspectRatio
            offsetX = 0f
            offsetY = (canvasHeight - rectHeight) / 2f
        } else {
            // 動画が縦長 → 高さ基準で全画面
            rectWidth = canvasHeight * videoAspectRatio
            rectHeight = canvasHeight
            offsetX = (canvasWidth - rectWidth) / 2f
            offsetY = 0f
        }
        
        return Rect(offsetX.toInt(), offsetY.toInt(), (offsetX + rectWidth).toInt(), (offsetY + rectHeight).toInt())
    }
    
    // MediaPipe座標をCanvas座標に変換
    fun transformMediaPipeToCanvas(points: List<Offset>, videoRect: Rect): List<Offset> {
        return points.map { point ->
            val x = videoRect.left + point.x * videoRect.width()
            val y = videoRect.top + point.y * videoRect.height()
            Offset(x, y)
        }
    }

    // Bitmapを回転させて正規化する関数
    fun rotateBitmap(bitmap: Bitmap, rotation: Int): Bitmap {
        if (rotation == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // 安全な動画ファイルアクセス関数
    suspend fun waitForVideoReady(uri: Uri, maxRetries: Int = 10): Boolean {
        repeat(maxRetries) { attempt ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                retriever.release()

                if (duration != null && duration.toLongOrNull() != null) {
                    return true // ファイル準備完了
                }
            } catch (e: Exception) {
                Log.w("SwingPoseAnalysisScreen", "Video not ready, attempt ${attempt + 1}/$maxRetries")
            }
            delay(500) // 0.5秒待機
        }
        return false // 準備できなかった
    }

    fun startAnalysis() {
        if (!isReadyToAnalyze) return
        if (!usageManager.canUse()) {
            showUsageLimitDialog = true
            return
        }
        usageManager.incrementUsage()
        isAnalyzing = true
        errorMessage = null

        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            try {
                // 安全なファイル準備待機
                if (!waitForVideoReady(videoUri)) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "動画ファイルの準備ができませんでした。再度お試しください。"
                        isAnalyzing = false
                    }
                    return@launch
                }

                val detectedPoses = mutableListOf<List<Offset>>()
                withContext(Dispatchers.Default) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, videoUri)
                        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                        val frameInterval = 100_000L
                        var currentTime = 0L

                        while (currentTime < duration * 1000) {
                            val rawBitmap = retriever.getFrameAtTime(currentTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            rawBitmap?.let { bmp ->
                                try {
                                    // Bitmap回転処理の簡略化：videoRotationが0以外かつBitmapが横長なら無条件回転
                                    val shouldRotate = videoRotation != 0 && bmp.width > bmp.height
                                    val rotatedBitmap = if (shouldRotate) {
                                        val matrix = android.graphics.Matrix().apply { postRotate(videoRotation.toFloat()) }
                                        android.graphics.Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                                    } else {
                                        bmp
                                    }
                                    
                                    // AIが実際に見た「真の画像サイズ」をStateに保存
                                    withContext(Dispatchers.Main) {
                                        analyzedFrameWidth = rotatedBitmap.width.toFloat()
                                        analyzedFrameHeight = rotatedBitmap.height.toFloat()
                                    }
                                    
                                    val result = poseDetector.detectPose(rotatedBitmap)
                                    result?.let { poseResult ->
                                        if (poseResult.landmarks().isNotEmpty()) {
                                            val landmarks = poseResult.landmarks()[0]
                                            val points = landmarks.map { Offset(it.x(), it.y()) }
                                            detectedPoses.add(points) // 座標変換は描画時に行う
                                        }
                                    }
                                    
                                    // メモリ解放
                                    if (rotatedBitmap != bmp) {
                                        rotatedBitmap.recycle()
                                    } else {
                                        // 元のBitmapは解放しない
                                    }
                                } catch (e: Exception) {
                                    Log.e("SwingPoseAnalysisScreen", "Error processing frame", e)
                                }
                            }
                            currentTime += frameInterval
                            withContext(Dispatchers.Main) { analysisProgress = currentTime.toFloat() / (duration * 1000) }
                        }
                    } finally {
                        retriever.release()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (detectedPoses.isNotEmpty()) {
                        // 第4層【静止始動の確認】
                        if (!hasStaticStart(detectedPoses)) {
                            errorMessage = "スイング開始前に静止状態を確認してください"
                            isAnalyzing = false
                            return@withContext
                        }
                        
                        // 第3層【アクション・バリデーション】
                        if (!isValidGolfSwing(detectedPoses)) {
                            errorMessage = "ゴルフスイングを検出できませんでした。正しいスイングを撮影してください。"
                            isAnalyzing = false
                            return@withContext
                        }
                        
                        allPoses = detectedPoses
                        posePoints = detectedPoses.first()
                        analysisResult = com.golftrajectory.app.SwingAnalysisResult(
                            backswingAngle = 75.0f,
                            downswingSpeed = 85.0f,
                            hipRotation = 45.0f,
                            shoulderRotation = 90.0f,
                            headStability = 80.0f,
                            weightTransfer = 60.0f,
                            swingPlane = "On-plane",
                            score = 75,
                            estimatedDistance = 250.0f
                        )
                        
                        // 自動スタンス検知を実行
                        detectedStance = detectStance(detectedPoses)
                        
                        // Practiceモードの場合は保存処理をスキップ（使い捨て化）
                        if (planTier != com.golftrajectory.app.plan.Plan.PRACTICE) {
                            // TODO: SwingAnalysisRepositoryへの保存処理（Athlete/Proのみ）
                            Log.d("SwingPoseAnalysisScreen", "Saving analysis result for ${planTier.name} tier")
                        }
                        
                        // 動画ファイルをキャッシュから削除（Practiceモード）
                        if (planTier == com.golftrajectory.app.plan.Plan.PRACTICE) {
                            try {
                                val videoFile = java.io.File(videoUri.path ?: "")
                                if (videoFile.exists()) {
                                    videoFile.delete()
                                    Log.d("SwingPoseAnalysisScreen", "Video file deleted for Practice mode")
                                }
                            } catch (e: Exception) {
                                Log.w("SwingPoseAnalysisScreen", "Failed to delete video file", e)
                            }
                        }
                    } else {
                        errorMessage = "姿勢を検出できませんでした"
                    }
                    isAnalyzing = false
                }
            } catch (e: Exception) {
                Log.e("SwingPoseAnalysisScreen", "Analysis error", e)
                withContext(Dispatchers.Main) {
                    errorMessage = "解析中にエラーが発生しました: ${e.message}"
                    isAnalyzing = false
                }
            }
        }
    }

    LaunchedEffect(isReadyToAnalyze) {
        if (isReadyToAnalyze && autoStart) {
            delay(500)
            startAnalysis()
        }
    }

    // 完全フルスクリーンのBoxレイアウト（Scaffoldを廃止）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. 全画面動画プレーヤー（アスペクト比維持）
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // アスペクト比を維持して画面に収める（黒帯を許容）
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    try { isSurfaceReady = true } catch (e: Exception) { isSurfaceReady = true }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. プレビュー画像（解析前）動画を隠さないようにフェード対応等の工夫が必要ですが、
        // 今回はシンプルに動画プレーヤーの上にプレビューを被せるのはやめ、黒背景＋インジケーターのみにします。
        if (!isReadyToAnalyze) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "映像を準備中...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. 骨格描画（絶対座標・完全同期コード）
        if (posePoints.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (posePoints.isEmpty() || analyzedFrameWidth <= 0f) return@Canvas
                
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 映像のアスペクト比を、Stateから直接計算（メタデータは信用しない）
                val videoAspect = analyzedFrameWidth / analyzedFrameHeight
                val canvasAspect = canvasWidth / canvasHeight

                // FITモード（黒帯あり）の描画領域を算出
                val drawWidth: Float
                val drawHeight: Float
                if (canvasAspect > videoAspect) {
                    // 画面が横長すぎる（左右に黒帯）
                    drawHeight = canvasHeight
                    drawWidth = canvasHeight * videoAspect
                } else {
                    // 画面が縦長すぎる（上下に黒帯）
                    drawWidth = canvasWidth
                    drawHeight = canvasWidth / videoAspect
                }

                val offsetX = (canvasWidth - drawWidth) / 2f
                val offsetY = (canvasHeight - drawHeight) / 2f

                val scaledPoints = posePoints.map { point ->
                    androidx.compose.ui.geometry.Offset(
                        x = offsetX + (point.x * drawWidth),
                        y = offsetY + (point.y * drawHeight)
                    )
                }
                drawMediaPipeSkeleton(scaledPoints, analysisResult)
            }
        }

        // 4. 分析中プログレス
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF2196F3), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "AI解析中... ${(analysisProgress * 100).toInt()}%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. 中央の分析開始ボタン（解析完了前のみ表示）
        if (isReadyToAnalyze && !isAnalyzing && analysisResult == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { startAnalysis() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("分析開始", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 6. エラー表示
        errorMessage?.let { error ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Card(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "解析エラー", color = Color.Red, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("戻る") }
                    }
                }
            }
        }

        // 7. オーバーレイ：戻るボタン（左上）
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = Color.White)
        }

        // 8. オーバーレイ：スタンス検知バッジ（解析完了後）
        detectedStance?.let { stance ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detected: ${if (stance == "LEFT_HANDED") "Left-handed" else "Right-handed"} Stance",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 9. オーバーレイ：AIコーチボタン（右下・解析完了後のみ）
        if (analysisResult != null && !isAnalyzing) {
            FloatingActionButton(
                onClick = { showAnalysisDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(Icons.Default.Info, contentDescription = "分析詳細", tint = Color.White)
            }
        }

        // 9. 分析ダイアログオーバーレイ
        if (showAnalysisDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "スイング分析詳細",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // プランによる表示項目制御
                        if (isLitePlan) {
                            // Practice版：基礎3点のみ
                            Text(
                                text = " プレミアム機能",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "詳細なバイオメカニクス分析は\nアスリート版・プロ版でご利用いただけます",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 基礎3項目のみ表示
                            AnalysisItem("スイングスコア", "${analysisResult?.score ?: 0}点")
                            AnalysisItem("姿勢評価", getPostureEvaluation(analysisResult?.score ?: 0))
                            AnalysisItem("改善アドバイス", "プレミアム版で表示")
                        } else {
                            // アスリート版・プロ版：6項目すべて表示
                            // 単位系を取得
                            val unitSystem = userPreferences?.getUnitSystem() ?: UnitSystem.METRIC
                            val stance = detectedStance ?: "RIGHT_HANDED"
                            
                            // バイオメカニクスデータを単位変換（関数内で自動変換）
                            val headMovementValue = getHeadMovement(allPoses, userPreferences)
                            val weightShiftValue = getWeightShift(allPoses, userPreferences)
                            val shaftLeanValue = getShaftLean(posePoints)
                            
                            // スタンス連動型の符号変換を適用
                            val adjustedShaftLean = adjustForStance(shaftLeanValue, stance, isShaftLean = true)
                            val adjustedWeightShift = adjustForStance(weightShiftValue, stance, isShaftLean = false)
                            
                            val headMovementFormatted = "${String.format("%.1f", headMovementValue)} ${unitSystem.getLengthUnit()}"
                            val weightShiftFormatted = "${String.format("%.1f", adjustedWeightShift)} ${unitSystem.getLengthUnit()}"
                            
                            AnalysisItem("肩の回転角", "${getShoulderRotation(posePoints)}°")
                            AnalysisItem("腰の回転角", "${getHipRotation(posePoints)}°")
                            AnalysisItem("X-Factor", "${getXFactor(posePoints)}°")
                            AnalysisItem("頭の移動量", headMovementFormatted)
                            AnalysisItem("体重移動", weightShiftFormatted)
                            AnalysisItem("シャフトレイン", "${String.format("%.1f", adjustedShaftLean)}°")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TextButton(
                                onClick = { showAnalysisDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("閉じる", color = Color.Gray)
                            }
                            
                            if (!isLitePlan) {
                                Button(
                                    onClick = { 
                                        showAnalysisDialog = false
                                        onAICoachClick(analysisResult!!)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Text("AIコーチ", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MediaPipe（33点）専用のプロ仕様骨格描画ロジック（ゴルフ・ノイズフィルター付き）
private fun DrawScope.drawMediaPipeSkeleton(
    points: List<Offset>, 
    analysisResult: com.golftrajectory.app.SwingAnalysisResult?
) {
    val strokeWidth = 4.dp.toPx()
    val jointRadius = 6.dp.toPx()
    
    // 第1層【ポーズ信頼度チェック】
    // ※実際の信頼度はPoseDetectorから渡す必要があるため、ここではポイント数で簡易判定
    if (points.size < 33) {
        return // 信頼度不足 - 描画中止
    }
    
    // 第2層【ゴルフ・アドレス検知】
    if (!isGolfAddressPose(points)) {
        return // ゴルフ構えではない - 描画中止
    }
    
    // 描画する接続（MediaPipe準拠）
    // 11: 左肩, 12: 右肩, 13: 左肘, 14: 右肘, 15: 左手首, 16: 右手首
    // 23: 左腰, 24: 右腰, 25: 左膝, 26: 右膝, 27: 左足首, 28: 右足首
    val connections = listOf(
        // 上半身
        11 to 12, // 両肩
        11 to 23, 12 to 24, // 体幹（肩から腰）
        23 to 24, // 両腰
        // 腕
        11 to 13, 13 to 15, // 左腕
        12 to 14, 14 to 16, // 右腕
        // 脚
        23 to 25, 25 to 27, // 左脚
        24 to 26, 26 to 28  // 右脚
    )

    // 線の描画（解析中は白、完了後に評価色）
    connections.forEach { (start, end) ->
        if (start < points.size && end < points.size) {
            // 線の描画色の決定
            val lineColor = when {
                analysisResult == null -> androidx.compose.ui.graphics.Color.White // 解析前・解析中は白
                analysisResult.score >= 80 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // 緑（適正）
                analysisResult.score >= 60 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // 黄（注意）
                else -> androidx.compose.ui.graphics.Color(0xFFF44336) // 赤（不良）
            }
            
            drawLine(
                color = lineColor,
                start = points[start],
                end = points[end],
                strokeWidth = strokeWidth
            )
        }
    }

    // 関節点の描画（解析中は白、完了後に評価色）
    points.forEachIndexed { index, point ->
        if (index < points.size) {
            // 関節点の描画色の決定
            val jointColor = when {
                analysisResult == null -> androidx.compose.ui.graphics.Color.White // 解析前・解析中は白
                analysisResult.score >= 80 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // 緑
                analysisResult.score >= 60 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // 黄
                else -> androidx.compose.ui.graphics.Color(0xFFF44336) // 赤
            }
            
            val radius = when (index) {
                // 重要な関節（肩・腰など）：大きめ
                11, 12, 23, 24 -> jointRadius * 1.5f
                // その他の関節：通常サイズ
                else -> jointRadius
            }
            
            drawCircle(color = jointColor, radius = radius, center = point)
            
            // 重要関節の白い中心点
            if (index in listOf(11, 12, 23, 24)) {
                drawCircle(color = Color.White, radius = jointRadius * 0.5f, center = point)
            }
        }
    }
}

// 第2層【ゴルフ・アドレス検知】
private fun isGolfAddressPose(points: List<Offset>): Boolean {
    if (points.size < 33) return false
    
    try {
        // 1. グリップ状態チェック：左右の手首（15, 16）の距離が一定値以下
        val leftWrist = points[15]
        val rightWrist = points[16]
        val gripDistance = kotlin.math.sqrt(
            ((leftWrist.x - rightWrist.x) * (leftWrist.x - rightWrist.x) + 
            (leftWrist.y - rightWrist.y) * (leftWrist.y - rightWrist.y)).toDouble()
        ).toFloat()
        
        // グリップ距離が0.1（正規化座標）以下であること
        if (gripDistance > 0.15f) return false
        
        // 2. 前傾（Spine Angle）チェック：肩と腰を結ぶ線が垂直でないこと
        val leftShoulder = points[11]
        val rightShoulder = points[12]
        val leftHip = points[23]
        val rightHip = points[24]
        
        val shoulderCenter = Offset(
            (leftShoulder.x + rightShoulder.x) / 2f,
            (leftShoulder.y + rightShoulder.y) / 2f
        )
        val hipCenter = Offset(
            (leftHip.x + rightHip.x) / 2f,
            (leftHip.y + rightHip.y) / 2f
        )
        
        // 肩と腰の垂直方向の差分（前傾角度の指標）
        val spineAngle = abs(shoulderCenter.y - hipCenter.y)
        // 前傾があること（肩が腰より上にある）
        if (spineAngle < 0.05f) return false
        
        return true
    } catch (e: Exception) {
        return false
    }
}

// 第3層【アクション・バリデーション】
private fun isValidGolfSwing(allPoses: List<List<Offset>>): Boolean {
    if (allPoses.isEmpty()) return false
    
    try {
        var wristAboveShoulder = false
        
        for (pose in allPoses) {
            if (pose.size >= 33) {
                val leftWrist = pose[15]
                val rightWrist = pose[16]
                val leftShoulder = pose[11]
                val rightShoulder = pose[12]
                
                // いずれかの手首が肩の高さを超えたかチェック
                if (leftWrist.y < leftShoulder.y || rightWrist.y < rightShoulder.y) {
                    wristAboveShoulder = true
                    break
                }
            }
        }
        
        return wristAboveShoulder // 手首が肩を超えない場合はスイングではない
    } catch (e: Exception) {
        return false
    }
}

// 第4層【静止始動の確認】
private fun hasStaticStart(allPoses: List<List<Offset>>): Boolean {
    if (allPoses.size < 5) return false // 最初の5フレームをチェック
    
    try {
        val threshold = 0.02f // 移動速度閾値
        
        for (i in 1 until kotlin.math.min(5, allPoses.size)) {
            val prevPose = allPoses[i - 1]
            val currPose = allPoses[i]
            
            if (prevPose.size >= 33 && currPose.size >= 33) {
                // 主要関節の移動距離を計算
                var totalMovement = 0f
                val keyJoints = listOf(11, 12, 15, 16, 23, 24) // 肩、手首、腰
                
                for (jointIndex in keyJoints) {
                    val prevPoint = prevPose[jointIndex]
                    val currPoint = currPose[jointIndex]
                    val distance = kotlin.math.sqrt(
                        ((currPoint.x - prevPoint.x) * (currPoint.x - prevPoint.x) + 
                        (currPoint.y - prevPoint.y) * (currPoint.y - prevPoint.y)).toDouble()
                    ).toFloat()
                    totalMovement += distance
                }
                
                // 移動が閾値を超えた場合は静止状態ではない
                if (totalMovement > threshold) {
                    return false
                }
            }
        }
        
        return true // 最初の数フレームは静止状態だった
    } catch (e: Exception) {
        return false
    }
}

// 分析項目表示用コンポーザブル
@Composable
private fun AnalysisItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

// 姿勢評価を取得
private fun getPostureEvaluation(score: Int): String {
    return when {
        score >= 80 -> "優秀"
        score >= 60 -> "良好"
        score >= 40 -> "要改善"
        else -> "再確認"
    }
}

// 肩の回転角を計算
private fun getShoulderRotation(points: List<Offset>): Double {
    if (points.size < 33) return 0.0
    try {
        val leftShoulder = points[11]
        val rightShoulder = points[12]
        val shoulderAngle = kotlin.math.atan2(
            rightShoulder.y - leftShoulder.y,
            rightShoulder.x - leftShoulder.x
        ) * 180 / kotlin.math.PI
        return kotlin.math.abs(shoulderAngle)
    } catch (e: Exception) {
        return 0.0
    }
}

// 腰の回転角を計算
private fun getHipRotation(points: List<Offset>): Double {
    if (points.size < 33) return 0.0
    try {
        val leftHip = points[23]
        val rightHip = points[24]
        val hipAngle = kotlin.math.atan2(
            rightHip.y - leftHip.y,
            rightHip.x - leftHip.x
        ) * 180 / kotlin.math.PI
        return kotlin.math.abs(hipAngle)
    } catch (e: Exception) {
        return 0.0
    }
}

// X-Factor（肩角-腰角の差分）を計算
private fun getXFactor(points: List<Offset>): Double {
    val shoulderRotation = getShoulderRotation(points)
    val hipRotation = getHipRotation(points)
    return kotlin.math.abs(shoulderRotation - hipRotation)
}

// 頭の移動量を計算 - 単位系対応
private fun getHeadMovement(allPoses: List<List<Offset>>, userPreferences: com.golftrajectory.app.UserPreferences?): Double {
    if (allPoses.isEmpty() || allPoses.first().size < 33) return 0.0
    try {
        val firstPose = allPoses.first()
        val lastPose = allPoses.last()
        
        // MediaPipeの鼻のインデックスは0
        val firstNose = firstPose[0]
        val lastNose = lastPose[0]
        
        val movement = kotlin.math.sqrt(
            ((lastNose.x - firstNose.x) * (lastNose.x - firstNose.x) + 
            (lastNose.y - firstNose.y) * (lastNose.y - firstNose.y)).toDouble()
        )
        
        // 単位系に応じた変換係数
        val unitSystem = userPreferences?.getUnitSystem() ?: UnitSystem.METRIC
        val conversionFactor = when (unitSystem) {
            UnitSystem.METRIC -> 50.0      // cm変換係数
            UnitSystem.IMPERIAL -> 19.7   // inches変換係数 (50cm × 0.3937)
        }
        
        return movement * conversionFactor
    } catch (e: Exception) {
        return 0.0
    }
}

// 体重移動を計算 - 単位系対応
private fun getWeightShift(allPoses: List<List<Offset>>, userPreferences: com.golftrajectory.app.UserPreferences?): Double {
    if (allPoses.isEmpty() || allPoses.first().size < 33) return 0.0
    try {
        val firstPose = allPoses.first()
        val lastPose = allPoses.last()
        
        // 両足の中点を計算
        val firstLeftAnkle = firstPose[27]
        val firstRightAnkle = firstPose[28]
        val firstCenter = Offset(
            (firstLeftAnkle.x + firstRightAnkle.x) / 2f,
            (firstLeftAnkle.y + firstRightAnkle.y) / 2f
        )
        
        val lastLeftAnkle = lastPose[27]
        val lastRightAnkle = lastPose[28]
        val lastCenter = Offset(
            (lastLeftAnkle.x + lastRightAnkle.x) / 2f,
            (lastLeftAnkle.y + lastRightAnkle.y) / 2f
        )
        
        val shift = kotlin.math.sqrt(
            ((lastCenter.x - firstCenter.x) * (lastCenter.x - firstCenter.x) + 
            (lastCenter.y - firstCenter.y) * (lastCenter.y - firstCenter.y)).toDouble()
        )
        
        // 単位系に応じた変換係数
        val unitSystem = userPreferences?.getUnitSystem() ?: UnitSystem.METRIC
        val conversionFactor = when (unitSystem) {
            UnitSystem.METRIC -> 50.0      // cm変換係数
            UnitSystem.IMPERIAL -> 19.7   // inches変換係数 (50cm × 0.3937)
        }
        
        return shift * conversionFactor
    } catch (e: Exception) {
        return 0.0
    }
}

// 自動スタンス検知エンジン
private fun detectStance(allPoses: List<List<Offset>>): String {
    if (allPoses.isEmpty()) return "RIGHT_HANDED" // デフォルト
    
    try {
        // 最初の5フレームを分析してノイズを除去
        val framesToAnalyze = allPoses.take(5)
        var rightHandedCount = 0
        var leftHandedCount = 0
        
        for (pose in framesToAnalyze) {
            if (pose.size >= 33) {
                val leftShoulder = pose[11]
                val rightShoulder = pose[12]
                
                // 肩の中点を計算
                val shoulderCenter = Offset(
                    (leftShoulder.x + rightShoulder.x) / 2f,
                    (leftShoulder.y + rightShoulder.y) / 2f
                )
                
                // 左肩と右肩のX座標を比較
                // 左肩の方がX座標が大きい（ターゲットに近い）→ 右打ち
                // 右肩の方がX座標が大きい（ターゲットに近い）→ 左打ち
                if (leftShoulder.x > rightShoulder.x) {
                    rightHandedCount++
                } else {
                    leftHandedCount++
                }
            }
        }
        
        // 多数決で判定
        return if (rightHandedCount > leftHandedCount) "RIGHT_HANDED" else "LEFT_HANDED"
    } catch (e: Exception) {
        return "RIGHT_HANDED" // エラー時はデフォルト
    }
}

// スタンス連動型の符号変換
private fun adjustForStance(value: Double, stance: String, isShaftLean: Boolean = false): Double {
    return if (stance == "LEFT_HANDED") {
        if (isShaftLean) {
            // Shaft Lean: 左打ちは符号を反転
            -value
        } else {
            // Weight Shift: 左打ちは軸を反転（ターゲット方向を正とする）
            -value
        }
    } else {
        value // 右打ちはそのまま
    }
}
private fun getShaftLean(points: List<Offset>): Double {
    if (points.size < 33) return 0.0
    try {
        // MediaPipeのキーポイントインデックス
        // 0: 鼻, 11: 左肩, 12: 右肩, 15: 左手首, 16: 右手首, 23: 左腰, 24: 右腰
        
        val leftWrist = points[15]
        val rightWrist = points[16]
        val leftShoulder = points[11]
        val rightShoulder = points[12]
        
        // 手首の中点（ハンドル位置）
        val wristCenter = Offset(
            (leftWrist.x + rightWrist.x) / 2f,
            (leftWrist.y + rightWrist.y) / 2f
        )
        
        // 肩の中点（身体の中心軸）
        val shoulderCenter = Offset(
            (leftShoulder.x + rightShoulder.x) / 2f,
            (leftShoulder.y + rightShoulder.y) / 2f
        )
        
        // 身体比率に基づいた正規化
        // 肩幅を基準単位として使用（身長やカメラ距離に依存しない）
        val shoulderWidth = kotlin.math.sqrt(
            ((rightShoulder.x - leftShoulder.x) * (rightShoulder.x - leftShoulder.x) + 
            (rightShoulder.y - leftShoulder.y) * (rightShoulder.y - leftShoulder.y)).toDouble()
        ).toFloat()
        
        if (shoulderWidth < 0.01f) return 0.0 // 肩幅が小さすぎる場合は無効
        
        // 正規化されたベクトルを計算
        // 垂直方向の基準線（身体の中心軸）に対する手首の位置
        val horizontalOffset = wristCenter.x - shoulderCenter.x
        val verticalOffset = shoulderCenter.y - wristCenter.y  // 上向きを正とする
        
        // atan2で角度を計算（垂直線を0°とする）
        // abs()を排除し、符号を保持して方向性を正確に反映
        val shaftLeanAngle = kotlin.math.atan2(
            horizontalOffset.toDouble(), // 水平方向の変位（符号付き）
            kotlin.math.abs(verticalOffset.toDouble())   // 垂直方向の変位（絶対値）
        ) * 180 / kotlin.math.PI
        
        // 身体比率で正規化（肩幅に対する比率）
        val normalizedAngle = shaftLeanAngle * (shoulderWidth / 0.2f) // 0.2は標準的な肩幅の正規化値
        
        return normalizedAngle // 符号付きの角度を返す
    } catch (e: Exception) {
        return 0.0
    }
}