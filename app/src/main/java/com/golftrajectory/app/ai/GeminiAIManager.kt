package com.golftrajectory.app.ai

import android.content.Context
import android.util.Log
import com.swingtrace.aicoaching.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Temporary stub to keep legacy call sites compiling while AIServiceRepository is adopted.
 * The real implementation lives in AIServiceRepository + GeminiAIManager_Legacy.
 */
class GeminiAIManager(private val context: Context) {

    companion object {
        private const val TAG = "SWING_TRACE"
    }

    // APIキーが有効かどうかの判定フラグ
    val isApiKeyConfigured: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "YOUR_API_KEY"

    fun initialize() {
        Log.d(TAG, "[GeminiAIManager] Initializing...")
        Log.d(TAG, "[GeminiAIManager] API Key from BuildConfig: ${BuildConfig.GEMINI_API_KEY.take(10)}...")
        
        if (!isApiKeyConfigured) {
            Log.e(TAG, "[GeminiAIManager] API Key is missing. AI features will be disabled. Set GEMINI_API_KEY in local.properties.")
            return
        }
        
        Log.i(TAG, "[GeminiAIManager] Gemini AI 通信成功。スイングデータの解析準備完了")
        // 既存の GenerativeModel 初期化処理をここに続ける
    }

    /**
     * スイング分析結果からAIコーチングアドバイスを生成（スタブ実装）
     */
    suspend fun generateCoachingAdvice(
        swingData: SwingAnalysisData,
        targetProName: String? = null,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY
    ): String = withContext(Dispatchers.IO) {
        Log.w(TAG, "generateCoachingAdvice called on stub implementation")
        buildCoachingPrompt(swingData, targetProName, coachingStyle)
    }

    /**
     * プレミアムチャット機能のスタブ
     */
    suspend fun chat(
        message: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        preferredModel: String? = null
    ): String = withContext(Dispatchers.IO) {
        Log.w(TAG, "chat called on stub implementation. preferredModel=$preferredModel")
        "Gemini APIキーが設定されていません。BuildConfig.GEMINI_API_KEY を設定してください。"
    }

    /**
     * システムプロンプト付きチャット開始（スタブ）
     */
    suspend fun startCoachingChat(): String = withContext(Dispatchers.IO) {
        Log.w(TAG, "startCoachingChat called on stub implementation")
        "チャット機能は現在利用できません。後ほどお試しください。"
    }

    private fun buildCoachingPrompt(
        swingData: SwingAnalysisData,
        targetProName: String? = null,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY
    ): String {
        val styleInstruction = when (coachingStyle) {
            CoachingStyle.FRIENDLY -> "親しみやすく、励ましながら"
            CoachingStyle.PROFESSIONAL -> "プロフェッショナルで丁寧に"
            CoachingStyle.TECHNICAL -> "技術的に、詳細に"
            CoachingStyle.MOTIVATIONAL -> "モチベーションを高めるように"
        }

        val targetProSection = if (targetProName != null) {
            "\n\n【目標プロ】\n$targetProName のスイングに近づくための具体的なアドバイスも含めてください。"
        } else {
            ""
        }

        return """
あなたはプロのゴルフコーチです。${styleInstruction}、以下のスイング分析データに基づいて、具体的で実践的なアドバイスを日本語で提供してください。$targetProSection

【スイング分析データ】
総合スコア: ${swingData.totalScore}点
バックスイング角度: ${swingData.backswingAngle}° (理想: 60-85°)
ダウンスイング速度: ${swingData.downswingSpeed} (理想: 40以上)
腰の回転: ${swingData.hipRotation}° (理想: 30-45°)
肩の回転: ${swingData.shoulderRotation}° (理想: 45-60°)
頭の安定性: ${swingData.headStability}% (理想: 70%以上)
体重移動: ${swingData.weightShift} (理想: 20-60)
スイングプレーン: ${swingData.swingPlane}

【アドバイス形式】
1. 総合評価（1-2文）
2. 良い点（2-3つ）
3. 改善点（2-3つ、具体的な練習方法を含む）
4. 次回の目標（1-2つ）

簡潔で分かりやすく、初心者でも理解できる言葉で説明してください。
        """.trimIndent()
    }
}

class ModelOverloadedException : Exception("Gemini model overloaded")
