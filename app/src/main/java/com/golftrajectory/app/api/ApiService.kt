package com.swingtrace.aicoaching.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * SwingTrace AI Server API
 */
interface ApiService {
    
    /**
     * スイング動画を分析
     */
    @Multipart
    @POST("api/analyze-swing")
    suspend fun analyzeSwing(
        @Part video: MultipartBody.Part
    ): Response<AnalysisResult>
    
    /**
     * AIコーチングアドバイスを取得
     */
    @POST("api/ai-coaching")
    suspend fun getAICoaching(
        @Body request: AICoachingRequest
    ): Response<AICoachingResponse>
    
    /**
     * ユーザーのサブスクリプション情報を取得
     */
    @GET("api/subscription/{user_id}")
    suspend fun getSubscription(
        @Path("user_id") userId: String
    ): Response<SubscriptionInfo>
    
    /**
     * 利用可能なプラン一覧を取得
     */
    @GET("api/plans")
    suspend fun getPlans(): Response<PlansResponse>
    
    /**
     * 新規ユーザー登録
     */
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>
    
    /**
     * ログイン
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>
    
    /**
     * ログアウト
     */
    @POST("api/auth/logout")
    suspend fun logout(): Response<LogoutResponse>
    
    /**
     * 現在のユーザー情報を取得
     */
    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<UserInfo>
}

// データモデル
data class AnalysisResult(
    val ball_detected: Boolean,
    val trajectory: List<TrajectoryPoint>,
    val carry_distance: Double,
    val max_height: Double,
    val flight_time: Double,
    val swing_data: SwingData?,
    val confidence: Double,
    val trajectory_video_path: String? = null  // 弾道線付き動画のパス
)

data class TrajectoryPoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val time: Double
)

data class SwingData(
    val swing_speed: Double,
    val backswing_time: Double,
    val downswing_time: Double,
    val impact_speed: Double,
    val tempo: Double
)

data class AICoachingRequest(
    val user_id: String,
    val swing_speed: Double,
    val backswing_time: Double,
    val downswing_time: Double,
    val impact_speed: Double,
    val carry_distance: Double
)

data class AICoachingResponse(
    val advice: String,
    val improvements: List<String>,
    val strengths: List<String>,
    val score: Int
)

data class SubscriptionInfo(
    val user_id: String,
    val plan_type: String,
    val plan_name: String,
    val price: Int,
    val monthly_limit: Int,
    val monthly_used: Int,
    val remaining: Int,
    val reset_date: String,
    val features: List<String>
)

data class PlansResponse(
    val plans: List<Plan>
)

data class Plan(
    val type: String,
    val name: String,
    val price: Int,
    val monthly_limit: Int,
    val features: List<String>
)

// 認証用データモデル
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val user_id: String,
    val email: String,
    val name: String
)

data class LogoutResponse(
    val success: Boolean,
    val message: String
)

data class UserInfo(
    val user_id: String,
    val email: String,
    val name: String,
    val created_at: String
)
