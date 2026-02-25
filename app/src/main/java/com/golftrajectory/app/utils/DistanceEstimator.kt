package com.swingtrace.aicoaching.utils

/**
 * スイングデータから飛距離を推定
 */
object DistanceEstimator {
    
    /**
     * スイングスピードとインパクトスピードから飛距離を推定
     * 
     * @param downswingSpeed ダウンスイング速度（0-100のスケール）
     * @param impactSpeed インパクト速度（m/s、オプション）
     * @param clubType クラブタイプ（デフォルト：ドライバー）
     * @return 推定飛距離（ヤード）
     */
    fun estimateDistance(
        downswingSpeed: Double,
        impactSpeed: Double? = null,
        clubType: ClubType = ClubType.DRIVER
    ): Double {
        // インパクトスピードがある場合はそれを使用
        if (impactSpeed != null && impactSpeed > 0) {
            return estimateFromImpactSpeed(impactSpeed, clubType)
        }
        
        // ダウンスイング速度から推定
        return estimateFromDownswingSpeed(downswingSpeed, clubType)
    }
    
    /**
     * インパクトスピード（ヘッドスピード）から飛距離を推定
     * 一般的な公式：飛距離(ヤード) = ヘッドスピード(m/s) × 4.0 ～ 4.5
     */
    private fun estimateFromImpactSpeed(impactSpeed: Double, clubType: ClubType): Double {
        val multiplier = when (clubType) {
            ClubType.DRIVER -> 4.3
            ClubType.WOOD_3 -> 3.8
            ClubType.WOOD_5 -> 3.5
            ClubType.UT_3 -> 3.3
            ClubType.UT_4 -> 3.2
            ClubType.IRON_4 -> 3.1
            ClubType.IRON_5 -> 3.0
            ClubType.IRON_6 -> 2.9
            ClubType.IRON_7 -> 2.8
            ClubType.IRON_8 -> 2.6
            ClubType.IRON_9 -> 2.4
            ClubType.WEDGE_PW -> 2.2
            ClubType.WEDGE_AW -> 2.0
            ClubType.WEDGE_SW -> 1.8
            ClubType.CUSTOM -> 4.0  // カスタムはデフォルト値
        }
        
        return impactSpeed * multiplier
    }
    
    /**
     * ダウンスイング速度（0-100スケール）から飛距離を推定
     * 
     * スケール対応：
     * - 100: プロレベル（ヘッドスピード 50m/s = 約220ヤード）
     * - 70: 上級者（ヘッドスピード 45m/s = 約200ヤード）
     * - 50: 中級者（ヘッドスピード 40m/s = 約180ヤード）
     * - 30: 初心者（ヘッドスピード 35m/s = 約150ヤード）
     */
    private fun estimateFromDownswingSpeed(downswingSpeed: Double, clubType: ClubType): Double {
        // ダウンスイング速度をヘッドスピード（m/s）に変換
        // 線形補間：0-100 → 25-55 m/s
        val headSpeed = 25.0 + (downswingSpeed / 100.0) * 30.0
        
        return estimateFromImpactSpeed(headSpeed, clubType)
    }
    
    /**
     * スイング分析データから総合的に飛距離を推定
     * 
     * @param downswingSpeed ダウンスイング速度
     * @param backswingAngle バックスイング角度
     * @param headStability 頭の安定性
     * @param weightTransfer 体重移動
     * @return 推定飛距離（ヤード）
     */
    fun estimateDistanceFromSwingData(
        downswingSpeed: Double,
        backswingAngle: Double,
        headStability: Double,
        weightTransfer: Double
    ): Double {
        // 基本飛距離
        val baseDistance = estimateFromDownswingSpeed(downswingSpeed, ClubType.DRIVER)
        
        // 効率係数を計算（0.7 ～ 1.0）
        var efficiency = 0.85
        
        // バックスイング角度の影響（理想：60-85度）
        if (backswingAngle in 60.0..85.0) {
            efficiency += 0.05
        } else if (backswingAngle < 50.0 || backswingAngle > 95.0) {
            efficiency -= 0.05
        }
        
        // 頭の安定性の影響（理想：70%以上）
        if (headStability >= 70.0) {
            efficiency += 0.05
        } else if (headStability < 50.0) {
            efficiency -= 0.05
        }
        
        // 体重移動の影響（理想：20-60）
        if (weightTransfer in 20.0..60.0) {
            efficiency += 0.05
        } else if (weightTransfer < 10.0 || weightTransfer > 70.0) {
            efficiency -= 0.05
        }
        
        // 効率を0.7～1.0の範囲に制限
        efficiency = efficiency.coerceIn(0.7, 1.0)
        
        return baseDistance * efficiency
    }
    
    /**
     * クラブタイプ
     */
    enum class ClubType {
        DRIVER,      // ドライバー
        WOOD_3,      // 3番ウッド
        WOOD_5,      // 5番ウッド
        UT_3,        // 3番ユーティリティ
        UT_4,        // 4番ユーティリティ
        IRON_4,      // 4番アイアン
        IRON_5,      // 5番アイアン
        IRON_6,      // 6番アイアン
        IRON_7,      // 7番アイアン
        IRON_8,      // 8番アイアン
        IRON_9,      // 9番アイアン
        WEDGE_PW,    // ピッチングウェッジ
        WEDGE_AW,    // アプローチウェッジ
        WEDGE_SW,    // サンドウェッジ
        CUSTOM       // カスタム
    }
    
    /**
     * カスタムクラブ設定
     */
    data class CustomClub(
        val name: String,           // クラブ名（例: "マイドライバー"）
        val distanceRatio: Double   // 飛距離係数（0.0-1.0、ドライバー基準）
    )
    
    /**
     * クラブタイプの飛距離係数を取得
     */
    fun getClubRatio(clubType: ClubType): Double {
        return when (clubType) {
            ClubType.DRIVER -> 1.00
            ClubType.WOOD_3 -> 0.88
            ClubType.WOOD_5 -> 0.82
            ClubType.UT_3 -> 0.78
            ClubType.UT_4 -> 0.76
            ClubType.IRON_4 -> 0.74
            ClubType.IRON_5 -> 0.72
            ClubType.IRON_6 -> 0.68
            ClubType.IRON_7 -> 0.65
            ClubType.IRON_8 -> 0.60
            ClubType.IRON_9 -> 0.56
            ClubType.WEDGE_PW -> 0.52
            ClubType.WEDGE_AW -> 0.48
            ClubType.WEDGE_SW -> 0.44
            ClubType.CUSTOM -> 1.00  // カスタムは別途設定
        }
    }
}
