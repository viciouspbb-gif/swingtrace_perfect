package com.golftrajectory.app.ai

/**
 * Data class for swing analysis results
 */
data class SwingAnalysisData(
    val totalScore: Int,
    val backswingAngle: Float,
    val hipRotation: Float,
    val shoulderRotation: Float,
    val weightShift: Float,
    val swingTempo: Float,
    val impactForce: Float,
    val followThrough: Float,
    val balance: Float,
    val timestamp: Long = System.currentTimeMillis()
)
