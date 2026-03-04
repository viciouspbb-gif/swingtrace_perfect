package com.golftrajectory.app.detection

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.golftrajectory.app.ai.BallDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * ゼロコピー・ボール検出器
 * ImageProxyから直接処理することでBitmap変換を回避
 */
class ZeroCopyBallDetector {
    
    companion object {
        private const val TAG = "ZeroCopyBallDetector"
        private const val MIN_BALL_RADIUS = 10f
        private const val MAX_BALL_RADIUS = 50f
        private const val WHITE_THRESHOLD = 200 // 白色判定閾値
    }
    
    /**
     * ImageProxyから直接ボールを検出
     */
    suspend fun detectWhiteBallFromProxy(imageProxy: ImageProxy, timestamp: Long): BallDetection? {
        return withContext(Dispatchers.Default) {
            try {
                // 簡易的な白色領域検出（実際はImageProxyから直接ピクセルデータを処理）
                // ここでは仮実装として中央付近の白色領域を検出
                
                val width = imageProxy.width
                val height = imageProxy.height
                
                // 中央付近に仮のボール位置を返す（実際は画像処理を実装）
                val centerX = width / 2f
                val centerY = height / 2f
                
                // 検出結果を返す
                BallDetection(
                    position = RectF(
                        centerX - 20f,
                        centerY - 20f,
                        centerX + 20f,
                        centerY + 20f
                    ),
                    confidence = 0.8f,
                    timestamp = timestamp
                )
                
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Bitmapからボールを検出（互換性のため）
     */
    fun detectWhiteBall(bitmap: Bitmap): BallDetection? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // 中央付近に仮のボール位置を返す
            val centerX = width / 2f
            val centerY = height / 2f
            
            return BallDetection(
                position = RectF(
                    centerX - 20f,
                    centerY - 20f,
                    centerX + 20f,
                    centerY + 20f
                ),
                confidence = 0.8f,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            return null
        }
    }
}
