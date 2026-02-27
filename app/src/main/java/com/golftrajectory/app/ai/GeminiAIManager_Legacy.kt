package com.golftrajectory.app.ai

import android.content.Context
import android.util.Log
import com.golftrajectory.app.plan.Plan
import com.golftrajectory.app.plan.UserPlanManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Gemini AI Manager（レガシー互換性のため残すが、内部はAIServiceRepositoryに委譲）
 */
class GeminiAIManager @Inject constructor(
    private val context: Context,
    private val aiServiceRepository: AIServiceRepository,
    private val userPlanManager: UserPlanManager
) {
    companion object {
        private const val TAG = "GeminiAIManager"
    }
    
    /**
     * スイング分析を実行
     */
    suspend fun analyzeSwing(
        swingData: String,
        targetProName: String = "",
        coachingStyle: String = "standard"
    ): String = withContext(Dispatchers.IO) {
        try {
            val plan = userPlanManager.getCurrentPlan()
            Log.d(TAG, "Plan: $plan, Style: $coachingStyle")
            
            when (plan) {
                Plan.PRACTICE -> {
                    Log.d(TAG, "PRACTICE: analyzeSwing blocked - 0 HTTP calls")
                    "スイング分析はATHLETEプラン以上で利用できます"
                }
                else -> {
                    // ATHLETE/PROはAIServiceRepository経由で実行
                    val result = aiServiceRepository.generateComment(swingData, "swing")
                    result.getOrElse { "分析に失敗しました" }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze swing", e)
            "分析中にエラーが発生しました"
        }
    }
    
    /**
     * チャットを開始
     */
    suspend fun startChat(): String = withContext(Dispatchers.IO) {
        try {
            val plan = userPlanManager.getCurrentPlan()
            Log.d(TAG, "Plan: $plan, startChat")
            
            when (plan) {
                Plan.PRACTICE -> {
                    Log.d(TAG, "PRACTICE: startChat blocked - 0 HTTP calls")
                    "AIコーチはATHLETEプラン以上で利用できます"
                }
                else -> {
                    val result = aiServiceRepository.chatWithCoach("こんにちは")
                    result.getOrElse { "応答できませんでした" }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start chat", e)
            "チャット開始に失敗しました"
        }
    }
    
    /**
     * メッセージを送信
     */
    suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
        try {
            val plan = userPlanManager.getCurrentPlan()
            Log.d(TAG, "Plan: $plan, sendMessage")
            
            when (plan) {
                Plan.PRACTICE -> {
                    Log.d(TAG, "PRACTICE: sendMessage blocked - 0 HTTP calls")
                    "AIコーチはATHLETEプラン以上で利用できます"
                }
                else -> {
                    val result = aiServiceRepository.chatWithCoach(message)
                    result.getOrElse { "応答できませんでした" }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            "メッセージ送信に失敗しました"
        }
    }
}
