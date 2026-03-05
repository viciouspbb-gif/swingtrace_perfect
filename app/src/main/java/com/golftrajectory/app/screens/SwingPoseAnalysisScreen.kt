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
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast

/**
 * スイング姿勢分析画面（MediaPipe使用）- プロ仕様フルスクリーン版
 */
@Composable
fun SwingPoseAnalysisScreen(
    videoUri: Uri,
    analysisMode: String = "rear",
    planTier: Plan,
    autoStart: Boolean = false,
    onBack: () -> Unit,
    onAICoachClick: (com.golftrajectory.app.SwingAnalysisResult) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)

    // 物理的な横固定（UI描画前に強制執行）
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        delay(100)
    }

    val usageManager = remember { UsageManager(context) }
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { originalOrientation?.let { activity?.requestedOrientation = it } }
    }

    // States
    var isOrientationLocked by remember { mutableStateOf(false) }
    var isVideoLoaded by remember { mutableStateOf(false) }
    var isSurfaceReady by remember { mutableStateOf(false) }
    var videoRotation by remember { mutableStateOf(0) }
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var letterboxRatio by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        delay(100)
        isOrientationLocked = true
    }

    // 動画読み込みとバリデーション
    LaunchedEffect(videoUri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            videoRotation = rotation

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080

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

    val isReadyToAnalyze = isOrientationLocked && isVideoLoaded && isSurfaceReady
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
        }
    }

    LaunchedEffect(poseDetector) {
        poseDetector.poseResultFlow.collect { resultPair ->
            biomechanicsData = resultPair.second
        }
    }

    fun transformCoordinatesForLetterbox(points: List<Offset>, letterboxRatio: Float): List<Offset> {
        if (letterboxRatio == 1.0f) return points
        val letterboxWidth = (1.0f - letterboxRatio) / 2.0f
        return points.map { point ->
            val normalizedX = (point.x - letterboxWidth) / letterboxRatio
            Offset(x = normalizedX.coerceIn(0f, 1f), y = point.y)
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
                            val frame = retriever.getFrameAtTime(currentTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            frame?.let { bitmap ->
                                try {
                                    // 回転角を補正して正規化
                                    val rotatedBitmap = rotateBitmap(bitmap, videoRotation)
                                    
                                    val result = poseDetector.detectPose(rotatedBitmap)
                                    result?.let { poseResult ->
                                        if (poseResult.landmarks().isNotEmpty()) {
                                            val landmarks = poseResult.landmarks()[0]
                                            val points = landmarks.map { Offset(it.x(), it.y()) }
                                            detectedPoses.add(transformCoordinatesForLetterbox(points, letterboxRatio))
                                        }
                                    }
                                    
                                    // メモリ解放
                                    if (rotatedBitmap != bitmap) {
                                        rotatedBitmap.recycle()
                                    } else {
                                        // 何もしない
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
                        allPoses = detectedPoses
                        posePoints = detectedPoses.first()
                        analysisResult = swingAnalyzer.analyze(detectedPoses)
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
        // 1. 全画面動画プレーヤー
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // 強制的にフルスクリーン（ズームして枠を消す）
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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

        // 3. 骨格描画（信号機カラー付きプロ仕様スケルトン）
        if (posePoints.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width
                val scaleY = size.height
                val correctedPoints = transformCoordinatesForLetterbox(posePoints, letterboxRatio)
                val scaledPoints = correctedPoints.map { Offset(it.x * scaleX, it.y * scaleY) }

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

        // 8. オーバーレイ：AIコーチボタン（右下・解析完了後のみ）
        if (analysisResult != null && !isAnalyzing) {
            FloatingActionButton(
                onClick = { onAICoachClick(analysisResult!!) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = Color(0xFF2196F3)
            ) {
                Icon(Icons.Default.Info, contentDescription = "AIコーチ", tint = Color.White)
            }
        }
    }
}

// MediaPipe（33点）専用のプロ仕様骨格描画ロジック（信号機カラー対応）
private fun DrawScope.drawMediaPipeSkeleton(
    points: List<Offset>, 
    analysisResult: com.golftrajectory.app.SwingAnalysisResult?
) {
    val strokeWidth = 6.dp.toPx()
    val jointRadius = 4.dp.toPx()
    
    // 信号機カラー判定関数
    fun getTrafficLightColor(score: Float): Color {
        return when {
            score >= 70f -> Color(0xFF4CAF50) // 緑：適正
            score >= 40f -> Color(0xFFFFC107) // 黄：要注意
            else -> Color(0xFFF44336) // 赤：不良
        }
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

    // 線の描画（信号機カラー対応）
    connections.forEach { (start, end) ->
        if (start < points.size && end < points.size) {
            val lineColor = when {
                // 肩ライン
                (start == 11 && end == 12) -> analysisResult?.let { getTrafficLightColor(it.shoulderRotation) } ?: Color(0xCCFFFFFF)
                // 腰ライン
                (start == 23 && end == 24) -> analysisResult?.let { getTrafficLightColor(it.hipRotation) } ?: Color(0xCCFFFFFF)
                // その他のライン
                else -> Color(0xCCFFFFFF)
            }
            
            drawLine(
                color = lineColor,
                start = points[start],
                end = points[end],
                strokeWidth = strokeWidth
            )
        }
    }

    // 関節点の描画（信号機カラー対応）
    points.forEachIndexed { index, point ->
        if (index < points.size) {
            val (jointColor, radius) = when (index) {
                // 肩：shoulderRotationスコア
                11, 12 -> {
                    val color = analysisResult?.let { getTrafficLightColor(it.shoulderRotation) } ?: Color(0xCCFFFFFF)
                    color to jointRadius * 1.5f
                }
                // 腰：hipRotationスコア
                23, 24 -> {
                    val color = analysisResult?.let { getTrafficLightColor(it.hipRotation) } ?: Color(0xCCFFFFFF)
                    color to jointRadius * 1.5f
                }
                // 膝：weightTransferスコア
                25, 26 -> {
                    val color = analysisResult?.let { getTrafficLightColor(it.weightTransfer) } ?: Color(0xCCFFFFFF)
                    color to jointRadius
                }
                // その他の関節
                else -> Color(0xCCFFFFFF) to jointRadius
            }
            
            drawCircle(color = jointColor, radius = radius, center = point)
            
            // 重要関節の白い中心点
            if (index in listOf(11, 12, 23, 24)) {
                drawCircle(color = Color.White, radius = jointRadius * 0.5f, center = point)
            }
        }
    }
}