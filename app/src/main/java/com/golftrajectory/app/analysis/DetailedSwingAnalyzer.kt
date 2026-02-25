package com.swingtrace.aicoaching.analysis

import kotlin.math.*

/**
 * MediaPipeの全データを使った詳細なスイング分析
 */
object DetailedSwingAnalyzer {
    
    /**
     * 詳細なスイング分析結果
     */
    data class DetailedAnalysis(
        // 既存の分析
        val headStability: Double,
        val weightTransfer: Double,
        val backswingAngle: Double,
        val hipRotation: Double,
        val shoulderRotation: Double,
        
        // 新しい詳細分析
        val kneeStability: Double,          // 膝の安定性（0-100）
        val ankleBalance: Double,           // 足首のバランス（0-100）
        val wristCocking: Double,           // リストコック角度（度）
        val eyeOnBall: Double,              // ボールへの視線（0-100）
        val lowerBodyPower: Double,         // 下半身のパワー（0-100）
        val upperBodySync: Double,          // 上半身の同期性（0-100）
        val followThrough: Double,          // フォロースルーの完成度（0-100）
        val posture: Double,                // 姿勢の良さ（0-100）
        
        // 総合評価
        val technicalScore: Double,         // 技術スコア（0-100）
        val powerScore: Double,             // パワースコア（0-100）
        val consistencyScore: Double        // 一貫性スコア（0-100）
    )
    
    /**
     * 膝の安定性を分析
     * 
     * @param leftKneeMovement 左膝の移動量（ピクセル）
     * @param rightKneeMovement 右膝の移動量（ピクセル）
     * @return 安定性スコア（0-100、高いほど安定）
     */
    fun analyzeKneeStability(
        leftKneeMovement: Double,
        rightKneeMovement: Double
    ): Double {
        // 膝の移動量が少ないほど安定
        // 理想：左右とも20ピクセル以内
        val avgMovement = (leftKneeMovement + rightKneeMovement) / 2.0
        
        return when {
            avgMovement <= 20.0 -> 100.0
            avgMovement <= 40.0 -> 100.0 - (avgMovement - 20.0) * 2.0
            avgMovement <= 80.0 -> 60.0 - (avgMovement - 40.0) * 1.0
            else -> max(0.0, 20.0 - (avgMovement - 80.0) * 0.5)
        }.coerceIn(0.0, 100.0)
    }
    
    /**
     * 足首のバランスを分析
     * 
     * @param leftAnkleMovement 左足首の移動量（ピクセル）
     * @param rightAnkleMovement 右足首の移動量（ピクセル）
     * @return バランススコア（0-100、高いほど良い）
     */
    fun analyzeAnkleBalance(
        leftAnkleMovement: Double,
        rightAnkleMovement: Double
    ): Double {
        // 左右の足首の移動量の差が小さいほど良い
        val difference = abs(leftAnkleMovement - rightAnkleMovement)
        
        return when {
            difference <= 10.0 -> 100.0
            difference <= 30.0 -> 100.0 - (difference - 10.0) * 2.5
            difference <= 60.0 -> 50.0 - (difference - 30.0) * 1.0
            else -> max(0.0, 20.0 - (difference - 60.0) * 0.5)
        }.coerceIn(0.0, 100.0)
    }
    
    /**
     * リストコック角度を計算
     * 
     * @param wristAngleAtTop トップでの手首の角度（度）
     * @param wristAngleAtImpact インパクトでの手首の角度（度）
     * @return リストコック角度（度）
     */
    fun analyzeWristCocking(
        wristAngleAtTop: Double,
        wristAngleAtImpact: Double
    ): Double {
        // リストコックの角度差
        // 理想：60-90度のコック
        val cockingAngle = abs(wristAngleAtTop - wristAngleAtImpact)
        
        return cockingAngle.coerceIn(0.0, 120.0)
    }
    
    /**
     * ボールへの視線を分析
     * 
     * @param noseMovement 鼻の移動量（ピクセル）
     * @param headRotation 頭の回転角度（度）
     * @return 視線スコア（0-100、高いほど良い）
     */
    fun analyzeEyeOnBall(
        noseMovement: Double,
        headRotation: Double
    ): Double {
        // 頭の移動が少なく、回転も適度なほど良い
        val movementScore = when {
            noseMovement <= 5.0 -> 100.0
            noseMovement <= 15.0 -> 100.0 - (noseMovement - 5.0) * 5.0
            noseMovement <= 30.0 -> 50.0 - (noseMovement - 15.0) * 2.0
            else -> max(0.0, 20.0 - (noseMovement - 30.0) * 1.0)
        }
        
        val rotationScore = when {
            headRotation <= 10.0 -> 100.0
            headRotation <= 30.0 -> 100.0 - (headRotation - 10.0) * 2.5
            else -> max(0.0, 50.0 - (headRotation - 30.0) * 1.5)
        }
        
        return ((movementScore + rotationScore) / 2.0).coerceIn(0.0, 100.0)
    }
    
    /**
     * 下半身のパワーを分析
     * 
     * @param hipRotation 腰の回転角度（度）
     * @param kneeFlexion 膝の屈曲角度（度）
     * @param weightShift 体重移動量
     * @return パワースコア（0-100、高いほど良い）
     */
    fun analyzeLowerBodyPower(
        hipRotation: Double,
        kneeFlexion: Double,
        weightShift: Double
    ): Double {
        // 腰の回転が大きいほど良い（30-50度が理想）
        val hipScore = when {
            hipRotation in 30.0..50.0 -> 100.0
            hipRotation in 20.0..60.0 -> 80.0
            hipRotation in 10.0..70.0 -> 60.0
            else -> 40.0
        }
        
        // 膝の屈曲が適度なほど良い（20-40度が理想）
        val kneeScore = when {
            kneeFlexion in 20.0..40.0 -> 100.0
            kneeFlexion in 10.0..50.0 -> 80.0
            else -> 60.0
        }
        
        // 体重移動が適度なほど良い（30-60が理想）
        val shiftScore = when {
            weightShift in 30.0..60.0 -> 100.0
            weightShift in 20.0..70.0 -> 80.0
            else -> 60.0
        }
        
        return ((hipScore + kneeScore + shiftScore) / 3.0).coerceIn(0.0, 100.0)
    }
    
    /**
     * 上半身の同期性を分析
     * 
     * @param shoulderRotation 肩の回転角度（度）
     * @param hipRotation 腰の回転角度（度）
     * @return 同期性スコア（0-100、高いほど良い）
     */
    fun analyzeUpperBodySync(
        shoulderRotation: Double,
        hipRotation: Double
    ): Double {
        // 肩と腰の回転差が適度なほど良い
        // 理想：肩が腰より10-20度多く回転
        val difference = shoulderRotation - hipRotation
        
        return when {
            difference in 10.0..20.0 -> 100.0
            difference in 5.0..25.0 -> 90.0
            difference in 0.0..30.0 -> 75.0
            difference in -5.0..35.0 -> 60.0
            else -> 40.0
        }.coerceIn(0.0, 100.0)
    }
    
    /**
     * フォロースルーの完成度を分析
     * 
     * @param followThroughAngle フォロースルーの角度（度）
     * @param balance フィニッシュでのバランス（0-100）
     * @return 完成度スコア（0-100、高いほど良い）
     */
    fun analyzeFollowThrough(
        followThroughAngle: Double,
        balance: Double
    ): Double {
        // フォロースルーが大きいほど良い（90-120度が理想）
        val angleScore = when {
            followThroughAngle in 90.0..120.0 -> 100.0
            followThroughAngle in 70.0..140.0 -> 80.0
            followThroughAngle in 50.0..160.0 -> 60.0
            else -> 40.0
        }
        
        return ((angleScore + balance) / 2.0).coerceIn(0.0, 100.0)
    }
    
    /**
     * 姿勢の良さを分析
     * 
     * @param spineAngle 背骨の角度（度）
     * @param headPosition 頭の位置（前傾度）
     * @return 姿勢スコア（0-100、高いほど良い）
     */
    fun analyzePosture(
        spineAngle: Double,
        headPosition: Double
    ): Double {
        // 背骨の角度が適度なほど良い（30-45度が理想）
        val spineScore = when {
            spineAngle in 30.0..45.0 -> 100.0
            spineAngle in 20.0..55.0 -> 85.0
            spineAngle in 10.0..65.0 -> 70.0
            else -> 50.0
        }
        
        // 頭の位置が適度なほど良い
        val headScore = when {
            headPosition in 0.0..10.0 -> 100.0
            headPosition in -5.0..15.0 -> 85.0
            else -> 70.0
        }
        
        return ((spineScore + headScore) / 2.0).coerceIn(0.0, 100.0)
    }
    
    /**
     * 総合的な技術スコアを計算
     */
    fun calculateTechnicalScore(analysis: DetailedAnalysis): Double {
        return (
            analysis.headStability * 0.15 +
            analysis.kneeStability * 0.15 +
            analysis.ankleBalance * 0.10 +
            analysis.eyeOnBall * 0.10 +
            analysis.upperBodySync * 0.20 +
            analysis.followThrough * 0.15 +
            analysis.posture * 0.15
        ).coerceIn(0.0, 100.0)
    }
    
    /**
     * 総合的なパワースコアを計算
     */
    fun calculatePowerScore(analysis: DetailedAnalysis): Double {
        return (
            analysis.lowerBodyPower * 0.40 +
            analysis.hipRotation * 0.30 +
            analysis.shoulderRotation * 0.20 +
            analysis.wristCocking * 0.10
        ).coerceIn(0.0, 100.0)
    }
    
    /**
     * 総合的な一貫性スコアを計算
     */
    fun calculateConsistencyScore(analysis: DetailedAnalysis): Double {
        return (
            analysis.headStability * 0.30 +
            analysis.kneeStability * 0.25 +
            analysis.ankleBalance * 0.20 +
            analysis.upperBodySync * 0.25
        ).coerceIn(0.0, 100.0)
    }
}
