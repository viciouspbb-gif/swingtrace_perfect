package com.golftrajectory.app.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini APIキーの提供者
 * BuildConfig直接参照を禁止し、DIで注入する
 */
@Singleton
class GeminiApiKeyProvider @Inject constructor() {
    
    fun getApiKey(): String {
        return com.swingtrace.aicoaching.BuildConfig.GEMINI_API_KEY
    }
}
