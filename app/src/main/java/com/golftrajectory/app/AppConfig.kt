package com.golftrajectory.app

/**
 * アプリ全体のモード設定
 */
object AppConfig {
    enum class Mode {
        PRACTICE,
        ATHLETE,
        PRO
    }

    /** 現在のモード。デフォルトは練習版 */
    var currentMode: Mode = Mode.PRACTICE

    fun isPractice(): Boolean = currentMode == Mode.PRACTICE
    fun isAthlete(): Boolean = currentMode == Mode.ATHLETE
    fun isPro(): Boolean = currentMode == Mode.PRO
}
