package com.swingtrace.aicoaching.analysis

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * プロゴルファーとの類似度計算
 */
object ProSimilarityCalculator {
    
    /**
     * プロゴルファーのデータ
     */
    data class ProGolferData(
        val name: String,
        val country: String,
        val emoji: String,
        val backswingAngle: Double,
        val downswingSpeed: Double,
        val hipRotation: Double,
        val shoulderRotation: Double,
        val headStability: Double,
        val weightTransfer: Double,
        val characteristics: String
    )
    
    /**
     * 類似度結果
     */
    data class SimilarityResult(
        val pro: ProGolferData,
        val similarity: Double,
        val breakdown: Map<String, Double>
    )
    
    /**
     * プロゴルファーのベンチマークデータ
     */
    private val proGolfers = listOf(
        ProGolferData(
            name = "松山英樹",
            country = "日本",
            emoji = "🇯🇵",
            backswingAngle = 85.0,
            downswingSpeed = 48.0,
            hipRotation = 42.0,
            shoulderRotation = 58.0,
            headStability = 85.0,
            weightTransfer = 55.0,
            characteristics = "安定したテンポと正確性"
        ),
        ProGolferData(
            name = "タイガー・ウッズ",
            country = "アメリカ",
            emoji = "🇺🇸",
            backswingAngle = 90.0,
            downswingSpeed = 52.0,
            hipRotation = 45.0,
            shoulderRotation = 60.0,
            headStability = 80.0,
            weightTransfer = 60.0,
            characteristics = "パワフルで攻撃的"
        ),
        ProGolferData(
            name = "ローリー・マキロイ",
            country = "アイルランド",
            emoji = "🇮🇪",
            backswingAngle = 88.0,
            downswingSpeed = 50.0,
            hipRotation = 44.0,
            shoulderRotation = 59.0,
            headStability = 82.0,
            weightTransfer = 58.0,
            characteristics = "スムーズで流れるような"
        ),
        ProGolferData(
            name = "ダスティン・ジョンソン",
            country = "アメリカ",
            emoji = "🇺🇸",
            backswingAngle = 92.0,
            downswingSpeed = 51.0,
            hipRotation = 43.0,
            shoulderRotation = 57.0,
            headStability = 78.0,
            weightTransfer = 62.0,
            characteristics = "シンプルで力強い"
        )
    )
    
    /**
     * 全プロゴルファーとの類似度を計算
     */
    fun calculateSimilarities(
        backswingAngle: Double,
        downswingSpeed: Double,
        hipRotation: Double,
        shoulderRotation: Double,
        headStability: Double,
        weightTransfer: Double
    ): List<SimilarityResult> {
        return proGolfers.map { pro ->
            val breakdown = mutableMapOf<String, Double>()
            
            // 各項目の類似度を計算（0-100%）
            breakdown["バックスイング"] = calculateItemSimilarity(backswingAngle, pro.backswingAngle, 20.0)
            breakdown["ダウンスイング"] = calculateItemSimilarity(downswingSpeed, pro.downswingSpeed, 15.0)
            breakdown["腰の回転"] = calculateItemSimilarity(hipRotation, pro.hipRotation, 15.0)
            breakdown["肩の回転"] = calculateItemSimilarity(shoulderRotation, pro.shoulderRotation, 15.0)
            breakdown["頭の安定性"] = calculateItemSimilarity(headStability, pro.headStability, 20.0)
            breakdown["体重移動"] = calculateItemSimilarity(weightTransfer, pro.weightTransfer, 20.0)
            
            // 総合類似度（加重平均）
            val totalSimilarity = breakdown.values.average()
            
            SimilarityResult(
                pro = pro,
                similarity = totalSimilarity,
                breakdown = breakdown
            )
        }.sortedByDescending { it.similarity }
    }
    
    /**
     * 個別項目の類似度を計算
     * @param userValue ユーザーの値
     * @param proValue プロの値
     * @param maxDiff 最大許容差（これ以上離れると0%）
     * @return 類似度（0-100%）
     */
    private fun calculateItemSimilarity(
        userValue: Double,
        proValue: Double,
        maxDiff: Double
    ): Double {
        val diff = abs(userValue - proValue)
        val similarity = ((maxDiff - diff) / maxDiff * 100).coerceIn(0.0, 100.0)
        return similarity
    }
    
    /**
     * プロゴルファーのリストを取得
     */
    fun getAllPros(): List<ProGolferData> = proGolfers
    
    /**
     * 名前でプロゴルファーを検索
     */
    fun getProByName(name: String): ProGolferData? {
        return proGolfers.find { it.name == name }
    }
}
