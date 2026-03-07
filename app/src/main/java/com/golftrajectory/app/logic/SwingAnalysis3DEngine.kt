package com.golftrajectory.app.logic

import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.atan2
import kotlin.math.abs

/**
 * 3Dキーフレーム解析用データクラス
 */
data class KeyFrame(
    val type: KeyFrameType,
    val timestampMs: Long,
    val frameIndex: Int,
    val landmarks: List<Landmark>,
    val biomechanics: BiomechanicsFrame
)

enum class KeyFrameType {
    ADDRESS,    // スイング開始時の静止状態
    TOP,        // 手首が最も高い位置
    IMPACT,     // 手首が最も低い位置（アドレス時の高さに戻る）
    FINISH      // スイング完了時
}

/**
 * 3Dスイング解析エンジン
 * worldLandmarks（3D空間座標）を使用して正確なバイオメカニクス解析を行う
 */
class SwingAnalysis3DEngine {
    private val biomechanicsEngine = BiomechanicsEngine()
    
    // 解析中の全フレームデータ
    private val allFrames = mutableListOf<BiomechanicsFrame>()
    private val allLandmarks = mutableListOf<List<Landmark>>()
    
    /**
     * 3D座標から回転角を計算（X-Z平面、上から見下ろした角度）
     */
    fun calculateRotationAngleXZ(leftPoint: Landmark, rightPoint: Landmark): Double {
        val vectorX = rightPoint.x() - leftPoint.x()
        val vectorZ = rightPoint.z() - leftPoint.z()
        
        // X-Z平面での角度を計算（上から見下ろした視点）
        return abs(Math.toDegrees(atan2(vectorZ.toDouble(), vectorX.toDouble())))
    }
    
    /**
     * 肩の回転角を計算（3D座標使用）
     */
    fun getShoulderRotation3D(landmarks: List<Landmark>): Double {
        if (landmarks.size < 33) return 0.0
        try {
            val leftShoulder = landmarks[11]
            val rightShoulder = landmarks[12]
            return calculateRotationAngleXZ(leftShoulder, rightShoulder)
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * 腰の回転角を計算（3D座標使用）
     */
    fun getHipRotation3D(landmarks: List<Landmark>): Double {
        if (landmarks.size < 33) return 0.0
        try {
            val leftHip = landmarks[23]
            val rightHip = landmarks[24]
            return calculateRotationAngleXZ(leftHip, rightHip)
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * X-Factorを計算（肩回転 - 腰回転）
     */
    fun getXFactor3D(landmarks: List<Landmark>): Double {
        val shoulderRotation = getShoulderRotation3D(landmarks)
        val hipRotation = getHipRotation3D(landmarks)
        return abs(shoulderRotation - hipRotation)
    }
    
    /**
     * 頭の移動量を計算（3D座標使用）
     */
    fun getHeadMovement3D(allLandmarks: List<List<Landmark>>): Double {
        if (allLandmarks.isEmpty() || allLandmarks.first().size < 33) return 0.0
        try {
            val firstPose = allLandmarks.first()
            val lastPose = allLandmarks.last()
            
            val firstNose = firstPose[0]  // MediaPipeの鼻のインデックス
            val lastNose = lastPose[0]
            
            // 3D距離を計算（メートル単位）
            val dx = lastNose.x() - firstNose.x()
            val dy = lastNose.y() - firstNose.y()
            val dz = lastNose.z() - firstNose.z()
            
            return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()) * 100.0 // cmに変換
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * 体重移動を計算（3D座標使用）
     */
    fun getWeightShift3D(allLandmarks: List<List<Landmark>>): Double {
        if (allLandmarks.isEmpty() || allLandmarks.first().size < 33) return 0.0
        try {
            val firstPose = allLandmarks.first()
            val lastPose = allLandmarks.last()
            
            // 両足の中点を計算
            val firstLeftAnkle = firstPose[27]
            val firstRightAnkle = firstPose[28]
            val firstCenter = PhysicsConverter.getMidPoint(firstLeftAnkle, firstRightAnkle)
            
            val lastLeftAnkle = lastPose[27]
            val lastRightAnkle = lastPose[28]
            val lastCenter = PhysicsConverter.getMidPoint(lastLeftAnkle, lastRightAnkle)
            
            // 3D距離を計算（メートル単位）
            val dx = lastCenter.x() - firstCenter.x()
            val dz = lastCenter.z() - firstCenter.z()
            
            return Math.sqrt((dx * dx + dz * dz).toDouble()) * 100.0 // cmに変換
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * シャフトレインを計算（3D座標使用）
     */
    fun getShaftLean3D(landmarks: List<Landmark>): Double {
        if (landmarks.size < 33) return 0.0
        try {
            val leftWrist = landmarks[15]
            val rightWrist = landmarks[16]
            val leftShoulder = landmarks[11]
            val rightShoulder = landmarks[12]
            
            // 手首の中点（ハンドル位置）
            val wristCenter = PhysicsConverter.getMidPoint(leftWrist, rightWrist)
            
            // 肩の中点（身体の中心軸）
            val shoulderCenter = PhysicsConverter.getMidPoint(leftShoulder, rightShoulder)
            
            // 垂直方向のベクトル（上向き）
            val verticalVector = floatArrayOf(0.0f, 1.0f, 0.0f)
            
            // 手首から肩へのベクトル
            val shaftVector = floatArrayOf(
                wristCenter.x() - shoulderCenter.x(),
                wristCenter.y() - shoulderCenter.y(),
                wristCenter.z() - shoulderCenter.z()
            )
            
            // ベクトルの長さを計算
            val shaftLength = Math.sqrt(
                (shaftVector[0] * shaftVector[0] + 
                 shaftVector[1] * shaftVector[1] + 
                 shaftVector[2] * shaftVector[2]).toDouble()
            )
            
            if (shaftLength == 0.0) return 0.0
            
            // 垂直方向との角度を計算
            val dotProduct = shaftVector[1] // 垂直ベクトル(0,1,0)との内積
            val cosTheta = (dotProduct / shaftLength).coerceIn(-1.0, 1.0)
            
            return Math.toDegrees(Math.acos(cosTheta)) - 90.0 // 垂直からの傾き
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * キーフレームを抽出
     */
    fun extractKeyFrames(inputLandmarks: List<List<Landmark>>): List<KeyFrame> {
        val keyFrames = mutableListOf<KeyFrame>()
        
        if (inputLandmarks.isEmpty()) return keyFrames
        
        // 全フレームのバイオメカニクスデータを計算
        allFrames.clear()
        allLandmarks.clear()
        
        for (i in inputLandmarks.indices) {
            val landmarks = inputLandmarks[i]
            val biomechanics = biomechanicsEngine.analyzeFrame(landmarks, i.toLong())
            allFrames.add(biomechanics)
            allLandmarks.add(landmarks)
        }
        
        // ADDRESSフレームを検出（最初の安定したフレーム）
        val addressFrame = findAddressFrame()
        if (addressFrame != null) {
            keyFrames.add(addressFrame)
        }
        
        // TOPフレームを検出（手首が最も高い位置）
        val topFrame = findTopFrame()
        if (topFrame != null) {
            keyFrames.add(topFrame)
        }
        
        // IMPACTフレームを検出（手首が最も低い位置）
        val impactFrame = findImpactFrame()
        if (impactFrame != null) {
            keyFrames.add(impactFrame)
        }
        
        // FINISHフレームを検出（最後の安定したフレーム）
        val finishFrame = findFinishFrame()
        if (finishFrame != null) {
            keyFrames.add(finishFrame)
        }
        
        return keyFrames
    }
    
    /**
     * ADDRESSフレームを検出（スイング開始時の静止状態）
     */
    private fun findAddressFrame(): KeyFrame? {
        // 最初の10フレームの中から最も安定したフレームを選択
        val searchRange = Math.min(10, allFrames.size)
        var mostStableIndex = 0
        var maxStability = 0
        
        for (i in 0 until searchRange) {
            val frame = allFrames[i]
            val stability = if (frame.isStable) 1 else 0
            if (stability > maxStability) {
                maxStability = stability
                mostStableIndex = i
            }
        }
        
        return KeyFrame(
            type = KeyFrameType.ADDRESS,
            timestampMs = mostStableIndex.toLong(),
            frameIndex = mostStableIndex,
            landmarks = allLandmarks[mostStableIndex],
            biomechanics = allFrames[mostStableIndex]
        )
    }
    
    /**
     * TOPフレームを検出（手首が最も高い位置）
     */
    private fun findTopFrame(): KeyFrame? {
        var highestWristY = Float.MAX_VALUE
        var topFrameIndex = -1
        
        for (i in allFrames.indices) {
            val landmarks = allLandmarks[i]
            if (landmarks.size >= 33) {
                // 左右手首の平均Y座標を計算
                val leftWristY = landmarks[15].y()
                val rightWristY = landmarks[16].y()
                val avgWristY = (leftWristY + rightWristY) / 2f
                
                if (avgWristY < highestWristY) {
                    highestWristY = avgWristY
                    topFrameIndex = i
                }
            }
        }
        
        return if (topFrameIndex >= 0) {
            KeyFrame(
                type = KeyFrameType.TOP,
                timestampMs = topFrameIndex.toLong(),
                frameIndex = topFrameIndex,
                landmarks = allLandmarks[topFrameIndex],
                biomechanics = allFrames[topFrameIndex]
            )
        } else null
    }
    
    /**
     * IMPACTフレームを検出（手首が最も低い位置）
     */
    private fun findImpactFrame(): KeyFrame? {
        var lowestWristY = Float.MIN_VALUE
        var impactFrameIndex = -1
        
        // TOPフレームの後から検索
        val startIndex = Math.max(0, allFrames.size / 2)
        
        for (i in startIndex until allFrames.size) {
            val landmarks = allLandmarks[i]
            if (landmarks.size >= 33) {
                // 左右手首の平均Y座標を計算
                val leftWristY = landmarks[15].y()
                val rightWristY = landmarks[16].y()
                val avgWristY = (leftWristY + rightWristY) / 2f
                
                if (avgWristY > lowestWristY) {
                    lowestWristY = avgWristY
                    impactFrameIndex = i
                }
            }
        }
        
        return if (impactFrameIndex >= 0) {
            KeyFrame(
                type = KeyFrameType.IMPACT,
                timestampMs = impactFrameIndex.toLong(),
                frameIndex = impactFrameIndex,
                landmarks = allLandmarks[impactFrameIndex],
                biomechanics = allFrames[impactFrameIndex]
            )
        } else null
    }
    
    /**
     * FINISHフレームを検出（最後の安定したフレーム）
     */
    private fun findFinishFrame(): KeyFrame? {
        // 最後の10フレームの中から最も安定したフレームを選択
        val startIndex = Math.max(0, allFrames.size - 10)
        var mostStableIndex = allFrames.size - 1
        var maxStability = 0
        
        for (i in startIndex until allFrames.size) {
            val frame = allFrames[i]
            val stability = if (frame.isStable) 1 else 0
            if (stability > maxStability) {
                maxStability = stability
                mostStableIndex = i
            }
        }
        
        return KeyFrame(
            type = KeyFrameType.FINISH,
            timestampMs = mostStableIndex.toLong(),
            frameIndex = mostStableIndex,
            landmarks = allLandmarks[mostStableIndex],
            biomechanics = allFrames[mostStableIndex]
        )
    }
    
    /**
     * キーフレームからスコアを算出
     */
    fun calculateScoreFromKeyFrames(keyFrames: List<KeyFrame>): SwingScore3D {
        val addressFrame = keyFrames.find { it.type == KeyFrameType.ADDRESS }
        val topFrame = keyFrames.find { it.type == KeyFrameType.TOP }
        
        if (addressFrame == null || topFrame == null) {
            return SwingScore3D(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        // TOP時とADDRESS時の回転角の差分を計算
        val addressShoulderRotation = getShoulderRotation3D(addressFrame.landmarks)
        val topShoulderRotation = getShoulderRotation3D(topFrame.landmarks)
        val shoulderRotationAmount = topShoulderRotation - addressShoulderRotation
        
        val addressHipRotation = getHipRotation3D(addressFrame.landmarks)
        val topHipRotation = getHipRotation3D(topFrame.landmarks)
        val hipRotationAmount = topHipRotation - addressHipRotation
        
        // X-Factorは回転量の差分
        val xFactor = abs(shoulderRotationAmount - hipRotationAmount)
        
        // 他の指標はTOPフレームを使用
        val headMovement = getHeadMovement3D(listOf(addressFrame.landmarks, topFrame.landmarks))
        val weightShift = getWeightShift3D(listOf(addressFrame.landmarks, topFrame.landmarks))
        val shaftLean = getShaftLean3D(topFrame.landmarks)
        
        return SwingScore3D(
            headMovement = headMovement,
            shoulderRotation = abs(shoulderRotationAmount),
            hipRotation = abs(hipRotationAmount),
            xFactor = xFactor,
            weightShift = weightShift,
            shaftLean = shaftLean
        )
    }
}

/**
 * 3Dスイングスコアデータ
 */
data class SwingScore3D(
    val headMovement: Double,
    val shoulderRotation: Double,
    val hipRotation: Double,
    val xFactor: Double,
    val weightShift: Double,
    val shaftLean: Double
)
