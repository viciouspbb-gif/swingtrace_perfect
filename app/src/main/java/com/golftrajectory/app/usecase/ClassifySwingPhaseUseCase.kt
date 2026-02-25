package com.golftrajectory.app.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

/**
 * スイングフェーズ分類UseCase
 */
class ClassifySwingPhaseUseCase(
    private val geminiApiKey: String
) {
    
    enum class SwingPhase {
        TAKEBACK,   // テイクバック
        DOWNSWING,  // ダウンスイング
        FOLLOW      // フォロー
    }
    
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = geminiApiKey
    )
    
    /**
     * Gemini APIでスイングフェーズを分類
     */
    suspend fun classifyWithGemini(
        trajectory: List<RecordTrajectoryUseCase.FrameData>
    ): Result<List<SwingPhase>> {
        return try {
            val prompt = buildPrompt(trajectory)
            
            val response = model.generateContent(
                content { text(prompt) }
            )
            
            val phases = parsePhases(response.text ?: "")
            Result.success(phases)
            
        } catch (e: Exception) {
            // Gemini失敗時はローカル判定にフォールバック
            Result.success(classifyLocally(trajectory))
        }
    }
    
    /**
     * ローカルでフェーズを分類（dx/dyベース）
     */
    fun classifyLocally(
        trajectory: List<RecordTrajectoryUseCase.FrameData>
    ): List<SwingPhase> {
        if (trajectory.size < 2) return emptyList()
        
        val phases = mutableListOf<SwingPhase>()
        
        for (i in 1 until trajectory.size) {
            val prev = trajectory[i - 1]
            val curr = trajectory[i]
            
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            
            val phase = when {
                dx < 0 -> SwingPhase.TAKEBACK
                dx > 0 && dy > 0 -> SwingPhase.DOWNSWING
                dx > 0 && dy < 0 -> SwingPhase.FOLLOW
                else -> phases.lastOrNull() ?: SwingPhase.TAKEBACK
            }
            
            phases.add(phase)
        }
        
        return phases
    }
    
    /**
     * Gemini用プロンプトを構築
     */
    private fun buildPrompt(trajectory: List<RecordTrajectoryUseCase.FrameData>): String {
        return buildString {
            append("以下はゴルフスイングのクラブヘッドの座標と時間です。\n")
            append("各行は「時刻(ms), x座標, y座標」を表します。\n\n")
            
            trajectory.forEachIndexed { index, frame ->
                append("Frame ${index + 1}: ${frame.timeMs}ms, x=${frame.x}, y=${frame.y}\n")
            }
            
            append("\nこのデータをもとに、各フレームのスイングフェーズ（TAKEBACK、DOWNSWING、FOLLOW）を判定してください。\n")
            append("\nフェーズの定義:\n")
            append("- TAKEBACK: テイクバック（クラブを後ろに引く動作、x座標が減少）\n")
            append("- DOWNSWING: ダウンスイング（クラブを振り下ろす動作、x座標が増加＋y座標が増加）\n")
            append("- FOLLOW: フォロースルー（インパクト後の動作、x座標が増加＋y座標が減少）\n")
            append("\n出力形式は「Frame 1: TAKEBACK」のようにお願いします。")
        }
    }
    
    /**
     * Geminiのレスポンスをパース（改良版）
     */
    private fun parsePhases(responseText: String): List<SwingPhase> {
        val phases = mutableListOf<SwingPhase>()
        
        responseText.lines().forEach { line ->
            // "Frame X: PHASE" 形式をパース
            val phaseMatch = Regex("Frame\\s+\\d+:\\s*(TAKEBACK|DOWNSWING|FOLLOW)", RegexOption.IGNORE_CASE)
                .find(line)
            
            phaseMatch?.let {
                val phaseName = it.groupValues[1].uppercase()
                when (phaseName) {
                    "TAKEBACK" -> phases.add(SwingPhase.TAKEBACK)
                    "DOWNSWING" -> phases.add(SwingPhase.DOWNSWING)
                    "FOLLOW" -> phases.add(SwingPhase.FOLLOW)
                }
            }
        }
        
        return phases
    }
}
