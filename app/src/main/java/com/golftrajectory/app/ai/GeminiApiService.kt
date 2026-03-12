package com.swingtrace.aicoaching.ai

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Gemini API v1 インターフェース
 */
interface GeminiApiService {
    // 利用可能なモデル一覧を取得
    @GET("v1/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): ModelListResponse
    
    // 動的にモデルを指定してコンテンツ生成
    @POST("v1/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GeminiRequest,
        @Query("key") apiKey: String
    ): GeminiResponse
}

/**
 * リクエストデータクラス
 */
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>,
    val role: String? = null
)

data class Part(
    val text: String
)

/**
 * レスポンスデータクラス
 */
data class GeminiResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content
)

/**
 * モデル一覧レスポンス
 */
data class ModelListResponse(
    val models: List<ModelInfo>
)

data class ModelInfo(
    val name: String,
    val version: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)
