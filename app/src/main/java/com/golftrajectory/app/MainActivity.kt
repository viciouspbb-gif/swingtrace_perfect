package com.swingtrace.swingiq

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.golftrajectory.app.SwingTraceSimulator
import com.golftrajectory.app.CameraScreen
import com.golftrajectory.app.TrajectoryEngine
import com.golftrajectory.app.UserPreferences

class MainActivity : ComponentActivity() {
    
    private var selectedVideoUri by mutableStateOf<Uri?>(null)
    private var navigateToAnalysis by mutableStateOf(false)
    private var trajectoryData by mutableStateOf<List<Offset>>(emptyList())
    private lateinit var userPreferences: UserPreferences
    private var showHandPreferenceDialog by mutableStateOf(false)
    
    // 動画選択用のランチャー（永続的なURI権限を取得）
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            handleSelectedVideo(it)
        }
    }
    
    // カメラ録画用のランチャー（将来的にCameraXで実装）
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "カメラとマイクの権限が許可されました", Toast.LENGTH_SHORT).show()
            // TODO: CameraXでカメラを起動
            launchCamera()
        } else {
            Toast.makeText(this, "カメラまたはマイクの権限が拒否されました", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ユーザー設定の初期化
        userPreferences = UserPreferences(this)
        
        // 初回起動時に利き手選択ダイアログを表示
        if (userPreferences.isFirstLaunch()) {
            showHandPreferenceDialog = true
        }
        
        setContent {
            GolfTrajectoryTheme {
                val navController = rememberNavController()
                
                // ナビゲーショントリガー
                LaunchedEffect(navigateToAnalysis) {
                    if (navigateToAnalysis && selectedVideoUri != null) {
                        navController.navigate("analyze")
                        navigateToAnalysis = false
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 利き手選択ダイアログ
                    if (showHandPreferenceDialog) {
                        HandPreferenceDialog(
                            onHandSelected = { hand ->
                                userPreferences.setHandPreference(hand)
                                userPreferences.setFirstLaunchComplete()
                                showHandPreferenceDialog = false
                            }
                        )
                    }
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            MainScreen(
                                onRecordClick = { 
                                    // CameraX録画画面へ遷移
                                    navController.navigate("record")
                                },
                                onUploadClick = { openVideoPicker() },
                                onSampleClick = {
                                    // サンプル動画を使用
                                    Toast.makeText(this@MainActivity, "サンプル動画機能は準備中です。\n自分の動画をアップロードしてください。", Toast.LENGTH_LONG).show()
                                },
                                selectedVideoUri = selectedVideoUri,
                                onAnalyzeClick = {
                                    navigateToAnalysis = true
                                },
                                onSimulatorClick = {
                                    navController.navigate("simulator")
                                },
                                onHandPreferenceClick = {
                                    showHandPreferenceDialog = true
                                },
                                userPreferences = userPreferences
                            )
                        }
                        
                        composable("record") {
                            // 録画画面に入る時に横向きに変更
                            DisposableEffect(Unit) {
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                onDispose {
                                    // 録画画面から出る時に縦向きに戻す
                                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }
                            
                            CameraScreen(
                                onVideoSaved = { uri, detectedPoints ->
                                    selectedVideoUri = uri
                                    trajectoryData = detectedPoints
                                    navigateToAnalysis = true
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                },
                                onBack = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                    }
                                }
                            )
                        }
                        
                        composable("analyze") {
                            selectedVideoUri?.let { uri ->
                                AnalyzeScreen(
                                    videoUri = uri,
                                    initialTrajectoryData = trajectoryData,
                                    onBack = {
                                        selectedVideoUri = null
                                        trajectoryData = emptyList()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        
                        composable("simulator") {
                            SwingTraceSimulator(
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun requestCameraPermission() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        cameraPermissionLauncher.launch(permissions)
    }
    
    private fun launchCamera() {
        // TODO: CameraXを使用してカメラを起動
        Toast.makeText(this, "カメラ機能は準備中です", Toast.LENGTH_SHORT).show()
    }
    
    private fun openVideoPicker() {
        videoPickerLauncher.launch(arrayOf("video/*"))
    }
    
    private fun handleSelectedVideo(uri: Uri) {
        try {
            // 永続的なURI権限を取得
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            // URIを保存
            selectedVideoUri = uri
            
            Toast.makeText(this, "動画を選択しました", Toast.LENGTH_SHORT).show()
            
            // 2タップ解析画面へ自動遷移
            navigateToAnalysis = true
        } catch (e: Exception) {
            Log.e("VideoPicker", "Error handling video URI", e)
            Toast.makeText(this, "動画の読み込みに失敗しました", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun GolfTrajectoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2196F3),
            secondary = Color(0xFF03A9F4),
            tertiary = Color(0xFF4CAF50),
            background = Color(0xFFF5F5F5),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121)
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    onRecordClick: () -> Unit,
    onUploadClick: () -> Unit,
    onSampleClick: () -> Unit = {},
    selectedVideoUri: Uri?,
    onAnalyzeClick: () -> Unit = {},
    onSimulatorClick: () -> Unit = {},
    onHandPreferenceClick: () -> Unit = {},
    userPreferences: UserPreferences? = null
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ヘッダー
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SwingTrace with SwingIQ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "2タップ方式で弾道解析",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Icon(
                        Icons.Default.SportsTennis,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // 利き手選択ボタン
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onHandPreferenceClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    val handText = if (userPreferences?.isRightHanded() == true) {
                        "🏌️ 右打ち"
                    } else {
                        "🏌️ 左打ち"
                    }
                    Text(
                        text = "$handText | タップで変更",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // メインコンテンツ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "スイング動画を選択",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "高精度な弾道シミュレーションを実現",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            // 録画ボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Button(
                    onClick = onRecordClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5722)
                    )
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "今すぐ録画",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "高フレームレートで撮影",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // アップロードボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Button(
                    onClick = onUploadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "既存の動画をアップロード",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ギャラリーから選択",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // サンプル動画ボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Button(
                    onClick = onSampleClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "サンプル動画で試す",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "デモ動画を使用",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // シミュレーターボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Button(
                    onClick = onSimulatorClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "弾道シミュレーター",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "アニメーションで確認",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // 選択された動画の表示（自動遷移するため表示のみ）
            if (selectedVideoUri != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "動画を読み込み中...",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "2タップ解析画面へ移動します",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyzeScreen(
    videoUri: Uri,
    initialTrajectoryData: List<Offset> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var tapCount by remember { mutableStateOf(0) }
    var tap1Position by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var tap2Position by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var ballSpeed by remember { mutableStateOf<Double?>(null) }
    var launchAngle by remember { mutableStateOf<Double?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var autoDetectMode by remember { mutableStateOf(true) } // 自動検出モード
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var isDetecting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // デバッグ情報
    var debugInfo by remember { mutableStateOf<TrajectoryDebugInfo?>(null) }
    var trajectoryPoints by remember { mutableStateOf<List<TrajectoryPoint>>(emptyList()) }
    
    // 録画時の軌跡データ（リアルタイム追跡結果）
    var recordedTrajectory by remember { mutableStateOf(initialTrajectoryData) }
    
    // 物理シミュレーション弾道線への切り替えフラグ
    var usePhysicsTrajectory by remember { mutableStateOf(false) }
    var physicsTrajectoryResult by remember { mutableStateOf<TrajectoryEngine.TrajectoryResult?>(null) }
    
    // 軌跡データから初速を推定して物理シミュレーション弾道線を生成
    LaunchedEffect(recordedTrajectory) {
        if (recordedTrajectory.isNotEmpty() && recordedTrajectory.size >= 10) {
            try {
                // 初速推定（最初の10点から計算）
                val initialPoints = recordedTrajectory.take(10)
                val distance = kotlin.math.sqrt(
                    (initialPoints.last().x - initialPoints.first().x) * (initialPoints.last().x - initialPoints.first().x) +
                    (initialPoints.last().y - initialPoints.first().y) * (initialPoints.last().y - initialPoints.first().y)
                )
                
                // 推定初速（m/s）- 画面座標からメートルへの変換
                val estimatedSpeed = (distance / 10.0) * 30.0 // 30fps想定
                
                // 推定打ち出し角度
                val deltaY = initialPoints.last().y - initialPoints.first().y
                val deltaX = initialPoints.last().x - initialPoints.first().x
                val estimatedAngle = Math.toDegrees(kotlin.math.atan2(-deltaY.toDouble(), deltaX.toDouble()))
                
                // TrajectoryEngineで物理シミュレーション実行
                val engine = TrajectoryEngine()
                val result = engine.calculateTrajectory(
                    ballSpeed = estimatedSpeed.coerceIn(20.0, 80.0),
                    launchAngle = estimatedAngle.coerceIn(5.0, 45.0),
                    spinRate = 2500.0
                )
                
                physicsTrajectoryResult = result
                
                // 5秒後に自動的に物理シミュレーション弾道線に切り替え
                kotlinx.coroutines.delay(5000)
                usePhysicsTrajectory = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // アニメーション用: 現在表示する軌跡のポイント数
    var visibleTrajectoryCount by remember { mutableStateOf(0) }
    
    // ExoPlayerの初期化
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = false
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // 動画再生位置に合わせて軌跡を更新
    LaunchedEffect(exoPlayer.currentPosition, trajectoryPoints, isPlaying) {
        if (trajectoryPoints.isNotEmpty() && isPlaying) {
            // 動画の現在位置（秒）
            val currentTimeSec = exoPlayer.currentPosition / 1000.0
            
            // 現在時刻までの軌跡ポイントを表示
            val visibleCount = trajectoryPoints.indexOfFirst { it.time > currentTimeSec }
            visibleTrajectoryCount = if (visibleCount == -1) trajectoryPoints.size else visibleCount
        }
        // 一時停止時は現在の表示をそのまま保持（何もしない）
    }
    
    // ML Kit物体検出器の初期化
    val objectDetector = remember {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }
    
    // 動画フレームから物体を検出（自動解析）
    fun detectObjectsInCurrentFrame() {
        if (isDetecting) return
        
        coroutineScope.launch {
            try {
                isDetecting = true
                
                // 現在のフレームをBitmapとして取得
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                val currentTimeUs = (exoPlayer.currentPosition * 1000).toLong()
                val bitmap = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                
                if (bitmap != null) {
                    // ML Kitで物体検出
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val objects = objectDetector.process(image).await()
                    
                    detectedObjects = objects
                    
                    Log.d("ObjectDetection", "検出された物体数: ${objects.size}")
                    objects.forEachIndexed { index, obj ->
                        Log.d("ObjectDetection", "物体$index: ${obj.boundingBox}, ラベル: ${obj.labels.firstOrNull()?.text}")
                    }
                    
                    // 検出された物体が2つ以上ある場合、自動的に2点を設定
                    if (objects.size >= 2 && tapCount == 0) {
                        val obj1 = objects[0].boundingBox
                        val obj2 = objects[1].boundingBox
                        
                        tap1Position = Pair(obj1.centerX().toFloat(), obj1.centerY().toFloat())
                        tap2Position = Pair(obj2.centerX().toFloat(), obj2.centerY().toFloat())
                        
                        // 物理シミュレーションを実行
                        val result = calculateBallPhysicsWithDebug(tap1Position!!, tap2Position!!)
                        ballSpeed = result.ballSpeed
                        launchAngle = result.launchAngle
                        debugInfo = result.debugInfo
                        trajectoryPoints = result.trajectory
                        tapCount = 2
                        
                        Toast.makeText(context, "✅ 自動検出完了！弾道を表示します", Toast.LENGTH_SHORT).show()
                    } else if (objects.isEmpty()) {
                        Toast.makeText(context, "物体が検出されませんでした", Toast.LENGTH_SHORT).show()
                    } else if (objects.size == 1) {
                        Toast.makeText(context, "物体が1つだけ検出されました。2つ必要です", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ObjectDetection", "エラー: ${e.message}", e)
                Toast.makeText(context, "検出エラー: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isDetecting = false
            }
        }
    }
    
    // 動画全体を解析してボールの軌跡を追跡
    fun autoAnalyzeVideo() {
        if (isDetecting) return
        
        coroutineScope.launch {
            try {
                isDetecting = true
                Toast.makeText(context, "🔍 ボールの軌跡を追跡中...", Toast.LENGTH_SHORT).show()
                
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                
                // 動画の長さを取得
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val frameCount = 60 // より多くのフレームをサンプリング
                val interval = durationMs / frameCount
                
                val ballTrajectory = mutableListOf<Pair<Float, Float>>()
                var firstBallPosition: Pair<Float, Float>? = null
                
                // フレームごとにボールを検出して軌跡を記録
                for (i in 0 until frameCount) {
                    val timeUs = (i * interval * 1000).toLong()
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val objects = objectDetector.process(image).await()
                        
                        // 最も小さい物体をボールと仮定
                        val ball = objects.minByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        
                        if (ball != null) {
                            val position = Pair(
                                ball.boundingBox.centerX().toFloat(),
                                ball.boundingBox.centerY().toFloat()
                            )
                            ballTrajectory.add(position)
                            
                            if (firstBallPosition == null) {
                                firstBallPosition = position
                            }
                            
                            Log.d("BallTracking", "Frame $i: Ball at (${"%.1f".format(position.first)}, ${"%.1f".format(position.second)})")
                        }
                    }
                }
                
                retriever.release()
                
                // 軌跡が見つかった場合
                if (ballTrajectory.size >= 3) {
                    // 最初と最後の位置を設定
                    tap1Position = ballTrajectory.first()
                    tap2Position = ballTrajectory.last()
                    
                    // 実際の軌跡をTrajectoryPointに変換
                    val realTrajectory = ballTrajectory.mapIndexed { index, pos ->
                        // 相対座標に変換（最初の位置を原点とする）
                        val relX = (pos.first - firstBallPosition!!.first).toDouble() / 100.0 // ピクセルをメートルに概算
                        val relY = (firstBallPosition!!.second - pos.second).toDouble() / 100.0 // Y軸反転
                        TrajectoryPoint(
                            x = kotlin.math.max(0.0, relX),
                            y = kotlin.math.max(0.0, relY),
                            time = index * (durationMs / frameCount.toDouble()) / 1000.0,
                            velocity = 0.0
                        )
                    }
                    
                    trajectoryPoints = realTrajectory
                    tapCount = 2
                    
                    // 初速と角度を推定
                    if (realTrajectory.size >= 2) {
                        val dx = realTrajectory[1].x - realTrajectory[0].x
                        val dy = realTrajectory[1].y - realTrajectory[0].y
                        val dt = realTrajectory[1].time - realTrajectory[0].time
                        
                        ballSpeed = kotlin.math.sqrt(dx * dx + dy * dy) / dt
                        launchAngle = Math.toDegrees(kotlin.math.atan2(dy, dx))
                    }
                    
                    Log.d("BallTracking", "軌跡検出完了: ${ballTrajectory.size}点")
                    Toast.makeText(context, "✅ ボールの軌跡を検出しました！(${ballTrajectory.size}点)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ ボールの軌跡が見つかりませんでした", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e("AutoAnalyze", "エラー: ${e.message}", e)
                Toast.makeText(context, "解析エラー: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isDetecting = false
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ヘッダー
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2タップ解析",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "タップ: $tapCount/2",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                // 自動解析ボタン
                IconButton(
                    onClick = { autoAnalyzeVideo() },
                    enabled = !isDetecting
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "自動解析",
                        tint = if (isDetecting) Color.Gray else Color(0xFFFFD700)
                    )
                }
                // モード切替ボタン
                IconButton(onClick = {
                    autoDetectMode = !autoDetectMode
                    tapCount = 0
                    tap1Position = null
                    tap2Position = null
                    ballSpeed = null
                    launchAngle = null
                }) {
                    Icon(
                        if (autoDetectMode) Icons.Default.TouchApp else Icons.Default.TouchApp,
                        contentDescription = if (autoDetectMode) "自動検出モード" else "手動タップモード",
                        tint = Color.White
                    )
                }
                // リセットボタン
                IconButton(onClick = {
                    tapCount = 0
                    tap1Position = null
                    tap2Position = null
                    ballSpeed = null
                    launchAngle = null
                    trajectoryPoints = emptyList()
                }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "リセット",
                        tint = Color.White
                    )
                }
            }
        }
        
        // 動画プレイヤー（画面の大部分を占める）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 残りのスペースを全て使用
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // デフォルトコントロールを無効化
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // タップ検出用のオーバーレイ
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tapCount, autoDetectMode) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (autoDetectMode) {
                                    // 自動検出モード: ML Kitで物体検出
                                    if (tapCount == 0 && !isDetecting) {
                                        detectObjectsInCurrentFrame()
                                    }
                                } else {
                                    // 手動モード: ダブルタップ不要、シングルタップで記録
                                    if (tapCount < 2) {
                                        if (tapCount == 0) {
                                            tap1Position = Pair(offset.x, offset.y)
                                        } else {
                                            tap2Position = Pair(offset.x, offset.y)
                                            
                                            // 2点目がマーキングされたら計算を実行
                                            tap1Position?.let { pos1 ->
                                                val result = calculateBallPhysicsWithDebug(pos1, Pair(offset.x, offset.y))
                                                ballSpeed = result.ballSpeed
                                                launchAngle = result.launchAngle
                                                debugInfo = result.debugInfo
                                                trajectoryPoints = result.trajectory
                                            }
                                        }
                                        tapCount++
                                        Toast
                                            .makeText(
                                                context,
                                                "マーキング $tapCount: (${offset.x.toInt()}, ${offset.y.toInt()})",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                }
                            }
                        )
                    }
            )
            
            // タップした位置をマーク表示
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1回目のタップ位置
                tap1Position?.let { pos ->
                    drawCircle(
                        color = Color.Blue,
                        radius = 30f,
                        center = Offset(pos.first, pos.second),
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = Color.Blue,
                        radius = 8f,
                        center = Offset(pos.first, pos.second)
                    )
                }
                
                // 2回目のタップ位置
                tap2Position?.let { pos ->
                    drawCircle(
                        color = Color.Green,
                        radius = 30f,
                        center = Offset(pos.first, pos.second),
                        style = Stroke(width = 4f)
                    )
                    drawCircle(
                        color = Color.Green,
                        radius = 8f,
                        center = Offset(pos.first, pos.second)
                    )
                }
                
                // 2点間の線
                if (tap1Position != null && tap2Position != null) {
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(tap1Position!!.first, tap1Position!!.second),
                        end = Offset(tap2Position!!.first, tap2Position!!.second),
                        strokeWidth = 3f
                    )
                }
                
                // ボールの軌跡を描画（青色の放物線・アニメーション）
                if (trajectoryPoints.isNotEmpty() && tap1Position != null) {
                    val startX = tap1Position!!.first
                    val startY = tap1Position!!.second
                    
                    // 表示する軌跡の範囲を決定
                    val displayCount = if (isPlaying) visibleTrajectoryCount else trajectoryPoints.size
                    val visiblePoints = trajectoryPoints.take(displayCount)
                    
                    if (visiblePoints.isNotEmpty()) {
                        Log.d("TrajectoryDraw", "描画: ${visiblePoints.size}/${trajectoryPoints.size} points")
                        
                        // 実際の軌跡データを使用（ピクセル座標で直接描画）
                        visiblePoints.forEachIndexed { index, point ->
                            if (index > 0) {
                                val prev = visiblePoints[index - 1]
                                
                                // メートル座標をピクセル座標に変換
                                val scale = 100.0 // 1メートル = 100ピクセル
                                val x1 = startX + (prev.x * scale).toFloat()
                                val y1 = startY - (prev.y * scale).toFloat()
                                val x2 = startX + (point.x * scale).toFloat()
                                val y2 = startY - (point.y * scale).toFloat()
                                
                                drawLine(
                                    color = Color(0xFF2196F3), // 青色
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 8f
                                )
                            }
                        }
                        
                        // 軌跡の各点を丸で表示
                        visiblePoints.forEach { point ->
                            val scale = 100.0
                            val x = startX + (point.x * scale).toFloat()
                            val y = startY - (point.y * scale).toFloat()
                            
                            drawCircle(
                                color = Color(0xFF2196F3),
                                radius = 4f,
                                center = Offset(x, y)
                            )
                        }
                        
                        // 現在のボール位置を大きく表示（再生中のみ）
                        if (isPlaying && visiblePoints.isNotEmpty()) {
                            val currentPoint = visiblePoints.last()
                            val scale = 100.0
                            val x = startX + (currentPoint.x * scale).toFloat()
                            val y = startY - (currentPoint.y * scale).toFloat()
                            
                            drawCircle(
                                color = Color(0xFFFFEB3B), // 黄色
                                radius = 12f,
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color(0xFF2196F3),
                                radius = 8f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
            
            // 専用の再生/一時停止ボタン（中央下部）
            FloatingActionButton(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "一時停止" else "再生",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // デバッグ情報（一時的）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (tapCount < 2) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (autoDetectMode) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (autoDetectMode) Icons.Default.AutoAwesome else Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (autoDetectMode) Color(0xFFFF9800) else Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (tapCount == 0) {
                            Text(
                                text = if (autoDetectMode) "画面をタップで自動検出" else "1回目: クラブヘッド",
                                fontSize = 13.sp
                            )
                        } else {
                            Text(
                                text = "2回目: ボール",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isDetecting) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
            
            // 🔍 デバッグ情報表示（一時的）
            if (tapCount == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🔍 デバッグ情報",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "V₀ (初速): ${ballSpeed?.let { "%.2f m/s".format(it) } ?: "N/A"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "θ (打ち出し角): ${launchAngle?.let { "%.2f°".format(it) } ?: "N/A"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "軌跡ポイント数: ${trajectoryPoints.size}",
                            fontSize = 11.sp
                        )
                        if (trajectoryPoints.isNotEmpty()) {
                            Text(
                                text = "開始: (${trajectoryPoints.first().x.format(2)}, ${trajectoryPoints.first().y.format(2)})",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "終了: (${trajectoryPoints.last().x.format(2)}, ${trajectoryPoints.last().y.format(2)})",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            val maxY = trajectoryPoints.maxOfOrNull { it.y } ?: 0.0
                            Text(
                                text = "最高点: ${maxY.format(2)} m",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        debugInfo?.let { debug ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "ピクセル距離: ${debug.pixelDistance.format(1)} px", fontSize = 10.sp, color = Color.Gray)
                            Text(text = "実距離: ${debug.realDistance.format(3)} m", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// フォーマット用拡張関数
fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

// 物理定数と補助関数
object PhysicsConstants {
    // 物理定数
    const val GRAVITY = 9.81                    // m/s² (重力加速度)
    const val AIR_DENSITY_SEA_LEVEL = 1.225     // kg/m³ (海面の空気密度)
    const val BALL_MASS = 0.0459                // kg (ゴルフボールの質量)
    const val BALL_DIAMETER = 0.0427            // m (42.7mm)
    val BALL_AREA = Math.PI * (BALL_DIAMETER / 2) * (BALL_DIAMETER / 2) // m²
    
    // 係数
    const val DRAG_COEFFICIENT = 0.25           // 抗力係数
    const val LIFT_COEFFICIENT = 0.15           // 揚力係数
    const val SMASH_FACTOR = 1.45               // スマッシュファクター
    
    /**
     * 気温と高度から空気密度を計算
     */
    fun calculateAirDensity(temperature: Double, altitude: Double): Double {
        val tempKelvin = temperature + 273.15
        val base = 1.0 - 0.0065 * altitude / 288.15
        val pressure = 101325.0 * Math.pow(base, 5.255)
        return pressure / (287.05 * tempKelvin)
    }
    
    /**
     * 抗力係数（レイノルズ数依存）
     */
    fun calculateDragCoefficient(velocity: Double, airDensity: Double): Double {
        val viscosity = 1.81e-5 // kg/(m·s) at 15°C
        val reynoldsNumber = (airDensity * velocity * BALL_DIAMETER) / viscosity
        
        return when {
            reynoldsNumber > 150000 -> 0.25
            reynoldsNumber > 50000 -> 0.3
            else -> 0.4
        }
    }
    
    /**
     * スピン量から揚力係数を調整
     */
    fun calculateLiftCoefficient(spinRate: Double, velocity: Double): Double {
        if (velocity < 0.01) return 0.0
        
        val omega = (spinRate * 2 * Math.PI) / 60.0 // rad/s
        val spinParameter = (omega * BALL_DIAMETER / 2.0) / velocity
        
        return LIFT_COEFFICIENT * kotlin.math.min(spinParameter * 2.0, 1.5)
    }
}

// データクラス
data class TrajectoryPoint(
    val x: Double,
    val y: Double,
    val time: Double,
    val velocity: Double
)

data class TrajectoryDebugInfo(
    val point1: Pair<Float, Float>,
    val point2: Pair<Float, Float>,
    val pixelDistance: Double,
    val realDistance: Double,
    val pixelsPerMeter: Double,
    val carryDistance: Double,
    val maxHeight: Double,
    val flightTime: Double
)

data class BallPhysicsResult(
    val ballSpeed: Double,
    val launchAngle: Double,
    val debugInfo: TrajectoryDebugInfo,
    val trajectory: List<TrajectoryPoint>
)

// スマッシュファクターを使った正確な初速推定（Web版ロジック移植）
fun calculateBallPhysicsWithDebug(
    point1: Pair<Float, Float>,
    point2: Pair<Float, Float>
): BallPhysicsResult {
    // ピクセル間の距離を計算
    val dx = point2.first - point1.first
    val dy = point2.second - point1.second
    val ballPixelDistance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
    
    // キャリブレーション: ゴルフボール直径 = 0.043m を基準
    val calibrationPixelDistance = 50.0
    val calibrationDistance = 0.043
    val pixelsPerMeter = calibrationPixelDistance / calibrationDistance
    
    // クラブヘッドの移動距離（メートル）
    val clubHeadDistanceMeters = ballPixelDistance / pixelsPerMeter
    
    // 時間差（秒）- 120fps想定で1フレーム
    val fps = 120.0
    val timeDiff = 1.0 / fps
    
    // クラブヘッド速度（m/s）
    val clubHeadSpeed = clubHeadDistanceMeters / timeDiff
    
    // ボール初速（m/s）= クラブヘッド速度 × スマッシュファクター
    val ballSpeed = clubHeadSpeed * PhysicsConstants.SMASH_FACTOR
    
    // 打ち出し角（度）- Y軸反転
    val dyInverted = -dy.toDouble()
    val launchAngleRadians = kotlin.math.atan2(dyInverted, dx.toDouble())
    val launchAngle = Math.toDegrees(launchAngleRadians)
    val clampedAngle = kotlin.math.max(0.0, launchAngle)
    
    // 弾道シミュレーション（スピンレート2500rpm固定）
    val spinRate = 2500.0
    val trajectory = simulateTrajectoryWithPhysics(ballSpeed, clampedAngle, spinRate)
    
    // キャリー距離と最高点
    val carryDistance = trajectory.lastOrNull()?.x ?: 0.0
    val maxHeight = trajectory.maxOfOrNull { it.y } ?: 0.0
    val flightTime = trajectory.lastOrNull()?.time ?: 0.0
    
    // デバッグ情報
    val debugInfo = TrajectoryDebugInfo(
        point1 = point1,
        point2 = point2,
        pixelDistance = ballPixelDistance,
        realDistance = clubHeadDistanceMeters,
        pixelsPerMeter = pixelsPerMeter,
        carryDistance = carryDistance,
        maxHeight = maxHeight,
        flightTime = flightTime
    )
    
    return BallPhysicsResult(ballSpeed, clampedAngle, debugInfo, trajectory)
}

// 完全な物理シミュレーション（抗力・揚力・重力を考慮）
fun simulateTrajectoryWithPhysics(
    ballSpeed: Double,
    launchAngle: Double,
    spinRate: Double,
    temperature: Double = 20.0,
    altitude: Double = 0.0
): List<TrajectoryPoint> {
    // 初期条件
    val launchAngleRad = Math.toRadians(launchAngle)
    var vx = ballSpeed * kotlin.math.cos(launchAngleRad)
    var vy = ballSpeed * kotlin.math.sin(launchAngleRad)
    
    var x = 0.0
    var y = 0.0
    var t = 0.0
    
    val dt = 0.01 // 時間刻み（秒）
    val maxTime = 20.0
    
    val airDensity = PhysicsConstants.calculateAirDensity(temperature, altitude)
    
    val trajectory = mutableListOf<TrajectoryPoint>()
    trajectory.add(TrajectoryPoint(x, y, t, ballSpeed))
    
    Log.d("TrajectoryDebug", "=== 弾道シミュレーション開始 ===")
    Log.d("TrajectoryDebug", "初期条件: V0=$ballSpeed m/s, θ=$launchAngle°")
    Log.d("TrajectoryDebug", "初期速度: vx=$vx m/s, vy=$vy m/s")
    
    // Euler法でシミュレーション
    while (y >= 0 && t < maxTime) {
        val v = kotlin.math.sqrt(vx * vx + vy * vy)
        
        if (v < 0.01) break
        
        // 抗力
        val cd = PhysicsConstants.calculateDragCoefficient(v, airDensity)
        val dragForce = 0.5 * airDensity * cd * PhysicsConstants.BALL_AREA * v * v
        val dragAccelX = -(dragForce / PhysicsConstants.BALL_MASS) * (vx / v)
        val dragAccelY = -(dragForce / PhysicsConstants.BALL_MASS) * (vy / v)
        
        // 揚力（マグヌス効果）
        val cl = PhysicsConstants.calculateLiftCoefficient(spinRate, v)
        val liftForce = 0.5 * airDensity * cl * PhysicsConstants.BALL_AREA * v * v
        val liftAccelY = liftForce / PhysicsConstants.BALL_MASS
        
        // 合計加速度
        val ax = dragAccelX
        val ay = dragAccelY + liftAccelY - PhysicsConstants.GRAVITY
        
        // 速度と位置の更新
        vx += ax * dt
        vy += ay * dt
        x += vx * dt
        y += vy * dt
        t += dt
        
        // 0.1秒ごとに記録
        if (trajectory.isEmpty() || t - trajectory.last().time >= 0.1) {
            trajectory.add(TrajectoryPoint(x, kotlin.math.max(0.0, y), t, v))
            if (trajectory.size <= 5) {
                Log.d("TrajectoryDebug", "Point ${trajectory.size}: x=${"%.2f".format(x)}, y=${"%.2f".format(y)}, t=${"%.2f".format(t)}")
            }
        }
    }
    
    Log.d("TrajectoryDebug", "=== シミュレーション完了 ===")
    Log.d("TrajectoryDebug", "総ポイント数: ${trajectory.size}")
    Log.d("TrajectoryDebug", "最終位置: x=${"%.2f".format(x)}, y=${"%.2f".format(y)}")
    Log.d("TrajectoryDebug", "最高点: ${"%.2f".format(trajectory.maxOfOrNull { it.y } ?: 0.0)} m")
    
    return trajectory
}

// 簡易版弾道シミュレーション（後方互換性のため残す）
fun simulateTrajectory(ballSpeed: Double, launchAngle: Double): List<TrajectoryPoint> {
    // 物理定数
    val GRAVITY = 9.81 // m/s²
    val BALL_MASS = 0.0459 // kg
    val BALL_DIAMETER = 0.0427 // m
    val BALL_AREA = Math.PI * (BALL_DIAMETER / 2) * (BALL_DIAMETER / 2)
    val AIR_DENSITY = 1.225 // kg/m³
    val DRAG_COEFFICIENT = 0.25
    val LIFT_COEFFICIENT = 0.15
    val SPIN_RATE = 2500.0 // rpm（仮定）
    
    // 初期条件
    val launchAngleRad = Math.toRadians(launchAngle)
    var vx = ballSpeed * kotlin.math.cos(launchAngleRad)
    var vy = ballSpeed * kotlin.math.sin(launchAngleRad)
    
    var x = 0.0
    var y = 0.0
    var t = 0.0
    
    val dt = 0.01 // 時間刻み
    val maxTime = 20.0
    
    val trajectory = mutableListOf<TrajectoryPoint>()
    trajectory.add(TrajectoryPoint(x, y, t, ballSpeed))
    
    // Euler法でシミュレーション
    while (y >= 0 && t < maxTime) {
        val v = kotlin.math.sqrt(vx * vx + vy * vy)
        
        if (v < 0.01) break // 速度がほぼゼロなら終了
        
        // 抗力
        val dragForce = 0.5 * AIR_DENSITY * DRAG_COEFFICIENT * BALL_AREA * v * v
        val dragAccelX = -(dragForce / BALL_MASS) * (vx / v)
        val dragAccelY = -(dragForce / BALL_MASS) * (vy / v)
        
        // 揚力（簡略版）
        val liftForce = 0.5 * AIR_DENSITY * LIFT_COEFFICIENT * BALL_AREA * v * v
        val liftAccelY = liftForce / BALL_MASS
        
        // 合計加速度
        val ax = dragAccelX
        val ay = dragAccelY + liftAccelY - GRAVITY
        
        // 速度と位置の更新
        vx += ax * dt
        vy += ay * dt
        x += vx * dt
        y += vy * dt
        t += dt
        
        // 0.1秒ごとに記録
        if (trajectory.isEmpty() || t - trajectory.last().time >= 0.1) {
            trajectory.add(TrajectoryPoint(x, kotlin.math.max(0.0, y), t, v))
        }
    }
    
    return trajectory
}

// 簡易的な飛距離推定（放物線運動の公式）
fun estimateDistance(ballSpeed: Double, launchAngle: Double): Double {
    val g = 9.81 // 重力加速度 (m/s²)
    val angleRad = Math.toRadians(launchAngle)
    
    // 理想的な飛距離 = v² * sin(2θ) / g
    val distance = (ballSpeed * ballSpeed * kotlin.math.sin(2 * angleRad)) / g
    
    return distance
}

// CameraX録画画面
@Composable
fun CameraRecordScreen(
    onVideoRecorded: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    val previewView = remember { PreviewView(context) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    
    // タイマー
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }
    
    // CameraXの初期化
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // プレビュー
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // VideoCapture - 60fps固定
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            
            videoCapture = VideoCapture.withOutput(recorder)
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "カメラバインドエラー", e)
                Toast.makeText(context, "カメラ起動エラー: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // カメラプレビュー
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // UI オーバーレイ
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ヘッダー
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "閉じる", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (isRecording) {
                        Text(
                            text = "⏺ 録画中 ${recordingTime}秒",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 録画ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isRecording) {
                            // 録画停止
                            recording?.stop()
                            recording = null
                            isRecording = false
                        } else {
                            // 録画開始
                            val outputFile = File(
                                context.filesDir,
                                "golf_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
                            )
                            
                            val outputOptions = FileOutputOptions.Builder(outputFile).build()
                            
                            recording = videoCapture?.output
                                ?.prepareRecording(context, outputOptions)
                                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Finalize -> {
                                            if (event.hasError()) {
                                                Toast.makeText(context, "録画エラー", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val uri = Uri.fromFile(outputFile)
                                                onVideoRecorded(uri)
                                            }
                                        }
                                    }
                                }
                            isRecording = true
                        }
                    },
                    containerColor = if (isRecording) Color.Red else Color.White,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "停止" else "録画",
                        modifier = Modifier.size(40.dp),
                        tint = if (isRecording) Color.White else Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun HandPreferenceDialog(
    onHandSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 初回起動時は閉じられない */ },
        title = {
            Text(
                text = "利き手を選択してください",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "撮影時のガイド表示を最適化します",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 右打ちボタン
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Button(
                            onClick = { onHandSelected(UserPreferences.HAND_RIGHT) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🏌️",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "右打ち",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // 左打ちボタン
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Button(
                            onClick = { onHandSelected(UserPreferences.HAND_LEFT) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🏌️",
                                    fontSize = 48.sp,
                                    modifier = Modifier.graphicsLayer(scaleX = -1f) // 左右反転
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "左打ち",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
