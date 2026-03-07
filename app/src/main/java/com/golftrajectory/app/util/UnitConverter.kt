package com.golftrajectory.app.util

import com.golftrajectory.app.UnitSystem
import kotlin.math.roundToInt

/**
 * 単位変換ユーティリティクラス
 */
object UnitConverter {
    
    // 変換定数
    private const val CM_TO_INCHES = 0.393701
    private const val INCHES_TO_CM = 2.54
    private const val KMH_TO_MPH = 0.621371
    private const val MPH_TO_KMH = 1.60934
    private const val G_TO_OZ = 0.035274
    private const val OZ_TO_G = 28.3495
    private const val M_TO_YARDS = 1.09361
    private const val YARDS_TO_M = 0.9144
    
    /**
     * 長さの変換
     * @param value 変換する値（cm）
     * @param unitSystem 変換先の単位系
     * @return 変換後の値と単位ラベルのペア
     */
    fun convertLength(value: Double, unitSystem: UnitSystem): Pair<Double, String> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(value, unitSystem.getLengthUnit())
            UnitSystem.IMPERIAL -> Pair(value * CM_TO_INCHES, unitSystem.getLengthUnit())
        }
    }
    
    /**
     * 長さの変換（フォーマット済み文字列）
     */
    fun formatLength(value: Double, unitSystem: UnitSystem): String {
        val (convertedValue, unit) = convertLength(value, unitSystem)
        return "${String.format("%.1f", convertedValue)} ${unit}"
    }
    
    /**
     * 速度の変換
     * @param value 変換する値（km/h）
     * @param unitSystem 変換先の単位系
     * @return 変換後の値と単位ラベルのペア
     */
    fun convertSpeed(value: Double, unitSystem: UnitSystem): Pair<Double, String> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(value, unitSystem.getSpeedUnit())
            UnitSystem.IMPERIAL -> Pair(value * KMH_TO_MPH, unitSystem.getSpeedUnit())
        }
    }
    
    /**
     * 速度の変換（フォーマット済み文字列）
     */
    fun formatSpeed(value: Double, unitSystem: UnitSystem): String {
        val (convertedValue, unit) = convertSpeed(value, unitSystem)
        return "${String.format("%.1f", convertedValue)} ${unit}"
    }
    
    /**
     * 重量の変換
     * @param value 変換する値（g）
     * @param unitSystem 変換先の単位系
     * @return 変換後の値と単位ラベルのペア
     */
    fun convertWeight(value: Int, unitSystem: UnitSystem): Pair<Double, String> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(value.toDouble(), unitSystem.getWeightUnit())
            UnitSystem.IMPERIAL -> Pair(value * G_TO_OZ, unitSystem.getWeightUnit())
        }
    }
    
    /**
     * 重量の変換（フォーマット済み文字列）
     */
    fun formatWeight(value: Int, unitSystem: UnitSystem): String {
        val (convertedValue, unit) = convertWeight(value, unitSystem)
        return "${String.format("%.1f", convertedValue)} ${unit}"
    }
    
    /**
     * 距離の変換
     * @param value 変換する値（m）
     * @param unitSystem 変換先の単位系
     * @return 変換後の値と単位ラベルのペア
     */
    fun convertDistance(value: Double, unitSystem: UnitSystem): Pair<Double, String> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(value, unitSystem.getDistanceUnit())
            UnitSystem.IMPERIAL -> Pair(value * M_TO_YARDS, unitSystem.getDistanceUnit())
        }
    }
    
    /**
     * 距離の変換（フォーマット済み文字列）
     */
    fun formatDistance(value: Double, unitSystem: UnitSystem): String {
        val (convertedValue, unit) = convertDistance(value, unitSystem)
        return "${String.format("%.1f", convertedValue)} ${unit}"
    }
    
    /**
     * 角度は変換不要（常に度）
     */
    fun formatAngle(value: Double): String {
        return "${String.format("%.1f", value)}°"
    }
    
    /**
     * スコアは変換不要（常に点）
     */
    fun formatScore(value: Int): String {
        return "${value}点"
    }
    
    /**
     * バイオメカニクスデータの単位系統一変換
     */
    fun formatBiomechanicsData(
        headMovement: Double,
        weightShift: Double,
        unitSystem: UnitSystem
    ): Pair<String, String> {
        val headMovementFormatted = formatLength(headMovement, unitSystem)
        val weightShiftFormatted = formatLength(weightShift, unitSystem)
        return Pair(headMovementFormatted, weightShiftFormatted)
    }
    
    /**
     * クラブスペックの単位系統一変換
     */
    fun formatClubSpecs(
        shaftWeight: Int,
        unitSystem: UnitSystem
    ): String {
        return formatWeight(shaftWeight, unitSystem)
    }
    
    // 逆変換メソッド
    
    /**
     * インチからセンチメートルに変換
     */
    fun inchesToCm(inches: Double): Double {
        return inches * INCHES_TO_CM
    }
    
    /**
     * mphからkm/hに変換
     */
    fun mphToKmh(mph: Double): Double {
        return mph * MPH_TO_KMH
    }
    
    /**
     * オンスからグラムに変換
     */
    fun ozToG(oz: Double): Double {
        return oz * OZ_TO_G
    }
    
    /**
     * ヤードからメートルに変換
     */
    fun yardsToM(yards: Double): Double {
        return yards * YARDS_TO_M
    }
}
