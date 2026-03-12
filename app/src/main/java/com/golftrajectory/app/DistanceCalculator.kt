package com.golftrajectory.app

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * 飛距離計算エンジン
 */
class DistanceCalculator {
    
    /**
     * 2点間の距離を計算（ピクセル）
     */
    fun calculatePixelDistance(start: Offset, end: Offset): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * ピクセル距離を実際の距離（メートル）に変換
     * 画面幅を基準にスケーリング
     */
    fun pixelToMeters(pixelDistance: Float, screenWidthPx: Float): Double {
        // 仮定：画面幅 = 50メートル（ゴルフ練習場の平均的な幅）
        val metersPerPixel = 50.0 / screenWidthPx
        return pixelDistance * metersPerPixel
    }
    
    /**
     * メートルをヤードに変換
     */
    fun metersToYards(meters: Double): Double {
        return meters * 1.09361
    }
    
    /**
     * 弾道の総距離を計算（メートル）
     */
    fun calculateTrajectoryDistance(
        start: Offset,
        end: Offset,
        screenWidthPx: Float
    ): DistanceResult {
        val pixelDistance = calculatePixelDistance(start, end)
        val meters = pixelToMeters(pixelDistance, screenWidthPx)
        val yards = metersToYards(meters)
        
        return DistanceResult(
            meters = meters,
            yards = yards,
            pixelDistance = pixelDistance
        )
    }
    
    data class DistanceResult(
        val meters: Double,
        val yards: Double,
        val pixelDistance: Float
    )
}
