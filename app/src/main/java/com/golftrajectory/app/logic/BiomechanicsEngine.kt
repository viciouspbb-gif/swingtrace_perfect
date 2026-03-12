package com.golftrajectory.app.logic

import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.atan2

/**
 * 1フレームあたりのバイオメカニクス解析結果を保持するデータクラス
 */
data class BiomechanicsFrame(
    val timestampMs: Long,
    val xFactorDegrees: Float,
    val spineAngleDegrees: Float,
    val centerOfGravityX: Float,
    val isStable: Boolean
)

class BiomechanicsEngine {
    // 各指標に対する独立した1€フィルタのインスタンス
    private val xFactorFilter = OneEuroFilter(minCutoff = 0.5f, beta = 0.01f)
    private val spineAngleFilter = OneEuroFilter(minCutoff = 0.5f, beta = 0.01f)
    private val cogFilter = OneEuroFilter(minCutoff = 0.1f, beta = 0.05f)

    /**
     * worldLandmarks (メートル単位の3D座標) から主要なバイオメカニクス指標を算出する
     */
    fun analyzeFrame(worldLandmarks: List<Landmark>, timestampMs: Long): BiomechanicsFrame {
        // 1. ランドマークの抽出
        val leftShoulder = worldLandmarks[11]
        val rightShoulder = worldLandmarks[12]
        val leftHip = worldLandmarks[23]
        val rightHip = worldLandmarks[24]
        val leftAnkle = worldLandmarks[27]
        val rightAnkle = worldLandmarks[28]

        // 2. 前傾角 (Spine Angle) の算出と平滑化
        val midShoulder = PhysicsConverter.getMidPoint(leftShoulder, rightShoulder)
        val midHip = PhysicsConverter.getMidPoint(leftHip, rightHip)
        val verticalVector = Landmark.create(midHip.x(), midHip.y() + 1.0f, midHip.z())
        val rawSpineAngle = PhysicsConverter.calculateAngle3D(midShoulder, midHip, verticalVector)
        val smoothedSpineAngle = spineAngleFilter.filter(rawSpineAngle, timestampMs)

        // 3. 捻転差 (X-Factor) の算出と平滑化 (XZ平面への投影)
        val shoulderVecX = rightShoulder.x() - leftShoulder.x()
        val shoulderVecZ = rightShoulder.z() - leftShoulder.z()
        val hipVecX = rightHip.x() - leftHip.x()
        val hipVecZ = rightHip.z() - leftHip.z()

        val shoulderAngle = atan2(shoulderVecZ.toDouble(), shoulderVecX.toDouble())
        val hipAngle = atan2(hipVecZ.toDouble(), hipVecX.toDouble())
        
        var rawXFactor = Math.toDegrees(shoulderAngle - hipAngle).toFloat()
        if (rawXFactor > 180f) rawXFactor -= 360f
        if (rawXFactor < -180f) rawXFactor += 360f
        val smoothedXFactor = xFactorFilter.filter(Math.abs(rawXFactor), timestampMs)

        // 4. 重心 (Center of Gravity) と安定性の推定
        // 簡易的に腰の中点のX座標を重心とみなし、両足の幅に対する位置で安定性を判定
        val rawCogX = midHip.x()
        val smoothedCogX = cogFilter.filter(rawCogX, timestampMs)
        
        val minFootX = Math.min(leftAnkle.x(), rightAnkle.x())
        val maxFootX = Math.max(leftAnkle.x(), rightAnkle.x())
        val isStable = smoothedCogX in minFootX..maxFootX

        return BiomechanicsFrame(
            timestampMs = timestampMs,
            xFactorDegrees = smoothedXFactor,
            spineAngleDegrees = smoothedSpineAngle,
            centerOfGravityX = smoothedCogX,
            isStable = isStable
        )
    }
}
