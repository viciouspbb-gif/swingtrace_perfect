package com.golftrajectory.app.usecase

import android.graphics.Bitmap
import com.golftrajectory.app.ClubHeadDetector
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * クラブヘッド軌道 × MediaPipe 統合分析UseCase
 */
class IntegratedSwingAnalysisUseCase(
    private val clubHeadDetector: ClubHeadDetector,
    private val detectClubHeadUseCase: DetectClubHeadUseCase,
    private val classifySwingPhaseUseCase: ClassifySwingPhaseUseCase
) {
    
    data class IntegratedResult(
        // クラブヘッド軌道
        val clubHeadTrajectory: List<RecordTrajectoryUseCase.FrameData>,
        val clubHeadPhases: List<ClassifySwingPhaseUseCase.SwingPhase>,
        val clubHeadSpeed: Float,
        
        // 身体ポーズ（MediaPipe統合時）
        val bodyPose: BodyPoseData? = null,
        
        // 統合分析
        val synchronization: SyncScore? = null,
        val timing: TimingAnalysis? = null,
        val powerTransfer: PowerScore? = null
    )
    
    data class BodyPoseData(
        val hipAngle: Float,
        val shoulderAngle: Float,
        val armAngle: Float,
        val kneeAngle: Float
    )
    
    data class SyncScore(
        val score: Float,  // 0-100
        val message: String
    )
    
    data class TimingAnalysis(
        val sequence: List<Pair<String, Long>>,
        val isCorrect: Boolean,
        val message: String
    )
    
    data class PowerScore(
        val efficiency: Float,  // 0-1
        val breakdown: Map<String, Float>
    )
    
    /**
     * 統合分析を実行
     */
    suspend fun analyze(
        bitmap: Bitmap,
        trajectory: List<RecordTrajectoryUseCase.FrameData>
    ): Result<IntegratedResult> = coroutineScope {
        try {
            // クラブヘッド検出
            val clubHeadJob = async {
                detectClubHeadUseCase.execute(bitmap)
            }
            
            // MediaPipe ポーズ検出（将来実装）
            // val poseJob = async {
            //     poseDetector.detect(bitmap)
            // }
            
            val clubHead = clubHeadJob.await().getOrNull()
            // val pose = poseJob.await()
            
            if (clubHead == null) {
                return@coroutineScope Result.failure(
                    Exception("クラブヘッド検出失敗")
                )
            }
            
            // フェーズ分類
            val phases = classifySwingPhaseUseCase.classifyLocally(trajectory)
            
            // クラブヘッド速度計算
            val speed = calculateClubHeadSpeed(trajectory)
            
            // 統合分析（MediaPipe統合時に有効化）
            // val sync = analyzeSynchronization(trajectory, pose)
            // val timing = analyzeTiming(trajectory, pose)
            // val power = analyzePowerTransfer(trajectory, pose)
            
            Result.success(
                IntegratedResult(
                    clubHeadTrajectory = trajectory,
                    clubHeadPhases = phases,
                    clubHeadSpeed = speed,
                    bodyPose = null,  // MediaPipe統合時に実装
                    synchronization = null,
                    timing = null,
                    powerTransfer = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * クラブヘッド速度を計算
     */
    private fun calculateClubHeadSpeed(
        trajectory: List<RecordTrajectoryUseCase.FrameData>
    ): Float {
        if (trajectory.size < 2) return 0f
        
        var maxSpeed = 0f
        
        for (i in 1 until trajectory.size) {
            val prev = trajectory[i - 1]
            val curr = trajectory[i]
            
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val dt = (curr.timeMs - prev.timeMs) / 1000f  // 秒
            
            if (dt > 0) {
                val distance = sqrt(dx * dx + dy * dy)
                val speed = distance / dt
                
                if (speed > maxSpeed) {
                    maxSpeed = speed
                }
            }
        }
        
        return maxSpeed
    }
    
    /**
     * 同期スコアを分析（MediaPipe統合時に実装）
     */
    private fun analyzeSynchronization(
        clubHead: List<RecordTrajectoryUseCase.FrameData>,
        bodyPose: BodyPoseData
    ): SyncScore {
        // 腰の回転とクラブヘッド速度の相関を計算
        val correlation = 0.85f  // TODO: 実際の計算
        
        return SyncScore(
            score = correlation * 100,
            message = when {
                correlation > 0.9f -> "完璧な同期！"
                correlation > 0.7f -> "良好な同期"
                else -> "同期を改善しましょう"
            }
        )
    }
    
    /**
     * タイミング分析（MediaPipe統合時に実装）
     */
    private fun analyzeTiming(
        clubHead: List<RecordTrajectoryUseCase.FrameData>,
        bodyPose: BodyPoseData
    ): TimingAnalysis {
        // キネティックチェーンの順序を分析
        val sequence = listOf(
            "腰" to 0L,
            "肩" to 100L,
            "腕" to 200L,
            "クラブ" to 300L
        )
        
        val isCorrect = true  // TODO: 実際の判定
        
        return TimingAnalysis(
            sequence = sequence,
            isCorrect = isCorrect,
            message = if (isCorrect) {
                "完璧なキネティックチェーン！"
            } else {
                "動作順序を改善しましょう"
            }
        )
    }
    
    /**
     * パワー伝達効率を分析（MediaPipe統合時に実装）
     */
    private fun analyzePowerTransfer(
        clubHead: List<RecordTrajectoryUseCase.FrameData>,
        bodyPose: BodyPoseData
    ): PowerScore {
        // 各部位のパワー貢献度を計算
        val breakdown = mapOf(
            "地面反力" to 30f,
            "腰の回転" to 25f,
            "肩の回転" to 20f,
            "腕の振り" to 15f,
            "クラブ速度" to 10f
        )
        
        val efficiency = 0.75f  // TODO: 実際の計算
        
        return PowerScore(
            efficiency = efficiency,
            breakdown = breakdown
        )
    }
}
