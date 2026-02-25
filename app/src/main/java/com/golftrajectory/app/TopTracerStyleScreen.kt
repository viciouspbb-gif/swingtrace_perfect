package com.golftrajectory.app

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun SwingTraceScreen(
    videoUri: Uri?,
    onBack: () -> Unit,
    onSelectVideo: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showTrajectory by remember { mutableStateOf(false) }
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var trajectoryPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var distance by remember { mutableStateOf(0.0) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }
    
    DisposableEffect(videoUri) {
        videoUri?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (videoUri != null) {
            // 動画プレイヤー
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // タップエリア（動画の上）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            if (startPoint == null) {
                                // 1回目：ボールの位置
                                startPoint = tapOffset
                            } else if (endPoint == null) {
                                // 2回目：着地点
                                endPoint = tapOffset
                                
                                // 放物線を生成
                                val points = mutableListOf<Offset>()
                                val numPoints = 50
                                
                                val dx = endPoint!!.x - startPoint!!.x
                                val dy = endPoint!!.y - startPoint!!.y
                                val dist = sqrt(dx * dx + dy * dy)
                                
                                // 放物線の高さ
                                val maxHeight = dist * 0.3f
                                
                                for (i in 0..numPoints) {
                                    val t = i.toFloat() / numPoints
                                    val x = startPoint!!.x + dx * t
                                    val parabola = 4 * maxHeight * t * (1 - t)
                                    val y = startPoint!!.y + dy * t - parabola
                                    points.add(Offset(x, y))
                                }
                                
                                trajectoryPoints = points
                                showTrajectory = true
                                distance = (dist / size.width.toFloat() * 100.0)
                            }
                        }
                    }
            )
            
            // 弾道オーバーレイ（動画の上に表示）
            if (showTrajectory && trajectoryPoints.isNotEmpty()) {
                SimpleTrajectoryOverlay(
                    points = trajectoryPoints,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 開始点マーカー
            if (startPoint != null && !showTrajectory) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFFF6B00),
                        radius = 20f,
                        center = startPoint!!,
                        alpha = 0.6f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = startPoint!!
                    )
                }
            }
            
            // 飛距離表示
            if (showTrajectory && distance > 0) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "飛距離",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.1f m".format(distance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "%.1f yd".format(distance * 1.09361),
                            fontSize = 18.sp,
                            color = Color(0xFFFF6B00)
                        )
                    }
                }
            }
            
            // 説明メッセージ
            if (!showTrajectory && startPoint == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ボールの弾道を表示",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. ボールの位置をタップ\n2. 着地点をタップ",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            if (startPoint != null && !showTrajectory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6B00).copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = "着地点をタップ",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // コントロールバー
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, "戻る", tint = Color.White)
                }
                
                Row {
                    if (showTrajectory) {
                        IconButton(
                            onClick = {
                                showTrajectory = false
                                startPoint = null
                                endPoint = null
                                trajectoryPoints = emptyList()
                                distance = 0.0
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, "リセット", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = onSelectVideo,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        )
                    ) {
                        Icon(Icons.Default.VideoLibrary, "動画選択", tint = Color.White)
                    }
                }
            }
        } else {
            // 動画未選択
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "動画を選択してください",
                    fontSize = 20.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSelectVideo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00)
                    )
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("動画を選択", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SimpleTrajectoryOverlay(
    points: List<Offset>,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(points) {
        animationProgress = 0f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 2000,
            easing = LinearEasing
        ),
        label = "simple_trajectory_animation"
    )
    
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        
        // アニメーション進行度に基づいて表示
        val visiblePointCount = (points.size * animatedProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(visiblePointCount)
        
        // TopTracer風の白い弾道線
        visiblePoints.forEachIndexed { index, point ->
            if (index > 0) {
                val prevPoint = visiblePoints[index - 1]
                
                // オレンジの外側線
                drawLine(
                    color = Color(0xFFFF6B00),
                    start = prevPoint,
                    end = point,
                    strokeWidth = 12f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    alpha = 0.4f
                )
                
                // 白い内側線
                drawLine(
                    color = Color.White,
                    start = prevPoint,
                    end = point,
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        // ボール
        if (visiblePointCount < points.size) {
            val currentPoint = points[visiblePointCount - 1]
            
            // ボールの光
            drawCircle(
                color = Color.White,
                radius = 25f,
                center = currentPoint,
                alpha = 0.3f
            )
            
            // ボール本体
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = currentPoint
            )
            
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 12f,
                center = currentPoint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
        
        // 着地点マーカー
        if (animatedProgress >= 0.99f && points.isNotEmpty()) {
            val landingPoint = points.last()
            
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 15f,
                center = landingPoint
            )
            
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = landingPoint
            )
        }
    }
}

/*
@Composable
fun TopTracerOverlay(
    trajectory: TopTracerTrajectory,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(trajectory) {
        animationProgress = 0f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = (trajectory.flightTime * 1000).toInt(),
            easing = LinearEasing
        ),
        label = "toptracer_animation"
    )
    
    Canvas(modifier = modifier) {
        val startX = size.width * 0.15f
        val startY = size.height * 0.75f
        val endX = size.width * 0.85f
        val endY = size.height * 0.70f
        
        // 弾道の放物線を計算
        val numPoints = 100
        val points = mutableListOf<Offset>()
        
        val distance = endX - startX
        val heightFactor = trajectory.maxHeight.toFloat() * 3f
        
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            val x = startX + distance * t
            
            // 放物線
            val parabola = 4 * heightFactor * t * (1 - t)
            val y = startY - parabola + (endY - startY) * t
            
            points.add(Offset(x, y))
        }
        
        // アニメーション進行度に基づいて表示
        val visiblePointCount = (points.size * animatedProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(visiblePointCount)
        
        // TopTracer風の白い弾道線
        visiblePoints.forEachIndexed { index, point ->
            if (index > 0) {
                val prevPoint = visiblePoints[index - 1]
                
                // 太い白線
                drawLine(
                    color = Color.White,
                    start = prevPoint,
                    end = point,
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                // オレンジの外側線（TopTracer風）
                drawLine(
                    color = Color(0xFFFF6B00),
                    start = prevPoint,
                    end = point,
                    strokeWidth = 10f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    alpha = 0.3f
                )
            }
        }
        
        // ボール
        if (visiblePointCount < points.size) {
            val currentPoint = points[visiblePointCount - 1]
            
            // ボールの光
            drawCircle(
                color = Color.White,
                radius = 20f,
                center = currentPoint,
                alpha = 0.3f
            )
            
            // ボール本体
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = currentPoint
            )
            
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 10f,
                center = currentPoint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        
        // 最高到達点マーカー
        if (animatedProgress >= 0.99f) {
            val maxHeightIndex = points.size / 2
            val maxPoint = points[maxHeightIndex]
            
            // 破線
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(maxPoint.x, maxPoint.y),
                end = Offset(maxPoint.x, size.height),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            
            // マーカー
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 8f,
                center = maxPoint
            )
        }
        
        // 着地点マーカー
        if (animatedProgress >= 0.99f) {
            val landingPoint = Offset(endX, endY)
            
            drawCircle(
                color = Color(0xFFFF6B00),
                radius = 12f,
                center = landingPoint
            )
            
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = landingPoint
            )
        }
    }
}

@Composable
fun TopTracerDataPanel(
    trajectory: TopTracerTrajectory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SHOT DATA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B00),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // キャリー
            DataRow("キャリー", "%.0f m".format(trajectory.carryDistance), "%.0f yd".format(trajectory.carryDistance * 1.09361))
            Spacer(modifier = Modifier.height(8.dp))
            
            // トータル
            DataRow("トータル", "%.0f m".format(trajectory.totalDistance), "%.0f yd".format(trajectory.totalDistance * 1.09361))
            Spacer(modifier = Modifier.height(8.dp))
            
            // 最高到達点
            DataRow("最高到達点", "%.1f m".format(trajectory.maxHeight), "")
            Spacer(modifier = Modifier.height(8.dp))
            
            // ボール初速
            DataRow("ボール初速", "%.1f m/s".format(trajectory.ballSpeed), "")
            Spacer(modifier = Modifier.height(8.dp))
            
            // 打ち出し角
            DataRow("打ち出し角", "%.1f°".format(trajectory.launchAngle), "")
            Spacer(modifier = Modifier.height(8.dp))
            
            // スピン量
            DataRow("スピン量", "%.0f rpm".format(trajectory.spinRate), "")
        }
    }
}

*/
