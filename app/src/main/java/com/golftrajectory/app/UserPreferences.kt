package com.golftrajectory.app

import android.content.Context
import android.content.SharedPreferences

/**
 * ユーザー設定を管理するクラス
 */
class UserPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "swing_trace_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_HAND_PREFERENCE = "hand_preference"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_AD_COUNT = "ad_count"
        private const val KEY_ANALYSIS_COUNT = "analysis_count"
        private const val KEY_COACHING_STYLE = "coaching_style"
        private const val KEY_TARGET_PRO = "target_pro"
        
        const val HAND_RIGHT = "right"
        const val HAND_LEFT = "left"
    }
    
    /**
     * 利き手設定を保存
     */
    fun setHandPreference(hand: String) {
        prefs.edit().putString(KEY_HAND_PREFERENCE, hand).apply()
    }
    
    /**
     * 利き手設定を取得（デフォルト: 右打ち）
     */
    fun getHandPreference(): String {
        return prefs.getString(KEY_HAND_PREFERENCE, HAND_RIGHT) ?: HAND_RIGHT
    }
    
    /**
     * 右打ちかどうか
     */
    fun isRightHanded(): Boolean {
        return getHandPreference() == HAND_RIGHT
    }
    
    /**
     * 左打ちかどうか
     */
    fun isLeftHanded(): Boolean {
        return getHandPreference() == HAND_LEFT
    }
    
    /**
     * 初回起動かどうか
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * 初回起動フラグをクリア
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    // ========== 認証トークン管理 ==========
    
    /**
     * 認証トークンを保存
     */
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
    
    /**
     * 認証トークンを取得
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    /**
     * ログインしているか
     */
    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }
    
    /**
     * ユーザー情報を保存
     */
    fun saveUserInfo(userId: String, email: String, name: String) {
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .apply()
    }
    
    /**
     * ユーザーIDを取得
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * ユーザーメールを取得
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * ユーザー名を取得
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    /**
     * ログアウト（すべての認証情報を削除）
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_IS_GUEST)
            .apply()
    }
    
    // ========== ゲストモード管理 ==========
    
    /**
     * ゲストモードを有効化
     */
    fun setGuestMode() {
        prefs.edit()
            .putBoolean(KEY_IS_GUEST, true)
            .putString(KEY_USER_ID, "guest_${System.currentTimeMillis()}")
            .putString(KEY_USER_NAME, "ゲスト")
            .apply()
    }
    
    /**
     * ゲストモードかどうか
     */
    fun isGuest(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }
    
    /**
     * 広告視聴回数を取得
     */
    fun getAdCount(): Int {
        return prefs.getInt(KEY_AD_COUNT, 0)
    }
    
    /**
     * 広告視聴回数を増やす
     */
    fun incrementAdCount() {
        val current = getAdCount()
        prefs.edit().putInt(KEY_AD_COUNT, current + 1).apply()
    }
    
    /**
     * 分析回数を取得
     */
    fun getAnalysisCount(): Int {
        return prefs.getInt(KEY_ANALYSIS_COUNT, 0)
    }
    
    /**
     * 分析回数を増やす
     */
    fun incrementAnalysisCount() {
        val current = getAnalysisCount()
        prefs.edit().putInt(KEY_ANALYSIS_COUNT, current + 1).apply()
    }
    
    /**
     * 広告視聴回数をリセット
     */
    fun resetAdCount() {
        prefs.edit().putInt(KEY_AD_COUNT, 0).apply()
    }
    
    /**
     * ゲストユーザーが分析可能かチェック
     * 2回の広告視聴で1回分析可能
     */
    fun canAnalyzeAsGuest(): Boolean {
        return getAdCount() >= 2
    }
    
    /**
     * ゲストユーザーの分析を消費
     */
    fun consumeGuestAnalysis() {
        val adCount = getAdCount()
        if (adCount >= 2) {
            prefs.edit().putInt(KEY_AD_COUNT, adCount - 2).apply()
            incrementAnalysisCount()
        }
    }
    
    // ========== コーチング設定 ==========
    
    /**
     * コーチングスタイルを保存
     */
    fun saveCoachingStyle(style: String) {
        prefs.edit().putString(KEY_COACHING_STYLE, style).apply()
    }
    
    /**
     * コーチングスタイルを取得
     */
    fun getCoachingStyle(): String? {
        return prefs.getString(KEY_COACHING_STYLE, null)
    }
    
    /**
     * 目標プロを保存
     */
    fun saveTargetPro(proName: String) {
        prefs.edit().putString(KEY_TARGET_PRO, proName).apply()
    }
    
    /**
     * 目標プロを取得
     */
    fun getTargetPro(): String? {
        return prefs.getString(KEY_TARGET_PRO, null)
    }
}

