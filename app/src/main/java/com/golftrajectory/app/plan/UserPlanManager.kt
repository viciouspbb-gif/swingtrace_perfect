package com.golftrajectory.app.plan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.planDataStore: DataStore<Preferences> by preferencesDataStore(name = "plan_settings")

enum class Plan {
    PRACTICE,
    ATHLETE,
    PRO;

    fun isPractice(): Boolean = this == PRACTICE
    fun isAthlete(): Boolean = this == ATHLETE
    fun isPro(): Boolean = this == PRO
}

/**
 * アプリ全体のプラン状態を管理するシングルトン
 */
class UserPlanManager private constructor(private val context: Context) {
    private val dataStore = context.planDataStore

    companion object {
        @Volatile
        private var INSTANCE: UserPlanManager? = null

        fun getInstance(context: Context): UserPlanManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPlanManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Debugビルド限定のプランオーバーライド
        private var debugPlanOverride: Plan? = null

        fun setDebugPlanOverride(plan: Plan?) {
            debugPlanOverride = plan
        }
    }

    // DataStore keys
    private val planKey = stringPreferencesKey("last_known_plan")
    private val useCloudKey = booleanPreferencesKey("use_cloud")

    // StateFlow for reactive UI
    private val _planFlow = MutableStateFlow(Plan.PRACTICE)
    val planFlow: Flow<Plan> = _planFlow.asStateFlow()

    private val _useCloudFlow = MutableStateFlow(false)
    val useCloudFlow: Flow<Boolean> = _useCloudFlow.asStateFlow()

    init {
        // 起動時に即時読み込み
        runBlocking {
            loadInitialPlan()
        }
    }

    private suspend fun loadInitialPlan() {
        val preferences = dataStore.data.first()
        val savedPlan = preferences[planKey]?.let { planName ->
            try {
                Plan.valueOf(planName)
            } catch (e: IllegalArgumentException) {
                Plan.PRACTICE
            }
        } ?: Plan.PRACTICE

        val useCloud = preferences[useCloudKey] ?: false

        _planFlow.value = debugPlanOverride ?: savedPlan
        _useCloudFlow.value = useCloud
    }

    suspend fun setPlan(plan: Plan) {
        dataStore.edit { preferences ->
            preferences[planKey] = plan.name
        }
        _planFlow.value = plan
    }

    suspend fun setUseCloud(useCloud: Boolean) {
        dataStore.edit { preferences ->
            preferences[useCloudKey] = useCloud
        }
        _useCloudFlow.value = useCloud
    }

    fun getCurrentPlan(): Plan {
        return debugPlanOverride ?: _planFlow.value
    }

    fun isUseCloud(): Boolean {
        return _useCloudFlow.value
    }

    // UI表示用のプラン名
    fun getPlanDisplayName(): String {
        return when (getCurrentPlan()) {
            Plan.PRACTICE -> "PRACTICE"
            Plan.ATHLETE -> "ATHLETE"
            Plan.PRO -> "PRO"
        }
    }

    // プランごとの機能可否
    val isAdEnabled: Boolean
        get() = getCurrentPlan().isPractice()

    val hasFullAIConversations: Boolean
        get() = !getCurrentPlan().isPractice()

    val allowsCloudProcessing: Boolean
        get() = getCurrentPlan().isPro() && isUseCloud()
}
