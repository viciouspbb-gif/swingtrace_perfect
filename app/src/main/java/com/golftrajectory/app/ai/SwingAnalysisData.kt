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
    val downswingSpeed: Float? = null,
    val headStability: String? = null,
    val swingPlane: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
