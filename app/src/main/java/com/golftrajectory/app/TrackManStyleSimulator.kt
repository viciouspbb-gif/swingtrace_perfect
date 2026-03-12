package com.golftrajectory.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun SwingTraceSimulator(onBack: () -> Unit) {
    var ballSpeed by remember { mutableStateOf("70.0") }
    var launchAngle by remember { mutableStateOf("12.0") }
    var launchDirection by remember { mutableStateOf("0.0") } // 左右の打ち出し角度
    var spinRate by remember { mutableStateOf("2500") }
    var spinAxis by remember { mutableStateOf("0.0") } // スピン軸の傾き
    var clubSpeed by remember { mutableStateOf("45.0") }
    
    var trajectoryResult by remember { mutableStateOf<TrajectoryEngine.TrajectoryResult?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf("side") } // side, top, 3d
    
    val trajectoryEngine = remember { TrajectoryEngine() }
    
    // スマッシュファクター計算
    val smashFactor = remember(ballSpeed, clubSpeed) {
        val ball = ballSpeed.toDoubleOrNull() ?: 0.0
        val club = clubSpeed.toDoubleOrNull() ?: 1.0
        if (club > 0) ball / club else 0.0
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // ヘッダー（TrackMan風）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "戻る",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SWINGTRACE",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D4FF),
                        letterSpacing = 2.sp
                    )
                }
                
                // ビューモード切り替え
                Row {
                    IconButton(onClick = { viewMode = "side" }) {
                        Icon(
                            Icons.Default.ViewAgenda,
                            contentDescription = "サイドビュー",
                            tint = if (viewMode == "side") Color(0xFF00D4FF) else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewMode = "top" }) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "トップビュー",
                            tint = if (viewMode == "top") Color(0xFF00D4FF) else Color.Gray
                        )
                    }
                }
            }
        }
        
        // 弾道表示エリア
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F0F0F))
        ) {
            if (trajectoryResult != null) {
                TrackManTrajectoryView(
                    trajectoryResult = trajectoryResult!!,
                    viewMode = viewMode,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 初期画面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.GolfCourse,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFF00D4FF).copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "パラメータを入力して\n計算を実行してください",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        // データ表示パネル（TrackMan風）
        if (trajectoryResult != null) {
            TrackManDataPanel(
                trajectoryResult = trajectoryResult!!,
                ballSpeed = ballSpeed.toDoubleOrNull() ?: 0.0,
                clubSpeed = clubSpeed.toDoubleOrNull() ?: 0.0,
                smashFactor = smashFactor,
                launchAngle = launchAngle.toDoubleOrNull() ?: 0.0,
                launchDirection = launchDirection.toDoubleOrNull() ?: 0.0,
                spinRate = spinRate.toDoubleOrNull() ?: 0.0,
                spinAxis = spinAxis.toDoubleOrNull() ?: 0.0
            )
        }
        
        // コントロールパネル
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 入力フィールド（2列）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackManInputField(
                        label = "ボール初速",
                        value = ballSpeed,
                        onValueChange = { ballSpeed = it },
                        unit = "m/s",
                        modifier = Modifier.weight(1f)
                    )
                    TrackManInputField(
                        label = "クラブ速度",
                        value = clubSpeed,
                        onValueChange = { clubSpeed = it },
                        unit = "m/s",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackManInputField(
                        label = "打ち出し角",
                        value = launchAngle,
                        onValueChange = { launchAngle = it },
                        unit = "°",
                        modifier = Modifier.weight(1f)
                    )
                    TrackManInputField(
                        label = "打ち出し方向",
                        value = launchDirection,
                        onValueChange = { launchDirection = it },
                        unit = "°",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackManInputField(
                        label = "スピン量",
                        value = spinRate,
                        onValueChange = { spinRate = it },
                        unit = "rpm",
                        modifier = Modifier.weight(1f)
                    )
                    TrackManInputField(
                        label = "スピン軸",
                        value = spinAxis,
                        onValueChange = { spinAxis = it },
                        unit = "°",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // プリセット＆計算ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ballSpeed = "70.0"
                            clubSpeed = "45.0"
                            launchAngle = "12.0"
                            launchDirection = "0.0"
                            spinRate = "2500"
                            spinAxis = "0.0"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A)
                        )
                    ) {
                        Text("ドライバー", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            ballSpeed = "50.0"
                            clubSpeed = "35.0"
                            launchAngle = "18.0"
                            launchDirection = "0.0"
                            spinRate = "5000"
                            spinAxis = "0.0"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2A2A)
                        )
                    ) {
                        Text("アイアン", fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val speed = ballSpeed.toDoubleOrNull() ?: 70.0
                                val angle = launchAngle.toDoubleOrNull() ?: 12.0
                                val spin = spinRate.toDoubleOrNull() ?: 2500.0
                                
                                trajectoryResult = trajectoryEngine.calculateTrajectory(
                                    ballSpeed = speed,
                                    launchAngle = angle,
                                    spinRate = spin
                                )
                                isPlaying = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D4FF)
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("計算", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackManInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text(unit, fontSize = 12.sp, color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00D4FF),
                unfocusedBorderColor = Color(0xFF3A3A3A),
                cursorColor = Color(0xFF00D4FF)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

@Composable
fun TrackManDataPanel(
    trajectoryResult: TrajectoryEngine.TrajectoryResult,
    ballSpeed: Double,
    clubSpeed: Double,
    smashFactor: Double,
    launchAngle: Double,
    launchDirection: Double,
    spinRate: Double,
    spinAxis: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF151515)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SHOT DATA",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D4FF),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // データグリッド（3列）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrackManDataItem("キャリー", "%.1f m".format(trajectoryResult.totalDistance), Color(0xFF00FF88))
                TrackManDataItem("最高到達点", "%.1f m".format(trajectoryResult.maxHeight), Color(0xFFFFAA00))
                TrackManDataItem("滞空時間", "%.2f s".format(trajectoryResult.flightTime), Color(0xFF00D4FF))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrackManDataItem("ボール初速", "%.1f m/s".format(ballSpeed), Color.White)
                TrackManDataItem("打ち出し角", "%.1f°".format(launchAngle), Color.White)
                TrackManDataItem("スピン量", "%.0f rpm".format(spinRate), Color.White)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrackManDataItem("スマッシュ", "%.2f".format(smashFactor), 
                    if (smashFactor >= 1.45) Color(0xFF00FF88) else Color.White)
                TrackManDataItem("打ち出し方向", "%.1f°".format(launchDirection), Color.White)
                TrackManDataItem("スピン軸", "%.1f°".format(spinAxis), Color.White)
            }
        }
    }
}

@Composable
fun TrackManDataItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun TrackManTrajectoryView(
    trajectoryResult: TrajectoryEngine.TrajectoryResult,
    viewMode: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(
            durationMillis = (trajectoryResult.flightTime * 1000).toInt().coerceIn(1500, 5000),
            easing = LinearEasing
        ),
        label = "trackman_animation"
    )
    
    Canvas(modifier = modifier.background(Color(0xFF0F0F0F))) {
        val points = trajectoryResult.points
        if (points.isEmpty()) return@Canvas
        
        val maxDistance = trajectoryResult.totalDistance.coerceAtLeast(10.0)
        val maxHeight = trajectoryResult.maxHeight.coerceAtLeast(5.0)
        
        val scaleX = size.width * 0.85f / maxDistance.toFloat()
        val scaleY = size.height * 0.7f / maxHeight.toFloat()
        
        val offsetX = size.width * 0.1f
        val groundY = size.height * 0.85f
        
        // グリッド線（TrackMan風）
        drawTrackManGrid(groundY, maxDistance, offsetX, scaleX)
        
        // 弾道線
        val visiblePointCount = (points.size * animatedProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(visiblePointCount)
        
        visiblePoints.forEachIndexed { index, point ->
            if (index > 0) {
                val prevPoint = visiblePoints[index - 1]
                
                val x1 = offsetX + prevPoint.x.toFloat() * scaleX
                val y1 = groundY - prevPoint.y.toFloat() * scaleY
                val x2 = offsetX + point.x.toFloat() * scaleX
                val y2 = groundY - point.y.toFloat() * scaleY
                
                drawLine(
                    color = Color(0xFF00D4FF),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 4f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        
        // ボール
        if (visiblePointCount < points.size) {
            val currentPoint = points[visiblePointCount - 1]
            val ballX = offsetX + currentPoint.x.toFloat() * scaleX
            val ballY = groundY - currentPoint.y.toFloat() * scaleY
            
            // 光エフェクト
            drawCircle(
                color = Color(0xFF00D4FF).copy(alpha = 0.3f),
                radius = 25f,
                center = Offset(ballX, ballY)
            )
            
            // ボール本体
            drawCircle(
                color = Color.White,
                radius = 14f,
                center = Offset(ballX, ballY)
            )
            
            drawCircle(
                color = Color(0xFF00D4FF),
                radius = 14f,
                center = Offset(ballX, ballY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        
        // 最高到達点マーカー
        if (animatedProgress >= 0.99f) {
            val maxHeightPoint = points.maxByOrNull { it.y }
            if (maxHeightPoint != null) {
                val x = offsetX + maxHeightPoint.x.toFloat() * scaleX
                val y = groundY - maxHeightPoint.y.toFloat() * scaleY
                
                drawCircle(
                    color = Color(0xFFFFAA00),
                    radius = 8f,
                    center = Offset(x, y)
                )
                
                // 破線
                drawLine(
                    color = Color(0xFFFFAA00).copy(alpha = 0.5f),
                    start = Offset(x, y),
                    end = Offset(x, groundY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrackManGrid(
    groundY: Float,
    maxDistance: Double,
    offsetX: Float,
    scaleX: Float
) {
    // 地面
    drawLine(
        color = Color(0xFF00FF88),
        start = Offset(0f, groundY),
        end = Offset(size.width, groundY),
        strokeWidth = 3f
    )
    
    // 距離マーカー
    val interval = if (maxDistance > 150) 50.0 else 25.0
    var distance = interval
    while (distance < maxDistance) {
        val x = offsetX + distance.toFloat() * scaleX
        
        drawLine(
            color = Color(0xFF2A2A2A),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        
        distance += interval
    }
}
