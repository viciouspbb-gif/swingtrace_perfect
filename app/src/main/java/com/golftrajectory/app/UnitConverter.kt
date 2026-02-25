package com.golftrajectory.app

/**
 * 単位変換ユーティリティ
 */
object UnitConverter {
    private const val METERS_TO_YARDS = 1.09361
    
    enum class DistanceUnit {
        METERS, YARDS
    }
    
    /**
     * メートルをヤードに変換
     */
    fun metersToYards(meters: Double): Double {
        return meters * METERS_TO_YARDS
    }
    
    /**
     * ヤードをメートルに変換
     */
    fun yardsToMeters(yards: Double): Double {
        return yards / METERS_TO_YARDS
    }
    
    /**
     * 距離を指定された単位で表示
     */
    fun formatDistance(meters: Double, unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.METERS -> "%.1f m".format(meters)
            DistanceUnit.YARDS -> "%.1f yd".format(metersToYards(meters))
        }
    }
    
    /**
     * 単位名を取得
     */
    fun getUnitName(unit: DistanceUnit): String {
        return when (unit) {
            DistanceUnit.METERS -> "メートル"
            DistanceUnit.YARDS -> "ヤード"
        }
    }
}
