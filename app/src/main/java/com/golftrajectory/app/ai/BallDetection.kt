package com.golftrajectory.app.ai

import android.graphics.RectF

/**
 * ボール検出結果
 */
data class BallDetection(
    val position: RectF,
    val confidence: Float,
    val timestamp: Long
)
