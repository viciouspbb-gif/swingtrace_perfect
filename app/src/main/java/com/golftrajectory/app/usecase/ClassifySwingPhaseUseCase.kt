package com.golftrajectory.app.usecase

import com.golftrajectory.app.ai.AIServiceRepository
import com.golftrajectory.app.ai.PlanUpgradeRequired
import javax.inject.Inject

/**
 * スイングフェーズ分類UseCase
 */
class ClassifySwingPhaseUseCase @Inject constructor(
    private val aiServiceRepository: AIServiceRepository
) {
    
    enum class SwingPhase {
        TAKEBACK,   // テイクバック
        DOWNSWING,  // ダウンスイング
        FOLLOW      // フォロー
    }
    
    /**
     * スイングフェーズを分類
     */
    suspend fun classify(swingData: String): Result<SwingPhase> {
        return try {
            val result = aiServiceRepository.classifyPhase(swingData)
            result.map { phaseName ->
                when (phaseName.trim().uppercase()) {
                    "TAKEBACK" -> SwingPhase.TAKEBACK
                    "DOWNSWING" -> SwingPhase.DOWNSWING
                    "FOLLOW" -> SwingPhase.FOLLOW
                    else -> SwingPhase.TAKEBACK
                }
            }
        } catch (e: PlanUpgradeRequired) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
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
