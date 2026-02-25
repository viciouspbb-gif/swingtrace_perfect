package com.golftrajectory.app.plan

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * アプリ全体のプラン状態を管理するシングルトンクラス
 *
 * Product Flavors に依存せず、単一アプリ内で UI/機能を切り替えるためのエントリーポイント.
 */
class UserPlanManager private constructor(context: Context) {

    /**
     * プラン種別.
     */
    enum class PlanTier(val displayName: String) {
        TIER_LITE("Lite"),   // 広告ON / AI会話制限 / オフライン処理
        TIER_PRO("Pro"),    // 広告OFF / AIフル機能 / オフライン処理
        TIER_CLOUD("Cloud")   // 広告OFF / AIフル機能 / クラウドAPI処理（将来）
    }

    private val prefs = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentPlan = MutableStateFlow(loadPersistedPlan())

    /**
     * 現在のプランを購読可能な StateFlow.
     */
    val currentPlan: StateFlow<PlanTier> = _currentPlan

    /**
     * 現在のプランを取得.
     */
    fun getPlan(): PlanTier = _currentPlan.value

    /**
     * プランを更新し、永続化する.
     */
    fun setPlan(planTier: PlanTier) {
        if (_currentPlan.value == planTier) return
        _currentPlan.value = planTier
        prefs.edit().putString(KEY_PLAN_TIER, planTier.name).apply()
    }

    /**
     * プラン情報を永続化から読み込み.
     */
    private fun loadPersistedPlan(): PlanTier {
        val saved = prefs.getString(KEY_PLAN_TIER, null)
        return saved?.let {
            runCatching { PlanTier.valueOf(it) }.getOrNull()
        } ?: PlanTier.TIER_LITE
    }

    companion object {
        private const val PREF_NAME = "user_plan_manager"
        private const val KEY_PLAN_TIER = "plan_tier"

        @Volatile
        private var INSTANCE: UserPlanManager? = null

        fun getInstance(context: Context): UserPlanManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPlanManager(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * プランごとの機能可否を簡便に問い合わせるヘルパー.
     */
    val isAdEnabled: Boolean
        get() = when (getPlan()) {
            PlanTier.TIER_LITE -> true
            PlanTier.TIER_PRO, PlanTier.TIER_CLOUD -> false
        }

    val hasFullAIConversations: Boolean
        get() = when (getPlan()) {
            PlanTier.TIER_LITE -> false
            PlanTier.TIER_PRO, PlanTier.TIER_CLOUD -> true
        }

    val allowsCloudProcessing: Boolean
        get() = getPlan() == PlanTier.TIER_CLOUD
}
