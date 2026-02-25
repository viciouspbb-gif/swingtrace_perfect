package com.golftrajectory.app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Gemini APIを使用したスイングフェーズ分類補助
 */
class GeminiPhaseClassifier(
    private val apiKey: String
) {
    
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash-exp",
        apiKey = apiKey
    )
    
    @Serializable
    data class PhaseClassificationRequest(
        val trajectoryPoints: List<TrajectoryPointData>
    )
    
    @Serializable
    data class TrajectoryPointData(
        val x: Float,
        val y: Float,
        val timestamp: Long,
        val velocity: VelocityData,
        val acceleration: AccelerationData
    )
    
    @Serializable
    data class VelocityData(
        val vx: Float,
        val vy: Float,
        val speed: Float
    )
    
    @Serializable
    data class AccelerationData(
        val ax: Float,
        val ay: Float,
        val magnitude: Float
    )
    
    @Serializable
    data class PhaseClassificationResult(
        val phases: List<PhaseSegment>,
        val confidence: Float,
        val analysis: String
    )
    
    @Serializable
    data class PhaseSegment(
        val startIndex: Int,
        val endIndex: Int,
        val phase: String,
        val confidence: Float
    )
    
    /**
     * 時系列データからフェーズを分類
     */
    suspend fun classifyPhases(points: List<ClubHeadPoint>): PhaseClassificationResult {
        val prompt = buildPrompt(points)
        
        val response = model.generateContent(
            content {
                text(prompt)
            }
        )
        
        return parseResponse(response.text ?: "")
    }
    
    /**
     * プロンプトを構築
     */
    private fun buildPrompt(points: List<ClubHeadPoint>): String {
        val trajectoryData = points.mapIndexed { index, point ->
            val velocity = if (index > 0) {
                val prev = points[index - 1]
                val dt = (point.timestamp - prev.timestamp) / 1000f
                val vx = (point.x - prev.x) / dt
                val vy = (point.y - prev.y) / dt
                "vx=$vx, vy=$vy"
            } else {
                "vx=0, vy=0"
            }
            
            "[$index] x=${point.x}, y=${point.y}, t=${point.timestamp}, $velocity"
        }.joinToString("\n")
        
        return """
            You are a golf swing analysis expert. Analyze the following club head trajectory data and classify it into swing phases.
            
            Swing Phases:
            - SETUP: Address position
            - TAKEAWAY: Initial movement
            - BACKSWING: Upward swing
            - TOP: Highest point
            - DOWNSWING: Downward swing
            - IMPACT: Ball contact
            - FOLLOW_THROUGH: After impact
            - FINISH: End position
            
            Trajectory Data (index, x, y, timestamp, velocity):
            $trajectoryData
            
            Please respond in JSON format:
            {
              "phases": [
                {
                  "startIndex": 0,
                  "endIndex": 5,
                  "phase": "SETUP",
                  "confidence": 0.95
                },
                ...
              ],
              "confidence": 0.92,
              "analysis": "Brief analysis of the swing"
            }
        """.trimIndent()
    }
    
    /**
     * レスポンスをパース
     */
    private fun parseResponse(responseText: String): PhaseClassificationResult {
        return try {
            // JSONブロックを抽出
            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}") + 1
            val jsonText = responseText.substring(jsonStart, jsonEnd)
            
            Json.decodeFromString<PhaseClassificationResult>(jsonText)
        } catch (e: Exception) {
            // パース失敗時はデフォルト値を返す
            PhaseClassificationResult(
                phases = emptyList(),
                confidence = 0f,
                analysis = "Failed to parse response: ${e.message}"
            )
        }
    }
    
    /**
     * フェーズ分類結果を軌道データに適用
     */
    fun applyClassification(
        points: List<ClubHeadPoint>,
        classification: PhaseClassificationResult
    ): List<ClubHeadPoint> {
        val updatedPoints = points.toMutableList()
        
        classification.phases.forEach { segment ->
            val phase = try {
                SwingPhase.valueOf(segment.phase)
            } catch (e: Exception) {
                SwingPhase.SETUP
            }
            
            for (i in segment.startIndex..segment.endIndex.coerceAtMost(points.size - 1)) {
                updatedPoints[i] = updatedPoints[i].copy(phase = phase)
            }
        }
        
        return updatedPoints
    }
}
