package com.golftrajectory.app

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    videoUri: Uri?,
    onBack: () -> Unit,
    onSelectVideo: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isVideoReady by remember { mutableStateOf(false) }
    
    // ボール検出結果
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var ballDetected by remember { mutableStateOf(false) }
    var distanceMeters by remember { mutableStateOf(0.0) }
    var distanceYards by remember { mutableStateOf(0.0) }
    var animationProgress by remember { mutableStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    
    val distanceCalculator = remember { DistanceCalculator() }
    
    // 画面サイズを取得
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // ワンタップで弾道を自動生成
    // タップした位置から自動的に弾道を推定
    
    // 動画変更時にリセット
    LaunchedEffect(videoUri) {
        isVideoReady = false
        startPoint = null
        endPoint = null
        ballDetected = false
    }
    
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true // 自動再生
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isVideoReady = true
                    }
                }
            })
        }
    }
    
    // 再生位置を更新
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(100)
        }
    }
    
    DisposableEffect(videoUri) {
        isVideoReady = false
        videoUri?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (videoUri != null) {
            // 動画プレイヤー（コントローラー表示）
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true // 標準コントローラーを使用
                        controllerShowTimeoutMs = 5000 // 5秒後に自動非表示
                        controllerHideOnTouch = true // タッチで表示/非表示切り替え
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 動画未選択
            NoVideoScreen(onSelectVideo = onSelectVideo)
        }
        
        // タップ可能なオーバーレイ
        if (videoUri != null && isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            // タップした位置からボールの弾道を推定
                            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
                            
                            // タップ位置を開始点として設定
                            startPoint = tapOffset
                            
                            // 弾道の終了点を推定
                            // 画面の右側、少し上方向に設定
                            val horizontalDistance = screenWidthPx * 0.5f // 画面幅の50%
                            val verticalDistance = -screenHeightPx * 0.15f // 上方向15%
                            
                            endPoint = Offset(
                                tapOffset.x + horizontalDistance,
                                tapOffset.y + verticalDistance
                            )
                            
                            // 画面外に出ないように調整
                            endPoint = Offset(
                                endPoint!!.x.coerceIn(0f, screenWidthPx),
                                endPoint!!.y.coerceIn(0f, screenHeightPx)
                            )
                            
                            ballDetected = true
                            
                            // 飛距離を計算（実際の距離）
                            val pixelDistance = kotlin.math.sqrt(
                                (endPoint!!.x - startPoint!!.x) * (endPoint!!.x - startPoint!!.x) +
                                (endPoint!!.y - startPoint!!.y) * (endPoint!!.y - startPoint!!.y)
                            )
                            
                            // 画面幅を基準にスケーリング（画面幅 = 100m と仮定）
                            distanceMeters = (pixelDistance / screenWidthPx * 100.0)
                            distanceYards = distanceMeters * 1.09361
                            
                            android.util.Log.d("GolfTrajectory", "Tap at $tapOffset, trajectory: $startPoint -> $endPoint, distance: ${distanceMeters}m")
                        }
                    }
            )
        }
        
        // 弾道オーバーレイ（タップ後に表示）
        if (videoUri != null && ballDetected && startPoint != null && endPoint != null) {
            RealTrajectoryOverlay(
                startPoint = startPoint!!,
                endPoint = endPoint!!,
                modifier = Modifier.fillMaxSize()
            )
            
            // 飛距離表示
            if (distanceMeters > 0) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "飛距離",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "%.1f m".format(distanceMeters),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%.1f yd".format(distanceYards),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // ワンタップ説明メッセージ
        if (videoUri != null && isVideoReady && !ballDetected) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⛳",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ボールをタップ",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1回タップするだけで弾道が表示されます",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        
        // トップバー（シンプル版）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
            
            IconButton(
                onClick = onSelectVideo,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.VideoLibrary, "動画選択")
            }
        }
    }
}

@Composable
fun RealTrajectoryOverlay(
    startPoint: Offset,
    endPoint: Offset,
    modifier: Modifier = Modifier
) {
    // アニメーション進行度
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(startPoint, endPoint) {
        animationProgress = 0f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 2000,
            easing = LinearEasing
        ),
        label = "ball_animation"
    )
    
    Canvas(modifier = modifier) {
        // 放物線を描画
        val numPoints = 50
        val points = mutableListOf<Offset>()
        
        // 放物線の高さを計算（距離の30%）
        val distance = kotlin.math.sqrt(
            (endPoint.x - startPoint.x) * (endPoint.x - startPoint.x) +
            (endPoint.y - startPoint.y) * (endPoint.y - startPoint.y)
        )
        val maxHeight = distance * 0.3f
        
        for (i in 0..numPoints) {
            val t = i.toFloat() / numPoints
            
            // 線形補間
            val x = startPoint.x + (endPoint.x - startPoint.x) * t
            
            // 放物線（二次関数）
            val parabola = 4 * maxHeight * t * (1 - t)
            val y = startPoint.y + (endPoint.y - startPoint.y) * t - parabola
            
            points.add(Offset(x, y))
        }
        
        // アニメーション進行度に基づいて表示するポイント数
        val visiblePointCount = (points.size * animatedProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(visiblePointCount)
        
        // グラデーション色で描画
        val colors = listOf(
            Color(0xFF00FF00), // 緑
            Color(0xFFFFFF00), // 黄
            Color(0xFFFF9800), // オレンジ
            Color(0xFFFF5722)  // 赤
        )
        
        visiblePoints.forEachIndexed { index, point ->
            if (index > 0) {
                val prevPoint = visiblePoints[index - 1]
                
                val progress = index.toFloat() / points.size
                val colorIndex = (progress * (colors.size - 1)).toInt().coerceIn(0, colors.size - 2)
                val colorProgress = (progress * (colors.size - 1)) - colorIndex
                
                val color = androidx.compose.ui.graphics.lerp(
                    colors[colorIndex],
                    colors[colorIndex + 1],
                    colorProgress
                )
                
                drawLine(
                    color = color,
                    start = prevPoint,
                    end = point,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // ポイントマーカー（既に通過した部分のみ）
        visiblePoints.forEachIndexed { index, point ->
            if (index % 5 == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = point
                )
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 4f,
                    center = point
                )
            }
        }
        
        // アニメーション中のボール
        if (visiblePointCount < points.size) {
            val currentPoint = points[visiblePointCount - 1]
            
            // 残像効果
            for (i in 1..3) {
                val trailIndex = (visiblePointCount - 1 - i * 3).coerceAtLeast(0)
                if (trailIndex < visiblePointCount) {
                    val trailPoint = points[trailIndex]
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f / i),
                        radius = 10f / i,
                        center = trailPoint
                    )
                }
            }
            
            // ボールの光エフェクト
            drawCircle(
                color = Color(0xFFFFEB3B).copy(alpha = 0.4f),
                radius = 20f,
                center = currentPoint
            )
            drawCircle(
                color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
                radius = 16f,
                center = currentPoint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            // ボール本体
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = currentPoint
            )
            
            // ボールの輪郭
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 12f,
                center = currentPoint,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            // 回転エフェクト
            val rotationAngle = (animatedProgress * 720f) % 360f
            for (i in 0..2) {
                val angle = Math.toRadians((rotationAngle + i * 120).toDouble())
                val dimpleX = currentPoint.x + kotlin.math.cos(angle).toFloat() * 6f
                val dimpleY = currentPoint.y + kotlin.math.sin(angle).toFloat() * 6f
                
                drawCircle(
                    color = Color(0xFFE0E0E0),
                    radius = 2f,
                    center = Offset(dimpleX, dimpleY)
                )
            }
        }
        
        // アニメーション完了後の終了点マーカー
        if (animatedProgress >= 0.99f) {
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 15f,
                center = endPoint
            )
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = endPoint
            )
        }
    }
}

@Composable
fun ColorfulTrajectoryOverlay(
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
            Color(0xFF00FF00), // 緑
            Color(0xFFFFFF00), // 黄
            Color(0xFFFF9800), // オレンジ
            Color(0xFFFF5722)  // 赤
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
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // ボールの軌跡ポイント
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

@Composable
fun NoVideoScreen(onSelectVideo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "動画が選択されていません",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "撮影した動画またはギャラリーから動画を選択してください",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onSelectVideo) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("動画を選択")
        }
    }
}
