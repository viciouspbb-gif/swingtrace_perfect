package com.golftrajectory.app.usecase

import androidx.compose.ui.graphics.Color
import com.golftrajectory.app.AppConfig
import kotlin.math.hypot

/**
 * 軌道描画UseCase
 */
class DrawTrajectoryUseCase {
    
    data class DrawablePoint(
        val x: Float,
        val y: Float,
        val color: Color
    )
    
    /**
     * 描画用データを生成
     */
    fun prepareDrawData(
        trajectory: List<RecordTrajectoryUseCase.FrameData>,
        phases: List<ClassifySwingPhaseUseCase.SwingPhase>
    ): List<DrawablePoint> {
        if (trajectory.isEmpty()) return emptyList()
        
        return trajectory.mapIndexed { index, frame ->
            val phase = phases.getOrNull(index - 1) ?: ClassifySwingPhaseUseCase.SwingPhase.TAKEBACK
            val baseColor = phaseToColor(phase)
            val color = when {
                AppConfig.isPractice() -> PRACTICE_COLOR
                AppConfig.isAthlete() -> determineAthleteColor(index, trajectory, phases, baseColor)
                else -> baseColor
            }
            
            DrawablePoint(
                x = frame.x,
                y = frame.y,
                color = color
            )
        }
    }
    
    /**
     * フェーズを色に変換
     */
    private fun phaseToColor(phase: ClassifySwingPhaseUseCase.SwingPhase): Color {
        return when (phase) {
            ClassifySwingPhaseUseCase.SwingPhase.TAKEBACK -> Color.Blue
            ClassifySwingPhaseUseCase.SwingPhase.DOWNSWING -> Color.Red
            ClassifySwingPhaseUseCase.SwingPhase.FOLLOW -> Color.Green
        }
    }
    
    private fun determineAthleteColor(
        index: Int,
        trajectory: List<RecordTrajectoryUseCase.FrameData>,
        phases: List<ClassifySwingPhaseUseCase.SwingPhase>,
        fallback: Color
    ): Color {
        val frame = trajectory.getOrNull(index) ?: return fallback
        val previous = trajectory.getOrNull((index - 1).coerceAtLeast(0)) ?: frame
        val dx = frame.x - previous.x
        val dy = frame.y - previous.y
        val speedScore = (hypot(dx.toDouble(), dy.toDouble()).times(8.0)).coerceIn(0.0, 1.0).toFloat()
        val stabilityScore = frame.confidence.coerceIn(0f, 1f)
        val phaseScore = when (phases.getOrNull(index - 1)) {
            ClassifySwingPhaseUseCase.SwingPhase.DOWNSWING -> 1.0f
            ClassifySwingPhaseUseCase.SwingPhase.FOLLOW -> 0.8f
            ClassifySwingPhaseUseCase.SwingPhase.TAKEBACK, null -> 0.6f
        }

        val composite = (speedScore * 0.45f) + (stabilityScore * 0.35f) + (phaseScore * 0.2f)

        return when {
            composite >= 0.75f -> ATHLETE_GREEN
            composite >= 0.45f -> ATHLETE_YELLOW
            else -> ATHLETE_RED
        }
    }

    companion object {
        private val PRACTICE_COLOR = Color(0xFF00FF66)
        private val ATHLETE_GREEN = Color(0xFF00E676)
        private val ATHLETE_YELLOW = Color(0xFFFFC107)
        private val ATHLETE_RED = Color(0xFFFF5252)
    }
    
    /**
     * パスポイントとカラーリストを取得（Canvas用）
     */
    fun getPathData(drawablePoints: List<DrawablePoint>): Pair<List<Pair<Float, Float>>, List<Color>> {
        val pathPoints = drawablePoints.map { Pair(it.x, it.y) }
        val colors = drawablePoints.map { it.color }
        return Pair(pathPoints, colors)
    }
}
