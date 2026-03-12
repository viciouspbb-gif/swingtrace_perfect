package com.golftrajectory.app

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
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

@Composable
fun SwingAnalysisScreen(
    videoUri: Uri?,
    onBack: () -> Unit,
    onSelectVideo: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    
    // スイング軌道の記録
    var swingPoints by remember { mutableStateOf<List<SwingAnalyzer.SwingPoint>>(emptyList()) }
    var isRecording by remember { mutableStateOf(false) }
    var swingAnalysis by remember { mutableStateOf<SwingAnalyzer.SwingAnalysis?>(null) }
    
    val swingAnalyzer = remember { SwingAnalyzer() }
    
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
    
    // 再生位置を更新
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
            delay(50)
        }
    }
    
    // 再生速度を変更
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }
    
    DisposableEffect(videoUri) {
        videoUri?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it))
            exoPlayer.prepare()
        }
        onDispose {
            exoPlayer.release()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // ヘッダー
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1E1E),
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
                Text(
                    text = "スイング分析",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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
                
                // スイング軌道オーバーレイ
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (isRecording) {
                                        swingPoints = listOf(
                                            SwingAnalyzer.SwingPoint(
                                                offset,
                                                System.currentTimeMillis()
                                            )
                                        )
                                    }
                                },
                                onDrag = { change, _ ->
                                    if (isRecording) {
                                        swingPoints = swingPoints + SwingAnalyzer.SwingPoint(
                                            change.position,
                                            System.currentTimeMillis()
                                        )
                                    }
                                },
                                onDragEnd = {
                                    if (isRecording && swingPoints.size >= 3) {
                                        swingAnalysis = swingAnalyzer.analyzeSwing(swingPoints)
                                        isRecording = false
                                    }
                                }
                            )
                        }
                ) {
                    // スイング軌道を描画
                    if (swingPoints.isNotEmpty()) {
                        // 軌道の線
                        for (i in 1 until swingPoints.size) {
                            val start = swingPoints[i - 1].position
                            val end = swingPoints[i].position
                            
                            // グラデーション色
                            val progress = i.toFloat() / swingPoints.size
                            val color = lerp(
                                Color(0xFF00FF00),
                                Color(0xFFFF0000),
                                progress
                            )
                            
                            drawLine(
                                color = color,
                                start = start,
                                end = end,
                                strokeWidth = 8f,
                                cap = StrokeCap.Round
                            )
                        }
                        
                        // ポイントマーカー
                        swingPoints.forEachIndexed { index, point ->
                            if (index % 5 == 0) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 6f,
                                    center = point.position
                                )
                                drawCircle(
                                    color = Color(0xFF00BCD4),
                                    radius = 4f,
                                    center = point.position
                                )
                            }
                        }
                        
                        // インパクトポイント
                        swingAnalysis?.impactPoint?.let { impact ->
                            drawCircle(
                                color = Color(0xFFFF5722),
                                radius = 12f,
                                center = impact,
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = Color(0xFFFF5722),
                                radius = 8f,
                                center = impact
                            )
                        }
                    }
                    
                    // 記録中のインジケーター
                    if (isRecording) {
                        drawCircle(
                            color = Color.Red,
                            radius = 20f,
                            center = Offset(size.width - 40f, 40f)
                        )
                    }
                }
            } else {
                // 動画未選択
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("動画を選択してください", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onSelectVideo) {
                            Text("動画を選択")
                        }
                    }
                }
            }
        }
        
        // コントロールパネル
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 分析結果
                if (swingAnalysis != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2E2E2E)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📊 スイング分析結果",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnalysisItem(
                                    "スイング速度",
                                    "%.1f m/s".format(swingAnalysis!!.swingSpeed),
                                    swingAnalyzer.evaluateSwingSpeed(swingAnalysis!!.swingSpeed)
                                )
                                AnalysisItem(
                                    "テンポ",
                                    "%.2f秒".format(swingAnalysis!!.swingTempo),
                                    swingAnalyzer.evaluateSwingTempo(swingAnalysis!!)
                                )
                                AnalysisItem(
                                    "インパクト速度",
                                    "%.1f m/s".format(swingAnalysis!!.impactSpeed),
                                    ""
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AnalysisItem(
                                    "バックスイング",
                                    "%.2f秒".format(swingAnalysis!!.backswingTime),
                                    ""
                                )
                                AnalysisItem(
                                    "ダウンスイング",
                                    "%.2f秒".format(swingAnalysis!!.downswingTime),
                                    ""
                                )
                                AnalysisItem(
                                    "スイングアーク",
                                    "%.0f°".format(swingAnalysis!!.swingArc),
                                    ""
                                )
                            }
                        }
                    }
                }
                
                // 再生コントロール
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 記録ボタン
                    IconButton(
                        onClick = {
                            isRecording = !isRecording
                            if (isRecording) {
                                swingPoints = emptyList()
                                swingAnalysis = null
                            }
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Edit,
                            contentDescription = if (isRecording) "停止" else "軌道を描画",
                            tint = if (isRecording) Color.Red else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // クリアボタン
                    IconButton(
                        onClick = {
                            swingPoints = emptyList()
                            swingAnalysis = null
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "クリア",
                            tint = Color.White
                        )
                    }
                    
                    // 再生/一時停止
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "一時停止" else "再生",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // 速度調整
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Row {
                            IconButton(
                                onClick = { playbackSpeed = (playbackSpeed - 0.25f).coerceAtLeast(0.25f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "遅く",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { playbackSpeed = (playbackSpeed + 0.25f).coerceAtMost(2f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "速く",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                // シークバー
                if (duration > 0) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { exoPlayer.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisItem(label: String, value: String, evaluation: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00BCD4)
        )
        if (evaluation.isNotEmpty()) {
            Text(
                text = evaluation,
                fontSize = 10.sp,
                color = Color(0xFF4CAF50)
            )
        }
    }
}
