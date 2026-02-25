package com.golftrajectory.app.usecase

import android.graphics.Bitmap
import com.golftrajectory.app.ClubHeadDetection
import com.golftrajectory.app.ClubHeadDetector

/**
 * クラブヘッド検出UseCase
 */
class DetectClubHeadUseCase(
    private val detector: ClubHeadDetector
) {
    /**
     * クラブヘッドを検出
     * @param bitmap カメラフレーム
     * @return 検出結果（信頼度0.6以上のみ）
     */
    suspend fun execute(bitmap: Bitmap): Result<ClubHeadDetection> {
        return try {
            val detection = detector.detect(bitmap)
            
            if (detection != null && detection.confidence >= 0.6f) {
                Result.success(detection)
            } else {
                Result.failure(Exception("No club head detected"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
