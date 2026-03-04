package com.golftrajectory.app.ai

/**
 * プラン階層（旧互換性のため）
 */
enum class PlanTier {
    TIER_LITE,
    TIER_PRO,
    TIER_CLOUD
}

/**
 * 新プラン体系
 */
enum class Plan {
    PRACTICE,
    ATHLETE,
    PRO
}

/**
 * アプリ設定
 */
object AppConfig {
    enum class Mode {
        PRACTICE,
        ATHLETE,
        PRO
    }
    
    var currentMode: Mode = Mode.PRACTICE
    
    fun setMode(mode: Mode) {
        currentMode = mode
    }
    
    fun resetToDefault() {
        currentMode = Mode.PRACTICE
    }
    
    fun isPractice(): Boolean = currentMode == Mode.PRACTICE
    fun isAthlete(): Boolean = currentMode == Mode.ATHLETE
    fun isPro(): Boolean = currentMode == Mode.PRO
}
