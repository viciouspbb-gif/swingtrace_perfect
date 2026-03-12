package com.golftrajectory.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/**
 * スイング分析エンジン
 * クラブヘッドの軌道を分析し、スイングプレーン、速度、テンポを計算
 */
class SwingAnalyzer {
    
    data class SwingPoint(
        val position: Offset,
        val timestamp: Long // ミリ秒
    )
    
    data class SwingAnalysis(
        val swingPath: List<SwingPoint>,
        val swingSpeed: Double, // m/s
        val swingTempo: Double, // 秒
        val backswingTime: Double, // 秒
        val downswingTime: Double, // 秒
        val impactSpeed: Double, // m/s
        val swingPlaneAngle: Double, // 度
        val swingArc: Double, // 度
        val maxHeight: Float,
        val impactPoint: Offset?
    )
    
    /**
     * スイング軌道を分析
     */
    fun analyzeSwing(swingPoints: List<SwingPoint>): SwingAnalysis? {
        if (swingPoints.size < 3) return null
        
        // トップ位置を検出（最も高い位置）
        val minY = swingPoints.minOf { p -> p.position.y }
        val topIndex = swingPoints.indexOfFirst { it.position.y == minY }
        
        // インパクト位置を検出（最も低い位置付近で速度が最大）
        val impactIndex = findImpactPoint(swingPoints)
        
        // バックスイングとダウンスイングを分離
        val backswing = if (topIndex > 0) swingPoints.subList(0, topIndex + 1) else emptyList()
        val downswing = if (topIndex < swingPoints.size - 1) swingPoints.subList(topIndex, swingPoints.size) else emptyList()
        
        // 時間計算
        val totalTime = (swingPoints.last().timestamp - swingPoints.first().timestamp) / 1000.0
        val backswingTime = if (backswing.isNotEmpty()) 
            (backswing.last().timestamp - backswing.first().timestamp) / 1000.0 else 0.0
        val downswingTime = if (downswing.isNotEmpty()) 
            (downswing.last().timestamp - downswing.first().timestamp) / 1000.0 else 0.0
        
        // スイング速度計算
        val swingSpeed = calculateAverageSpeed(swingPoints)
        val impactSpeed = if (impactIndex >= 0) calculateSpeedAtPoint(swingPoints, impactIndex) else 0.0
        
        // スイングプレーン角度
        val swingPlaneAngle = calculateSwingPlaneAngle(swingPoints)
        
        // スイングアーク（振り幅）
        val swingArc = calculateSwingArc(swingPoints)
        
        // 最高到達点
        val maxHeight = swingPoints.minByOrNull { it.position.y }?.position?.y ?: 0f
        
        // インパクトポイント
        val impactPoint = if (impactIndex >= 0) swingPoints[impactIndex].position else null
        
        return SwingAnalysis(
            swingPath = swingPoints,
            swingSpeed = swingSpeed,
            swingTempo = totalTime,
            backswingTime = backswingTime,
            downswingTime = downswingTime,
            impactSpeed = impactSpeed,
            swingPlaneAngle = swingPlaneAngle,
            swingArc = swingArc,
            maxHeight = maxHeight,
            impactPoint = impactPoint
        )
    }
    
    /**
     * インパクトポイントを検出
     */
    private fun findImpactPoint(points: List<SwingPoint>): Int {
        if (points.size < 3) return -1
        
        var maxSpeed = 0.0
        var impactIndex = -1
        
        // 下半分のポイントで最大速度を探す
        val lowerHalfStart = points.size / 2
        for (i in lowerHalfStart until points.size - 1) {
            val speed = calculateSpeedAtPoint(points, i)
            if (speed > maxSpeed) {
                maxSpeed = speed
                impactIndex = i
            }
        }
        
        return impactIndex
    }
    
    /**
     * 平均速度を計算
     */
    private fun calculateAverageSpeed(points: List<SwingPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            val dx = points[i].position.x - points[i - 1].position.x
            val dy = points[i].position.y - points[i - 1].position.y
            totalDistance += sqrt(dx * dx + dy * dy)
        }
        
        val totalTime = (points.last().timestamp - points.first().timestamp) / 1000.0
        return if (totalTime > 0) totalDistance / totalTime else 0.0
    }
    
    /**
     * 特定ポイントでの速度を計算
     */
    private fun calculateSpeedAtPoint(points: List<SwingPoint>, index: Int): Double {
        if (index <= 0 || index >= points.size) return 0.0
        
        val prev = points[index - 1]
        val curr = points[index]
        
        val dx = curr.position.x - prev.position.x
        val dy = curr.position.y - prev.position.y
        val distance = sqrt(dx * dx + dy * dy)
        
        val timeDiff = (curr.timestamp - prev.timestamp) / 1000.0
        return if (timeDiff > 0) distance / timeDiff else 0.0
    }
    
    /**
     * スイングプレーン角度を計算
     */
    private fun calculateSwingPlaneAngle(points: List<SwingPoint>): Double {
        if (points.size < 2) return 0.0
        
        val start = points.first().position
        val end = points.last().position
        
        val dx = end.x - start.x
        val dy = end.y - start.y
        
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    }
    
    /**
     * スイングアーク（振り幅）を計算
     */
    private fun calculateSwingArc(points: List<SwingPoint>): Double {
        if (points.size < 3) return 0.0
        
        // 開始点、トップ、終了点の角度を計算
        val start = points.first().position
        val top = points.minByOrNull { it.position.y }?.position ?: return 0.0
        val end = points.last().position
        
        // 中心点を推定（開始点と終了点の中間）
        val centerX = (start.x + end.x) / 2
        val centerY = (start.y + end.y) / 2
        
        // 各点から中心への角度
        val angleStart = atan2((start.y - centerY).toDouble(), (start.x - centerX).toDouble())
        val angleTop = atan2((top.y - centerY).toDouble(), (top.x - centerX).toDouble())
        val angleEnd = atan2((end.y - centerY).toDouble(), (end.x - centerX).toDouble())
        
        // 最大角度差を計算
        val arc1 = abs(angleTop - angleStart)
        val arc2 = abs(angleEnd - angleTop)
        
        return Math.toDegrees(arc1 + arc2)
    }
    
    /**
     * スイングテンポを評価
     */
    fun evaluateSwingTempo(analysis: SwingAnalysis): String {
        val ratio = if (analysis.downswingTime > 0) 
            analysis.backswingTime / analysis.downswingTime else 0.0
        
        return when {
            ratio < 2.0 -> "速い"
            ratio < 3.0 -> "理想的"
            ratio < 4.0 -> "やや遅い"
            else -> "遅い"
        }
    }
    
    /**
     * スイング速度を評価
     */
    fun evaluateSwingSpeed(speedMps: Double): String {
        return when {
            speedMps < 30 -> "遅い"
            speedMps < 40 -> "平均"
            speedMps < 50 -> "速い"
            else -> "非常に速い"
        }
    }
}
