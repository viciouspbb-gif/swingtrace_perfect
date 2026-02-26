package com.golftrajectory.app

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onVideoSaved: (Uri, List<Offset>) -> Unit, // 軌跡データも渡す
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscapeOrientation = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val userPreferences = remember { UserPreferences(context) }
    val isRightHanded = userPreferences.isRightHanded()
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    )
    
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }
    
    if (permissionsState.allPermissionsGranted) {
        CameraContent(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onVideoSaved = onVideoSaved,
            onBack = onBack,
            isRightHanded = isRightHanded,
            isLandscape = isLandscapeOrientation
        )
    } else {
        PermissionRequestScreen(
            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
            onBack = onBack
        )
    }
}

@Composable
fun ShootingGuide(isRightHanded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 70.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = if (isRightHanded) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRightHanded) {
                    // 右打ち: ゴルファーは左側
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "📐 撮影ガイド",
                            color = Color(0xFF00FF00),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• ゴルファーの真後ろから",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• 距離: 2〜3m離れる",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• 高さ: 腰の高さ",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "⚠️ カメラを完全に固定",
                            color = Color(0xFFFFAA00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "キャディバッグ、椅子等",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                    
                    // ゴルファーのイラスト（右打ち）
                    Text(
                        text = "🏌️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                } else {
                    // 左打ち: ゴルファーは右側
                    Text(
                        text = "🏌️",
                        fontSize = 48.sp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .graphicsLayer(scaleX = -1f) // 左右反転
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "📐 撮影ガイド",
                            color = Color(0xFF00FF00),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• ゴルファーの真後ろから",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• 距離: 2〜3m離れる",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• 高さ: 腰の高さ",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "⚠️ カメラを完全に固定",
                            color = Color(0xFFFFAA00),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "キャディバッグ、椅子等",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StabilityWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(text = "⚠️", fontSize = 48.sp)
        },
        title = {
            Text(
                text = "カメラの固定について",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "手持ちで撮影すると、解析の精度が大きく落ちる可能性があります。",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "推奨: キャディバッグのポケット、椅子、カートなど身近なものでカメラを完全に固定してください。",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("理解して続行")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "カメラと音声の権限が必要です",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "動画撮影にはカメラとマイクへのアクセスが必要です",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onRequestPermission) {
            Text("権限を許可")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("戻る")
        }
    }
}

@Composable
fun CameraContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onVideoSaved: (Uri, List<Offset>) -> Unit,
    onBack: () -> Unit,
    isRightHanded: Boolean = true,
    isLandscape: Boolean = true
) {
    var isRecording by remember { mutableStateOf(false) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var autoDetectEnabled by remember { mutableStateOf(true) }
    var ballLocked by remember { mutableStateOf(false) }
    var ballPosition by remember { mutableStateOf<Offset?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var showStabilityWarning by remember { mutableStateOf(false) }
    var recorderLimitJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    // 自動検出された弾道ポイント（サーバーAI診断用の重要データ）
    val detectedPoints = remember { mutableStateListOf<Offset>() }
    
    // ML Kit検出器
    val ballDetector = remember { MLKitBallDetector() }
    
    // 追跡開始時の初期化
    LaunchedEffect(ballLocked, ballPosition) {
        if (ballLocked && ballPosition != null && detectedPoints.isEmpty()) {
            // 初期位置を起点として追跡を開始
            detectedPoints.clear()
            detectedPoints.add(ballPosition!!)
        }
    }
    
    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            ballDetector.close()
        }
    }
    
    // 手ブレ警告ダイアログ
    if (showStabilityWarning) {
        StabilityWarningDialog(
            onConfirm = {
                showStabilityWarning = false
                // 録画を開始する処理は後で実装
            },
            onDismiss = {
                showStabilityWarning = false
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // カメラプレビュー
        AndroidView(
            factory = { ctx ->
                val preview = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val cameraPreview = Preview.Builder().build().also {
                        it.setSurfaceProvider(preview.surfaceProvider)
                    }
                    
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    
                    videoCapture = VideoCapture.withOutput(recorder)
                    
                    // ImageAnalysisの設定（バックグラウンドスレッドで実行）
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                // 録画中かつボールがロックオンされている場合のみ解析
                                if (isRecording && ballLocked && ballPosition != null) {
                                    try {
                                        // ImageProxyをBitmapに変換
                                        val bitmap = imageProxy.toBitmap()
                                        val timestamp = System.currentTimeMillis()
                                        
                                        // ボール検出（色ベース検出を使用）
                                        val result = ballDetector.detectWhiteBall(bitmap, timestamp)
                                        
                                        result?.let { detection ->
                                            // 検出された正確な座標を取得
                                            // 左打ちの場合はX座標を反転
                                            val rawX = detection.position.x
                                            val adjustedX = if (isRightHanded) rawX else (bitmap.width - rawX)
                                            
                                            val newPosition = Offset(
                                                x = adjustedX,
                                                y = detection.position.y
                                            )
                                            
                                            // ノイズ除去：前回の位置から妥当な範囲内の場合のみ追加
                                            val lastPoint = detectedPoints.lastOrNull()
                                            val isValidPosition = if (lastPoint != null) {
                                                val distance = kotlin.math.sqrt(
                                                    (newPosition.x - lastPoint.x) * (newPosition.x - lastPoint.x) +
                                                    (newPosition.y - lastPoint.y) * (newPosition.y - lastPoint.y)
                                                )
                                                // 300px以内の移動のみ有効とする
                                                distance < 300f
                                            } else {
                                                true
                                            }
                                            
                                            if (isValidPosition) {
                                                // サーバーAI診断用の重要データとして追加
                                                detectedPoints.add(newPosition)
                                                // 現在の追跡位置を更新
                                                ballPosition = newPosition
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                imageProxy.close()
                            }
                        }
                    
                    imageAnalysis = imageAnalyzer
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            cameraPreview,
                            videoCapture,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                preview
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // トップバー
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.ArrowBack, "戻る")
            }
        }
        
        // 撮影ガイド（録画前のみ表示）
        if (!isRecording && !ballLocked) {
            ShootingGuide(isRightHanded = isRightHanded)
        }
        
        // ボール照準ガイド（中央下部）または追跡表示
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (!isRecording) {
                            // タップでボールをロックオン
                            ballPosition = offset
                            ballLocked = true
                        }
                    }
                }
        ) {
            if (!isRecording && !ballLocked) {
                // 録画前：照準ガイド表示
                val centerX = size.width / 2
                val targetY = size.height * 0.65f
                
                // 外側の円（半透明）
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 60f,
                    center = Offset(centerX, targetY),
                    style = Stroke(width = 3f)
                )
                
                // 中間の円
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 40f,
                    center = Offset(centerX, targetY),
                    style = Stroke(width = 2f)
                )
                
                // 内側の円（ボール位置）
                drawCircle(
                    color = Color(0xFF00FF00).copy(alpha = 0.6f),
                    radius = 20f,
                    center = Offset(centerX, targetY),
                    style = Stroke(width = 3f)
                )
                
                // 中心点
                drawCircle(
                    color = Color(0xFF00FF00),
                    radius = 4f,
                    center = Offset(centerX, targetY)
                )
                
                // 十字線
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(centerX - 80f, targetY),
                    end = Offset(centerX + 80f, targetY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(centerX, targetY - 80f),
                    end = Offset(centerX, targetY + 80f),
                    strokeWidth = 2f
                )
            } else if (ballLocked && ballPosition != null) {
                // ボールロックオン表示
                val pos = ballPosition!!
                
                // ロックオンリング（パルスアニメーション風）
                drawCircle(
                    color = Color(0xFF00FF00).copy(alpha = 0.8f),
                    radius = 35f,
                    center = pos,
                    style = Stroke(width = 4f)
                )
                
                drawCircle(
                    color = Color(0xFFFFFF00).copy(alpha = 0.6f),
                    radius = 25f,
                    center = pos,
                    style = Stroke(width = 3f)
                )
                
                // 中心マーカー
                drawCircle(
                    color = Color(0xFF00FF00),
                    radius = 6f,
                    center = pos
                )
                
                // コーナーマーカー（ロックオン感を演出）
                val markerSize = 15f
                val markerOffset = 40f
                
                // 左上
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x - markerOffset, pos.y - markerOffset),
                    end = Offset(pos.x - markerOffset + markerSize, pos.y - markerOffset),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x - markerOffset, pos.y - markerOffset),
                    end = Offset(pos.x - markerOffset, pos.y - markerOffset + markerSize),
                    strokeWidth = 3f
                )
                
                // 右上
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x + markerOffset, pos.y - markerOffset),
                    end = Offset(pos.x + markerOffset - markerSize, pos.y - markerOffset),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x + markerOffset, pos.y - markerOffset),
                    end = Offset(pos.x + markerOffset, pos.y - markerOffset + markerSize),
                    strokeWidth = 3f
                )
                
                // 左下
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x - markerOffset, pos.y + markerOffset),
                    end = Offset(pos.x - markerOffset + markerSize, pos.y + markerOffset),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x - markerOffset, pos.y + markerOffset),
                    end = Offset(pos.x - markerOffset, pos.y + markerOffset - markerSize),
                    strokeWidth = 3f
                )
                
                // 右下
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x + markerOffset, pos.y + markerOffset),
                    end = Offset(pos.x + markerOffset - markerSize, pos.y + markerOffset),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF00FF00),
                    start = Offset(pos.x + markerOffset, pos.y + markerOffset),
                    end = Offset(pos.x + markerOffset, pos.y + markerOffset - markerSize),
                    strokeWidth = 3f
                )
                
                if (isRecording && detectedPoints.size > 1) {
                    // リアルタイム軌跡描画（黄色の線で楽しさを実現）
                    detectedPoints.forEachIndexed { index, point ->
                        if (index > 0) {
                            val prevPoint = detectedPoints[index - 1]
                            
                            // グラデーション効果（新しい軌跡ほど明るく）
                            val progress = index.toFloat() / detectedPoints.size
                            val alpha = 0.6f + (progress * 0.4f) // 0.6 -> 1.0
                            
                            // 黄色の軌跡線を描画
                            drawLine(
                                color = Color(0xFFFFFF00).copy(alpha = alpha),
                                start = prevPoint,
                                end = point,
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    // 軌跡上のポイントマーカー（5個おきに表示）
                    detectedPoints.forEachIndexed { index, point ->
                        if (index % 5 == 0) {
                            // 外側の光輪
                            drawCircle(
                                color = Color(0xFFFFFF00).copy(alpha = 0.5f),
                                radius = 8f,
                                center = point
                            )
                            // 内側のマーカー
                            drawCircle(
                                color = Color(0xFFFFFF00),
                                radius = 4f,
                                center = point
                            )
                        }
                    }
                }
            }
        }
        
        if (!isLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "スイング解析には横向き撮影が必須です",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ガイドテキスト
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (!ballLocked) {
                            "⛳ ボールをタップしてロックオン"
                        } else {
                            "🎯 ロックオン完了！録画ボタンを押してください"
                        },
                        color = if (ballLocked) Color(0xFF00FF00) else Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (ballLocked) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        } else if (ballLocked) {
            // 録画中のステータス表示
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 80.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "🎯 ボール追跡中...",
                        color = Color(0xFF00FF00),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 録画ボタン
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecording) {
                Text(
                    text = "● 録画中",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            FloatingActionButton(
                onClick = {
                    if (!isLandscape) {
                        Toast.makeText(
                            context,
                            "端末を横向きにしてから録画してください",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@FloatingActionButton
                    }
                    if (isRecording) {
                        // 録画停止
                        recorderLimitJob?.cancel()
                        recorderLimitJob = null
                        recording?.stop()
                        recording = null
                        isRecording = false
                        // ロックオンをリセット
                        ballLocked = false
                        ballPosition = null
                        detectedPoints.clear()
                    } else {
                        // 録画開始
                        val name = "SwingTrace_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SwingTrace")
                            }
                        }
                        
                        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                            context.contentResolver,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        )
                            .setContentValues(contentValues)
                            .build()
                        
                        recording = videoCapture?.output
                            ?.prepareRecording(context, mediaStoreOutput)
                            ?.withAudioEnabled()
                            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        recorderLimitJob?.cancel()
                                        recorderLimitJob = null
                                        if (event.hasError()) {
                                            recording?.close()
                                            recording = null
                                            Toast.makeText(
                                                context,
                                                "録画エラー: ${event.cause?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val savedUri = event.outputResults.outputUri
                                            Toast.makeText(
                                                context,
                                                "動画を保存しました（${detectedPoints.size}点の軌跡データ）",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // 軌跡データと一緒に渡す
                                            onVideoSaved(savedUri, detectedPoints.toList())
                                        }
                                    }
                                }
                            }
                        isRecording = true
                        if (AppConfig.isPractice()) {
                            recorderLimitJob?.cancel()
                            recorderLimitJob = scope.launch {
                                delay(15_000)
                                if (isRecording) {
                                    recording?.stop()
                                    recording = null
                                    isRecording = false
                                    ballLocked = false
                                    ballPosition = null
                                    detectedPoints.clear()
                                    Toast.makeText(
                                        context,
                                        "LITE版の制限です",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                recorderLimitJob = null
                            }
                        } else {
                            recorderLimitJob?.cancel()
                            recorderLimitJob = null
                        }
                    }
                },
                shape = CircleShape,
                containerColor = when {
                    !isLandscape -> Color.Gray
                    isRecording -> Color.Red
                    else -> Color.White
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "停止" else "録画",
                    tint = when {
                        !isLandscape -> Color.DarkGray
                        isRecording -> Color.White
                        else -> Color.Red
                    },
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun TrajectoryOverlay(
    trajectoryResult: TrajectoryEngine.TrajectoryResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val points = trajectoryResult.points
        if (points.isEmpty()) return@Canvas
        
        val maxDistance = trajectoryResult.totalDistance.coerceAtLeast(10.0)
        val maxHeight = trajectoryResult.maxHeight.coerceAtLeast(5.0)
        
        // 画面の下半分に描画
        val drawHeight = size.height * 0.4f
        val drawWidth = size.width * 0.9f
        val offsetX = size.width * 0.05f
        val offsetY = size.height * 0.5f
        
        val scaleX = drawWidth / maxDistance.toFloat()
        val scaleY = drawHeight / maxHeight.toFloat()
        
        // グラデーション色で弾道を描画
        val colors = listOf(
            Color(0xFF00FF00), // 緑（開始）
            Color(0xFFFFFF00), // 黄
            Color(0xFFFF9800), // オレンジ
            Color(0xFFFF5722)  // 赤（終了）
        )
        
        points.forEachIndexed { index, point ->
            if (index > 0) {
                val prevPoint = points[index - 1]
                
                val x1 = offsetX + prevPoint.x.toFloat() * scaleX
                val y1 = offsetY + drawHeight - prevPoint.y.toFloat() * scaleY
                val x2 = offsetX + point.x.toFloat() * scaleX
                val y2 = offsetY + drawHeight - point.y.toFloat() * scaleY
                
                // 進行度に応じて色を変更
                val progress = index.toFloat() / points.size
                val colorIndex = (progress * (colors.size - 1)).toInt().coerceIn(0, colors.size - 2)
                val colorProgress = (progress * (colors.size - 1)) - colorIndex
                
                val color = androidx.compose.ui.graphics.lerp(
                    colors[colorIndex],
                    colors[colorIndex + 1],
                    colorProgress
                )
                
                drawLine(
                    color = color.copy(alpha = 0.9f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // ボールの軌跡ポイント（白い点）
        points.forEachIndexed { index, point ->
            if (index % 10 == 0) {
                val x = offsetX + point.x.toFloat() * scaleX
                val y = offsetY + drawHeight - point.y.toFloat() * scaleY
                
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
