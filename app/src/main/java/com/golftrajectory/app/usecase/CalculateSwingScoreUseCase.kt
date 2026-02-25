package com.golftrajectory.app.usecase

import kotlin.math.sqrt

/**
 * スイングスコア計算UseCase
 * クラブヘッド軌道 × MediaPipe姿勢 の統合スコア
 */
class CalculateSwingScoreUseCase {
    
    data class SwingScore(
        val totalScore: Int,              // 総合スコア (0-100)
        val smoothnessScore: Float,       // 軌道の滑らかさ (0-1)
        val poseStability: Float,         // 姿勢の安定性 (0-1)
        val rotationScore: Float,         // 回転スコア (0-1)
        val impactTimingScore: Float,     // インパクトタイミング (0-1)
        val breakdown: Map<String, Float> // 詳細内訳
    )
    
    /**
     * 総合スコアを計算
     */
    fun calculateSwingScore(
        trajectory: List<RecordTrajectoryUseCase.FrameData>,
        poseStability: Float = 0.8f,      // MediaPipeから取得
        rotationScore: Float = 0.85f,     // 肩・腰の回転スコア
        impactTimingScore: Float = 0.9f   // インパクトタイミング
    ): SwingScore {
        // 軌道の滑らかさを計算
        val smoothnessScore = calculateTrajectorySmoothness(trajectory)
        
        // 重み付け総合スコア
        val finalScore = (
            smoothnessScore * 0.3f +      // 30%: 軌道の滑らかさ
            poseStability * 0.25f +       // 25%: 姿勢の安定性
            rotationScore * 0.25f +       // 25%: 回転スコア
            impactTimingScore * 0.2f      // 20%: インパクトタイミング
        ) * 100f
        
        return SwingScore(
            totalScore = finalScore.toInt().coerceIn(0, 100),
            smoothnessScore = smoothnessScore,
            poseStability = poseStability,
            rotationScore = rotationScore,
            impactTimingScore = impactTimingScore,
            breakdown = mapOf(
                "軌道の滑らかさ" to smoothnessScore,
                "姿勢の安定性" to poseStability,
                "回転スコア" to rotationScore,
                "インパクトタイミング" to impactTimingScore
            )
        )
    }
    
    /**
     * 軌道の滑らかさを計算
     * 加速度の分散が小さいほど滑らか
     */
    private fun calculateTrajectorySmoothness(
        data: List<RecordTrajectoryUseCase.FrameData>
    ): Float {
        if (data.size < 3) return 0f
        
        // 各フレーム間の距離を計算
        val deltas = data.zipWithNext { a, b ->
            val dx = b.x - a.x
            val dy = b.y - a.y
            sqrt(dx * dx + dy * dy)
        }
        
        if (deltas.size < 3) return 0f
        
        // 3フレームごとの標準偏差を計算
        val variances = deltas.windowed(3).map { window ->
            standardDeviation(window)
        }
        
        val avgVariance = variances.average()
        
        // 分散が小さいほどスコアが高い
        return (1.0f / (1.0f + avgVariance.toFloat())).coerceIn(0f, 1f)
    }
    
    /**
     * 標準偏差を計算
     */
    private fun standardDeviation(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return sqrt(variance)
    }
    
    /**
     * 姿勢の安定性を計算（MediaPipe統合時）
     */
    fun calculatePoseStability(
        landmarks: List<PoseLandmark>
    ): Float {
        // TODO: MediaPipe統合時に実装
        // 頭・肩・腰のランドマークの揺れ（標準偏差）を計算
        return 0.8f
    }
    
    /**
     * 回転スコアを計算（MediaPipe統合時）
     */
    fun calculateRotationScore(
        shoulderAngle: Float,
        hipAngle: Float
    ): Float {
        // 理想値との一致度
        val shoulderIdeal = 90f  // 80-100°
        val hipIdeal = 37.5f     // 30-45°
        
        val shoulderScore = 1f - (kotlin.math.abs(shoulderAngle - shoulderIdeal) / 20f).coerceIn(0f, 1f)
        val hipScore = 1f - (kotlin.math.abs(hipAngle - hipIdeal) / 15f).coerceIn(0f, 1f)
        
        return (shoulderScore + hipScore) / 2f
    }
    
    /**
     * インパクトタイミングスコアを計算
     */
    fun calculateImpactTimingScore(
        trajectory: List<RecordTrajectoryUseCase.FrameData>
    ): Float {
        if (trajectory.size < 3) return 0f
        
        // 最高速度地点を検出
        val speeds = trajectory.zipWithNext { a, b ->
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dt = (b.timeMs - a.timeMs) / 1000f
            if (dt > 0) sqrt(dx * dx + dy * dy) / dt else 0f
        }
        
        val maxSpeedIndex = speeds.indices.maxByOrNull { speeds[it] } ?: 0
        val impactPosition = maxSpeedIndex.toFloat() / speeds.size
        
        // 理想的なインパクト位置は60-70%付近
        val idealPosition = 0.65f
        val positionScore = 1f - kotlin.math.abs(impactPosition - idealPosition) / 0.35f
        
        return positionScore.coerceIn(0f, 1f)
    }
    
    // MediaPipe統合用のダミークラス
    data class PoseLandmark(
        val x: Float,
        val y: Float,
        val z: Float,
        val visibility: Float
    )
}
