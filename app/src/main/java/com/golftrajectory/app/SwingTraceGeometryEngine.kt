package com.golftrajectory.app

import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.*

/**
 * SwingTrace 幾何計算エンジン
 * worldLandmarks (3D) を使用して頭移動・肩回転・腰回転を計算
 */
class SwingTraceGeometryEngine {
    
    companion object {
        private const val TAG = "SwingTraceGeo"
    }
    
    /**
     * 3D座標ベクトル
     */
    data class Vec3(val x: Double, val y: Double, val z: Double)
    
    /**
     * ADDRESS/TOPフレームの幾何データ
     */
    data class GeometryFrames(
        val addressFrame: FrameGeometry,
        val topFrame: FrameGeometry
    )
    
    /**
     * 1フレームの幾何データ
     */
    data class FrameGeometry(
        val headPoint: Vec3,
        val leftShoulder: Vec3,
        val rightShoulder: Vec3,
        val leftHip: Vec3,
        val rightHip: Vec3,
        val timestampMs: Long
    )
    
    /**
     * 幾何計算結果
     */
    data class GeometryResult(
        val headMoveCm: Double,
        val shoulderRotationDeg: Double,
        val hipRotationDeg: Double,
        val isUsingFallback: Boolean = false
    )
    
    /**
     * XZ平面でのYAW角度計算（度）
     */
    private fun yawDegXZ(a: Vec3, b: Vec3): Double {
        val vx = b.x - a.x
        val vz = b.z - a.z
        return Math.toDegrees(atan2(vx, vz))
    }
    
    /**
     * 角度差分の正規化（0-180度）
     */
    private fun normalizedAngleDiffDeg(a: Double, b: Double): Double {
        var diff = abs(a - b)
        if (diff > 180.0) diff = 360.0 - diff
        return diff
    }
    
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
     * 頭の位置を取得（nose優先、ears fallback）
     */
    private fun getHeadPoint(worldLandmarks: List<Landmark>): Vec3? {
        return try {
            // 第一候補：nose (index 0)
            if (worldLandmarks.isNotEmpty()) {
                return landmarkToVec3(worldLandmarks[0])
            }
            
            // Fallback：左右耳の中点
            if (worldLandmarks.size > 7 && worldLandmarks.size > 8) {
                val leftEar = landmarkToVec3(worldLandmarks[7])
                val rightEar = landmarkToVec3(worldLandmarks[8])
                return Vec3(
                    (leftEar.x + rightEar.x) / 2.0,
                    (leftEar.y + rightEar.y) / 2.0,
                    (leftEar.z + rightEar.z) / 2.0
                )
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get head point: ${e.message}")
            null
        }
    }
    
    /**
     * 頭移動計算（cm）- 左右ブレのみ
     */
    private fun calcHeadMoveCm(addressHead: Vec3, topHead: Vec3): Double {
        val headMoveMeters = abs(topHead.x - addressHead.x)
        val headMoveCm = headMoveMeters * 100.0
        
        // 異常値判定と警告
        if (headMoveCm > 20.0) {
            Log.w(TAG, "Head movement abnormal: ${headMoveCm}cm")
        }
        
        return headMoveCm
    }
    
    /**
     * 肩回転計算（度）
     */
    private fun calcShoulderRotationDeg(
        leftShoulderAddress: Vec3,
        rightShoulderAddress: Vec3,
        leftShoulderTop: Vec3,
        rightShoulderTop: Vec3
    ): Double {
        val addressYaw = yawDegXZ(leftShoulderAddress, rightShoulderAddress)
        val topYaw = yawDegXZ(leftShoulderTop, rightShoulderTop)
        return normalizedAngleDiffDeg(addressYaw, topYaw)
    }
    
    /**
     * 腰回転計算（度）
     */
    private fun calcHipRotationDeg(
        leftHipAddress: Vec3,
        rightHipAddress: Vec3,
        leftHipTop: Vec3,
        rightHipTop: Vec3
    ): Double {
        val addressYaw = yawDegXZ(leftHipAddress, rightHipAddress)
        val topYaw = yawDegXZ(leftHipTop, rightHipTop)
        return normalizedAngleDiffDeg(addressYaw, topYaw)
    }
    
    /**
     * worldLandmarksからフレーム幾何データを抽出
     */
    fun extractFrameGeometry(worldLandmarks: List<Landmark>, timestampMs: Long): FrameGeometry? {
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
            
            FrameGeometry(
                headPoint = headPoint,
                leftShoulder = leftShoulder,
                rightShoulder = rightShoulder,
                leftHip = leftHip,
                rightHip = rightHip,
                timestampMs = timestampMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frame geometry: ${e.message}")
            null
        }
    }
    
    /**
     * スイングフレームから幾何計算を実行
     */
    fun calculateGeometry(frames: GeometryFrames): GeometryResult {
        val addressFrame = frames.addressFrame
        val topFrame = frames.topFrame
        
        // 頭移動計算
        val headMoveCm = calcHeadMoveCm(addressFrame.headPoint, topFrame.headPoint)
        
        // 肩回転計算
        val shoulderRotationDeg = calcShoulderRotationDeg(
            addressFrame.leftShoulder, addressFrame.rightShoulder,
            topFrame.leftShoulder, topFrame.rightShoulder
        )
        
        // 腰回転計算
        val hipRotationDeg = calcHipRotationDeg(
            addressFrame.leftHip, addressFrame.rightHip,
            topFrame.leftHip, topFrame.rightHip
        )
        
        // Debugログ出力
        logGeometryResults(addressFrame, topFrame, headMoveCm, shoulderRotationDeg, hipRotationDeg)
        
        return GeometryResult(
            headMoveCm = headMoveCm,
            shoulderRotationDeg = shoulderRotationDeg,
            hipRotationDeg = hipRotationDeg
        )
    }
    
    /**
     * Debugログ出力
     */
    private fun logGeometryResults(
        addressFrame: FrameGeometry,
        topFrame: FrameGeometry,
        headMoveCm: Double,
        shoulderRotationDeg: Double,
        hipRotationDeg: Double
    ) {
        // 頭位置ログ
        Log.d(TAG, "AddressHead: x=${addressFrame.headPoint.x}, y=${addressFrame.headPoint.y}, z=${addressFrame.headPoint.z}")
        Log.d(TAG, "TopHead: x=${topFrame.headPoint.x}, y=${topFrame.headPoint.y}, z=${topFrame.headPoint.z}")
        Log.d(TAG, "HeadMoveCm: ${headMoveCm}")
        
        // 肩回転ログ
        val addressShoulderYawDeg = yawDegXZ(addressFrame.leftShoulder, addressFrame.rightShoulder)
        val topShoulderYawDeg = yawDegXZ(topFrame.leftShoulder, topFrame.rightShoulder)
        Log.d(TAG, "AddressShoulderYaw: ${addressShoulderYawDeg}")
        Log.d(TAG, "TopShoulderYaw: ${topShoulderYawDeg}")
        Log.d(TAG, "ShoulderRotationDeg: ${shoulderRotationDeg}")
        
        // 腰回転ログ
        val addressHipYawDeg = yawDegXZ(addressFrame.leftHip, addressFrame.rightHip)
        val topHipYawDeg = yawDegXZ(topFrame.leftHip, topFrame.rightHip)
        Log.d(TAG, "AddressHipYaw: ${addressHipYawDeg}")
        Log.d(TAG, "TopHipYaw: ${topHipYawDeg}")
        Log.d(TAG, "HipRotationDeg: ${hipRotationDeg}")
    }
    
    /**
     * worldLandmarksリストからADDRESSとTOPフレームを検出
     * これはSwingPhaseDetectorと連携する必要がある
     */
    fun findAddressAndTopFrames(
        allFrames: List<Pair<List<Landmark>, Long>>
    ): GeometryFrames? {
        if (allFrames.size < 2) return null
        
        // TODO: SwingPhaseDetectorと連携してADDRESS/TOPフレームを検出
        // 現在は暫定的に最初と中間フレームを使用
        val addressIndex = 0
        val topIndex = allFrames.size / 2
        
        val addressGeometry = extractFrameGeometry(allFrames[addressIndex].first, allFrames[addressIndex].second)
        val topGeometry = extractFrameGeometry(allFrames[topIndex].first, allFrames[topIndex].second)
        
        return if (addressGeometry != null && topGeometry != null) {
            GeometryFrames(addressGeometry, topGeometry)
        } else {
            null
        }
    }
}
