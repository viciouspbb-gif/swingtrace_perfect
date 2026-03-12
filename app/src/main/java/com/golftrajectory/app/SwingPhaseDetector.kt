package com.golftrajectory.app

import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * スイングフェーズ検出器
 * 速度ベクトルと加速度を使用した高精度判定
 */
class SwingPhaseDetector {
    
    private val history = mutableListOf<ClubHeadPoint>()
    private var currentPhase = SwingPhase.SETUP
    
    data class VelocityVector(
        val vx: Float,  // X方向速度
        val vy: Float,  // Y方向速度
        val speed: Float, // 速さ
        val angle: Float  // 角度（度）
    )
    
    data class Acceleration(
        val ax: Float,  // X方向加速度
        val ay: Float,  // Y方向加速度
        val magnitude: Float // 加速度の大きさ
    )
    
    /**
     * フェーズを判定
     */
    fun detectPhase(point: ClubHeadPoint): SwingPhase {
        history.add(point)
        
        // 最低3点必要
        if (history.size < 3) {
            return SwingPhase.SETUP
        }
        
        // 古いデータを削除（最新30点のみ保持）
        if (history.size > 30) {
            history.removeAt(0)
        }
        
        // 速度と加速度を計算
        val velocity = calculateVelocity()
        val acceleration = calculateAcceleration()
        
        // フェーズを判定
        currentPhase = determinePhase(velocity, acceleration)
        
        return currentPhase
    }
    
    /**
     * 速度ベクトルを計算
     */
    private fun calculateVelocity(): VelocityVector {
        if (history.size < 2) {
            return VelocityVector(0f, 0f, 0f, 0f)
        }
        
        val p1 = history[history.size - 2]
        val p2 = history[history.size - 1]
        
        val dt = (p2.timestamp - p1.timestamp) / 1000f // 秒
        if (dt <= 0) {
            return VelocityVector(0f, 0f, 0f, 0f)
        }
        
        val vx = (p2.x - p1.x) / dt
        val vy = (p2.y - p1.y) / dt
        val speed = sqrt(vx * vx + vy * vy)
        val angle = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat()
        
        return VelocityVector(vx, vy, speed, angle)
    }
    
    /**
     * 加速度を計算
     */
    private fun calculateAcceleration(): Acceleration {
        if (history.size < 3) {
            return Acceleration(0f, 0f, 0f)
        }
        
        val p1 = history[history.size - 3]
        val p2 = history[history.size - 2]
        val p3 = history[history.size - 1]
        
        val dt1 = (p2.timestamp - p1.timestamp) / 1000f
        val dt2 = (p3.timestamp - p2.timestamp) / 1000f
        
        if (dt1 <= 0 || dt2 <= 0) {
            return Acceleration(0f, 0f, 0f)
        }
        
        val vx1 = (p2.x - p1.x) / dt1
        val vx2 = (p3.x - p2.x) / dt2
        val vy1 = (p2.y - p1.y) / dt1
        val vy2 = (p3.y - p2.y) / dt2
        
        val ax = (vx2 - vx1) / dt2
        val ay = (vy2 - vy1) / dt2
        val magnitude = sqrt(ax * ax + ay * ay)
        
        return Acceleration(ax, ay, magnitude)
    }
    
    /**
     * フェーズを判定（速度・加速度ベース）
     */
    private fun determinePhase(velocity: VelocityVector, acceleration: Acceleration): SwingPhase {
        val speed = velocity.speed
        val angle = velocity.angle
        val accel = acceleration.magnitude
        
        return when (currentPhase) {
            SwingPhase.SETUP -> {
                // セットアップ→テイクアウェイ：速度が閾値を超えたら
                if (speed > 50f && angle < -45f) {
                    SwingPhase.TAKEAWAY
                } else {
                    SwingPhase.SETUP
                }
            }
            
            SwingPhase.TAKEAWAY -> {
                // テイクアウェイ→バックスイング：上方向＋左方向
                if (velocity.vy < -100f && velocity.vx < 0f) {
                    SwingPhase.BACKSWING
                } else {
                    SwingPhase.TAKEAWAY
                }
            }
            
            SwingPhase.BACKSWING -> {
                // バックスイング→トップ：速度が減少＋加速度が反転
                if (speed < 100f && accel > 500f) {
                    SwingPhase.TOP
                } else {
                    SwingPhase.BACKSWING
                }
            }
            
            SwingPhase.TOP -> {
                // トップ→ダウンスイング：下方向＋右方向＋加速度大
                if (velocity.vy > 100f && velocity.vx > 0f && accel > 1000f) {
                    SwingPhase.DOWNSWING
                } else {
                    SwingPhase.TOP
                }
            }
            
            SwingPhase.DOWNSWING -> {
                // ダウンスイング→インパクト：最大速度付近
                if (speed > 800f && accel < 500f) {
                    SwingPhase.IMPACT
                } else {
                    SwingPhase.DOWNSWING
                }
            }
            
            SwingPhase.IMPACT -> {
                // インパクト→フォロースルー：速度が維持＋右方向
                if (velocity.vx > 200f && speed > 500f) {
                    SwingPhase.FOLLOW_THROUGH
                } else {
                    SwingPhase.IMPACT
                }
            }
            
            SwingPhase.FOLLOW_THROUGH -> {
                // フォロースルー→フィニッシュ：速度が減少
                if (speed < 200f) {
                    SwingPhase.FINISH
                } else {
                    SwingPhase.FOLLOW_THROUGH
                }
            }
            
            SwingPhase.FINISH -> SwingPhase.FINISH
        }
    }
    
    /**
     * 時系列データを取得（Gemini API用）
     */
    fun getTimeSeriesData(): List<Map<String, Any>> {
        return history.map { point ->
            mapOf(
                "x" to point.x,
                "y" to point.y,
                "timestamp" to point.timestamp,
                "phase" to point.phase.name
            )
        }
    }
    
    /**
     * リセット
     */
    fun reset() {
        history.clear()
        currentPhase = SwingPhase.SETUP
    }
}
