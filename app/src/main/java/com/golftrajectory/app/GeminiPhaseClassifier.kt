package com.golftrajectory.app

import android.content.Context
import android.util.Log
import com.golftrajectory.app.ai.PlanUpgradeRequired
import com.golftrajectory.app.ai.PhaseClassificationResult
import com.golftrajectory.app.ai.AIServiceRepository
import com.golftrajectory.app.ai.GeminiApiKeyProvider
import com.golftrajectory.app.ai.UserPlanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Geminiフェーズ分類器
 */
class GeminiPhaseClassifier(
    private val context: Context,
    private val aiServiceRepository: AIServiceRepository,
    private val userPlanManager: UserPlanManager
) {
    
    private val TAG = "GeminiPhaseClassifier"
    
    /**
     * スイングフェーズを分類
     */
    suspend fun classifyPhase(trajectoryPoints: List<String>): Result<PhaseClassificationResult> = withContext(Dispatchers.IO) {
        try {
            // プランチェック
            val currentPlan = userPlanManager.getCurrentPlan()
            if (currentPlan.name == "PRACTICE") {
                return@withContext Result.failure(PlanUpgradeRequired("フェーズ分類にはATHLETEプラン以上が必要です"))
            }
            
            val result = aiServiceRepository.classifyPhase(trajectoryPoints)
            result.fold(
                onSuccess = { phase ->
                    Result.success(
                        PhaseClassificationResult(
                            phase = phase,
                            confidence = 0.8f,
                            reasoning = "AIによる分類"
                        )
                    )
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: PlanUpgradeRequired) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
