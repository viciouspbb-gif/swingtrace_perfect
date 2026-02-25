package com.golftrajectory.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 動画処理クラス
 * 
 * @param context アプリケーションコンテキスト
 * @param detector クラブヘッド検出器
 * @param uiConfidenceThreshold UI表示用の信頼度閾値（デフォルト: 0.25f = 25%）
 */
class VideoProcessor(
    private val context: Context,
    private val detector: ClubHeadDetector,
    private val uiConfidenceThreshold: Float = 0.25f
) {
    
    /**
     * 検出統計情報
     */
    data class DetectionStats(
        val totalFrames: Int,              // 総フレーム数
        val detectedFrames: Int,           // 検出されたフレーム数（0.001f以上）
        val adoptedFrames: Int,            // 採用されたフレーム数（UI閾値以上）
        val averageConfidence: Float,      // 平均信頼度（検出されたもののみ）
        val maxConfidence: Float,          // 最大信頼度
        val minConfidence: Float,          // 最小信頼度（検出されたもののみ）
        val detectionRate: Float,          // 検出率 (detectedFrames / totalFrames)
        val adoptionRate: Float,           // 採用率 (adoptedFrames / totalFrames)
        val confidenceDistribution: Map<String, Int>  // 信頼度分布
    ) {
        /**
         * 統計情報を人間が読める形式で出力
         */
        fun toReadableString(): String {
            return """
                |📊 検出統計:
                |  総フレーム数: $totalFrames
                |  検出数: $detectedFrames (${(detectionRate * 100).toInt()}%)
                |  採用数: $adoptedFrames (${(adoptionRate * 100).toInt()}%)
                |  平均信頼度: ${(averageConfidence * 100).toInt()}%
                |  最大信頼度: ${(maxConfidence * 100).toInt()}%
                |  最小信頼度: ${(minConfidence * 100).toInt()}%
                |  信頼度分布:
                |    0-10%: ${confidenceDistribution["0-10%"] ?: 0}
                |    10-25%: ${confidenceDistribution["10-25%"] ?: 0}
                |    25-50%: ${confidenceDistribution["25-50%"] ?: 0}
                |    50-75%: ${confidenceDistribution["50-75%"] ?: 0}
                |    75-100%: ${confidenceDistribution["75-100%"] ?: 0}
            """.trimMargin()
        }
    }
    
    /**
     * 処理結果
     */
    data class ProcessResult(
        val trajectoryPoints: List<PointF>,        // UI表示用（閾値以上）
        val stats: DetectionStats,                 // 統計情報
        val allDetections: List<ClubHeadDetection> // 全検出結果（デバッグ用）
    )
    
    /**
     * 動画を処理してクラブヘッド軌道を検出
     */
    suspend fun processVideo(videoUri: Uri): ProcessResult = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        
        try {
            // 動画情報を取得
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            
            val frameIntervalUs = FRAME_INTERVAL_US
            
            val trajectoryPoints = mutableListOf<PointF>()
            val allDetections = mutableListOf<ClubHeadDetection>()  // 全検出結果
            
            // 統計用カウンタ
            var totalFrames = 0
            var detectedCount = 0  // Detector閾値(0.001f)以上
            var adoptedCount = 0   // UI閾値以上
            var confidenceSum = 0f
            var maxConf = 0f
            var minConf = Float.MAX_VALUE
            val confidenceRanges = mutableMapOf(
                "0-10%" to 0,
                "10-25%" to 0,
                "25-50%" to 0,
                "50-75%" to 0,
                "75-100%" to 0
            )
            
            val frameStream = frameSequence(
                retriever = retriever,
                durationMs = durationMs,
                frameIntervalUs = frameIntervalUs,
                maxEdge = MAX_FRAME_EDGE_PX
            )
            
            for (chunk in frameStream) {
                totalFrames++
                try {
                    // クラブヘッド検出
                    val detection = detector.detect(chunk.bitmap)
                    
                    if (detection != null) {
                        detectedCount++
                        allDetections.add(detection)
                        
                        // 統計情報を更新
                        confidenceSum += detection.confidence
                        maxConf = maxOf(maxConf, detection.confidence)
                        minConf = minOf(minConf, detection.confidence)
                        
                        // 信頼度分布を更新
                        val range = when {
                            detection.confidence >= 0.75f -> "75-100%"
                            detection.confidence >= 0.5f -> "50-75%"
                            detection.confidence >= 0.25f -> "25-50%"
                            detection.confidence >= 0.1f -> "10-25%"
                            else -> "0-10%"
                        }
                        confidenceRanges[range] = (confidenceRanges[range] ?: 0) + 1
                        
                        // 2段階フィルタリング: UI表示用閾値でフィルタ
                        if (detection.confidence >= uiConfidenceThreshold) {
                            adoptedCount++
                            trajectoryPoints.add(detection.position)
                        }
                    }
                } finally {
                    chunk.bitmap.recycle()
                }
            }
            
            // 統計情報を作成
            val stats = DetectionStats(
                totalFrames = totalFrames,
                detectedFrames = detectedCount,
                adoptedFrames = adoptedCount,
                averageConfidence = if (detectedCount > 0) confidenceSum / detectedCount else 0f,
                maxConfidence = if (detectedCount > 0) maxConf else 0f,
                minConfidence = if (detectedCount > 0) minConf else 0f,
                detectionRate = if (totalFrames > 0) detectedCount.toFloat() / totalFrames else 0f,
                adoptionRate = if (totalFrames > 0) adoptedCount.toFloat() / totalFrames else 0f,
                confidenceDistribution = confidenceRanges
            )
            
            ProcessResult(trajectoryPoints, stats, allDetections)
        } finally {
            retriever.release()
        }
    }
    
    private fun frameSequence(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        frameIntervalUs: Long,
        maxEdge: Int
    ): Sequence<FrameChunk> = sequence {
        for (timeUs in 0 until durationMs * 1000 step frameIntervalUs) {
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            frame?.let {
                val prepared = it.downscaleIfNeeded(maxEdge)
                if (prepared !== it) {
                    it.recycle()
                }
                yield(FrameChunk(timeUs, prepared))
            }
        }
    }
    
    private fun Bitmap.downscaleIfNeeded(maxEdge: Int): Bitmap {
        val largestEdge = max(width, height)
        if (largestEdge <= maxEdge) return this
        
        val scale = maxEdge.toFloat() / largestEdge
        val scaledWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    }
    
    private data class FrameChunk(
        val timeUs: Long,
        val bitmap: Bitmap
    )
    
    companion object {
        private const val FRAME_INTERVAL_US = 33_000L
        private const val MAX_FRAME_EDGE_PX = 1280
    }
}
