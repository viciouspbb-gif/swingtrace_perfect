package com.golftrajectory.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * 使用回数制限管理
 * 無料版: 1日3回まで
 */
class UsageManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "usage_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USAGE_COUNT = "usage_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_AD_REVIVE_COUNT = "ad_revive_count"
        private const val FREE_DAILY_LIMIT = 2 // 無料版: 2回/日
        private const val AD_REVIVE_COUNT = 1 // 広告で+1回復活
        private const val MAX_AD_REVIVE_PER_DAY = 2 // 1日2回まで広告復活可能
        
        // テスト用: trueにすると無制限
        private const val DEBUG_UNLIMITED = true
    }
    
    /**
     * 今日の使用回数を取得
     */
    fun getTodayUsageCount(): Int {
        checkAndResetIfNewDay()
        return prefs.getInt(KEY_USAGE_COUNT, 0)
    }
    
    /**
     * 残り使用可能回数を取得
     */
    fun getRemainingCount(): Int {
        val used = getTodayUsageCount()
        return (FREE_DAILY_LIMIT - used).coerceAtLeast(0)
    }
    
    /**
     * 使用可能かチェック
     */
    fun canUse(): Boolean {
        // テスト用: 無制限モード
        if (DEBUG_UNLIMITED) return true
        
        return getRemainingCount() > 0
    }
    
    /**
     * 使用回数をカウント
     */
    fun incrementUsage() {
        checkAndResetIfNewDay()
        val currentCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        prefs.edit().putInt(KEY_USAGE_COUNT, currentCount + 1).apply()
    }
    
    /**
     * 日付が変わったかチェックしてリセット
     */
    private fun checkAndResetIfNewDay() {
        val today = getCurrentDate()
        val lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "")
        
        if (today != lastResetDate) {
            // 新しい日になったのでリセット
            prefs.edit()
                .putInt(KEY_USAGE_COUNT, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_AD_REVIVE_COUNT, 0) // 広告復活回数もリセット
                .apply()
        }
    }
    
    /**
     * 現在の日付を取得（YYYY-MM-DD形式）
     */
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    
    /**
     * 次のリセット時刻を取得
     */
    fun getNextResetTime(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        val now = System.currentTimeMillis()
        val resetTime = calendar.timeInMillis
        val diff = resetTime - now
        
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
        
        return "${hours}時間${minutes}分後"
    }
    
    /**
     * 広告視聴による復活が可能か
     */
    fun canReviveWithAd(): Boolean {
        checkAndResetIfNewDay()
        val adReviveCount = prefs.getInt(KEY_AD_REVIVE_COUNT, 0)
        return adReviveCount < MAX_AD_REVIVE_PER_DAY && getTodayUsageCount() >= FREE_DAILY_LIMIT
    }
    
    /**
     * 広告視聴で回数を復活
     */
    fun reviveWithAd() {
        checkAndResetIfNewDay()
        val currentUsageCount = prefs.getInt(KEY_USAGE_COUNT, 0)
        val currentAdReviveCount = prefs.getInt(KEY_AD_REVIVE_COUNT, 0)
        
        prefs.edit()
            .putInt(KEY_USAGE_COUNT, currentUsageCount - AD_REVIVE_COUNT)
            .putInt(KEY_AD_REVIVE_COUNT, currentAdReviveCount + 1)
            .apply()
    }
    
    /**
     * 今日の広告復活回数を取得
     */
    fun getAdReviveCount(): Int {
        checkAndResetIfNewDay()
        return prefs.getInt(KEY_AD_REVIVE_COUNT, 0)
    }
    
    /**
     * 残り広告復活可能回数を取得
     */
    fun getRemainingAdReviveCount(): Int {
        return (MAX_AD_REVIVE_PER_DAY - getAdReviveCount()).coerceAtLeast(0)
    }
    
    /**
     * 現在の色を取得（1回目=青、2回目=緑、3回目=赤）
     */
    fun getCurrentColor(): TrajectoryColor {
        val count = getTodayUsageCount()
        return when {
            count == 0 -> TrajectoryColor.BLUE
            count == 1 -> TrajectoryColor.GREEN
            count >= 2 -> TrajectoryColor.RED
            else -> TrajectoryColor.BLUE
        }
    }
    
    enum class TrajectoryColor {
        BLUE,   // 1回目
        GREEN,  // 2回目
        RED     // 3回目
    }
    
    /**
     * デバッグ用: 使用回数をリセット
     */
    fun resetForDebug() {
        prefs.edit()
            .putInt(KEY_USAGE_COUNT, 0)
            .putString(KEY_LAST_RESET_DATE, getCurrentDate())
            .putInt(KEY_AD_REVIVE_COUNT, 0)
            .apply()
    }
}
