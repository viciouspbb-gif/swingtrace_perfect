package com.golftrajectory.app

import kotlin.math.*

/**
 * ゴルフボールの弾道計算エンジン
 * 空気抵抗とマグナス効果を考慮した物理シミュレーション
 */
class TrajectoryEngine {
    
    companion object {
        private const val GRAVITY = 9.81 // m/s^2
        private const val AIR_DENSITY = 1.225 // kg/m^3
        private const val BALL_MASS = 0.0459 // kg (ゴルフボール標準質量)
        private const val BALL_DIAMETER = 0.0427 // m
        private val BALL_AREA = PI * (BALL_DIAMETER / 2).pow(2)
        private const val DRAG_COEFFICIENT = 0.25
        private const val LIFT_COEFFICIENT = 0.15
        private const val TIME_STEP = 0.01 // 秒
    }
    
    data class TrajectoryPoint(
        val x: Double,  // 水平距離 (m)
        val y: Double,  // 高さ (m)
        val z: Double,  // 横方向 (m)
        val time: Double // 時間 (秒)
    )
    
    data class TrajectoryResult(
        val points: List<TrajectoryPoint>,
        val totalDistance: Double,
        val maxHeight: Double,
        val flightTime: Double
    )
    
    /**
     * 弾道を計算
     * @param ballSpeed ボール初速 (m/s)
     * @param launchAngle 打ち出し角 (度)
     * @param spinRate スピン量 (rpm)
     * @return 弾道計算結果
     */
    fun calculateTrajectory(
        ballSpeed: Double,
        launchAngle: Double,
        spinRate: Double
    ): TrajectoryResult {
        val launchAngleRad = Math.toRadians(launchAngle)
        
        // 初期速度成分
        var vx = ballSpeed * cos(launchAngleRad)
        var vy = ballSpeed * sin(launchAngleRad)
        var vz = 0.0
        
        // 初期位置
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var time = 0.0
        
        val points = mutableListOf<TrajectoryPoint>()
        points.add(TrajectoryPoint(x, y, z, time))
        
        var maxHeight = 0.0
        
        // スピン量をrad/sに変換
        val spinRateRadPerSec = spinRate * 2 * PI / 60
        
        // シミュレーション
        while (y >= 0.0 && time < 30.0) { // 最大30秒または地面に到達まで
            val velocity = sqrt(vx * vx + vy * vy + vz * vz)
            
            if (velocity < 0.1) break // 速度が極端に小さくなったら終了
            
            // 空気抵抗
            val dragForce = 0.5 * AIR_DENSITY * velocity * velocity * BALL_AREA * DRAG_COEFFICIENT
            val dragAccelX = -(dragForce / BALL_MASS) * (vx / velocity)
            val dragAccelY = -(dragForce / BALL_MASS) * (vy / velocity)
            val dragAccelZ = -(dragForce / BALL_MASS) * (vz / velocity)
            
            // マグナス効果（揚力）
            val liftForce = 0.5 * AIR_DENSITY * velocity * velocity * BALL_AREA * LIFT_COEFFICIENT * 
                           (spinRateRadPerSec * BALL_DIAMETER / (2 * velocity))
            val liftAccelY = liftForce / BALL_MASS
            
            // 加速度
            val ax = dragAccelX
            val ay = dragAccelY + liftAccelY - GRAVITY
            val az = dragAccelZ
            
            // 速度更新（オイラー法）
            vx += ax * TIME_STEP
            vy += ay * TIME_STEP
            vz += az * TIME_STEP
            
            // 位置更新
            x += vx * TIME_STEP
            y += vy * TIME_STEP
            z += vz * TIME_STEP
            
            time += TIME_STEP
            
            if (y > maxHeight) {
                maxHeight = y
            }
            
            // 一定間隔でポイントを記録（描画用）
            if (points.size < 1000 && time % 0.05 < TIME_STEP) {
                points.add(TrajectoryPoint(x, y, z, time))
            }
        }
        
        return TrajectoryResult(
            points = points,
            totalDistance = x,
            maxHeight = maxHeight,
            flightTime = time
        )
    }
}
