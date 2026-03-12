package com.golftrajectory.app.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * 無料枠管理マネージャー
 */
class FreeQuotaManager(private val context: Context) {
    
    companion object {
        // 無料枠制限
        const val FREE_ANALYSIS_PER_DAY = 1               // 無料分析：1日1回
        const val MAX_REWARDED_ADS_PER_DAY = 3            // リワード広告：1日3回まで
        const val FREE_VIDEO_ANALYSIS_PER_DAY = 1         // 動画分析：1日1回
        const val FREE_GEMINI_CLASSIFICATION_PER_DAY = 2  // Gemini分類：1日2回
        
        // DataStore Keys
        private val FREE_ANALYSIS_COUNT = intPreferencesKey("free_analysis_count")
        private val REWARDED_ADS_COUNT = intPreferencesKey("rewarded_ads_count")
        private val VIDEO_COUNT = intPreferencesKey("video_count")
        private val GEMINI_COUNT = intPreferencesKey("gemini_count")
        private val LAST_RESET_DATE = longPreferencesKey("last_reset_date")
        
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quota_prefs")
    }
    
    /**
     * 無料分析の残り回数
     */
    val remainingFreeAnalysis: Flow<Int> = context.dataStore.data.map { prefs ->
        checkAndResetIfNeeded(prefs)
        val used = prefs[FREE_ANALYSIS_COUNT] ?: 0
        (FREE_ANALYSIS_PER_DAY - used).coerceAtLeast(0)
    }
    
    /**
     * リワード広告の残り回数
     */
    val remainingRewardedAds: Flow<Int> = context.dataStore.data.map { prefs ->
        checkAndResetIfNeeded(prefs)
        val used = prefs[REWARDED_ADS_COUNT] ?: 0
        (MAX_REWARDED_ADS_PER_DAY - used).coerceAtLeast(0)
    }
    
    /**
     * 動画分析の残り回数
     */
    val remainingVideoAnalysis: Flow<Int> = context.dataStore.data.map { prefs ->
        checkAndResetIfNeeded(prefs)
        val used = prefs[VIDEO_COUNT] ?: 0
        (FREE_VIDEO_ANALYSIS_PER_DAY - used).coerceAtLeast(0)
    }
    
    /**
     * Gemini分類の残り回数
     */
    val remainingGeminiClassification: Flow<Int> = context.dataStore.data.map { prefs ->
        checkAndResetIfNeeded(prefs)
        val used = prefs[GEMINI_COUNT] ?: 0
        (FREE_GEMINI_CLASSIFICATION_PER_DAY - used).coerceAtLeast(0)
    }
    
    /**
     * 無料分析を使用
     */
    suspend fun useFreeAnalysis(): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            checkAndResetIfNeeded(prefs)
            val current = prefs[FREE_ANALYSIS_COUNT] ?: 0
            if (current < FREE_ANALYSIS_PER_DAY) {
                prefs[FREE_ANALYSIS_COUNT] = current + 1
                result = true
            }
        }
        return result
    }
    
    /**
     * リワード広告を使用
     */
    suspend fun useRewardedAd(): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            checkAndResetIfNeeded(prefs)
            val current = prefs[REWARDED_ADS_COUNT] ?: 0
            if (current < MAX_REWARDED_ADS_PER_DAY) {
                prefs[REWARDED_ADS_COUNT] = current + 1
                result = true
            }
        }
        return result
    }
    
    /**
     * 動画分析を使用
     */
    suspend fun useVideoAnalysis(): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            checkAndResetIfNeeded(prefs)
            val current = prefs[VIDEO_COUNT] ?: 0
            if (current < FREE_VIDEO_ANALYSIS_PER_DAY) {
                prefs[VIDEO_COUNT] = current + 1
                result = true
            }
        }
        return result
    }
    
    /**
     * Gemini分類を使用
     */
    suspend fun useGeminiClassification(): Boolean {
        var result = false
        context.dataStore.edit { prefs ->
            checkAndResetIfNeeded(prefs)
            val current = prefs[GEMINI_COUNT] ?: 0
            if (current < FREE_GEMINI_CLASSIFICATION_PER_DAY) {
                prefs[GEMINI_COUNT] = current + 1
                result = true
            }
        }
        return result
    }
    
    /**
     * 日付が変わったかチェックしてリセット
     */
    private suspend fun checkAndResetIfNeeded(prefs: Preferences) {
        val lastReset = prefs[LAST_RESET_DATE] ?: 0L
        val today = getTodayStartMillis()
        
        if (lastReset < today) {
            context.dataStore.edit { mutablePrefs ->
                mutablePrefs[FREE_ANALYSIS_COUNT] = 0
                mutablePrefs[REWARDED_ADS_COUNT] = 0
                mutablePrefs[VIDEO_COUNT] = 0
                mutablePrefs[GEMINI_COUNT] = 0
                mutablePrefs[LAST_RESET_DATE] = today
            }
        }
    }
    
    /**
     * 今日の0時0分0秒のミリ秒を取得
     */
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 次のリセット時刻を取得
     */
    fun getNextResetTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
