package com.golftrajectory.app.ai

import android.content.Context
import android.util.Log
import com.golftrajectory.app.plan.Plan
import com.golftrajectory.app.plan.UserPlanManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AIサービスの統一エントリーポイント
 * Plan判定に基づき実行/遮断する
 */
@Singleton
class AIServiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiService: GeminiAIService,
    private val cloudAIService: CloudAIService,
    private val geminiApiKeyProvider: GeminiApiKeyProvider,
    private val userPlanManager: UserPlanManager
) {
    
    companion object {
        private var httpCallCount = 0
        private var analysisSessionCount = 0
    }
    
    /**
     * スイングフェーズ分類（解析フローの一部）
     */
    suspend fun classifyPhase(swingData: String): Result<String> = withContext(Dispatchers.IO) {
        val plan = userPlanManager.getCurrentPlan()
        
        return@withContext when (plan) {
            Plan.PRACTICE -> {
                // PRACTICEは通信0回（絶対）
                Log.d("AI_SERVICE", "PRACTICE: classifyPhase blocked - 0 HTTP calls")
                Result.failure(PlanUpgradeRequired("フェーズ分類はATHLETEプラン以上で利用できます"))
            }
            Plan.ATHLETE -> {
                incrementHttpCallCount()
                Log.d("AI_SERVICE", "ATHLETE: classifyPhase via flash - HTTP call #$httpCallCount")
                geminiService.classifyPhase(swingData)
            }
            Plan.PRO -> {
                if (userPlanManager.isUseCloud()) {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO+Cloud: classifyPhase via CloudAIService - HTTP call #$httpCallCount")
                    cloudAIService.classifyPhase(swingData)
                } else {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO: classifyPhase via pro - HTTP call #$httpCallCount")
                    geminiService.classifyPhaseWithPro(swingData)
                }
            }
        }
    }
    
    /**
     * AIコメント生成（解析フローの一部）
     */
    suspend fun generateComment(swingData: String, phase: String): Result<String> = withContext(Dispatchers.IO) {
        val plan = userPlanManager.getCurrentPlan()
        
        return@withContext when (plan) {
            Plan.PRACTICE -> {
                // PRACTICEは通信0回（絶対）
                Log.d("AI_SERVICE", "PRACTICE: generateComment blocked - 0 HTTP calls")
                Result.failure(PlanUpgradeRequired("AIコメントはATHLETEプラン以上で利用できます"))
            }
            Plan.ATHLETE -> {
                incrementHttpCallCount()
                Log.d("AI_SERVICE", "ATHLETE: generateComment via flash - HTTP call #$httpCallCount")
                geminiService.generateComment(swingData, phase)
            }
            Plan.PRO -> {
                if (userPlanManager.isUseCloud()) {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO+Cloud: generateComment via CloudAIService - HTTP call #$httpCallCount")
                    cloudAIService.generateComment(swingData, phase)
                } else {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO: generateComment via pro - HTTP call #$httpCallCount")
                    geminiService.generateCommentWithPro(swingData, phase)
                }
            }
        }
    }
    
    /**
     * 解析フロー実行（phase + comment の2回に限定）
     */
    suspend fun executeAnalysisFlow(swingData: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        startAnalysisSession()
        
        try {
            val phaseResult = classifyPhase(swingData)
            val phase = phaseResult.getOrNull() ?: "UNKNOWN"
            
            val commentResult = generateComment(swingData, phase)
            val comment = commentResult.getOrNull() ?: "分析できませんでした"
            
            Log.d("AI_SERVICE", "解析実行完了: HTTP $httpCallCount 回（セッション#$analysisSessionCount）")
            Result.success(Pair(phase, comment))
        } catch (e: Exception) {
            Log.d("AI_SERVICE", "解析実行完了: HTTP $httpCallCount 回（セッション#$analysisSessionCount） - 失敗")
            Result.failure(e)
        }
    }
    
    /**
     * AIコーチ会話（解析とは別トリガー）
     */
    suspend fun chatWithCoach(message: String): Result<String> = withContext(Dispatchers.IO) {
        val plan = userPlanManager.getCurrentPlan()
        
        return@withContext when (plan) {
            Plan.PRACTICE -> {
                // PRACTICEは通信0回（絶対）
                Log.d("AI_SERVICE", "PRACTICE: chatWithCoach blocked - 0 HTTP calls")
                Result.failure(PlanUpgradeRequired("AIコーチはATHLETEプラン以上で利用できます"))
            }
            Plan.ATHLETE -> {
                incrementHttpCallCount()
                Log.d("AI_SERVICE", "ATHLETE: chatWithCoach via flash - HTTP call #$httpCallCount (会話別)")
                geminiService.chatWithCoach(message)
            }
            Plan.PRO -> {
                if (userPlanManager.isUseCloud()) {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO+Cloud: chatWithCoach via CloudAIService - HTTP call #$httpCallCount (会話別)")
                    cloudAIService.chatWithCoach(message)
                } else {
                    incrementHttpCallCount()
                    Log.d("AI_SERVICE", "PRO: chatWithCoach via pro - HTTP call #$httpCallCount (会話別)")
                    geminiService.chatWithCoachWithPro(message)
                }
            }
        }
    }
    
    private fun incrementHttpCallCount() {
        httpCallCount++
    }
    
    private fun startAnalysisSession() {
        analysisSessionCount++
        Log.d("AI_SERVICE", "解析セッション開始#$analysisSessionCount")
    }
    
    /**
     * HTTP呼び出し統計を取得
     */
    fun getHttpCallStats(): Pair<Int, Int> = Pair(httpCallCount, analysisSessionCount)
}

/**
 * プランアップグレード要求例外
 */
class PlanUpgradeRequired(message: String) : Exception(message)
