package com.golftrajectory.app.usecase

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * 既存動画分析UseCase
 */
class AnalyzeVideoUseCase(
    private val context: Context,
    private val detectClubHeadUseCase: DetectClubHeadUseCase,
    private val classifySwingPhaseUseCase: ClassifySwingPhaseUseCase
) {
    
    data class AnalysisProgress(
        val currentFrame: Int,
        val totalFrames: Int,
        val progress: Float
    )
    
    data class VideoAnalysisResult(
        val trajectory: List<RecordTrajectoryUseCase.FrameData>,
        val phases: List<ClassifySwingPhaseUseCase.SwingPhase>,
        val duration: Long,
        val fps: Float
    )
    
    /**
     * 動画を分析
     */
    suspend fun analyzeVideo(
        videoUri: Uri,
        useGemini: Boolean = false
    ): Flow<Result<Any>> = flow {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                
                // 動画情報を取得
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloat() ?: 30f
                val totalFrames = ((duration / 1000f) * frameRate).toInt()
                
                val trajectory = mutableListOf<RecordTrajectoryUseCase.FrameData>()
                var frameIndex = 0
                
                // フレームごとに処理
                val frameInterval = (1000000L / frameRate).toLong() // マイクロ秒
                var currentTime = 0L
                
                while (currentTime < duration * 1000) {
                    // フレームを取得
                    val bitmap = retriever.getFrameAtTime(
                        currentTime,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    
                    if (bitmap != null) {
                        // クラブヘッドを検出
                        detectClubHeadUseCase.execute(bitmap)
                            .onSuccess { detection ->
                                trajectory.add(
                                    RecordTrajectoryUseCase.FrameData(
                                        timeMs = currentTime / 1000,
                                        x = detection.position.x,
                                        y = detection.position.y,
                                        confidence = detection.confidence
                                    )
                                )
                            }
                        
                        bitmap.recycle()
                    }
                    
                    frameIndex++
                    currentTime += frameInterval
                    
                    // 進捗を通知
                    emit(Result.success(
                        AnalysisProgress(
                            currentFrame = frameIndex,
                            totalFrames = totalFrames,
                            progress = frameIndex.toFloat() / totalFrames
                        )
                    ))
                }
                
                retriever.release()
                
                // フェーズ分類
                val phases: List<ClassifySwingPhaseUseCase.SwingPhase> = if (useGemini) {
                    val trajectoryString = trajectory.joinToString(",") { "${it.x},${it.y}" }
                    classifySwingPhaseUseCase.classify(trajectoryString)
                        .map { phase -> List(trajectory.size) { phase } }
                        .getOrElse {
                            classifySwingPhaseUseCase.classifyLocally(trajectory)
                        }
                } else {
                    classifySwingPhaseUseCase.classifyLocally(trajectory)
                }
                
                // 結果を返す
                emit(Result.success(
                    VideoAnalysisResult(
                        trajectory = trajectory,
                        phases = phases,
                        duration = duration,
                        fps = frameRate
                    )
                ))
                
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}
