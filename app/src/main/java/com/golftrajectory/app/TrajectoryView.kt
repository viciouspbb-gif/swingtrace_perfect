package com.golftrajectory.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * 3D弾道表示ビュー（アニメーション対応）
 */
@Composable
fun TrajectoryView(
    trajectoryResult: TrajectoryEngine.TrajectoryResult?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    animationSpeed: Float = 1f
) {
    // アニメーション進行度（0.0〜1.0）
    var targetProgress by remember { mutableStateOf(0f) }
    
    // 新しい弾道が設定されたらアニメーションをリセット
    LaunchedEffect(trajectoryResult) {
        if (trajectoryResult != null) {
            targetProgress = 1f
        }
    }
    
    // 再生/一時停止の制御
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            targetProgress = 0f
        } else if (trajectoryResult != null) {
            targetProgress = 1f
        }
    }
    
    // アニメーション実行
    val animatedProgress by animateFloatAsState(
        targetValue = if (trajectoryResult != null && isPlaying) targetProgress else 0f,
        animationSpec = tween(
            durationMillis = ((trajectoryResult?.flightTime?.times(1000)?.toInt() ?: 2000) / animationSpeed).toInt().coerceIn(1000, 8000),
            easing = LinearEasing
        ),
        label = "trajectory_animation"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFE3F2FD))
    ) {
        if (trajectoryResult == null) {
            drawEmptyState()
            return@Canvas
        }
        
        drawTrajectory(trajectoryResult, animatedProgress)
    }
}

private fun DrawScope.drawEmptyState() {
    // グリッド線
    drawGrid()
    
    // 地面
    drawLine(
        color = Color(0xFF4CAF50),
        start = Offset(0f, size.height * 0.8f),
        end = Offset(size.width, size.height * 0.8f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawTrajectory(result: TrajectoryEngine.TrajectoryResult, animationProgress: Float) {
    val points = result.points
    if (points.isEmpty()) return
    
    // スケール計算
    val maxDistance = result.totalDistance.coerceAtLeast(10.0)
    val maxHeight = result.maxHeight.coerceAtLeast(5.0)
    
    val scaleX = size.width * 0.9f / maxDistance.toFloat()
    val scaleY = size.height * 0.6f / maxHeight.toFloat()
    
    val offsetX = size.width * 0.05f
    val groundY = size.height * 0.8f
    
    // グリッド線
    drawGrid()
    
    // 地面
    drawLine(
        color = Color(0xFF4CAF50),
        start = Offset(0f, groundY),
        end = Offset(size.width, groundY),
        strokeWidth = 4f
    )
    
    // 距離マーカー
    drawDistanceMarkers(maxDistance, groundY, offsetX, scaleX)
    
    // アニメーション進行度に基づいて表示するポイント数を計算
    val visiblePointCount = (points.size * animationProgress).toInt().coerceAtLeast(1)
    val visiblePoints = points.take(visiblePointCount)
    
    // グラデーション色で弾道を描画
    val colors = listOf(
        Color(0xFF00FF00), // 緑（開始）
        Color(0xFFFFFF00), // 黄
        Color(0xFFFF9800), // オレンジ
        Color(0xFFFF5722)  // 赤（終了）
    )
    
    visiblePoints.forEachIndexed { index, point ->
        if (index > 0) {
            val prevPoint = visiblePoints[index - 1]
            
            val x1 = offsetX + prevPoint.x.toFloat() * scaleX
            val y1 = groundY - prevPoint.y.toFloat() * scaleY
            val x2 = offsetX + point.x.toFloat() * scaleX
            val y2 = groundY - point.y.toFloat() * scaleY
            
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
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }
    
    // ボールの軌跡ポイント（白い点）- 既に通過した部分のみ
    visiblePoints.forEachIndexed { index, point ->
        if (index % 10 == 0) {
            val x = offsetX + point.x.toFloat() * scaleX
            val y = groundY - point.y.toFloat() * scaleY
            
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 2f,
                center = Offset(x, y)
            )
        }
    }
    
    // アニメーション中のボール位置
    if (visiblePointCount < points.size) {
        val currentPoint = points[visiblePointCount - 1]
        val ballX = offsetX + currentPoint.x.toFloat() * scaleX
        val ballY = groundY - currentPoint.y.toFloat() * scaleY
        
        // 残像効果（過去3フレーム分）
        for (i in 1..3) {
            val trailIndex = (visiblePointCount - 1 - i * 5).coerceAtLeast(0)
            if (trailIndex < visiblePointCount) {
                val trailPoint = points[trailIndex]
                val trailX = offsetX + trailPoint.x.toFloat() * scaleX
                val trailY = groundY - trailPoint.y.toFloat() * scaleY
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f / i),
                    radius = 10f / i,
                    center = Offset(trailX, trailY)
                )
            }
        }
        
        // ボールの影（地面）- 高さに応じてサイズ変更
        val shadowSize = 10f * (1f - (ballY - groundY) / (size.height * 0.6f)).coerceIn(0.3f, 1f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = shadowSize,
            center = Offset(ballX, groundY)
        )
        
        // ボールの軌跡エフェクト（外側の光）
        drawCircle(
            color = Color(0xFFFFEB3B).copy(alpha = 0.4f),
            radius = 20f,
            center = Offset(ballX, ballY)
        )
        drawCircle(
            color = Color(0xFFFFEB3B).copy(alpha = 0.6f),
            radius = 16f,
            center = Offset(ballX, ballY),
            style = Stroke(width = 2f)
        )
        
        // ボール本体（白いゴルフボール）
        drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(ballX, ballY)
        )
        
        // ボールのディンプル模様（回転表現）
        val rotationAngle = (animationProgress * 720f) % 360f // 2回転
        for (i in 0..2) {
            val angle = Math.toRadians((rotationAngle + i * 120).toDouble())
            val dimpleX = ballX + cos(angle).toFloat() * 6f
            val dimpleY = ballY + sin(angle).toFloat() * 6f
            
            drawCircle(
                color = Color(0xFFE0E0E0),
                radius = 2f,
                center = Offset(dimpleX, dimpleY)
            )
        }
        
        // ボールの輪郭
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 12f,
            center = Offset(ballX, ballY),
            style = Stroke(width = 2f)
        )
        
        // スピード感を表現する線
        if (visiblePointCount > 5) {
            val prevPoint = points[(visiblePointCount - 6).coerceAtLeast(0)]
            val prevX = offsetX + prevPoint.x.toFloat() * scaleX
            val prevY = groundY - prevPoint.y.toFloat() * scaleY
            
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(prevX, prevY),
                end = Offset(ballX, ballY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
    
    // アニメーション完了後のマーカー表示
    if (animationProgress >= 0.99f) {
        // 最高到達点マーカー
        val maxHeightPoint = points.maxByOrNull { it.y }
        if (maxHeightPoint != null) {
            val x = offsetX + maxHeightPoint.x.toFloat() * scaleX
            val y = groundY - maxHeightPoint.y.toFloat() * scaleY
            
            drawCircle(
                color = Color(0xFFFF5722),
                radius = 6f,
                center = Offset(x, y)
            )
            
            // 最高到達点から地面への破線
            drawDashedLine(
                color = Color(0xFFFF5722).copy(alpha = 0.5f),
                start = Offset(x, y),
                end = Offset(x, groundY),
                intervals = floatArrayOf(10f, 10f)
            )
        }
        
        // 着地点マーカー
        val lastPoint = points.last()
        val landingX = offsetX + lastPoint.x.toFloat() * scaleX
        
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = 8f,
            center = Offset(landingX, groundY)
        )
    }
}

private fun DrawScope.drawGrid() {
    val gridColor = Color(0xFFBBDEFB)
    
    // 縦線
    for (i in 0..10) {
        val x = size.width * i / 10f
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
    
    // 横線
    for (i in 0..10) {
        val y = size.height * i / 10f
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawDistanceMarkers(
    maxDistance: Double,
    groundY: Float,
    offsetX: Float,
    scaleX: Float
) {
    val markerInterval = when {
        maxDistance > 200 -> 50.0
        maxDistance > 100 -> 25.0
        else -> 10.0
    }
    
    var distance = markerInterval
    while (distance < maxDistance) {
        val x = offsetX + distance.toFloat() * scaleX
        
        drawLine(
            color = Color(0xFF757575).copy(alpha = 0.3f),
            start = Offset(x, groundY - 10f),
            end = Offset(x, groundY + 10f),
            strokeWidth = 2f
        )
        
        distance += markerInterval
    }
}

private fun DrawScope.drawDashedLine(
    color: Color,
    start: Offset,
    end: Offset,
    intervals: FloatArray
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(intervals, 0f)
    )
}
