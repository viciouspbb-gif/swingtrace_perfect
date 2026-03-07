package com.golftrajectory.app

/**
 * 単位系を定義する列挙型
 */
enum class UnitSystem(val displayName: String, val description: String) {
    METRIC("Metric", "cm, km/h"),
    IMPERIAL("Imperial", "in, mph");
    
    /**
     * 長さの単位ラベルを取得
     */
    fun getLengthUnit(): String {
        return when (this) {
            METRIC -> "cm"
            IMPERIAL -> "in"
        }
    }
    
    /**
     * 速度の単位ラベルを取得
     */
    fun getSpeedUnit(): String {
        return when (this) {
            METRIC -> "km/h"
            IMPERIAL -> "mph"
        }
    }
    
    /**
     * 重量の単位ラベルを取得
     */
    fun getWeightUnit(): String {
        return when (this) {
            METRIC -> "g"
            IMPERIAL -> "oz"
        }
    }
    
    /**
     * 距離の単位ラベルを取得
     */
    fun getDistanceUnit(): String {
        return when (this) {
            METRIC -> "m"
            IMPERIAL -> "yd"
        }
    }
}
