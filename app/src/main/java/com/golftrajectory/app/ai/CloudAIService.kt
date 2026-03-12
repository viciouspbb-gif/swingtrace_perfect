package com.golftrajectory.app.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * クラウドAIサービス（将来実装用）
 * 現在はStub実装
 */
@Singleton
class CloudAIService @Inject constructor() {
    
    /**
     * スイングフェーズ分類（クラウド）
     */
    suspend fun classifyPhase(swingData: String): Result<String> {
        // TODO: クラウドサーバーAPI実装
        return Result.success("TAKEBACK") // Stub
    }
    
    /**
     * AIコメント生成（クラウド）
     */
    suspend fun generateComment(swingData: String, phase: String): Result<String> {
        // TODO: クラウドサーバーAPI実装
        return Result.success("クラウドAIによる分析結果") // Stub
    }
    
    /**
     * AIコーチ会話（クラウド）
     */
    suspend fun chatWithCoach(message: String): Result<String> {
        // TODO: クラウドサーバーAPI実装
        return Result.success("クラウドAIコーチの応答") // Stub
    }
}
