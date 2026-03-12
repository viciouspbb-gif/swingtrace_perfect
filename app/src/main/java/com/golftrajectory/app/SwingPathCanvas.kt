package com.golftrajectory.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * スイング軌道Canvas（シンプル版）
 */
@Composable
fun SwingPathCanvas(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pathPoints.size < 2) return@Canvas
        
        // 軌道を描画
        for (i in 1 until pathPoints.size) {
            val (x1, y1) = pathPoints[i - 1]
            val (x2, y2) = pathPoints[i]
            
            drawLine(
                color = phaseColors.getOrElse(i - 1) { Color.Gray },
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        
        // 開始点（緑の丸）
        val (startX, startY) = pathPoints.first()
        drawCircle(
            color = Color.Green,
            radius = 12f,
            center = Offset(startX, startY)
        )
        
        // 終了点（赤の丸）
        val (endX, endY) = pathPoints.last()
        drawCircle(
            color = Color.Red,
            radius = 12f,
            center = Offset(endX, endY)
        )
    }
}

/**
 * 使用例
 */
@Composable
fun SwingPathCanvasExample() {
    val pathPoints = listOf(
        Pair(100f, 300f),
        Pair(120f, 280f),
        Pair(140f, 260f),
        Pair(160f, 240f),
        Pair(180f, 260f),
        Pair(200f, 280f),
        Pair(220f, 300f)
    )
    
    val phaseColors = listOf(
        Color.Blue,   // テイクバック
        Color.Blue,
        Color.Red,    // ダウンスイング
        Color.Red,
        Color.Green,  // フォロー
        Color.Green
    )
    
    SwingPathCanvas(
        pathPoints = pathPoints,
        phaseColors = phaseColors,
        strokeWidth = 6f
    )
}
