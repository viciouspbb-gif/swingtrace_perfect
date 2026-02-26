package com.golftrajectory.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

private val PRACTICE_GREEN = Color(0xFF00FF66)

/**
 * アニメーション付きスイング軌道Canvas
 */
@Composable
fun AnimatedSwingPathCanvas(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 6f,
    animationDuration: Int = 1500,
    showImpactMarker: Boolean = true,
    showScore: Boolean = false,
    score: Float? = null
) {
    val isPracticeMode = AppConfig.isPractice()

    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(pathPoints) {
        if (pathPoints.isNotEmpty()) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = LinearEasing
                )
            )
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pathPoints.isEmpty()) return@Canvas
        
        val offsets = pathPoints.map { Offset(it.first, it.second) }
        val visibleCount = (offsets.size * animatedProgress.value).toInt().coerceAtLeast(1)
        
        // 軌道を描画
        for (i in 1 until visibleCount) {
            val baseColor = phaseColors.getOrElse(i - 1) { Color.Gray }
            val lineColor = if (isPracticeMode) PRACTICE_GREEN else baseColor
            drawLine(
                color = lineColor,
                start = offsets[i - 1],
                end = offsets[i],
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        
        // 開始点マーカー
        if (visibleCount > 0) {
            drawStartMarker(offsets.first())
        }
        
        // 終了点マーカー（アニメーション完了後）
        if (animatedProgress.value >= 1f) {
            drawEndMarker(offsets.last())
        }
        
        // インパクトマーカー
        if (showImpactMarker && animatedProgress.value >= 0.5f) {
            val impactIndex = findImpactIndex(pathPoints, phaseColors)
            if (impactIndex >= 0 && impactIndex < offsets.size) {
                drawImpactMarker(offsets[impactIndex])
            }
        }
        
        // スコア表示
        if (showScore && score != null && animatedProgress.value >= 1f) {
            drawScoreText(score, offsets.last())
        }
    }
}

/**
 * 開始点マーカー
 */
private fun DrawScope.drawStartMarker(position: Offset) {
    // 外側の円（緑）
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 15f,
        center = position
    )
    // 内側の円（白）
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = position
    )
}

/**
 * 終了点マーカー
 */
private fun DrawScope.drawEndMarker(position: Offset) {
    // 外側の円（赤）
    drawCircle(
        color = Color(0xFFF44336),
        radius = 15f,
        center = position
    )
    // 内側の円（白）
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = position
    )
}

/**
 * インパクトマーカー
 */
private fun DrawScope.drawImpactMarker(position: Offset) {
    // 外側の円（黄色）
    drawCircle(
        color = Color(0xFFFFEB3B),
        radius = 30f,
        center = position,
        alpha = 0.5f
    )
    // 中間の円（オレンジ）
    drawCircle(
        color = Color(0xFFFF9800),
        radius = 20f,
        center = position
    )
    // 内側の円（白）
    drawCircle(
        color = Color.White,
        radius = 10f,
        center = position
    )
}

/**
 * スコアテキスト
 */
private fun DrawScope.drawScoreText(score: Float, position: Offset) {
    // スコアカード背景
    drawCircle(
        color = Color(0xFF2196F3),
        radius = 40f,
        center = Offset(position.x + 60f, position.y - 60f)
    )
    
    // TODO: テキスト描画はTextを使用
    // Canvasでテキストを描画するにはnativeCanvasを使用
}

/**
 * インパクト位置を検出
 */
private fun findImpactIndex(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>
): Int {
    // ダウンスイング（赤）からフォロー（緑）への切り替わり点を検出
    for (i in 1 until phaseColors.size) {
        if (phaseColors[i - 1] == Color.Red && phaseColors[i] == Color.Green) {
            return i
        }
    }
    return -1
}

/**
 * 再生制御付きCanvas
 */
@Composable
fun PlayableSwingPathCanvas(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onPlaybackComplete: () -> Unit = {}
) {
    var playbackProgress by remember { mutableStateOf(0f) }
    val isPracticeMode = AppConfig.isPractice()
    
    LaunchedEffect(isPlaying) {
        if (isPlaying && pathPoints.isNotEmpty()) {
            playbackProgress = 0f
            
            // 60fpsで再生
            val frameDuration = 16L // ms
            val totalFrames = pathPoints.size
            
            for (frame in 0..totalFrames) {
                playbackProgress = frame.toFloat() / totalFrames
                kotlinx.coroutines.delay(frameDuration)
            }
            
            onPlaybackComplete()
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pathPoints.isEmpty()) return@Canvas
        
        val offsets = pathPoints.map { Offset(it.first, it.second) }
        val visibleCount = (offsets.size * playbackProgress).toInt().coerceAtLeast(1)
        
        // 軌道を描画
        for (i in 1 until visibleCount) {
            val baseColor = phaseColors.getOrElse(i - 1) { Color.Gray }
            val lineColor = if (isPracticeMode) PRACTICE_GREEN else baseColor
            drawLine(
                color = lineColor,
                start = offsets[i - 1],
                end = offsets[i],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        
        // 現在位置マーカー
        if (visibleCount > 0 && visibleCount <= offsets.size) {
            drawCircle(
                color = Color.Yellow,
                radius = 12f,
                center = offsets[visibleCount - 1]
            )
        }
    }
}
