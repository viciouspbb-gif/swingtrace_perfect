package com.golftrajectory.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 自動ボール検出エンジン
 * 色ベースの簡易的なボール検出（白いボールを検出）
 */
class AutoBallDetector {
    
    private var lastDetectedPosition: Offset? = null
    private val detectionHistory = mutableListOf<DetectionResult>()
    
    data class DetectionResult(
        val position: Offset,
        val confidence: Float,
        val timestamp: Long
    )
    
    /**
     * ビットマップからボールを検出
     */
    fun detectBall(bitmap: Bitmap): DetectionResult? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // 検出領域を制限（画面中央付近）
            val startX = (width * 0.1).toInt()
            val endX = (width * 0.9).toInt()
            val startY = (height * 0.1).toInt()
            val endY = (height * 0.9).toInt()
            
            var maxWhiteScore = 0f
            var bestX = 0
            var bestY = 0
            
            // グリッドサンプリング（高速化のため）
            val step = 10
            
            for (y in startY until endY step step) {
                for (x in startX until endX step step) {
                    if (x >= width || y >= height) continue
                    
                    val pixel = bitmap.getPixel(x, y)
                    val whiteScore = calculateWhiteScore(pixel)
                    
                    // 周辺ピクセルもチェック（ボールの円形を考慮）
                    val neighborScore = checkNeighborhood(bitmap, x, y, 5)
                    val totalScore = whiteScore * 0.7f + neighborScore * 0.3f
                    
                    if (totalScore > maxWhiteScore) {
                        maxWhiteScore = totalScore
                        bestX = x
                        bestY = y
                    }
                }
            }
            
            // スコアが閾値以上なら検出成功
            if (maxWhiteScore > 0.6f) {
                val position = Offset(bestX.toFloat(), bestY.toFloat())
                
                // 前回の位置から大きく離れている場合は無視（ノイズ除去）
                lastDetectedPosition?.let { last ->
                    val distance = sqrt(
                        (position.x - last.x).pow(2) + (position.y - last.y).pow(2)
                    )
                    if (distance > 200f) {
                        return null
                    }
                }
                
                val result = DetectionResult(
                    position = position,
                    confidence = maxWhiteScore,
                    timestamp = System.currentTimeMillis()
                )
                
                lastDetectedPosition = position
                detectionHistory.add(result)
                
                // 履歴を最大50件に制限
                if (detectionHistory.size > 50) {
                    detectionHistory.removeAt(0)
                }
                
                return result
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * ピクセルの白さスコアを計算
     */
    private fun calculateWhiteScore(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        
        // 白に近いほど高スコア
        val brightness = (r + g + b) / 3f / 255f
        
        // RGBのバランスが取れているか（白は均等）
        val variance = abs(r - g) + abs(g - b) + abs(b - r)
        val balance = 1f - (variance / 765f)
        
        return brightness * 0.7f + balance * 0.3f
    }
    
    /**
     * 周辺ピクセルをチェック
     */
    private fun checkNeighborhood(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int): Float {
        var totalScore = 0f
        var count = 0
        
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = centerX + dx
                val y = centerY + dy
                
                if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    totalScore += calculateWhiteScore(pixel)
                    count++
                }
            }
        }
        
        return if (count > 0) totalScore / count else 0f
    }
    
    /**
     * 検出履歴を取得
     */
    fun getDetectionHistory(): List<DetectionResult> {
        return detectionHistory.toList()
    }
    
    /**
     * 履歴をクリア
     */
    fun clearHistory() {
        detectionHistory.clear()
        lastDetectedPosition = null
    }
    
    /**
     * スムージングされた軌跡を取得
     */
    fun getSmoothedTrajectory(): List<Offset> {
        if (detectionHistory.size < 3) {
            return detectionHistory.map { it.position }
        }
        
        val smoothed = mutableListOf<Offset>()
        
        // 移動平均でスムージング
        for (i in 1 until detectionHistory.size - 1) {
            val prev = detectionHistory[i - 1].position
            val curr = detectionHistory[i].position
            val next = detectionHistory[i + 1].position
            
            val smoothedX = (prev.x + curr.x + next.x) / 3f
            val smoothedY = (prev.y + curr.y + next.y) / 3f
            
            smoothed.add(Offset(smoothedX, smoothedY))
        }
        
        return smoothed
    }
}
