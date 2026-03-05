package com.swingtrace.aicoaching.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.swingtrace.aicoaching.repository.SwingAnalysisRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 強化されたカメラ画面
 * - ウォーターマーク表示
 * - 自動アップロード
 * - 動画保存・共有
 */
@Composable
fun EnhancedCameraScreen(
    onVideoRecorded: (Uri) -> Unit,
    onAutoAnalysisStart: (Uri) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // 動的オリエンテーション・ロック（開いた時だけ横画面、閉じたら元に戻す）
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }
    
    // 現在の向きを検出
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    var isRecording by remember { mutableStateOf(false) }
    var recordingState by remember { mutableStateOf<Recording?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recordedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var recordingTime by remember { mutableStateOf(0) }
    
    // 録画時間カウンター（タイマー制限なし）
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTime++
            }
        }
    }
    
    // カメラプレビュー
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        setupCamera(cameraProvider, previewView, lifecycleOwner) { capture ->
            videoCapture = capture
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // カメラプレビュー
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // ウォーターマーク（左下）
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "SwingTrace",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "with AI Coaching",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        
        // 横向きガイド（非横向きの場合）
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "横にしてください",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "スイング分析は横向きで撮影してください。\nスマホを横にしてから録画を開始できます。",
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            
            // 録画中インジケーター
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color.Red.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "録画中 ${recordingTime}秒",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // コントロールボタン
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 録画ボタン（横向きの場合のみ有効）
            FloatingActionButton(
                onClick = {
                    if (!isLandscape) return@FloatingActionButton // 横向きでなければ無効
                    
                    if (isRecording) {
                        // 録画停止
                        recordingState?.stop()
                        isRecording = false
                    } else {
                        // 録画開始
                        startRecording(
                            context = context,
                            videoCapture = videoCapture,
                            onRecordingStarted = { recording ->
                                recordingState = recording
                                isRecording = true
                            },
                            onVideoSaved = { uri ->
                                recordedVideoUri = uri
                                // 自動保存＆即時解析開始
                                onVideoRecorded(uri)
                                // 録画完了後、自動的に解析画面へ遷移して解析開始
                                onAutoAnalysisStart(uri)
                            }
                        )
                    }
                },
                modifier = Modifier.size(80.dp),
                containerColor = if (!isLandscape) Color.Gray else if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "停止" else "録画",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
        
        // 戻るボタン
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "戻る",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
    
}

private fun setupCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit
) {
    val preview = androidx.camera.core.Preview.Builder().build()
    preview.setSurfaceProvider(previewView.surfaceProvider)
    
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    
    val videoCapture = VideoCapture.withOutput(recorder)
    
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        videoCapture
    )
    
    onVideoCaptureReady(videoCapture)
}

private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    onRecordingStarted: (Recording) -> Unit,
    onVideoSaved: (Uri) -> Unit
) {
    if (videoCapture == null) return
    
    val name = "SwingTrace_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, name)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SwingTrace")
    }
    
    val mediaStoreOutput = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    )
        .setContentValues(contentValues)
        .build()
    
    val recording = videoCapture.output
        .prepareRecording(context, mediaStoreOutput)
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        Toast.makeText(context, "録画エラー: ${event.error}", Toast.LENGTH_SHORT).show()
                    } else {
                        onVideoSaved(event.outputResults.outputUri)
                    }
                }
            }
        }
    
    onRecordingStarted(recording)
}

private fun shareVideo(context: Context, videoUri: Uri) {
    val shareIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_STREAM, videoUri)
        type = "video/mp4"
        putExtra(android.content.Intent.EXTRA_TEXT, "SwingTrace with AI Coaching で撮影\n#SwingTrace #ゴルフ #AI分析")
    }
    context.startActivity(android.content.Intent.createChooser(shareIntent, "動画を共有"))
}
