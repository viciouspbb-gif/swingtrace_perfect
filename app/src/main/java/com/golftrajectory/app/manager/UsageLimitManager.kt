package com.swingtrace.aicoaching.manager

import android.content.Context
import android.content.SharedPreferences
import java.util.*

/**
 * 使用回数制限管理
 */
class UsageLimitManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "usage_limits",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_AI_COACHING_COUNT = "ai_coaching_count"
        private const val KEY_CHAT_COACHING_COUNT = "chat_coaching_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        
        // 月間制限
        const val PREMIUM_MONTHLY_LIMIT = 50      // 有料版：月50回
        const val PRO_AI_COACHING_LIMIT = 200     // プレミアム版：月200回
        const val PRO_CHAT_COACHING_LIMIT = 100   // プレミアム版：対話式月100回
    }
    
    /**
     * 今月のAIコーチング使用回数を取得
     */
    fun getAICoachingCount(): Int {
        checkAndResetIfNewMonth()
        return prefs.getInt(KEY_AI_COACHING_COUNT, 0)
    }
    
    /**
     * AIコーチング使用回数を増やす
     */
    fun incrementAICoachingCount() {
        checkAndResetIfNewMonth()
        val currentCount = getAICoachingCount()
        prefs.edit().putInt(KEY_AI_COACHING_COUNT, currentCount + 1).apply()
    }
    
    /**
     * 対話式コーチング使用回数を取得
     */
    fun getChatCoachingCount(): Int {
        checkAndResetIfNewMonth()
        return prefs.getInt(KEY_CHAT_COACHING_COUNT, 0)
    }
    
    /**
     * 対話式コーチング使用回数を増やす
     */
    fun incrementChatCoachingCount() {
        checkAndResetIfNewMonth()
        val currentCount = getChatCoachingCount()
        prefs.edit().putInt(KEY_CHAT_COACHING_COUNT, currentCount + 1).apply()
    }
    
    /**
     * AIコーチング使用可能かチェック
     */
    fun canUseAICoaching(isPremium: Boolean, isProPlan: Boolean): Boolean {
        if (isProPlan) {
            // プレミアム版は月200回まで
            return getAICoachingCount() < PRO_AI_COACHING_LIMIT
        }
        
        if (isPremium) {
            // 有料版は月50回まで
            return getAICoachingCount() < PREMIUM_MONTHLY_LIMIT
        }
        
        // 無料版はAIコーチング不可
        return false
    }
    
    /**
     * 対話式AIコーチング使用可能かチェック
     */
    fun canUseChatCoaching(isProPlan: Boolean): Boolean {
        if (!isProPlan) {
            // プレミアム版のみ
            return false
        }
        
        // 月100回まで
        return getChatCoachingCount() < PRO_CHAT_COACHING_LIMIT
    }
    
    /**
     * AIコーチング残り回数を取得
     */
    fun getRemainingAICoachingCount(isPremium: Boolean, isProPlan: Boolean): Int {
        if (isProPlan) {
            val used = getAICoachingCount()
            return (PRO_AI_COACHING_LIMIT - used).coerceAtLeast(0)
        }
        
        if (isPremium) {
            val used = getAICoachingCount()
            return (PREMIUM_MONTHLY_LIMIT - used).coerceAtLeast(0)
        }
        
        return 0
    }
    
    /**
     * 対話式AIコーチング残り回数を取得
     */
    fun getRemainingChatCoachingCount(isProPlan: Boolean): Int {
        if (!isProPlan) {
            return 0
        }
        
        val used = getChatCoachingCount()
        return (PRO_CHAT_COACHING_LIMIT - used).coerceAtLeast(0)
    }
    
    /**
     * 月が変わったらリセット
     */
    private fun checkAndResetIfNewMonth() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
        
        val lastResetMonth = prefs.getInt(KEY_LAST_RESET_DATE, 0)
        
        if (currentMonth != lastResetMonth) {
            // 月が変わったのでリセット
            prefs.edit()
                .putInt(KEY_AI_COACHING_COUNT, 0)
                .putInt(KEY_LAST_RESET_DATE, currentMonth)
                .apply()
        }
    }
    
    /**
     * 手動リセット（テスト用）
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
}
