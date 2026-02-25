package com.golftrajectory.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * ボール追跡システム
 * 動画内のボールの軌跡を記録・表示
 */
class BallTracker {
    private val trackedPoints = mutableListOf<TrackedPoint>()
    
    data class TrackedPoint(
        val position: Offset,
        val timestamp: Long,
        val confidence: Float = 1f
    )
    
    /**
     * ボール位置を追加
     */
    fun addPoint(x: Float, y: Float, timestamp: Long) {
        trackedPoints.add(TrackedPoint(Offset(x, y), timestamp))
        
        // 最大100ポイントまで保持
        if (trackedPoints.size > 100) {
            trackedPoints.removeAt(0)
        }
    }
    
    /**
     * 軌跡をクリア
     */
    fun clear() {
        trackedPoints.clear()
    }
    
    /**
     * 追跡ポイントを取得
     */
    fun getPoints(): List<TrackedPoint> {
        return trackedPoints.toList()
    }
    
    /**
     * 指定時間範囲のポイントを取得
     */
    fun getPointsInTimeRange(startTime: Long, endTime: Long): List<TrackedPoint> {
        return trackedPoints.filter { it.timestamp in startTime..endTime }
    }
    
    /**
     * 最新のポイントを取得
     */
    fun getLatestPoint(): TrackedPoint? {
        return trackedPoints.lastOrNull()
    }
    
    /**
     * ポイント数を取得
     */
    fun getPointCount(): Int {
        return trackedPoints.size
    }
    
    /**
     * 軌跡の色を取得（進行度に応じて変化）
     */
    fun getColorForProgress(progress: Float): Color {
        val colors = listOf(
            Color(0xFF00FF00), // 緑
            Color(0xFFFFFF00), // 黄
            Color(0xFFFF9800), // オレンジ
            Color(0xFFFF5722)  // 赤
        )
        
        val index = (progress * (colors.size - 1)).toInt().coerceIn(0, colors.size - 2)
        val localProgress = (progress * (colors.size - 1)) - index
        
        return androidx.compose.ui.graphics.lerp(
            colors[index],
            colors[index + 1],
            localProgress
        )
    }
}

/**
 * 簡易的なボール検出（タップベース）
 * 実際のコンピュータビジョンによる自動検出は高度な実装が必要
 */
class SimpleBallDetector {
    
    /**
     * タップ位置をボール位置として記録
     * 実際のアプリでは、画像処理やMLを使用してボールを自動検出
     */
    fun detectBallAtPosition(x: Float, y: Float): Offset {
        return Offset(x, y)
    }
    
    /**
     * 色ベースの簡易検出（白いボールを検出）
     * 注: 実際の実装にはOpenCVやML Kitが必要
     */
    fun detectWhiteBall(frameData: ByteArray, width: Int, height: Int): Offset? {
        // プレースホルダー実装
        // 実際にはフレームデータを解析してボールを検出
        return null
    }
}
