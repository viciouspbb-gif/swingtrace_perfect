package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ビデオフレームを解析してボールを検出
 */
class VideoFrameAnalyzer(private val context: Context) {
    
    data class BallPosition(
        val position: Offset,
        val timestamp: Long,
        val confidence: Float
    )
    
    /**
     * ビデオからボールを検出
     */
    suspend fun analyzeBallTrajectory(videoUri: Uri): Pair<Offset?, Offset?> = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
            
            if (duration == 0L) {
                retriever.release()
                return@withContext Pair(null, null)
            }
            
            val detectedPositions = mutableListOf<BallPosition>()
            
            // 動画の最初の2秒間を解析（8フレームサンプリング）- バランス重視
            val sampleCount = 8
            val interval = minOf(2000L, duration) / sampleCount
            
            for (i in 0 until sampleCount) {
                val timestamp = i * interval
                val frame = retriever.getFrameAtTime(timestamp * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                
                if (frame != null) {
                    val ballPos = detectBallInFrame(frame, width.toFloat(), height.toFloat())
                    if (ballPos != null) {
                        detectedPositions.add(BallPosition(ballPos, timestamp, 1.0f))
                    }
                    frame.recycle()
                }
            }
            
            android.util.Log.d("GolfTrajectory", "Detected ${detectedPositions.size} ball positions out of $sampleCount frames")
            
            retriever.release()
            
            // 検出されたポイントから開始点と終了点を推定
            if (detectedPositions.size >= 2) {
                val startPoint = detectedPositions.first().position
                val endPoint = detectedPositions.last().position
                android.util.Log.d("GolfTrajectory", "Ball detected: start=$startPoint, end=$endPoint")
                Pair(startPoint, endPoint)
            } else {
                // 検出できない場合はnullを返す（弾道を表示しない）
                android.util.Log.d("GolfTrajectory", "No ball detected (${detectedPositions.size} points)")
                Pair(null, null)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
    
    /**
     * フレーム内からボールを検出（簡易版：白い円形物体）
     */
    private fun detectBallInFrame(frame: Bitmap, videoWidth: Float, videoHeight: Float): Offset? {
        try {
            val width = frame.width
            val height = frame.height
            
            var maxWhiteScore = 0f
            var bestX = 0
            var bestY = 0
            
            // グリッドサンプリング（バランス重視）
            val step = 30
            
            for (y in 0 until height step step) {
                for (x in 0 until width step step) {
                    if (x >= width || y >= height) continue
                    
                    val pixel = frame.getPixel(x, y)
                    val whiteScore = calculateWhiteScore(pixel)
                    
                    // 周辺ピクセルもチェック（範囲を拡大）
                    val neighborScore = checkNeighborhood(frame, x, y, 4)
                    val totalScore = whiteScore * 0.6f + neighborScore * 0.4f
                    
                    if (totalScore > maxWhiteScore) {
                        maxWhiteScore = totalScore
                        bestX = x
                        bestY = y
                    }
                }
            }
            
            android.util.Log.d("GolfTrajectory", "Max white score in frame: $maxWhiteScore at ($bestX, $bestY)")
            
            // スコアが閾値以上なら検出成功（閾値を上げて誤検出を防ぐ）
            if (maxWhiteScore > 0.8f) {
                // フレームサイズからビデオサイズへの変換
                val scaleX = videoWidth / width
                val scaleY = videoHeight / height
                return Offset(bestX * scaleX, bestY * scaleY)
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
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        
        // 白に近いほど高スコア
        val brightness = (r + g + b) / 3f / 255f
        
        // RGBのバランスが取れているか
        val variance = kotlin.math.abs(r - g) + kotlin.math.abs(g - b) + kotlin.math.abs(b - r)
        val balance = 1f - (variance / 765f)
        
        return brightness * 0.7f + balance * 0.3f
    }
    
    /**
     * 周辺ピクセルをチェック
     */
    private fun checkNeighborhood(frame: Bitmap, centerX: Int, centerY: Int, radius: Int): Float {
        var totalScore = 0f
        var count = 0
        
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = centerX + dx
                val y = centerY + dy
                
                if (x >= 0 && x < frame.width && y >= 0 && y < frame.height) {
                    val pixel = frame.getPixel(x, y)
                    totalScore += calculateWhiteScore(pixel)
                    count++
                }
            }
        }
        
        return if (count > 0) totalScore / count else 0f
    }
}
