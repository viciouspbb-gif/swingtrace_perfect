package com.swingtrace.aicoaching.api

import android.content.Context
import com.golftrajectory.app.UserPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 認証インターセプター（トークンを自動追加）
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val userPreferences = UserPreferences(context)
        val token = userPreferences.getAuthToken()
        
        val request = if (token != null) {
            // トークンがある場合、Authorizationヘッダーを追加
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            // トークンがない場合はそのまま
            chain.request()
        }
        
        return chain.proceed(request)
    }
}

/**
 * Retrofitクライアント
 */
object RetrofitClient {
    
    // サーバーURL
    private const val BASE_URL = "https://swingtrace-ai-server.onrender.com/"
    
    // 開発用ローカルURL（必要に応じて切り替え）
    // private const val BASE_URL = "http://10.0.2.2:8000/" // Androidエミュレータ用
    // private const val BASE_URL = "http://192.168.0.7:8000/" // 実機用
    
    // Contextを保持（認証インターセプター用）
    private var appContext: Context? = null
    
    /**
     * Contextを初期化（ApplicationまたはActivityから呼び出す）
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    // ログ用インターセプター（メモリ節約のため最小限に）
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC  // BODYからBASICに変更
    }
    
    // OkHttpクライアント（タイムアウト延長）
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(300, TimeUnit.SECONDS)     // 接続タイムアウト: 5分
            .readTimeout(300, TimeUnit.SECONDS)        // 読み取りタイムアウト: 5分
            .writeTimeout(300, TimeUnit.SECONDS)       // 書き込みタイムアウト: 5分
            .callTimeout(600, TimeUnit.SECONDS)        // 全体タイムアウト: 10分
            .retryOnConnectionFailure(true)            // 接続失敗時に自動リトライ
        
        // 認証インターセプターを追加（Contextが初期化されている場合）
        appContext?.let {
            builder.addInterceptor(AuthInterceptor(it))
        }
        
        return builder.build()
    }
    
    // Retrofitインスタンス
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // APIサービス
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
