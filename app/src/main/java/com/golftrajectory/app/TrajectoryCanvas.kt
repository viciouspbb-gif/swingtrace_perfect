package com.golftrajectory.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * クラブヘッド軌道描画Canvas
 */
@Composable
fun TrajectoryCanvas(
    trajectory: List<ClubHeadPoint>,
    modifier: Modifier = Modifier,
    showImpact: Boolean = true,
    smoothing: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (trajectory.isEmpty()) return@Canvas
        
        // フェーズごとにグループ化
        val phaseGroups = groupByPhase(trajectory)
        
        // 各フェーズを描画
        phaseGroups.forEach { (phase, points) ->
            if (points.size >= 2) {
                val color = getPhaseColor(phase)
                
                if (smoothing) {
                    drawSmoothPath(points, color)
                } else {
                    drawLinePath(points, color)
                }
            }
        }
        
        // インパクトポイントを描画
        if (showImpact) {
            val impactPoint = trajectory.firstOrNull { it.phase == SwingPhase.IMPACT }
            impactPoint?.let {
                drawImpactMarker(Offset(it.x, it.y))
            }
        }
        
        // 開始点と終了点を描画
        drawStartEndMarkers(trajectory.first(), trajectory.last())
    }
}

/**
 * フェーズごとにグループ化
 */
private fun groupByPhase(points: List<ClubHeadPoint>): Map<SwingPhase, List<ClubHeadPoint>> {
    val groups = mutableMapOf<SwingPhase, MutableList<ClubHeadPoint>>()
    
    var currentPhase = points.first().phase
    var currentGroup = mutableListOf<ClubHeadPoint>()
    
    points.forEach { point ->
        if (point.phase != currentPhase) {
            // フェーズが変わったら新しいグループを作成
            groups[currentPhase] = currentGroup
            currentPhase = point.phase
            currentGroup = mutableListOf()
        }
        currentGroup.add(point)
    }
    
    // 最後のグループを追加
    groups[currentPhase] = currentGroup
    
    return groups
}

/**
 * フェーズごとの色を取得
 */
private fun getPhaseColor(phase: SwingPhase): Color {
    return when (phase) {
        SwingPhase.SETUP -> Color(0xFF9E9E9E)           // グレー
        SwingPhase.TAKEAWAY -> Color(0xFF2196F3)        // 青
        SwingPhase.BACKSWING -> Color(0xFF2196F3)       // 青
        SwingPhase.TOP -> Color(0xFFFFEB3B)             // 黄色
        SwingPhase.DOWNSWING -> Color(0xFFF44336)       // 赤
        SwingPhase.IMPACT -> Color(0xFFFF9800)          // オレンジ
        SwingPhase.FOLLOW_THROUGH -> Color(0xFF4CAF50)  // 緑
        SwingPhase.FINISH -> Color(0xFF4CAF50)          // 緑
    }
}

/**
 * 滑らかなパスを描画（Bezier補間）
 */
private fun DrawScope.drawSmoothPath(points: List<ClubHeadPoint>, color: Color) {
    if (points.size < 2) return
    
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    
    // Catmull-Rom スプライン補間
    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
        
        // 制御点を計算
        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f
        
        path.cubicTo(
            cp1x, cp1y,
            cp2x, cp2y,
            p2.x, p2.y
        )
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * 直線パスを描画
 */
private fun DrawScope.drawLinePath(points: List<ClubHeadPoint>, color: Color) {
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        
        drawLine(
            color = color,
            start = Offset(p1.x, p1.y),
            end = Offset(p2.x, p2.y),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * インパクトマーカーを描画
 */
private fun DrawScope.drawImpactMarker(position: Offset) {
    // 外側の円（黄色）
    drawCircle(
        color = Color(0xFFFFEB3B),
        radius = 30f,
        center = position,
        style = Stroke(width = 4f)
    )
    
    // 内側の円（オレンジ）
    drawCircle(
        color = Color(0xFFFF9800),
        radius = 20f,
        center = position
    )
    
    // 中心点（白）
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = position
    )
}

/**
 * 開始点と終了点のマーカーを描画
 */
private fun DrawScope.drawStartEndMarkers(start: ClubHeadPoint, end: ClubHeadPoint) {
    // 開始点（緑）
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 15f,
        center = Offset(start.x, start.y)
    )
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = Offset(start.x, start.y)
    )
    
    // 終了点（赤）
    drawCircle(
        color = Color(0xFFF44336),
        radius = 15f,
        center = Offset(end.x, end.y)
    )
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = Offset(end.x, end.y)
    )
}
