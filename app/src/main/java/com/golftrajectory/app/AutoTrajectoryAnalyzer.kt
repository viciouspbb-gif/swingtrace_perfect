package com.golftrajectory.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 自動弾道分析システム
 * YOLOv8で検出したボール位置から弾道を計算
 */
class AutoTrajectoryAnalyzer {
    
    private val trackedPoints = mutableListOf<TrackPoint>()
    
    data class TrackPoint(
        val position: Offset,
        val timestamp: Long,
        val frameIndex: Int
    )
    
    data class TrajectoryResult(
        val points: List<Offset>,
        val launchPoint: Offset,
        val landingPoint: Offset,
        val apexPoint: Offset,
        val distance: Float,
        val maxHeight: Float,
        val flightTime: Float
    )
    
    /**
     * ボール位置を追加
     */
    fun addPoint(position: Offset, timestamp: Long, frameIndex: Int) {
        trackedPoints.add(TrackPoint(position, timestamp, frameIndex))
    }
    
    /**
     * 軌跡をクリア
     */
    fun clear() {
        trackedPoints.clear()
    }
    
    /**
     * 弾道を分析
     */
    fun analyzeTrajectory(
        videoWidth: Int,
        videoHeight: Int,
        fps: Float = 30f
    ): TrajectoryResult? {
        if (trackedPoints.size < 5) {
            // 最低5ポイント必要
            return null
        }
        
        // 発射点を検出（最初の動き始め）
        val launchPoint = detectLaunchPoint() ?: trackedPoints.first().position
        
        // 着地点を検出（最後の位置または地面）
        val landingPoint = detectLandingPoint(videoHeight) ?: trackedPoints.last().position
        
        // 最高点を検出
        val apexPoint = detectApexPoint() ?: trackedPoints[trackedPoints.size / 2].position
        
        // 物理スムージングで滑らかな弾道を生成
        val smoothedPoints = smoothTrajectory(launchPoint, apexPoint, landingPoint)
        
        // 飛距離を計算（ピクセル → メートル変換）
        val distance = calculateDistance(launchPoint, landingPoint, videoWidth)
        
        // 最高到達点（ピクセル → メートル変換）
        val maxHeight = calculateHeight(launchPoint, apexPoint, videoHeight)
        
        // 滞空時間（フレーム数 → 秒）
        val flightTime = trackedPoints.size / fps
        
        return TrajectoryResult(
            points = smoothedPoints,
            launchPoint = launchPoint,
            landingPoint = landingPoint,
            apexPoint = apexPoint,
            distance = distance,
            maxHeight = maxHeight,
            flightTime = flightTime
        )
    }
    
    /**
     * 発射点を検出（最初の大きな動き）
     */
    private fun detectLaunchPoint(): Offset? {
        if (trackedPoints.size < 3) return null
        
        for (i in 0 until trackedPoints.size - 2) {
            val p1 = trackedPoints[i].position
            val p2 = trackedPoints[i + 1].position
            val p3 = trackedPoints[i + 2].position
            
            val dist1 = distance(p1, p2)
            val dist2 = distance(p2, p3)
            
            // 急激な動きが始まった点
            if (dist2 > dist1 * 1.5f && dist2 > 10f) {
                return p1
            }
        }
        
        return trackedPoints.first().position
    }
    
    /**
     * 着地点を検出（動きが止まった点）
     */
    private fun detectLandingPoint(videoHeight: Int): Offset? {
        if (trackedPoints.size < 3) return null
        
        // 後ろから探索
        for (i in trackedPoints.size - 1 downTo 2) {
            val p1 = trackedPoints[i].position
            val p2 = trackedPoints[i - 1].position
            val p3 = trackedPoints[i - 2].position
            
            val dist1 = distance(p1, p2)
            val dist2 = distance(p2, p3)
            
            // 動きが急激に遅くなった点
            if (dist1 < dist2 * 0.3f && dist1 < 5f) {
                return p1
            }
        }
        
        return trackedPoints.last().position
    }
    
    /**
     * 最高点を検出（Y座標が最小の点）
     */
    private fun detectApexPoint(): Offset? {
        return trackedPoints.minByOrNull { it.position.y }?.position
    }
    
    /**
     * 物理スムージングで滑らかな弾道を生成
     */
    private fun smoothTrajectory(
        launch: Offset,
        apex: Offset,
        landing: Offset
    ): List<Offset> {
        val points = mutableListOf<Offset>()
        val steps = 50
        
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            
            // 3点を通る放物線を計算
            val x = lerp(launch.x, landing.x, t)
            
            // 放物線の高さ計算
            val midX = (launch.x + landing.x) / 2
            val heightFactor = 1f - 4f * (t - 0.5f).pow(2)
            val maxHeightDiff = launch.y - apex.y
            val y = launch.y - maxHeightDiff * heightFactor
            
            points.add(Offset(x, y))
        }
        
        return points
    }
    
    /**
     * 飛距離を計算（ピクセル → メートル）
     */
    private fun calculateDistance(
        launch: Offset,
        landing: Offset,
        videoWidth: Int
    ): Float {
        val pixelDistance = distance(launch, landing)
        
        // 簡易的な変換（画面幅 = 50m と仮定）
        // 実際にはカメラの距離・角度から計算が必要
        val metersPerPixel = 50f / videoWidth
        
        return pixelDistance * metersPerPixel
    }
    
    /**
     * 最高到達点を計算（ピクセル → メートル）
     */
    private fun calculateHeight(
        launch: Offset,
        apex: Offset,
        videoHeight: Int
    ): Float {
        val pixelHeight = launch.y - apex.y
        
        // 簡易的な変換（画面高さ = 30m と仮定）
        val metersPerPixel = 30f / videoHeight
        
        return pixelHeight * metersPerPixel
    }
    
    /**
     * 2点間の距離
     */
    private fun distance(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * 線形補間
     */
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
    
    /**
     * 追跡ポイント数を取得
     */
    fun getPointCount(): Int = trackedPoints.size
    
    /**
     * すべてのポイントを取得
     */
    fun getAllPoints(): List<TrackPoint> = trackedPoints.toList()
}
