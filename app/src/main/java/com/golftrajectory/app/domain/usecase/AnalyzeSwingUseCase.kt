package com.swingtrace.aicoaching.domain.usecase

import android.net.Uri
import com.swingtrace.aicoaching.ai.GeminiAIManager
import com.swingtrace.aicoaching.ai.SwingAnalysisData
import com.swingtrace.aicoaching.analysis.ProSimilarityCalculator
import com.swingtrace.aicoaching.database.AnalysisHistoryEntity
import com.swingtrace.aicoaching.database.AppDatabase
import com.swingtrace.aicoaching.repository.SwingAnalysisRepository
import com.swingtrace.aicoaching.utils.DistanceEstimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * スイング分析のユースケース
 */
class AnalyzeSwingUseCase(
    private val repository: SwingAnalysisRepository,
    private val database: AppDatabase,
    private val aiManager: GeminiAIManager
) {
    
    /**
     * スイング動画を分析
     */
    suspend fun execute(
        videoUri: Uri,
        userId: String,
        isPremium: Boolean,
        targetProName: String? = null
    ): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        try {
            // TODO: サーバー統合時に実装
            // 現在は仮のデータを返す
            val backswingAngle = 80.0
            val downswingSpeed = 45.0
            val headStability = 75.0
            val weightShift = 50.0
            
            // 推定飛距離を計算
            val estimatedDistance = DistanceEstimator.estimateDistanceFromSwingData(
                downswingSpeed = downswingSpeed,
                backswingAngle = backswingAngle,
                headStability = headStability,
                weightTransfer = weightShift
            )
            
            val swingData = SwingData(
                totalScore = 75,
                backswingAngle = backswingAngle,
                downswingSpeed = downswingSpeed,
                hipRotation = 40.0,
                shoulderRotation = 55.0,
                headStability = headStability,
                weightShift = weightShift,
                swingPlane = "正常",
                estimatedDistance = estimatedDistance
            )
            
            // 2. プロ類似度を計算
            val similarities = ProSimilarityCalculator.calculateSimilarities(
                backswingAngle = swingData.backswingAngle,
                downswingSpeed = swingData.downswingSpeed,
                hipRotation = swingData.hipRotation,
                shoulderRotation = swingData.shoulderRotation,
                headStability = swingData.headStability,
                weightTransfer = swingData.weightShift
            )
            val topPro = similarities.firstOrNull()
            
            // 3. AIコーチング（有料版のみ）
            val aiAdvice = if (isPremium) {
                val swingAnalysisData = SwingAnalysisData(
                    totalScore = swingData.totalScore,
                    backswingAngle = swingData.backswingAngle,
                    downswingSpeed = swingData.downswingSpeed,
                    hipRotation = swingData.hipRotation,
                    shoulderRotation = swingData.shoulderRotation,
                    headStability = swingData.headStability,
                    weightShift = swingData.weightShift,
                    swingPlane = swingData.swingPlane
                )
                
                aiManager.generateCoachingAdvice(
                    swingData = swingAnalysisData,
                    targetProName = targetProName
                )
            } else {
                null
            }
            
            // 4. データベースに保存
            val historyEntity = AnalysisHistoryEntity(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                videoUri = videoUri.toString(),
                ballDetected = false,
                carryDistance = 0.0,
                maxHeight = 0.0,
                flightTime = 0.0,
                confidence = 0.0,
                aiAdvice = aiAdvice,
                aiScore = swingData.totalScore,
                swingSpeed = null,
                backswingTime = null,
                downswingTime = null,
                impactSpeed = null,
                tempo = null,
                totalScore = swingData.totalScore,
                backswingAngle = swingData.backswingAngle,
                downswingSpeed = swingData.downswingSpeed,
                hipRotation = swingData.hipRotation,
                shoulderRotation = swingData.shoulderRotation,
                headStability = swingData.headStability,
                weightTransfer = swingData.weightShift,
                swingPlane = swingData.swingPlane,
                topProName = topPro?.pro?.name,
                topProSimilarity = topPro?.similarity
            )
            
            database.analysisHistoryDao().insert(historyEntity)
            
            Result.success(
                AnalysisResult(
                    swingData = swingData,
                    aiAdvice = aiAdvice,
                    historyEntity = historyEntity
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 分析結果
 */
data class AnalysisResult(
    val swingData: SwingData,
    val aiAdvice: String?,
    val historyEntity: AnalysisHistoryEntity
)

/**
 * スイングデータ（仮の定義）
 */
data class SwingData(
    val totalScore: Int,
    val backswingAngle: Double,
    val downswingSpeed: Double,
    val hipRotation: Double,
    val shoulderRotation: Double,
    val headStability: Double,
    val weightShift: Double,
    val swingPlane: String,
    val estimatedDistance: Double = 0.0,  // 推定飛距離（ヤード）
    val clubType: com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType = com.swingtrace.aicoaching.utils.DistanceEstimator.ClubType.DRIVER,  // クラブタイプ
    
    // 詳細分析データ
    val kneeStability: Double = 0.0,      // 膝の安定性
    val ankleBalance: Double = 0.0,       // 足首のバランス
    val wristCocking: Double = 0.0,       // リストコック角度
    val eyeOnBall: Double = 0.0,          // ボールへの視線
    val lowerBodyPower: Double = 0.0,     // 下半身のパワー
    val upperBodySync: Double = 0.0,      // 上半身の同期性
    val followThrough: Double = 0.0,      // フォロースルー
    val posture: Double = 0.0,            // 姿勢
    val technicalScore: Double = 0.0,     // 技術スコア
    val powerScore: Double = 0.0,         // パワースコア
    val consistencyScore: Double = 0.0    // 一貫性スコア
)
