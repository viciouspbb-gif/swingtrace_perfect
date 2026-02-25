package com.swingtrace.aicoaching.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.swingtrace.aicoaching.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * スイング分析リポジトリ
 */
class SwingAnalysisRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.apiService
    private val TAG = "SwingAnalysisRepository"
    
    /**
     * 動画をアップロードして分析
     */
    suspend fun analyzeSwingVideo(videoUri: Uri): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "動画分析開始: $videoUri")
            
            // URIからファイルを作成
            val videoFile = uriToFile(videoUri)
            
            // MultipartBodyを作成
            val requestBody = videoFile.asRequestBody("video/*".toMediaTypeOrNull())
            val videoPart = MultipartBody.Part.createFormData(
                "video",
                videoFile.name,
                requestBody
            )
            
            Log.d(TAG, "サーバーにアップロード中...")
            
            // APIを呼び出し
            val response = apiService.analyzeSwing(videoPart)
            
            // 一時ファイルを削除
            videoFile.delete()
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "分析成功: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorMsg = "分析失敗: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "エラー: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * AIコーチングアドバイスを取得
     */
    suspend fun getAICoaching(
        userId: String,
        swingSpeed: Double,
        backswingTime: Double,
        downswingTime: Double,
        impactSpeed: Double,
        carryDistance: Double
    ): Result<AICoachingResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "AIコーチング取得中...")
            
            val request = AICoachingRequest(
                user_id = userId,
                swing_speed = swingSpeed,
                backswing_time = backswingTime,
                downswing_time = downswingTime,
                impact_speed = impactSpeed,
                carry_distance = carryDistance
            )
            
            val response = apiService.getAICoaching(request)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "AIコーチング取得成功")
                Result.success(response.body()!!)
            } else {
                val errorMsg = "AIコーチング取得失敗: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                
                // 403エラー（制限超過）の場合
                if (response.code() == 403) {
                    Result.failure(Exception("月間制限に達しました。プランをアップグレードしてください。"))
                } else {
                    Result.failure(Exception(errorMsg))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "エラー: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * サブスクリプション情報を取得
     */
    suspend fun getSubscription(userId: String): Result<SubscriptionInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "サブスクリプション情報取得中...")
            
            val response = apiService.getSubscription(userId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "サブスクリプション情報取得成功")
                Result.success(response.body()!!)
            } else {
                val errorMsg = "サブスクリプション情報取得失敗: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "エラー: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * URIからファイルを作成
     */
    private fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("動画ファイルを開けませんでした")
        
        val tempFile = File.createTempFile("swing_video", ".mp4", context.cacheDir)
        
        FileOutputStream(tempFile).use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
}
