package com.golftrajectory.app

import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import kotlin.math.*

/**
 * スイングフェーズ検出器（worldLandmarks対応版）
 * ADDRESS/TOPフレームの検出をworldLandmarksで実行
 */
class SwingPhaseDetectorWorld {
    
    companion object {
        private const val TAG = "SwingPhaseWorld"
    }
    
    private val poseHistory = mutableListOf<PoseFrame>()
    private var currentPhase = SwingPhase.SETUP
    
    /**
     * 1フレームのposeデータ
     */
    data class PoseFrame(
        val worldLandmarks: List<Landmark>,
        val timestampMs: Long,
        var phase: SwingPhase = SwingPhase.SETUP
    )
    
    /**
     * ADDRESS/TOPフレーム検出結果
     */
    data class KeyFrames(
        val addressIndex: Int,
        val topIndex: Int,
        val addressFrame: PoseFrame,
        val topFrame: PoseFrame
    )
    
    /**
     * 新しいフレームを追加してフェーズを検出
     */
    fun addFrame(worldLandmarks: List<Landmark>, timestampMs: Long): SwingPhase {
        val frame = PoseFrame(worldLandmarks, timestampMs, currentPhase)
        poseHistory.add(frame)
        
        // 履歴を最新60フレームに制限
        if (poseHistory.size > 60) {
            poseHistory.removeAt(0)
        }
        
        // フェーズを検出
        currentPhase = detectCurrentPhase()
        
        // 最新フレームのフェーズを更新
        poseHistory.lastOrNull()?.let { it.phase = currentPhase }
        
        return currentPhase
    }
    
    /**
     * 現在のフェーズを検出
     */
    private fun detectCurrentPhase(): SwingPhase {
        if (poseHistory.size < 3) return SwingPhase.SETUP
        
        // 手首のY座標変化を検出
        val wristHeights = poseHistory.mapNotNull { frame ->
            if (frame.worldLandmarks.size > 16) {
                frame.worldLandmarks[16].y()
            } else null
        }
        
        if (wristHeights.size < 3) return SwingPhase.SETUP
        
        val recent = wristHeights.takeLast(10)
        if (recent.size < 3) return currentPhase
        
        // ADDRESS検出：安定した低い位置
        val isAddress = detectAddressPhase(recent)
        
        // TOP検出：最高点付近
        val isTop = detectTopPhase(recent)
        
        return when {
            isTop -> SwingPhase.TOP
            isAddress && currentPhase != SwingPhase.TOP -> SwingPhase.SETUP
            recent.last() > recent.first() -> SwingPhase.BACKSWING
            recent.last() < recent.first() -> SwingPhase.DOWNSWING
            else -> currentPhase
        }
    }
    
    /**
     * ADDRESSフェーズを検出（安定区間）
     */
    private fun detectAddressPhase(wristHeights: List<Float>): Boolean {
        if (wristHeights.size < 5) return false
        
        // 分散が小さい（安定）かつ低い位置
        val avg = wristHeights.average()
        val variance = wristHeights.map { (it - avg).pow(2) }.average()
        
        return variance < 0.001f && avg < 0.3f // 閾値は調整必要
    }
    
    /**
     * TOPフェーズを検出（最高点）- 符号反転対応
     */
    private fun detectTopPhase(wristHeights: List<Float>): Boolean {
        if (wristHeights.size < 3) return false
        
        // 上昇から下降への転換点
        val recent = wristHeights.takeLast(5)
        if (recent.size < 3) return false
        
        val trend = recent.last() - recent.first()
        // 符号反転：Yの極大値（最高点）を検出
        val isTurningPoint = recent[2] <= recent[1] && recent[2] <= recent[3] // Yの極大値（最高点）
        
        return isTurningPoint && trend > 0
    }
    
    /**
     * ADDRESSとTOPフレームを検出
     */
    fun findAddressAndTopFrames(): KeyFrames? {
        if (poseHistory.size < 10) return null
        
        // ADDRESSフレーム：前半の安定区間
        val addressIndex = findAddressFrameIndex() ?: return null
        val addressFrame = poseHistory[addressIndex]
        
        // TOPフレーム：手首が最も高い位置
        val topIndex = findTopFrameIndex(addressIndex) ?: return null
        val topFrame = poseHistory[topIndex]
        
        Log.d(TAG, "Key frames found - Address: $addressIndex, Top: $topIndex")
        
        return KeyFrames(addressIndex, topIndex, addressFrame, topFrame)
    }
    
    /**
     * ADDRESSフレームインデックスを検出
     */
    private fun findAddressFrameIndex(): Int? {
        val earlyFrames = poseHistory.take(poseHistory.size / 3)
        if (earlyFrames.size < 5) return null
        
        // 手首の高さが最も安定しているフレーム
        var bestIndex = 0
        var minVariance = Double.MAX_VALUE
        
        for (i in 0 until earlyFrames.size - 4) {
            val heights = mutableListOf<Float>()
            
            for (j in 0 until 5) {
                val frameIndex = i + j
                if (frameIndex < earlyFrames.size && earlyFrames[frameIndex].worldLandmarks.size > 16) {
                    heights.add(earlyFrames[frameIndex].worldLandmarks[16].y())
                }
            }
            
            if (heights.size >= 3) {
                val avg = heights.average()
                val variance = heights.map { (it - avg).pow(2) }.average()
                
                if (variance < minVariance) {
                    minVariance = variance
                    bestIndex = i
                }
            }
        }
        
        return if (minVariance < 0.01f) bestIndex else null
    }
    
    /**
     * TOPフレームインデックスを検出 - 符号反転対応
     */
    private fun findTopFrameIndex(afterAddressIndex: Int): Int? {
        val searchFrames = poseHistory.drop(afterAddressIndex + 2)
        if (searchFrames.isEmpty()) return null
        
        var minHeight = Float.MAX_VALUE
        var topIndex = -1
        
        for ((relativeIndex, frame) in searchFrames.withIndex()) {
            if (frame.worldLandmarks.size > 16) {
                val wristY = frame.worldLandmarks[16].y()
                
                // 符号反転：Y座標が小さいほど高い位置
                if (wristY < minHeight) {
                    minHeight = wristY
                    topIndex = afterAddressIndex + 2 + relativeIndex
                }
            }
        }
        
        return if (topIndex >= 0 && topIndex < poseHistory.size) topIndex else null
    }
    
    /**
     * worldLandmarksリストから直接ADDRESS/TOPを検出
     */
    fun findKeyFramesFromLandmarks(allFrames: List<Pair<List<Landmark>, Long>>): KeyFrames? {
        // 一時的にクリアして新しいデータで処理
        val originalHistory = poseHistory.toList()
        poseHistory.clear()
        
        try {
            // 全フレームを追加
            for ((landmarks, timestamp) in allFrames) {
                addFrame(landmarks, timestamp)
            }
            
            // キーフレームを検出
            return findAddressAndTopFrames()
        } finally {
            // 元の履歴を復元
            poseHistory.clear()
            poseHistory.addAll(originalHistory)
        }
    }
    
    /**
     * 現在のフェーズを取得
     */
    fun getCurrentPhase(): SwingPhase = currentPhase
    
    /**
     * 履歴をクリア
     */
    fun reset() {
        poseHistory.clear()
        currentPhase = SwingPhase.SETUP
    }
    
    /**
     * デバッグ情報を出力
     */
    fun logDebugInfo() {
        Log.d(TAG, "Current phase: $currentPhase, History size: ${poseHistory.size}")
        
        if (poseHistory.isNotEmpty()) {
            val lastFrame = poseHistory.last()
            if (lastFrame.worldLandmarks.size > 16) {
                val wristY = lastFrame.worldLandmarks[16].y()
                Log.d(TAG, "Latest wrist Y: $wristY")
            }
        }
    }
}
