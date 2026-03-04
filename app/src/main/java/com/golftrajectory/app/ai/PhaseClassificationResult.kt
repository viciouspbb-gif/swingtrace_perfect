package com.golftrajectory.app.ai

/**
 * フェーズ分類結果
 */
data class PhaseClassificationResult(
    val phase: String,
    val confidence: Float,
    val reasoning: String
)
