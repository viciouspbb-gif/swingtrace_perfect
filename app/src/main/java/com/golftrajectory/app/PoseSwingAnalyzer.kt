package com.golftrajectory.app

import androidx.compose.ui.geometry.Offset
import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.*

/**
 * MediaPipe Poseを使ったスイング分析（統合エンジン対応版）
 */
class PoseSwingAnalyzer {
    
    private val integratedAnalyzer = IntegratedSwingAnalyzer()
    
    private data class SwingFrames(
        val addressIndex: Int,
        val topIndex: Int
    )
    
    /**
     * 全フレームの姿勢からスイングを分析（worldLandmarks優先）
     */
    fun analyze(allPoses: List<List<Offset>>): SwingAnalysisResult {
        if (allPoses.isEmpty()) {
            return SwingAnalysisResult(
                backswingAngle = 0f,
                downswingSpeed = 0f,
                hipRotation = 0f,
                shoulderRotation = 0f,
                headStability = 0f,
                weightTransfer = 0f,
                swingPlane = "不明",
                score = 0,
                estimatedDistance = 0f
            )
        }
        
        // 従来の2Dベース計算（フォールバック用）
        val backswingAngle = calculateBackswingAngle(allPoses)
        val downswingSpeed = calculateDownswingSpeed(allPoses)
        val hipRotation = calculateHipRotation(allPoses)
        val shoulderRotation = calculateShoulderRotation(allPoses)
        val headStability = calculateHeadStability(allPoses)
        val weightTransfer = calculateWeightTransfer(allPoses)
        
        // スイングプレーンを判定
        val swingPlane = determineSwingPlane(shoulderRotation)
        
        // 総合スコアを計算
        val score = calculateScore(
            backswingAngle,
            downswingSpeed,
            hipRotation,
            shoulderRotation,
            headStability,
            weightTransfer
        )
        
        // 推定飛距離を計算
        val estimatedDistance = calculateEstimatedDistance(
            downswingSpeed,
            backswingAngle,
            hipRotation,
            shoulderRotation,
            headStability,
            weightTransfer
        )
        
        return SwingAnalysisResult(
            backswingAngle = backswingAngle,
            downswingSpeed = downswingSpeed,
            hipRotation = hipRotation,
            shoulderRotation = shoulderRotation,
            headStability = headStability,
            weightTransfer = weightTransfer,
            swingPlane = swingPlane,
            score = score,
            estimatedDistance = estimatedDistance
        )
    }
    
    /**
     * worldLandmarksを使用した高精度幾何計算（統合エンジン使用）
     */
    fun analyzeWithWorldLandmarks(allWorldLandmarks: List<Pair<List<Landmark>, Long>>): SwingAnalysisResult {
        if (allWorldLandmarks.isEmpty()) {
            Log.w("SwingTrace", "No worldLandmarks provided")
            return SwingAnalysisResult(
                backswingAngle = 0f,
                downswingSpeed = 0f,
                hipRotation = 0f,
                shoulderRotation = 0f,
                headStability = 0f,
                weightTransfer = 0f,
                swingPlane = "不明",
                score = 0,
                estimatedDistance = 0f
            )
        }
        
        try {
            // 統合エンジンで解析実行
            val integratedResult = integratedAnalyzer.analyzeSwing(allWorldLandmarks, null)
            
            // 既存のSwingAnalysisResult形式に変換
            val headStability = calculateHeadStabilityFromCm(integratedResult.headMoveCm)
            val totalScore = calculateTotalScore(integratedResult)
            
            Log.i("SwingTrace", "Integrated analysis completed - Head: ${"%.1f".format(integratedResult.headMoveCm)}cm, Shoulder: ${"%.1f".format(integratedResult.shoulderRotationDeg)}°, Hip: ${"%.1f".format(integratedResult.hipRotationDeg)}°, X-Factor: ${"%.1f".format(integratedResult.xFactorDeg)}°")
            
            return SwingAnalysisResult(
                backswingAngle = 0f, // 統合エンジンでは使用しない
                downswingSpeed = 0f,
                hipRotation = integratedResult.hipRotationDeg.toFloat(),
                shoulderRotation = integratedResult.shoulderRotationDeg.toFloat(),
                headStability = headStability,
                weightTransfer = integratedResult.weightShiftCm.toFloat(),
                swingPlane = determineSwingPlane(integratedResult.shoulderRotationDeg.toFloat()),
                score = totalScore,
                estimatedDistance = 0f
            )
            
        } catch (e: Exception) {
            Log.e("SwingTrace", "Error in integrated analysis: ${e.message}", e)
            return createFallbackResult()
        }
    }
    
    /**
     * 総合スコア計算（統合エンジン結果使用）
     */
    private fun calculateTotalScore(result: IntegratedSwingAnalyzer.SwingAnalysisResult): Int {
        val headScore = calculateHeadStabilityFromCm(result.headMoveCm)
        val shoulderScore = calculateRotationScore(result.shoulderRotationDeg, 100.0, 90.0, 110.0)
        val hipScore = calculateRotationScore(result.hipRotationDeg, 40.0, 35.0, 45.0)
        val xFactorScore = calculateRotationScore(result.xFactorDeg, 60.0, 50.0, 65.0)
        val weightScore = calculateRotationScore(result.weightShiftCm, 10.0, 8.0, 12.0)
        val shaftScore = calculateShaftLeanScore(result.shaftLeanDeg)
        
        return (
            headScore * 0.2f +
            shoulderScore * 0.2f +
            hipScore * 0.15f +
            xFactorScore * 0.15f +
            weightScore * 0.15f +
            shaftScore * 0.15f
        ).toInt().coerceIn(0, 100)
    }
    
    /**
     * シャフトリーンスコア計算
     */
    private fun calculateShaftLeanScore(shaftLeanDeg: Double): Float {
        return when {
            shaftLeanDeg in -12.0..-5.0 -> 100f
            shaftLeanDeg in -15.0..-3.0 -> 80f
            shaftLeanDeg in -18.0..0.0 -> 60f
            else -> 40f
        }
    }
    
    /**
     * バックスイング角度を計算
     * 改善版：腕の角度（肩-肘-手首）と体の回転を組み合わせて計算
     */
    private fun calculateBackswingAngle(poses: List<List<Offset>>): Float {
        if (poses.size < 2) return 0f

        val keyFrames = findAddressAndTopFrames(poses) ?: return 0f
        val addressAngle = poses[keyFrames.addressIndex].let(::calculateArmCompositeAngle) ?: return 0f
        val topAngle = poses[keyFrames.topIndex].let(::calculateArmCompositeAngle) ?: return 0f

        return abs(topAngle - addressAngle).coerceIn(0f, 120f)
    }
    
    /**
     * ダウンスイング速度を計算
     */
    private fun calculateDownswingSpeed(poses: List<List<Offset>>): Float {
        if (poses.size < 2) return 0f
        
        var maxSpeed = 0f
        
        for (i in 1 until poses.size) {
            val prev = poses[i - 1]
            val curr = poses[i]
            
            if (prev.size > 16 && curr.size > 16) {
                // 右手首の速度を計算
                val prevWrist = prev[16]
                val currWrist = curr[16]
                
                val distance = sqrt(
                    (currWrist.x - prevWrist.x).pow(2) +
                    (currWrist.y - prevWrist.y).pow(2)
                )
                
                // 正規化座標（0-1）をピクセル相当に変換して速度計算
                // 画面幅1920pxと仮定、フレームレートを30fpsと仮定
                val speed = distance * 1920f * 30f / 200f // スケール調整（より厳しく）
                
                if (speed > maxSpeed) {
                    maxSpeed = speed
                }
            }
        }
        
        // より現実的な範囲に制限（腰・肩の回転が悪い場合は速度も低くなるはず）
        return (maxSpeed * 0.7f).coerceIn(0f, 100f)
    }
    
    /**
     * 腰の回転を計算
     */
    private fun calculateHipRotation(poses: List<List<Offset>>): Float {
        if (poses.size < 2) return 0f

        val keyFrames = findAddressAndTopFrames(poses) ?: return 0f
        val addressAngle = poses[keyFrames.addressIndex].let(::calculateHipAngle) ?: return 0f
        val topAngle = poses[keyFrames.topIndex].let(::calculateHipAngle) ?: return 0f

        return abs(topAngle - addressAngle).coerceIn(0f, 70f)
    }
    
    /**
     * 肩の回転を計算
     */
    private fun calculateShoulderRotation(poses: List<List<Offset>>): Float {
        if (poses.size < 2) return 0f

        val keyFrames = findAddressAndTopFrames(poses) ?: return 0f
        val addressAngle = poses[keyFrames.addressIndex].let(::calculateShoulderAngle) ?: return 0f
        val topAngle = poses[keyFrames.topIndex].let(::calculateShoulderAngle) ?: return 0f

        return abs(topAngle - addressAngle).coerceIn(0f, 80f)
    }

    /**
     * 右手首の高さ推移からアドレス／トップのフレームを特定
     */
    private fun findAddressAndTopFrames(poses: List<List<Offset>>): SwingFrames? {
        if (poses.isEmpty()) return null

        val wristFrames = poses.mapIndexedNotNull { index, landmarks ->
            if (landmarks.size > 16) index to landmarks[16] else null
        }

        if (wristFrames.size < 2) return null

        val earlyBound = (poses.size * 0.4f).toInt().coerceAtLeast(1)
        val addressCandidate = wristFrames
            .filter { it.first <= earlyBound }
            .maxByOrNull { it.second.y } ?: wristFrames.maxByOrNull { it.second.y } ?: return null

        val addressIndex = addressCandidate.first

        val topCandidate = wristFrames
            .filter { it.first > addressIndex + 2 }
            .minByOrNull { it.second.y } ?: wristFrames.minByOrNull { it.second.y } ?: return null

        val topIndex = if (topCandidate.first > addressIndex) {
            topCandidate.first
        } else {
            // 最後の手段として後半から最小値を探す
            wristFrames
                .filter { it.first > poses.size / 2 }
                .minByOrNull { it.second.y }
                ?.first ?: topCandidate.first
        }

        return SwingFrames(
            addressIndex = addressIndex,
            topIndex = topIndex
        )
    }

    private fun calculateArmCompositeAngle(landmarks: List<Offset>): Float? {
        if (landmarks.size <= 16) return null
        val rightShoulder = landmarks[12]
        val rightElbow = landmarks[14]
        val rightWrist = landmarks[16]

        val shoulderToElbow = Offset(
            rightElbow.x - rightShoulder.x,
            rightElbow.y - rightShoulder.y
        )

        val armAngle = abs(
            Math.toDegrees(
                atan2(
                    shoulderToElbow.y.toDouble(),
                    shoulderToElbow.x.toDouble()
                )
            )
        ).toFloat()

        val leftShoulder = if (landmarks.size > 11) landmarks[11] else return armAngle
        val shoulderRotation = abs(
            Math.toDegrees(
                atan2(
                    (rightShoulder.y - leftShoulder.y).toDouble(),
                    (rightShoulder.x - leftShoulder.x).toDouble()
                )
            )
        ).toFloat()

        return (armAngle * 0.7f + shoulderRotation * 0.3f).coerceIn(0f, 140f)
    }

    private fun calculateHipAngle(landmarks: List<Offset>): Float? {
        if (landmarks.size <= 24) return null
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        return abs(
            Math.toDegrees(
                atan2(
                    (rightHip.y - leftHip.y).toDouble(),
                    (rightHip.x - leftHip.x).toDouble()
                )
            )
        ).toFloat()
    }

    private fun calculateShoulderAngle(landmarks: List<Offset>): Float? {
        if (landmarks.size <= 12) return null
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        return abs(
            Math.toDegrees(
                atan2(
                    (rightShoulder.y - leftShoulder.y).toDouble(),
                    (rightShoulder.x - leftShoulder.x).toDouble()
                )
            )
        ).toFloat()
    }
    
    /**
     * 頭の安定性を計算
     */
    private fun calculateHeadStability(poses: List<List<Offset>>): Float {
        println("=== calculateHeadStability called with ${poses.size} poses ===")
        
        if (poses.size < 2) {
            println("=== Not enough poses, returning 100% ===")
            return 100f
        }
        
        var totalMovement = 0f
        var validFrames = 0
        
        for (i in 1 until poses.size) {
            val prev = poses[i - 1]
            val curr = poses[i]
            
            if (prev.size > 0 && curr.size > 0) {
                // 鼻の位置の移動量
                val prevNose = prev[0]
                val currNose = curr[0]
                
                val movement = sqrt(
                    (currNose.x - prevNose.x).pow(2) +
                    (currNose.y - prevNose.y).pow(2)
                )
                
                totalMovement += movement
                validFrames++
            }
        }
        
        if (validFrames == 0) {
            println("=== No valid frames, returning 100% ===")
            return 100f
        }
        
        // 平均移動量を計算
        val avgMovement = totalMovement / validFrames
        
        // 正規化座標（0-1）をピクセル相当に変換（画面幅を1920pxと仮定）
        val avgMovementInPixels = avgMovement * 1920f
        
        // スケーリング調整：正規化座標に対応した基準
        // 理想的な頭の動き: 0.003以下（約5px） → 90%以上
        // 0.006（約10px） → 70%
        // 0.01（約16px） → 50%
        val stability = when {
            avgMovement < 0.003f -> 100f - (avgMovement * 3333)  // 0-0.003: 90-100%
            avgMovement < 0.006f -> 90f - ((avgMovement - 0.003f) * 3333)  // 0.003-0.006: 80-90%
            avgMovement < 0.015f -> 80f - ((avgMovement - 0.006f) * 2222)  // 0.006-0.015: 60-80%
            avgMovement < 0.025f -> 60f - ((avgMovement - 0.015f) * 2000)  // 0.015-0.025: 40-60%
            avgMovement < 0.050f -> 40f - ((avgMovement - 0.025f) * 1600)  // 0.025-0.050: 0-40%
            else -> 0f
        }.coerceIn(0f, 100f)
        
        return stability
    }
    
    /**
     * 体重移動を計算
     */
    private fun calculateWeightTransfer(poses: List<List<Offset>>): Float {
        if (poses.isEmpty()) return 0f
        
        var maxShift = 0f
        
        for (i in 1 until poses.size) {
            val prev = poses[i - 1]
            val curr = poses[i]
            
            if (prev.size > 24 && curr.size > 24) {
                // 腰の中心の移動
                val prevHipCenter = Offset(
                    (prev[23].x + prev[24].x) / 2,
                    (prev[23].y + prev[24].y) / 2
                )
                val currHipCenter = Offset(
                    (curr[23].x + curr[24].x) / 2,
                    (curr[23].y + curr[24].y) / 2
                )
                
                // 正規化座標（0-1）をピクセル相当に変換
                // 画面幅1920pxと仮定
                val shift = abs(currHipCenter.x - prevHipCenter.x) * 1920f
                
                if (shift > maxShift) {
                    maxShift = shift
                }
            }
        }
        
        println("=== Max weight transfer: $maxShift pixels ===")
        // 理想的な体重移動: 20-60ピクセル
        return (maxShift / 6f).coerceIn(0f, 100f)
    }
    
    /**
     * スイングプレーンを判定
     */
    private fun determineSwingPlane(shoulderRotation: Float): String {
        return when {
            shoulderRotation < 30f -> "フラット"
            shoulderRotation > 60f -> "アップライト"
            else -> "正常"
        }
    }
    
    /**
     * 推定飛距離を計算（ドライバー想定・ヤード単位）
     * 注意: これは実測値ではなく、スイングデータからの理論値です
     */
    private fun calculateEstimatedDistance(
        downswingSpeed: Float,
        backswingAngle: Float,
        hipRotation: Float,
        shoulderRotation: Float,
        headStability: Float,
        weightTransfer: Float
    ): Float {
        // ベース飛距離（スイング速度ベース）
        // スイング速度100 = 約240m（プロレベル）
        // スイング速度40 = 約150m（アマチュア平均）
        val baseDistance = (downswingSpeed / 100f * 240f).coerceIn(100f, 300f)
        
        // 効率係数を計算（0.6-1.0）
        var efficiency = 0.6f
        
        // バックスイング角度の効率（理想: 60-85°）
        val backswingEfficiency = when {
            backswingAngle in 60f..85f -> 1.0f
            backswingAngle in 50f..90f -> 0.9f
            backswingAngle in 40f..100f -> 0.8f
            else -> 0.7f
        }
        efficiency += backswingEfficiency * 0.15f
        
        // 腰の回転効率（理想: 35-50°）
        val hipEfficiency = when {
            hipRotation in 35f..50f -> 1.0f
            hipRotation in 30f..55f -> 0.9f
            else -> 0.8f
        }
        efficiency += hipEfficiency * 0.1f
        
        // 肩の回転効率（理想: 50-70°）
        val shoulderEfficiency = when {
            shoulderRotation in 50f..70f -> 1.0f
            shoulderRotation in 45f..75f -> 0.9f
            else -> 0.8f
        }
        efficiency += shoulderEfficiency * 0.1f
        
        // 頭の安定性効率
        val stabilityEfficiency = (headStability / 100f).coerceIn(0.7f, 1.0f)
        efficiency += stabilityEfficiency * 0.05f
        
        // 体重移動効率（理想: 20-60）
        val transferEfficiency = when {
            weightTransfer in 20f..60f -> 1.0f
            weightTransfer in 15f..70f -> 0.9f
            else -> 0.8f
        }
        efficiency += transferEfficiency * 0.05f
        
        // 最終的な推定飛距離（メートル）
        val estimatedDistanceMeters = baseDistance * efficiency.coerceIn(0.6f, 1.0f)
        
        // ヤードに変換（1m = 1.09361ヤード）
        val estimatedDistanceYards = estimatedDistanceMeters * 1.09361f
        
        return estimatedDistanceYards.coerceIn(110f, 330f)
    }
    
    /**
     * 総合スコアを計算
     */
    private fun calculateScore(
        backswingAngle: Float,
        downswingSpeed: Float,
        hipRotation: Float,
        shoulderRotation: Float,
        headStability: Float,
        weightTransfer: Float
    ): Int {
        // 各要素のスコア（0-100）
        val backswingScore = (backswingAngle / 90f * 100).coerceIn(0f, 100f)
        val speedScore = (downswingSpeed / 100f * 100).coerceIn(0f, 100f)
        val hipScore = (hipRotation / 45f * 100).coerceIn(0f, 100f)
        val shoulderScore = (shoulderRotation / 45f * 100).coerceIn(0f, 100f)
        val stabilityScore = headStability
        val transferScore = (weightTransfer / 50f * 100).coerceIn(0f, 100f)
        
        // 加重平均
        val totalScore = (
            backswingScore * 0.2f +
            speedScore * 0.2f +
            hipScore * 0.15f +
            shoulderScore * 0.15f +
            stabilityScore * 0.15f +
            transferScore * 0.15f
        )
        
        return totalScore.toInt().coerceIn(0, 100)
    }
    
    /**
     * 頭移動(cm)から安定性スコアを計算
     */
    private fun calculateHeadStabilityFromCm(headMoveCm: Double): Float {
        return when {
            headMoveCm <= 2.0 -> 100f
            headMoveCm <= 5.0 -> 90f - ((headMoveCm - 2.0) * 6.67f).toFloat()
            headMoveCm <= 10.0 -> 70f - ((headMoveCm - 5.0) * 6f).toFloat()
            headMoveCm <= 20.0 -> 40f - ((headMoveCm - 10.0) * 2f).toFloat()
            else -> 0f
        }
    }
    
    /**
     * 回転角度からスコアを計算
     */
    private fun calculateRotationScore(rotationDeg: Double, ideal: Double, min: Double, max: Double): Float {
        return when {
            rotationDeg in (min - 5)..(max + 5) -> 100f
            rotationDeg < min -> {
                val diff = min - rotationDeg
                (100f - (diff * 2f).toFloat()).coerceIn(0f, 100f)
            }
            rotationDeg > max -> {
                val diff = rotationDeg - max
                (100f - (diff * 2f).toFloat()).coerceIn(0f, 100f)
            }
            else -> 100f
        }
    }
    
    /**
     * フォールバック結果を生成
     */
    private fun createFallbackResult(): SwingAnalysisResult {
        return SwingAnalysisResult(
            backswingAngle = 0f,
            downswingSpeed = 0f,
            hipRotation = 0f,
            shoulderRotation = 0f,
            headStability = 0f,
            weightTransfer = 0f,
            swingPlane = "不明",
            score = 0,
            estimatedDistance = 0f
        )
    }
    
    /**
     * アドバイスを生成（worldLandmarks対応版）
     */
    fun getAdvice(result: SwingAnalysisResult): List<String> {
        val advice = mutableListOf<String>()
        
        // 頭移動（理想: 0-5cm）
        when {
            result.headStability < 70 -> advice.add("💡 頭の動きが大きいです。安定させましょう（理想: 0-5cm）")
            result.headStability >= 90 -> advice.add("👍 頭の安定性が素晴らしいです！")
        }
        
        // 腰の回転（理想: 35-45°）
        when {
            result.hipRotation < 30 -> advice.add("💡 腰の回転を意識しましょう（理想: 35-45°）")
            result.hipRotation > 55 -> advice.add("⚠️ 腰の回転が大きすぎます（理想: 35-45°）")
            result.hipRotation in 35.0..45.0 -> advice.add("� 腰の回転が良好です！")
        }
        
        // 肩の回転（理想: 90-110°）
        when {
            result.shoulderRotation < 80 -> advice.add("💡 肩の回転を大きくしましょう（理想: 90-110°）")
            result.shoulderRotation > 120 -> advice.add("⚠️ 肩の回転が大きすぎます（理想: 90-110°）")
            result.shoulderRotation in 90.0..110.0 -> advice.add("👍 肩の回転が良好です！")
        }
        
        // スイングプレーン
        when (result.swingPlane) {
            "フラット" -> advice.add("💡 スイングプレーンが平坦です。もう少し縦振りを意識しましょう")
            "アップライト" -> advice.add("💡 スイングプレーンが急です。もう少し横振りを意識しましょう")
        }
        
        // 総合スコアに応じたメッセージ
        when {
            result.score >= 80 -> advice.add(0, "🎉 素晴らしいスイングです！")
            result.score >= 60 -> advice.add(0, "👍 良いスイングです！")
            else -> advice.add(0, "💪 改善の余地があります。頑張りましょう！")
        }
        
        return advice
    }
}

/**
 * スイング分析結果
 */
data class SwingAnalysisResult(
    val backswingAngle: Float,      // バックスイング角度（度）
    val downswingSpeed: Float,      // ダウンスイング速度
    val hipRotation: Float,         // 腰の回転角度（度）
    val shoulderRotation: Float,    // 肩の回転角度（度）
    val headStability: Float,       // 頭の安定性（0-100）
    val weightTransfer: Float,      // 体重移動
    val swingPlane: String,         // スイングプレーン
    val score: Int,                 // 総合スコア（0-100）
    val estimatedDistance: Float    // 推定飛距離（ヤード・理論値）
)
