package com.golftrajectory.app.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AIサービスリポジトリ
 */
class AIServiceRepository(
    private val context: Context,
    private val geminiApiKeyProvider: GeminiApiKeyProvider
) {
    
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = geminiApiKeyProvider.getApiKey()
        )
    }
    
    /**
     * フェーズ分類を実行
     */
    suspend fun classifyPhase(trajectory: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                appendLine("ゴルフスイングの軌跡データを分析してください。")
                appendLine("軌跡ポイント: ${trajectory.joinToString(", ")}")
                appendLine("このスイングのフェーズ（アドレス、テイクバック、トップ、ダウン、インパクト、フォロースルー）を特定してください。")
                appendLine("回答はフェーズ名のみで返してください。")
            }
            
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "Unknown")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * AIコメントを生成
     */
    suspend fun generateComment(phase: String, trajectory: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                appendLine("ゴルフスイングの${phase}フェーズについて分析してください。")
                appendLine("軌跡データ: ${trajectory.joinToString(", ")}")
                appendLine("具体的な改善点を日本語で簡潔に教えてください。")
            }
            
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "分析できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * コーチング対話
     */
    suspend fun chatWithCoach(message: String, history: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildString {
                appendLine("ゴルフコーチとして回答してください。")
                appendLine("会話履歴: ${history.joinToString("\n")}")
                appendLine("ユーザー: $message")
                appendLine("プロのゴルフコーチとして、親切丁寧に日本語で回答してください。")
            }
            
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "回答できませんでした")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
