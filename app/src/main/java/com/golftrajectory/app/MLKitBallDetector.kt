package com.golftrajectory.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

/**
 * ML Kitを使った自動ボール検出
 */
class MLKitBallDetector {
    
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .build()
    
    private val objectDetector = ObjectDetection.getClient(options)
    
    data class DetectionResult(
        val position: Offset,
        val confidence: Float,
        val timestamp: Long
    )
    
    /**
     * ビットマップから物体を検出
     */
    suspend fun detectBall(bitmap: Bitmap, timestamp: Long): DetectionResult? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val objects = objectDetector.process(image).await()
            
            // 検出された物体の中から最も小さいもの（ボールの可能性が高い）を選択
            val smallestObject = objects
                .filter { it.boundingBox.width() < bitmap.width / 10 } // 画面の1/10以下のサイズ
                .minByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            
            smallestObject?.let { obj ->
                val centerX = obj.boundingBox.centerX().toFloat()
                val centerY = obj.boundingBox.centerY().toFloat()
                
                DetectionResult(
                    position = Offset(centerX, centerY),
                    confidence = obj.trackingId?.toFloat() ?: 0.5f,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 簡易的な色ベース検出（フォールバック）
     */
    fun detectWhiteBall(bitmap: Bitmap, timestamp: Long): DetectionResult? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            var maxWhiteScore = 0f
            var bestX = 0
            var bestY = 0
            
            // グリッドサンプリング
            val step = 20
            for (y in 0 until height step step) {
                for (x in 0 until width step step) {
                    if (x >= width || y >= height) continue
                    
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    
                    // 白さスコア
                    val brightness = (r + g + b) / 3f / 255f
                    val balance = 1f - (abs(r - g) + abs(g - b) + abs(b - r)) / 765f
                    val whiteScore = brightness * 0.7f + balance * 0.3f
                    
                    if (whiteScore > maxWhiteScore) {
                        maxWhiteScore = whiteScore
                        bestX = x
                        bestY = y
                    }
                }
            }
            
            if (maxWhiteScore > 0.7f) {
                return DetectionResult(
                    position = Offset(bestX.toFloat(), bestY.toFloat()),
                    confidence = maxWhiteScore,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    fun close() {
        objectDetector.close()
    }
}
