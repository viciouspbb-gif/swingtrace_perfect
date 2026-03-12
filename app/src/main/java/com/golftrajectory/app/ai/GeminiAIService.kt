package com.golftrajectory.app.ai

import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini AIサービスの唯一の実装クラス
 * GenerativeModelの生成はこのクラスのみが担当
 */
@Singleton
class GeminiAIService @Inject constructor(
    private val geminiApiKeyProvider: GeminiApiKeyProvider
) {
    
    private val flashModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = geminiApiKeyProvider.getApiKey()
        )
    }
    
    private val proModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = geminiApiKeyProvider.getApiKey()
        )
    }
    
    /**
     * スイングフェーズ分類（ATHLETE用）
     */
    suspend fun classifyPhase(swingData: String): Result<String> {
        return try {
            val prompt = buildPhasePrompt(swingData)
            val response = flashModel.generateContent(prompt)
            Result.success(response.text ?: "UNKNOWN")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * スイングフェーズ分類（PRO用）
     */
    suspend fun classifyPhaseWithPro(swingData: String): Result<String> {
        return try {
            val prompt = buildPhasePrompt(swingData)
            val response = proModel.generateContent(prompt)
            Result.success(response.text ?: "UNKNOWN")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AIコメント生成（ATHLETE用）
     */
    suspend fun generateComment(swingData: String, phase: String): Result<String> {
        return try {
            val prompt = buildCommentPrompt(swingData, phase)
            val response = flashModel.generateContent(prompt)
            Result.success(response.text ?: "分析できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AIコメント生成（PRO用）
     */
    suspend fun generateCommentWithPro(swingData: String, phase: String): Result<String> {
        return try {
            val prompt = buildCommentPrompt(swingData, phase)
            val response = proModel.generateContent(prompt)
            Result.success(response.text ?: "分析できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AIコーチ会話（ATHLETE用）
     */
    suspend fun chatWithCoach(message: String): Result<String> {
        return try {
            val prompt = buildChatPrompt(message)
            val response = flashModel.generateContent(prompt)
            Result.success(response.text ?: "応答できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AIコーチ会話（PRO用）
     */
    suspend fun chatWithCoachWithPro(message: String): Result<String> {
        return try {
            val prompt = buildChatPrompt(message)
            val response = proModel.generateContent(prompt)
            Result.success(response.text ?: "応答できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildPhasePrompt(swingData: String): String {
        return """
            分析するスイングデータ: $swingData
            
            以下のフェーズに分類してください:
            - TAKEBACK (テイクバック)
            - DOWNSWING (ダウンスイング)  
            - FOLLOW (フォロー)
            
            フェーズ名のみで回答してください。
        """.trimIndent()
    }
    
    private fun buildCommentPrompt(swingData: String, phase: String): String {
        return """
            スイングデータ: $swingData
            フェーズ: $phase
            
            ゴルフスイングについて簡潔なアドバイスを日本語で教えてください。
        """.trimIndent()
    }
    
    private fun buildChatPrompt(message: String): String {
        return """
            ゴルフコーチとして以下の質問に日本語で答えてください:
            $message
        """.trimIndent()
    }
}
