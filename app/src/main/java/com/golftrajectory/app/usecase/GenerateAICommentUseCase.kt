package com.golftrajectory.app.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Gemini AIでスイングコメント生成UseCase
 */
class GenerateAICommentUseCase(
    private val geminiApiKey: String
) {
    
    data class AiComment(
        val goodPoint: String,
        val improvement: String,
        val advice: String
    )
    
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = geminiApiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 500
            }
        )
    }
    
    /**
     * スイング分析結果からAIコメントを生成
     */
    suspend fun generateComment(
        score: CalculateSwingScoreUseCase.SwingScore,
        shoulderAngle: Float = 85f,
        hipAngle: Float = 35f
    ): Result<AiComment> {
        return try {
            val prompt = buildPrompt(score, shoulderAngle, hipAngle)
            val response = model.generateContent(prompt)
            val text = response.text ?: return Result.failure(Exception("レスポンスが空です"))
            
            val comment = parseResponse(text)
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * プロンプトを構築
     */
    private fun buildPrompt(
        score: CalculateSwingScoreUseCase.SwingScore,
        shoulderAngle: Float,
        hipAngle: Float
    ): String {
        return buildString {
            append("以下はゴルフスイングの解析結果です。\n")
            append("クラブヘッド軌道、身体の姿勢、回転角度、インパクトタイミングなどが含まれます。\n")
            append("このデータをもとに、スイングの良い点・改善点・アドバイスを日本語で簡潔にコメントしてください。\n")
            append("出力形式：\n")
            append("良い点：...\n")
            append("改善点：...\n")
            append("アドバイス：...\n\n")
            
            append("【解析データ】\n")
            append("総合スコア：${score.totalScore}点\n")
            append("クラブ軌道の滑らかさ：${String.format("%.2f", score.smoothnessScore)}（1.0が最高）\n")
            append("姿勢の安定性：${String.format("%.2f", score.poseStability)}（1.0が最高）\n")
            append("肩の回転角度：${String.format("%.1f", shoulderAngle)}°（理想：80〜100°）\n")
            append("腰の回転角度：${String.format("%.1f", hipAngle)}°（理想：30〜45°）\n")
            append("インパクトタイミング：${String.format("%.2f", score.impactTimingScore)}（1.0が最高）\n")
            append("回転スコア：${String.format("%.2f", score.rotationScore)}（1.0が最高）\n")
        }
    }
    
    /**
     * Geminiのレスポンスをパース
     */
    private fun parseResponse(text: String): AiComment {
        val lines = text.lines()
        
        var goodPoint = ""
        var improvement = ""
        var advice = ""
        
        for (line in lines) {
            when {
                line.startsWith("良い点：") || line.startsWith("・良い点：") -> {
                    goodPoint = line.substringAfter("：").trim()
                }
                line.startsWith("改善点：") || line.startsWith("・改善点：") -> {
                    improvement = line.substringAfter("：").trim()
                }
                line.startsWith("アドバイス：") || line.startsWith("・アドバイス：") -> {
                    advice = line.substringAfter("：").trim()
                }
            }
        }
        
        // フォールバック：パースに失敗した場合
        if (goodPoint.isEmpty() && improvement.isEmpty() && advice.isEmpty()) {
            val parts = text.split("\n\n")
            return AiComment(
                goodPoint = parts.getOrNull(0) ?: "分析中...",
                improvement = parts.getOrNull(1) ?: "分析中...",
                advice = parts.getOrNull(2) ?: "分析中..."
            )
        }
        
        return AiComment(
            goodPoint = goodPoint.ifEmpty { "スイングデータを分析中..." },
            improvement = improvement.ifEmpty { "分析中..." },
            advice = advice.ifEmpty { "分析中..." }
        )
    }
}
