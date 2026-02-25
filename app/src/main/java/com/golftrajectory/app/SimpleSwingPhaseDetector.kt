package com.golftrajectory.app

/**
 * シンプルなスイングフェーズ検出器
 * dx/dyベースの判定
 */
class SimpleSwingPhaseDetector {
    
    enum class SwingPhase {
        TAKEBACK,   // テイクバック（青）
        DOWNSWING,  // ダウンスイング（赤）
        FOLLOW      // フォロー（緑）
    }
    
    data class TrajectoryPoint(
        val timestamp: Long,
        val x: Float,
        val y: Float
    )
    
    /**
     * スイングフェーズを検出
     * @param points 時系列の軌道ポイント
     * @return 各ポイントのフェーズ
     */
    fun detectSwingPhases(points: List<TrajectoryPoint>): List<SwingPhase> {
        if (points.size < 2) return emptyList()
        
        val phases = mutableListOf<SwingPhase>()
        
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            
            val phase = when {
                // テイクバック：左方向（dx < 0）
                dx < 0 -> SwingPhase.TAKEBACK
                
                // ダウンスイング：右方向 + 下方向（dx > 0 && dy > 0）
                dx > 0 && dy > 0 -> SwingPhase.DOWNSWING
                
                // フォロー：右方向 + 上方向（dx > 0 && dy < 0）
                dx > 0 && dy < 0 -> SwingPhase.FOLLOW
                
                // その他：前のフェーズを維持
                else -> phases.lastOrNull() ?: SwingPhase.TAKEBACK
            }
            
            phases.add(phase)
        }
        
        return phases
    }
    
    /**
     * フェーズを色に変換
     */
    fun phaseToColor(phase: SwingPhase): androidx.compose.ui.graphics.Color {
        return when (phase) {
            SwingPhase.TAKEBACK -> androidx.compose.ui.graphics.Color.Blue
            SwingPhase.DOWNSWING -> androidx.compose.ui.graphics.Color.Red
            SwingPhase.FOLLOW -> androidx.compose.ui.graphics.Color.Green
        }
    }
}
