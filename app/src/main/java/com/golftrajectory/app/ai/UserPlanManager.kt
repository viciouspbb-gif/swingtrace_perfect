package com.golftrajectory.app.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ユーザープラン管理
 */
class UserPlanManager private constructor(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_plan")
        private val PLAN_KEY = stringPreferencesKey("current_plan")
        
        @Volatile
        private var INSTANCE: UserPlanManager? = null
        
        fun getInstance(context: Context): UserPlanManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPlanManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    val currentPlan: Flow<Plan> = context.dataStore.data.map { preferences ->
        val planName = preferences[PLAN_KEY] ?: Plan.PRACTICE.name
        try {
            Plan.valueOf(planName)
        } catch (e: IllegalArgumentException) {
            Plan.PRACTICE
        }
    }
    
    suspend fun setPlan(plan: Plan) {
        context.dataStore.edit { preferences ->
            preferences[PLAN_KEY] = plan.name
        }
    }
    
    suspend fun getCurrentPlan(): Plan {
        var plan = Plan.PRACTICE
        currentPlan.collect { plan = it }
        return plan
    }
}
