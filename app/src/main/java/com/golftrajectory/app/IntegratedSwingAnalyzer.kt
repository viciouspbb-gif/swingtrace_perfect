package com.golftrajectory.app

import android.content.Context
import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.*

/**
 * SwingTrace 統合スイング解析エンジン
 * 単一処理パイプラインで6指標を計算
 */
class IntegratedSwingAnalyzer(private val context: Context? = null) {
    
    private val userPreferences by lazy {
        context?.let { UserPreferences(it) }
    }
    
    companion object {
        private const val TAG = "SwingTraceGeo"
    }
    
    /**
     * 3D座標ベクトル
     */
    data class Vec3(val x: Double, val y: Double, val z: Double)
    
    /**
     * 解析結果
     */
    data class SwingAnalysisResult(
        val headMoveCm: Double,
        val shoulderRotationDeg: Double,
        val hipRotationDeg: Double,
        val xFactorDeg: Double,
        val weightShiftCm: Double,
        val shaftLeanDeg: Double,
        val swingPlaneAngleDeg: Double = 0.0,
        val xFactorStretchDeg: Double = 0.0,
        val temporalMetrics: TemporalMetrics = TemporalMetrics(),
        val kinematicSequence: KinematicSequencePeaks? = null,
        val isUsingFallback: Boolean = false,
        val addressFrame: FrameData? = null,
        val topFrame: FrameData? = null,
        val impactFrame: FrameData? = null
    )
    
    /**
     * フレームデータ
     */
    data class FrameData(
        val headPoint: Vec3,
        val leftShoulder: Vec3,
        val rightShoulder: Vec3,
        val leftHip: Vec3,
        val rightHip: Vec3,
        val leftElbow: Vec3,
        val rightElbow: Vec3,
        val leftWrist: Vec3,
        val rightWrist: Vec3,
        val timestampMs: Long
    )

    data class TemporalMetrics(
        val timestampsMs: List<Long> = emptyList(),
        val shoulderAnglesDeg: List<Double> = emptyList(),
        val hipAnglesDeg: List<Double> = emptyList(),
        val wristSpeeds: List<Double> = emptyList(),
        val shoulderAngularVelDegPerSec: List<Double> = emptyList(),
        val hipAngularVelDegPerSec: List<Double> = emptyList()
    )

    data class SequencePeak(
        val label: String,
        val frameIndex: Int,
        val timestampMs: Long,
        val value: Double
    )

    data class KinematicSequencePeaks(
        val hip: SequencePeak? = null,
        val shoulder: SequencePeak? = null,
        val wrist: SequencePeak? = null,
        val peakOrder: List<String> = emptyList()
    )
    
    /**
     * UI表示モード
     */
    enum class DisplayMode {
        PRACTICE,    // 頭移動・肩回転・腰回転
        ATHLETE,     // + X-Factor・体重移動・シャフトリーン
        PRO          // ATHLETEと同じ（将来拡張用）
    }
    
    /**
     * 打ち手設定
     */
    data class HandednessConfig(
        val isRightHanded: Boolean = true,  // デフォルトは右打ち
        val autoDetect: Boolean = true       // 自動判定フラグ
    )
    
    // ===== 基本計算関数 =====
    
    /**
     * 中点計算
     */
    private fun midpoint(a: Vec3, b: Vec3): Vec3 {
        return Vec3(
            (a.x + b.x) / 2.0,
            (a.y + b.y) / 2.0,
            (a.z + b.z) / 2.0
        )
    }

    private fun subtract(a: Vec3, b: Vec3): Vec3 {
        return Vec3(
            a.x - b.x,
            a.y - b.y,
            a.z - b.z
        )
    }

    private fun magnitude(v: Vec3): Double {
        return sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
    }

    private fun angleBetweenDeg(a: Vec3, b: Vec3): Double {
        val denominator = magnitude(a) * magnitude(b)
        if (denominator == 0.0) return 0.0
        val dot = a.x * b.x + a.y * b.y + a.z * b.z
        val cosTheta = (dot / denominator).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosTheta))
    }
    
    /**
     * XZ平面でのYAW角度計算（度）
     */
    private fun yawDegXZ(a: Vec3, b: Vec3): Double {
        return Math.toDegrees(
            atan2(
                b.x - a.x,
                b.z - a.z
            )
        )
    }
    
    /**
     * 角度差分の正規化（0-180度）
     */
    private fun normalizedAngleDiffDeg(a: Double, b: Double): Double {
        var d = abs(a - b)
        if (d > 180.0) d = 360.0 - d
        return d
    }

    private fun signedAngleDeltaDeg(current: Double, previous: Double): Double {
        var delta = current - previous
        while (delta > 180.0) delta -= 360.0
        while (delta < -180.0) delta += 360.0
        return delta
    }
    
    /**
     * 3点間距離計算（cm）
     */
    private fun distanceCm(a: Vec3, b: Vec3): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        return sqrt(dx * dx + dy * dy + dz * dz) * 100.0
    }
    
    /**
     * シャフトリーン角度計算（度）
     */
    private fun shaftLeanDeg(elbow: Vec3, wrist: Vec3): Double {
        val dx = wrist.x - elbow.x
        val dy = wrist.y - elbow.y
        val forearmAngle = Math.toDegrees(atan2(dy, dx))
        val vertical = -90.0
        return forearmAngle - vertical
    }

    private fun calculateSwingPlaneAngle(frame: FrameData, handednessConfig: HandednessConfig): Double {
        val shoulderCenter = midpoint(frame.leftShoulder, frame.rightShoulder)
        val leadWrist = selectLeadWrist(frame, handednessConfig)
        val armVector = subtract(leadWrist, shoulderCenter)
        if (magnitude(armVector) == 0.0) return 0.0
        val angleDeg = Math.toDegrees(atan2(armVector.y, armVector.x))
        return ((angleDeg % 360.0) + 360.0) % 360.0
    }

    private fun calculatePerFrameWristSpeeds(
        frames: List<FrameData>,
        handednessConfig: HandednessConfig
    ): List<Double> {
        if (frames.isEmpty()) return emptyList()
        val speeds = MutableList(frames.size) { 0.0 }
        for (i in 1 until frames.size) {
            val curr = selectLeadWrist(frames[i], handednessConfig)
            val prev = selectLeadWrist(frames[i - 1], handednessConfig)
            val dt = (frames[i].timestampMs - frames[i - 1].timestampMs) / 1000.0
            if (dt > 0) {
                val displacement = subtract(curr, prev)
                speeds[i] = magnitude(displacement) / dt
            }
        }
        return speeds
    }

    private fun angularVelocityDegPerSec(
        previousAngleDeg: Double,
        currentAngleDeg: Double,
        deltaTimeSec: Double
    ): Double {
        if (deltaTimeSec <= 0.0) return 0.0
        val deltaAngle = signedAngleDeltaDeg(currentAngleDeg, previousAngleDeg)
        return deltaAngle / deltaTimeSec
    }

    private fun computeAngularVelocitySeries(
        anglesDeg: List<Double>,
        timestampsMs: List<Long>
    ): List<Double> {
        if (anglesDeg.isEmpty() || anglesDeg.size != timestampsMs.size) return emptyList()
        val angularVelocities = MutableList(anglesDeg.size) { 0.0 }
        for (i in 1 until anglesDeg.size) {
            val dt = (timestampsMs[i] - timestampsMs[i - 1]) / 1000.0
            angularVelocities[i] = angularVelocityDegPerSec(anglesDeg[i - 1], anglesDeg[i], dt)
        }
        return angularVelocities
    }

    private fun calculateXFactorStretch(
        shoulderAnglesDeg: List<Double>,
        hipAnglesDeg: List<Double>,
        topIndex: Int,
        impactIndex: Int
    ): Double {
        if (
            shoulderAnglesDeg.isEmpty() ||
            hipAnglesDeg.isEmpty() ||
            shoulderAnglesDeg.size != hipAnglesDeg.size ||
            topIndex < 0 ||
            impactIndex < 0 ||
            impactIndex <= topIndex ||
            impactIndex >= shoulderAnglesDeg.size
        ) {
            return 0.0
        }

        val topXFactor = normalizedAngleDiffDeg(
            shoulderAnglesDeg[topIndex],
            hipAnglesDeg[topIndex]
        )

        var maxXFactor = topXFactor
        for (i in topIndex..impactIndex) {
            val frameXFactor = normalizedAngleDiffDeg(
                shoulderAnglesDeg[i],
                hipAnglesDeg[i]
            )
            if (frameXFactor > maxXFactor) {
                maxXFactor = frameXFactor
            }
        }

        return maxOf(0.0, maxXFactor - topXFactor)
    }

    private fun detectKinematicSequencePeaks(
        temporalMetrics: TemporalMetrics
    ): KinematicSequencePeaks? {
        val timestamps = temporalMetrics.timestampsMs
        if (timestamps.isEmpty()) return null

        val hipPeak = findPeak(
            label = "Hip",
            values = temporalMetrics.hipAngularVelDegPerSec,
            timestamps = timestamps,
            useAbsolute = true
        )
        val shoulderPeak = findPeak(
            label = "Shoulder",
            values = temporalMetrics.shoulderAngularVelDegPerSec,
            timestamps = timestamps,
            useAbsolute = true
        )
        val wristPeak = findPeak(
            label = "Wrist",
            values = temporalMetrics.wristSpeeds,
            timestamps = timestamps
        )

        if (hipPeak == null && shoulderPeak == null && wristPeak == null) {
            return null
        }

        val orderedLabels = listOfNotNull(
            hipPeak?.let { it.label to it.timestampMs },
            shoulderPeak?.let { it.label to it.timestampMs },
            wristPeak?.let { it.label to it.timestampMs }
        ).sortedBy { it.second }
            .map { it.first }

        return KinematicSequencePeaks(
            hip = hipPeak,
            shoulder = shoulderPeak,
            wrist = wristPeak,
            peakOrder = orderedLabels
        )
    }

    private fun findPeak(
        label: String,
        values: List<Double>,
        timestamps: List<Long>,
        useAbsolute: Boolean = false
    ): SequencePeak? {
        if (values.isEmpty() || values.size != timestamps.size) return null
        val indexedValue = values.withIndex().maxByOrNull { indexValue ->
            if (useAbsolute) abs(indexValue.value) else indexValue.value
        } ?: return null
        return SequencePeak(
            label = label,
            frameIndex = indexedValue.index,
            timestampMs = timestamps[indexedValue.index],
            value = indexedValue.value
        )
    }
    
    /**
     * 2D角度計算（fallback用）
     */
    private fun angleDeg2D(ax: Double, ay: Double, bx: Double, by: Double): Double {
        return Math.toDegrees(atan2(by - ay, bx - ax))
    }

    private fun selectLeadWrist(frame: FrameData, handednessConfig: HandednessConfig): Vec3 {
        return if (handednessConfig.isRightHanded) frame.leftWrist else frame.rightWrist
    }
    
    // ===== Landmark変換 =====
    
    /**
     * LandmarkからVec3へ変換
     */
    private fun landmarkToVec3(landmark: Landmark): Vec3 {
        return Vec3(
            x = landmark.x().toDouble(),
            y = landmark.y().toDouble(),
            z = landmark.z().toDouble()
        )
    }
    
    /**
     * 頭の位置を取得（NOSE優先、ears fallback）
     */
    private fun getHeadPoint(worldLandmarks: List<Landmark>): Vec3? {
        return try {
            // 第一候補：NOSE (index 0)
            if (worldLandmarks.isNotEmpty()) {
                return landmarkToVec3(worldLandmarks[0])
            }
            
            // Fallback：左右耳の中点
            if (worldLandmarks.size > 7 && worldLandmarks.size > 8) {
                val leftEar = landmarkToVec3(worldLandmarks[7])
                val rightEar = landmarkToVec3(worldLandmarks[8])
                return midpoint(leftEar, rightEar)
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get head point: ${e.message}")
            null
        }
    }
    
    /**
     * worldLandmarksからフレームデータを抽出
     */
    private fun extractFrameData(worldLandmarks: List<Landmark>, timestampMs: Long): FrameData? {
        return try {
            if (worldLandmarks.size < 25) {
                Log.w(TAG, "Insufficient landmarks: ${worldLandmarks.size}")
                return null
            }
            
            val headPoint = getHeadPoint(worldLandmarks) ?: return null
            val leftShoulder = landmarkToVec3(worldLandmarks[11])
            val rightShoulder = landmarkToVec3(worldLandmarks[12])
            val leftHip = landmarkToVec3(worldLandmarks[23])
            val rightHip = landmarkToVec3(worldLandmarks[24])
            val leftElbow = landmarkToVec3(worldLandmarks[13])
            val rightElbow = landmarkToVec3(worldLandmarks[14])
            val leftWrist = landmarkToVec3(worldLandmarks[15])
            val rightWrist = landmarkToVec3(worldLandmarks[16])
            
            FrameData(
                headPoint = headPoint,
                leftShoulder = leftShoulder,
                rightShoulder = rightShoulder,
                leftHip = leftHip,
                rightHip = rightHip,
                leftElbow = leftElbow,
                rightElbow = rightElbow,
                leftWrist = leftWrist,
                rightWrist = rightWrist,
                timestampMs = timestampMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frame data: ${e.message}")
            null
        }
    }
    
    // ===== フレーム抽出ロジック =====
    
    /**
     * 手首速度計算（移動平均でスムージング）
     */
    private fun calculateWristVelocity(frames: List<FrameData>, windowSize: Int = 3): List<Double> {
        val velocities = mutableListOf<Double>()
        
        for (i in windowSize until frames.size) {
            var totalVx = 0.0
            var totalVy = 0.0
            var totalDt = 0.0
            
            for (j in 1..windowSize) {
                val curr = frames[i - j + 1]
                val prev = frames[i - j]
                val dt = (curr.timestampMs - prev.timestampMs) / 1000.0
                
                if (dt > 0) {
                    totalVx += (curr.rightWrist.x - prev.rightWrist.x) / dt
                    totalVy += (curr.rightWrist.y - prev.rightWrist.y) / dt
                    totalDt += dt
                }
            }
            
            if (totalDt > 0) {
                val avgVx = totalVx / windowSize
                val avgVy = totalVy / windowSize
                velocities.add(sqrt(avgVx * avgVx + avgVy * avgVy))
            } else {
                velocities.add(0.0)
            }
        }
        
        return velocities
    }
    
    /**
     * ADDRESSフレームを検出
     */
    private fun findAddressFrame(frames: List<FrameData>): Int? {
        if (frames.size < 10) return null
        
        val earlyFrames = frames.take(frames.size / 3)
        val velocities = calculateWristVelocity(frames)
        
        var bestIndex = -1
        var minVariance = Double.MAX_VALUE
        
        // 手首速度が低く、安定している区間を検索
        for (i in 0 until earlyFrames.size - 4) {
            val frameVelocities = mutableListOf<Double>()
            
            for (j in 0 until 5) {
                val velIndex = i + j + 2 // velocity配列のインデックス調整
                if (velIndex < velocities.size) {
                    frameVelocities.add(velocities[velIndex])
                }
            }
            
            if (frameVelocities.size >= 3) {
                val avg = frameVelocities.average()
                val variance = frameVelocities.map { (it - avg).pow(2) }.average()
                
                if (variance < minVariance && avg < 0.1) { // 速度閾値
                    minVariance = variance
                    bestIndex = i
                }
            }
        }
        
        return if (bestIndex >= 0 && minVariance < 0.01) bestIndex else null
    }
    
    /**
     * TOPフレームを検出（ハーフスイング対応・ノイズチェック強化）
     */
    private fun findTopFrame(frames: List<FrameData>, addressIndex: Int): Int? {
        if (frames.size < addressIndex + 5) return null
        
        val velocities = calculateWristVelocity(frames)
        val yVelocities = mutableListOf<Double>()
        
        // Y方向速度を計算
        for (i in 1 until frames.size) {
            val curr = frames[i]
            val prev = frames[i - 1]
            val dt = (curr.timestampMs - prev.timestampMs) / 1000.0
            
            if (dt > 0) {
                yVelocities.add((curr.rightWrist.y - prev.rightWrist.y) / dt)
            } else {
                yVelocities.add(0.0)
            }
        }
        
        // 移動平均でスムージング（5フレームウィンドウ）
        val smoothedYVel = yVelocities.windowed(5, 1) { it.average() }
        
        // 上昇→下降への反転点を検出（MediaPipe Y軸反転対応：符号反転）
        for (i in addressIndex + 2 until smoothedYVel.size - 2) {
            val prev2Vel = smoothedYVel[i - 2]
            val prev1Vel = smoothedYVel[i - 1]
            val currVel = smoothedYVel[i]
            val next1Vel = smoothedYVel[i + 1]
            val next2Vel = smoothedYVel[i + 2]
            
            // MediaPipe Y軸反転対応（符号反転）：
            // バックスイング（上昇）: Velocity < 0  →  Velocity > 0
            // ダウンスイング（下降）: Velocity > 0  →  Velocity < 0  
            // TOP（切り返し）: Velocity が マイナスからプラス  →  プラスからマイナス
            val isUpwardTrend = prev2Vel > 0 && prev1Vel > 0 && currVel >= 0  // 上昇中（正の速度）
            val isDownwardTrend = next1Vel < 0 && next2Vel < 0              // 下降中（負の速度）
            val isClearTurningPoint = currVel >= 0 && next1Vel < 0          // 正から負への明確な転換点
            
            // ノイズでないことを確認：反転前後の速度差が十分にあること
            val velocityChange = abs(currVel - next1Vel)
            val minChangeThreshold = 0.05 // 最小変化閾値
            
            if (isUpwardTrend && isDownwardTrend && isClearTurningPoint && velocityChange > minChangeThreshold) {
                Log.d(TAG, "TOP frame detected at index ${i + 2} (velocity change: $velocityChange, MediaPipe Y-axis corrected)")
                return i + 2 // 元のフレームインデックスに調整
            }
        }
        
        // フォールバック：手首が最も高い位置（符号反転：Y最大値を探索）
        var maxHeight = Double.MIN_VALUE
        var topIndex = -1
        var maxStabilityScore = 0.0
        
        for (i in addressIndex + 2 until frames.size) {
            val wristY = frames[i].rightWrist.y
            
            // 符号反転：Y座標が大きいほど高い位置
            if (wristY > maxHeight) {
                // 安定性スコアを計算（前後フレームとの変動が小さいほど高スコア）
                val stabilityScore = calculateStabilityScore(frames, i, 3)
                
                if (wristY > maxHeight || stabilityScore > maxStabilityScore) {
                    maxHeight = wristY
                    topIndex = i
                    maxStabilityScore = stabilityScore
                }
            }
        }
        
        if (topIndex >= 0) {
            Log.d(TAG, "TOP frame fallback: highest point (maximum Y) at index $topIndex (Y: $maxHeight, stability: $maxStabilityScore)")
        }
        
        return if (topIndex >= 0) topIndex else null
    }
    
    /**
     * 安定性スコア計算（周辺フレームとの変動量）
     */
    private fun calculateStabilityScore(frames: List<FrameData>, centerIndex: Int, windowSize: Int): Double {
        if (centerIndex < windowSize || centerIndex >= frames.size - windowSize) {
            return 0.0
        }
        
        val centerWristY = frames[centerIndex].rightWrist.y
        var totalVariation = 0.0
        var count = 0
        
        for (i in (centerIndex - windowSize)..(centerIndex + windowSize)) {
            if (i != centerIndex && i in frames.indices) {
                val variation = abs(frames[i].rightWrist.y - centerWristY)
                totalVariation += variation
                count++
            }
        }
        
        val avgVariation = if (count > 0) totalVariation / count else Double.MAX_VALUE
        // 変動が小さいほど高いスコア
        return maxOf(0.0, 1.0 - avgVariation * 10.0)
    }
    
    /**
     * IMPACTフレームを検出（符号反転対応）
     */
    private fun findImpactFrame(frames: List<FrameData>, topIndex: Int): Int? {
        if (frames.size < topIndex + 5) return null
        
        val velocities = calculateWristVelocity(frames)
        
        // TOP以降で速度が最大になるフレーム（ダウンスイングの最速点）
        var maxVelocity = 0.0
        var impactIndex = -1
        
        for (i in topIndex + 2 until velocities.size) {
            val frameIndex = i + 2 // velocity配列のインデックス調整
            if (frameIndex < frames.size) {
                if (velocities[i] > maxVelocity) {
                    maxVelocity = velocities[i]
                    impactIndex = frameIndex
                }
            }
        }
        
        // フォールバック：手首Yが最も低い位置（符号反転：Y最小値）
        if (impactIndex < 0) {
            var minY = Double.MAX_VALUE
            for (i in topIndex + 2 until frames.size) {
                val wristY = frames[i].rightWrist.y
                // 符号反転：Y座標が小さいほど低い位置
                if (wristY < minY) {
                    minY = wristY
                    impactIndex = i
                }
            }
            Log.d(TAG, "IMPACT frame fallback: lowest point (minimum Y) at index $impactIndex (Y: $minY)")
        }
        
        return if (impactIndex >= 0 && impactIndex < frames.size) impactIndex else null
    }
    
    // ===== 各指標計算 =====
    
    /**
     * 頭移動計算（cm）
     */
    private fun calculateHeadMove(addressFrame: FrameData, topFrame: FrameData): Double {
        return abs(topFrame.headPoint.x - addressFrame.headPoint.x) * 100.0
    }
    
    /**
     * 肩回転計算（度）
     */
    private fun calculateShoulderRotation(addressFrame: FrameData, topFrame: FrameData): Double {
        val addressShoulderYaw = yawDegXZ(addressFrame.leftShoulder, addressFrame.rightShoulder)
        val topShoulderYaw = yawDegXZ(topFrame.leftShoulder, topFrame.rightShoulder)
        return normalizedAngleDiffDeg(addressShoulderYaw, topShoulderYaw)
    }
    
    /**
     * 腰回転計算（度）
     */
    private fun calculateHipRotation(addressFrame: FrameData, topFrame: FrameData): Double {
        val addressHipYaw = yawDegXZ(addressFrame.leftHip, addressFrame.rightHip)
        val topHipYaw = yawDegXZ(topFrame.leftHip, topFrame.rightHip)
        return normalizedAngleDiffDeg(addressHipYaw, topHipYaw)
    }
    
    /**
     * X-Factor計算（度）
     */
    private fun calculateXFactor(shoulderRotationDeg: Double, hipRotationDeg: Double): Double {
        val xFactor = shoulderRotationDeg - hipRotationDeg
        return maxOf(0.0, xFactor)
    }
    
    /**
     * 体重移動計算（cm）- 方向性を厳格化
     */
    private fun calculateWeightShift(addressFrame: FrameData, impactFrame: FrameData, handednessConfig: HandednessConfig): Double {
        val addressHipCenter = midpoint(addressFrame.leftHip, addressFrame.rightHip)
        val impactHipCenter = midpoint(impactFrame.leftHip, impactFrame.rightHip)
        
        // MediaPipe座標系: 右がプラス
        // 右打ちの場合: 左方向への移動（address.x > impact.x）が正しい体重移動
        val rawShift = if (handednessConfig.isRightHanded) {
            addressHipCenter.x - impactHipCenter.x  // 右打ち：左への移動を正とする
        } else {
            impactHipCenter.x - addressHipCenter.x  // 左打ち：右への移動を正とする
        }
        
        // ターゲット方向への純粋な踏み込み量のみ（逆体重は0とする）
        val directedShift = maxOf(0.0, rawShift)
        
        Log.d(TAG, "WeightShift - Raw: ${rawShift * 100.0}cm, Directed: ${directedShift * 100.0}cm, Handedness: ${if (handednessConfig.isRightHanded) "Right" else "Left"}")
        
        return directedShift * 100.0
    }
    
    /**
     * シャフトリーン計算（度）- リード腕自動選択
     */
    private fun calculateShaftLean(addressFrame: FrameData, handednessConfig: HandednessConfig): Double {
        val (leadElbow, leadWrist) = if (handednessConfig.isRightHanded) {
            // 右打ち：左腕がリード腕
            Pair(addressFrame.leftElbow, addressFrame.leftWrist)
        } else {
            // 左打ち：右腕がリード腕
            Pair(addressFrame.rightElbow, addressFrame.rightWrist)
        }
        
        val shaftLean = shaftLeanDeg(leadElbow, leadWrist)
        
        Log.d(TAG, "ShaftLean - ${shaftLean}°, LeadArm: ${if (handednessConfig.isRightHanded) "Left" else "Right"}")
        
        return shaftLean
    }
    
    // ===== メイン解析処理 =====
    
    /**
     * 2D landmarksを使用したfallback解析
     */
    private fun analyzeWith2DLandmarks(allPoses: List<List<Offset>>, handednessConfig: HandednessConfig): SwingAnalysisResult {
        Log.w(TAG, "2D fallback used")
        
        if (allPoses.isEmpty()) {
            return createEmptyResult()
        }
        
        try {
            // 肩幅を基準としたスケーリング係数を計算
            val shoulderWidthRatio = calculateShoulderWidthRatio(allPoses)
            
            // 簡易的なフレーム検出（手首のY座標ベース）
            val addressIndex = findAddressFrame2D(allPoses) ?: return createEmptyResult()
            val topIndex = findTopFrame2D(allPoses, addressIndex) ?: return createEmptyResult()
            val impactIndex = findImpactFrame2D(allPoses, topIndex) ?: return createEmptyResult()
            
            val addressPose = allPoses[addressIndex]
            val topPose = allPoses[topIndex]
            val impactPose = allPoses[impactIndex]
            
            // 2D座標での計算（肩幅基準）
            val headMoveRatio = calculateHeadMoveRatio2D(addressPose, topPose)
            val shoulderRotationDeg = calculateShoulderRotation2D(addressPose, topPose)
            val hipRotationDeg = calculateHipRotation2D(addressPose, topPose)
            val xFactorDeg = calculateXFactor(shoulderRotationDeg, hipRotationDeg)
            val weightShiftRatio = calculateWeightShiftRatio2D(addressPose, impactPose, handednessConfig)
            val shaftLeanDeg = calculateShaftLean2D(addressPose, handednessConfig)
            
            // 肩幅からcmを推定（成人男性平均肩幅約40cmを基準）
            val estimatedCmPerShoulderWidth = 40.0
            
            logResults(
                headMoveRatio * estimatedCmPerShoulderWidth,
                shoulderRotationDeg,
                hipRotationDeg,
                xFactorDeg,
                weightShiftRatio * estimatedCmPerShoulderWidth,
                shaftLeanDeg
            )
            
            return SwingAnalysisResult(
                headMoveCm = headMoveRatio * estimatedCmPerShoulderWidth,
                shoulderRotationDeg = shoulderRotationDeg,
                hipRotationDeg = hipRotationDeg,
                xFactorDeg = xFactorDeg,
                weightShiftCm = weightShiftRatio * estimatedCmPerShoulderWidth,
                shaftLeanDeg = shaftLeanDeg,
                isUsingFallback = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in 2D fallback analysis: ${e.message}")
            return createEmptyResult()
        }
    }
    
    /**
     * 2DでのADDRESSフレーム検出
     */
    private fun findAddressFrame2D(poses: List<List<Offset>>): Int? {
        if (poses.size < 10) return null
        
        val earlyPoses = poses.take(poses.size / 3)
        var bestIndex = -1
        var minVariance = Double.MAX_VALUE
        
        for (i in 0 until earlyPoses.size - 4) {
            val wristHeights = mutableListOf<Double>()
            
            for (j in 0 until 5) {
                val pose = earlyPoses[i + j]
                if (pose.size > 16) {
                    wristHeights.add(pose[16].y.toDouble())
                }
            }
            
            if (wristHeights.size >= 3) {
                val avg = wristHeights.average()
                val variance = wristHeights.map { (it - avg).pow(2) }.average()
                
                if (variance < minVariance && avg < 0.3) {
                    minVariance = variance
                    bestIndex = i
                }
            }
        }
        
        return if (bestIndex >= 0 && minVariance < 0.01) bestIndex else null
    }
    
    /**
     * 2DでのTOPフレーム検出（符号反転対応）
     */
    private fun findTopFrame2D(poses: List<List<Offset>>, addressIndex: Int): Int? {
        if (poses.size < addressIndex + 5) return null
        
        var maxY = Float.MIN_VALUE
        var topIndex = -1
        
        for (i in addressIndex + 2 until poses.size) {
            val pose = poses[i]
            if (pose.size > 16) {
                val wristY = pose[16].y
                // 符号反転：Y座標が大きいほど高い位置
                if (wristY > maxY) {
                    maxY = wristY
                    topIndex = i
                }
            }
        }
        
        return if (topIndex >= 0) topIndex else null
    }
    
    /**
     * 2DでのIMPACTフレーム検出（符号反転対応）
     */
    private fun findImpactFrame2D(poses: List<List<Offset>>, topIndex: Int): Int? {
        if (poses.size < topIndex + 5) return null
        
        var minY = Float.MIN_VALUE
        var impactIndex = -1
        
        for (i in topIndex + 2 until poses.size) {
            val pose = poses[i]
            if (pose.size > 16) {
                val wristY = pose[16].y
                // 符号反転：Y座標が小さいほど低い位置
                if (wristY < minY) {
                    minY = wristY
                    impactIndex = i
                }
            }
        }
        
        return if (impactIndex >= 0) impactIndex else null
    }
    
    /**
     * 肩幅を基準としたスケーリング係数を計算
     */
    private fun calculateShoulderWidthRatio(allPoses: List<List<Offset>>): Double {
        val shoulderWidths = allPoses.mapNotNull { pose ->
            if (pose.size > 12) {
                val leftShoulder = pose[11]
                val rightShoulder = pose[12]
                abs(rightShoulder.x - leftShoulder.x).toDouble()
            } else null
        }
        
        return if (shoulderWidths.isNotEmpty()) {
            shoulderWidths.average()
        } else {
            0.15 // デフォルト値（画面幅の15%）
        }
    }
    
    /**
     * 2Dでの頭移動計算（肩幅基準の比率）
     */
    private fun calculateHeadMoveRatio2D(addressPose: List<Offset>, topPose: List<Offset>): Double {
        if (addressPose.isEmpty() || topPose.isEmpty()) return 0.0
        
        val shoulderWidth = if (addressPose.size > 12) {
            abs(addressPose[12].x - addressPose[11].x).toDouble()
        } else {
            0.15 // デフォルト肩幅
        }
        
        val headMoveRatio = abs(topPose[0].x - addressPose[0].x).toDouble() / shoulderWidth
        Log.d(TAG, "HeadMoveRatio: $headMoveRatio (shoulder width: $shoulderWidth)")
        
        return headMoveRatio
    }
    
    /**
     * 2Dでの肩回転計算
     */
    private fun calculateShoulderRotation2D(addressPose: List<Offset>, topPose: List<Offset>): Double {
        if (addressPose.size < 13 || topPose.size < 13) return 0.0
        
        val addressAngle = angleDeg2D(
            addressPose[11].x.toDouble(), addressPose[11].y.toDouble(),
            addressPose[12].x.toDouble(), addressPose[12].y.toDouble()
        )
        
        val topAngle = angleDeg2D(
            topPose[11].x.toDouble(), topPose[11].y.toDouble(),
            topPose[12].x.toDouble(), topPose[12].y.toDouble()
        )
        
        return normalizedAngleDiffDeg(addressAngle, topAngle)
    }
    
    /**
     * 2Dでの腰回転計算
     */
    private fun calculateHipRotation2D(addressPose: List<Offset>, topPose: List<Offset>): Double {
        if (addressPose.size < 25 || topPose.size < 25) return 0.0
        
        val addressAngle = angleDeg2D(
            addressPose[23].x.toDouble(), addressPose[23].y.toDouble(),
            addressPose[24].x.toDouble(), addressPose[24].y.toDouble()
        )
        
        val topAngle = angleDeg2D(
            topPose[23].x.toDouble(), topPose[23].y.toDouble(),
            topPose[24].x.toDouble(), topPose[24].y.toDouble()
        )
        
        return normalizedAngleDiffDeg(addressAngle, topAngle)
    }
    
    /**
     * 2Dでの体重移動計算（肩幅基準の比率）
     */
    private fun calculateWeightShiftRatio2D(addressPose: List<Offset>, impactPose: List<Offset>, handednessConfig: HandednessConfig): Double {
        if (addressPose.size < 25 || impactPose.size < 25) return 0.0
        
        val shoulderWidth = if (addressPose.size > 12) {
            abs(addressPose[12].x - addressPose[11].x).toDouble()
        } else {
            0.15 // デフォルト肩幅
        }
        
        val addressHipCenterX = ((addressPose[23].x + addressPose[24].x) / 2f).toDouble()
        val impactHipCenterX = ((impactPose[23].x + impactPose[24].x) / 2f).toDouble()
        
        // 方向性を考慮した計算
        val rawShiftRatio = if (handednessConfig.isRightHanded) {
            (addressHipCenterX - impactHipCenterX) / shoulderWidth
        } else {
            (impactHipCenterX - addressHipCenterX) / shoulderWidth
        }
        
        // ターゲット方向への純粋な踏み込み量のみ
        val directedShiftRatio = maxOf(0.0, rawShiftRatio)
        
        Log.d(TAG, "WeightShiftRatio: $directedShiftRatio (shoulder width: $shoulderWidth)")
        
        return directedShiftRatio
    }
    
    /**
     * 2Dでのシャフトリーン計算（リード腕自動選択）
     */
    private fun calculateShaftLean2D(addressPose: List<Offset>, handednessConfig: HandednessConfig): Double {
        if (addressPose.size < 16) return 0.0
        
        val (elbow, wrist) = if (handednessConfig.isRightHanded) {
            // 右打ち：左腕
            Pair(addressPose[13], addressPose[15])
        } else {
            // 左打ち：右腕
            Pair(addressPose[14], addressPose[16])
        }
        
        val dx = wrist.x - elbow.x
        val dy = wrist.y - elbow.y
        val forearmAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        val vertical = -90.0
        
        val shaftLean = forearmAngle - vertical
        Log.d(TAG, "ShaftLean2D: $shaftLean°, LeadArm: ${if (handednessConfig.isRightHanded) "Left" else "Right"}")
        
        return shaftLean
    }
    
    /**
     * 打ち手設定の自動取得
     */
    private fun getHandednessConfig(): HandednessConfig {
        val isRightHanded = userPreferences?.isRightHanded() ?: true
        return HandednessConfig(
            isRightHanded = isRightHanded,
            autoDetect = userPreferences != null
        )
    }
    
    /**
     * スイング解析実行（UserPreferencesから打ち手設定を自動取得）
     */
    fun analyzeSwing(
        allWorldLandmarks: List<Pair<List<Landmark>, Long>>? = null,
        allPoses: List<List<Offset>>? = null
    ): SwingAnalysisResult {
        val handednessConfig = getHandednessConfig()
        return analyzeSwing(allWorldLandmarks, allPoses, handednessConfig)
    }
    
    /**
     * スイング解析実行（fallback対応）
     */
    fun analyzeSwing(
        allWorldLandmarks: List<Pair<List<Landmark>, Long>>? = null,
        allPoses: List<List<Offset>>? = null,
        handednessConfig: HandednessConfig = HandednessConfig()
    ): SwingAnalysisResult {
        // worldLandmarksを優先
        if (allWorldLandmarks != null && allWorldLandmarks.isNotEmpty()) {
            return analyzeSwing(allWorldLandmarks, handednessConfig)
        }
        
        // fallback to 2D landmarks
        if (allPoses != null && allPoses.isNotEmpty()) {
            return analyzeWith2DLandmarks(allPoses, handednessConfig)
        }
        
        Log.w(TAG, "No data provided for analysis")
        return createEmptyResult()
    }
    
    /**
     * worldLandmarksを使用したスイング解析実行
     */
    private fun analyzeSwing(allWorldLandmarks: List<Pair<List<Landmark>, Long>>, handednessConfig: HandednessConfig): SwingAnalysisResult {
        if (allWorldLandmarks.isEmpty()) {
            Log.w(TAG, "No worldLandmarks provided")
            return createEmptyResult()
        }
        
        try {
            // フレームデータを抽出
            val frames = allWorldLandmarks.mapNotNull { (landmarks, timestamp) ->
                extractFrameData(landmarks, timestamp)
            }
            
            if (frames.size < 10) {
                Log.w(TAG, "Insufficient frames: ${frames.size}")
                return createEmptyResult()
            }
            
            // キーフレームを検出
            val addressIndex = findAddressFrame(frames)
            if (addressIndex == null) {
                Log.w(TAG, "Failed to find ADDRESS frame")
                return createEmptyResult()
            }
            
            val topIndex = findTopFrame(frames, addressIndex)
            if (topIndex == null) {
                Log.w(TAG, "Failed to find TOP frame")
                return createEmptyResult()
            }
            
            val impactIndex = findImpactFrame(frames, topIndex)
            if (impactIndex == null) {
                Log.w(TAG, "Failed to find IMPACT frame")
                return createEmptyResult()
            }
            
            val addressFrame = frames[addressIndex]
            val topFrame = frames[topIndex]
            val impactFrame = frames[impactIndex]
            
            // 各指標を計算（打ち手設定を反映）
            val headMoveCm = calculateHeadMove(addressFrame, topFrame)
            val shoulderRotationDeg = calculateShoulderRotation(addressFrame, topFrame)
            val hipRotationDeg = calculateHipRotation(addressFrame, topFrame)
            val xFactorDeg = calculateXFactor(shoulderRotationDeg, hipRotationDeg)
            val weightShiftCm = calculateWeightShift(addressFrame, impactFrame, handednessConfig)
            val shaftLeanDeg = calculateShaftLean(addressFrame, handednessConfig)
            val swingPlaneAngleDeg = calculateSwingPlaneAngle(topFrame, handednessConfig)

            // 全フレームの時系列データを構築
            val timestamps = frames.map { it.timestampMs }
            val shoulderAngles = frames.map { yawDegXZ(it.leftShoulder, it.rightShoulder) }
            val hipAngles = frames.map { yawDegXZ(it.leftHip, it.rightHip) }
            val wristSpeeds = calculatePerFrameWristSpeeds(frames, handednessConfig)
            val shoulderAngularVel = computeAngularVelocitySeries(shoulderAngles, timestamps)
            val hipAngularVel = computeAngularVelocitySeries(hipAngles, timestamps)
            val temporalMetrics = TemporalMetrics(
                timestampsMs = timestamps,
                shoulderAnglesDeg = shoulderAngles,
                hipAnglesDeg = hipAngles,
                wristSpeeds = wristSpeeds,
                shoulderAngularVelDegPerSec = shoulderAngularVel,
                hipAngularVelDegPerSec = hipAngularVel
            )

            val xFactorStretchDeg = calculateXFactorStretch(shoulderAngles, hipAngles, topIndex, impactIndex)
            val kinematicSequence = detectKinematicSequencePeaks(temporalMetrics)
            
            // Debugログ出力
            logResults(headMoveCm, shoulderRotationDeg, hipRotationDeg, xFactorDeg, weightShiftCm, shaftLeanDeg)
            
            return SwingAnalysisResult(
                headMoveCm = headMoveCm,
                shoulderRotationDeg = shoulderRotationDeg,
                hipRotationDeg = hipRotationDeg,
                xFactorDeg = xFactorDeg,
                weightShiftCm = weightShiftCm,
                shaftLeanDeg = shaftLeanDeg,
                swingPlaneAngleDeg = swingPlaneAngleDeg,
                xFactorStretchDeg = xFactorStretchDeg,
                temporalMetrics = temporalMetrics,
                kinematicSequence = kinematicSequence,
                addressFrame = addressFrame,
                topFrame = topFrame,
                impactFrame = impactFrame
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in swing analysis: ${e.message}", e)
            return createEmptyResult()
        }
    }
    
    /**
     * 空の結果を生成
     */
    private fun createEmptyResult(): SwingAnalysisResult {
        return SwingAnalysisResult(
            headMoveCm = 0.0,
            shoulderRotationDeg = 0.0,
            hipRotationDeg = 0.0,
            xFactorDeg = 0.0,
            weightShiftCm = 0.0,
            shaftLeanDeg = 0.0,
            isUsingFallback = true
        )
    }
    
    /**
     * Debugログ出力
     */
    private fun logResults(
        headMoveCm: Double,
        shoulderRotationDeg: Double,
        hipRotationDeg: Double,
        xFactorDeg: Double,
        weightShiftCm: Double,
        shaftLeanDeg: Double
    ) {
        Log.d(TAG, "HeadMove=${headMoveCm}")
        Log.d(TAG, "ShoulderRot=${shoulderRotationDeg}")
        Log.d(TAG, "HipRot=${hipRotationDeg}")
        Log.d(TAG, "XFactor=${xFactorDeg}")
        Log.d(TAG, "WeightShift=${weightShiftCm}")
        Log.d(TAG, "ShaftLean=${shaftLeanDeg}")
    }
    
    /**
     * UI表示用データを取得
     */
    fun getDisplayData(result: SwingAnalysisResult, mode: DisplayMode): Map<String, Double> {
        return when (mode) {
            DisplayMode.PRACTICE -> mapOf(
                "頭移動" to result.headMoveCm,
                "肩回転" to result.shoulderRotationDeg,
                "腰回転" to result.hipRotationDeg
            )
            DisplayMode.ATHLETE, DisplayMode.PRO -> mapOf(
                "頭移動" to result.headMoveCm,
                "肩回転" to result.shoulderRotationDeg,
                "腰回転" to result.hipRotationDeg,
                "X-Factor" to result.xFactorDeg,
                "体重移動" to result.weightShiftCm,
                "シャフトリーン" to result.shaftLeanDeg
            )
        }
    }
    
    /**
     * 理想値との比較で評価
     */
    fun evaluateResults(result: SwingAnalysisResult): Map<String, String> {
        val evaluation = mutableMapOf<String, String>()
        
        // 頭移動（理想: 2cm, 許容: 0-5cm）
        evaluation["頭移動"] = when {
            result.headMoveCm <= 2.0 -> "素晴らしい"
            result.headMoveCm <= 5.0 -> "良好"
            result.headMoveCm <= 10.0 -> "要改善"
            else -> "要注意"
        }
        
        // 肩回転（理想: 100°, 許容: 90-110°）
        evaluation["肩回転"] = when {
            result.shoulderRotationDeg in 90.0..110.0 -> "適切"
            result.shoulderRotationDeg in 80.0..120.0 -> "概ね良好"
            else -> "要調整"
        }
        
        // 腰回転（理想: 40°, 許容: 35-45°）
        evaluation["腰回転"] = when {
            result.hipRotationDeg in 35.0..45.0 -> "適切"
            result.hipRotationDeg in 30.0..50.0 -> "概ね良好"
            else -> "要調整"
        }
        
        // X-Factor（理想: 60°, 許容: 50-65°）
        evaluation["X-Factor"] = when {
            result.xFactorDeg in 50.0..65.0 -> "適切"
            result.xFactorDeg in 40.0..70.0 -> "概ね良好"
            else -> "要調整"
        }
        
        // 体重移動（理想: 10cm, 許容: 8-12cm）
        evaluation["体重移動"] = when {
            result.weightShiftCm in 8.0..12.0 -> "適切"
            result.weightShiftCm in 5.0..15.0 -> "概ね良好"
            else -> "要調整"
        }
        
        // シャフトリーン（理想: -8°, 許容: -5~-12°）
        evaluation["シャフトリーン"] = when {
            result.shaftLeanDeg in -12.0..-5.0 -> "適切"
            result.shaftLeanDeg in -15.0..-3.0 -> "概ね良好"
            else -> "要調整"
        }
        
        return evaluation
    }
}
