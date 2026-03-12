package com.golftrajectory.app

/**
 * バイオメカニクス詳細データ
 */
data class BiomechanicsData(
    val shoulderRotation: Double,
    val hipRotation: Double,
    val xFactor: Double,
    val headMovement: Double,
    val weightShift: Double,
    val shaftLean: Double
)
