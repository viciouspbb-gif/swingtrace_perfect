package com.swingtrace.aicoaching.ai

import android.content.Context
import android.util.Log
import com.swingtrace.aicoaching.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Gemini AI 管理クラス
 */
class GeminiAIManager(private val context: Context) {
    companion object {
        private const val TAG = "GeminiAIManager"
        private const val API_BASE_URL = "https://generativelanguage.googleapis.com/"
        
        // モデル定義
        const val MODEL_FLASH = "gemini-1.5-flash"  // 高速モデル
        const val MODEL_PRO = "gemini-1.5-pro"      // 高精度モデル
        private val actionKeywords = listOf("ドリル", "意識", "素振り", "確認", "練習")
        private val tonePrefixes = listOf(
            "確認いたしました。", "良い質問です。", "データを見ると", "解析結果によれば", "直近の計測では"
        )
        private val topicDictionary = mapOf(
            "バックスイング" to listOf("バックスイング", "テイクバック"),
            "ダウンスイング" to listOf("ダウンスイング"),
            "飛距離" to listOf("飛距離", "距離"),
            "角度" to listOf("角度"),
            "腰の回転" to listOf("腰", "ヒップ", "腰の回転"),
            "肩の回転" to listOf("肩", "ショルダー", "肩の回転")
        )

        private fun buildSystemInstruction(
            lastUserMessage: String?,
            lastCoachMessage: String?,
            isQuantitativeQuestion: Boolean,
            topicKeyword: String?
        ): String {
            val topic = topicKeyword ?: "該当項目"
            return """
システム指示:
1. 直前ユーザー: ${lastUserMessage ?: "なし"} / 直前回答: ${lastCoachMessage ?: "なし"} を参照し矛盾を避ける。
2. 応答は分析結果を参照し、ユーザーが触れた項目「$topic」を必ず文中に含める。
3. 分析データの具体的事実（例: $topic に関する計測値）を引用して論理展開する。
4. 100文字以内で収め、参照した項目名とドリル名/意識すべきアクションをセットで含める。
5. 数値質問=${if (isQuantitativeQuestion) "はい" else "いいえ"} の場合も同様に制約→定性的コメント→ドリル提案の順序で、謝罪は禁止。
6. 日本語・敬体・絵文字/Markdown禁止。
            """.trimIndent()
        }

    private fun isQuantitativeQuestion(text: String): Boolean {
        val normalized = text.replace("？", "?")
        val keywords = listOf(
            "何度", "角度", "スコア", "数値", "距離", "ヤード", "m/s", "速度", "スピード",
            "点", "どのくらい", "どれくらい", "計測", "パーセンテージ"
        )
        return keywords.any { normalized.contains(it) } || normalized.any { it.isDigit() }
    }

    private fun detectTopicKeyword(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val normalized = text.replace("？", "").replace("?", "")
        return topicDictionary.entries.firstOrNull { (_, variations) ->
            variations.any { normalized.contains(it) }
        }?.key
    }

    private fun buildTonePrefix(seedSource: String?): String {
        if (seedSource.isNullOrBlank()) return tonePrefixes.first()
        val index = seedSource.hashCode().absoluteValue % tonePrefixes.size
        return tonePrefixes[index]
    }

    private fun normalizeCoachResponse(
        raw: String,
        isQuantitativeQuestion: Boolean,
        topicKeyword: String?,
        seedSource: String?
    ): String {
        var result = raw.replace("\n", " ").replace(Regex("\\s+"), " ").trim()
        if (result.isEmpty()) {
            result = "データは把握済み。テンポは安定気味、ハーフスイングドリルで再確認しましょう。"
        }
        val tonePrefix = buildTonePrefix(seedSource)
        val topicPart = topicKeyword?.let { "${it}の計測から見ると" } ?: "分析結果では"
        var enriched = "$tonePrefix$topicPart $result"
        if (actionKeywords.none { enriched.contains(it) }) {
            enriched = (enriched.take((100 - 12).coerceAtLeast(0)) + " ハーフスイングドリルで整えましょう").trim()
        }
        if (isQuantitativeQuestion && !enriched.contains("制約")) {
            enriched = "計測値は制約で直答不可。$enriched"
        }
        return if (enriched.length > 100) enriched.take(100) else enriched
    }
}

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",  // SDK 0.9.0互換モデル
        apiKey = apiKey
    )
    
    // OkHttpクライアント（タイムアウト設定）
    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "Request URL: ${request.url}")
            Log.d(TAG, "Request Headers: ${request.headers}")
            val response = chain.proceed(request)
            Log.d(TAG, "Response Code: ${response.code}")
            response
        }
        .build()
    
    // Retrofit インスタンス
    private val retrofit = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val geminiApi = retrofit.create(GeminiApiService::class.java)
    private val actionKeywords = listOf("ドリル", "意識", "素振り", "確認", "練習")
    
    // キャッシュされたモデル名
    private var cachedModelName: String? = null
    
    /**
     * 利用可能なモデルを取得
     */
    private suspend fun getUsableModel(): String {
        if (apiKey.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY is blank. Please set BuildConfig.GEMINI_API_KEY.")
            return "gemini-1.5-pro"
        }
        // キャッシュがあればそれを使用
        cachedModelName?.let { return it }
        
        return try {
            val response = geminiApi.listModels(apiKey)
            val usable = response.models.firstOrNull { model ->
                model.supportedGenerationMethods?.contains("generateContent") == true
            }
            val modelName = usable?.name ?: "models/gemini-1.5-pro"
            
            // models/ プレフィックスを削除
            val cleanModelName = modelName.removePrefix("models/")
            
            Log.d(TAG, "Selected model: $cleanModelName")
            cachedModelName = cleanModelName
            cleanModelName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch models, using fallback", e)
            "gemini-1.5-pro" // フォールバック
        }
    }
    
    /**
     * スイング分析結果からAIコーチングアドバイスを生成
     */
    suspend fun generateCoachingAdvice(
        swingData: SwingAnalysisData,
        targetProName: String? = null,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildCoachingPrompt(swingData, targetProName, coachingStyle)
            
            val response = generativeModel.generateContent(prompt)
            
            response.text ?: "アドバイスの生成に失敗しました"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate coaching advice", e)
            "エラーが発生しました: ${e.message}"
        }
    }
    
    /**
     * プロンプトを構築
     */
    private fun buildCoachingPrompt(
        swingData: SwingAnalysisData,
        targetProName: String? = null,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY
    ): String {
        val styleInstruction = when (coachingStyle) {
            CoachingStyle.FRIENDLY -> "親しみやすく、励ましながら"
            CoachingStyle.PROFESSIONAL -> "プロフェッショナルで丁寧に"
            CoachingStyle.STRICT -> "厳しく、的確に"
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
    
    /**
     * 対話式コーチング（プレミアム版）
     * @param message ユーザーメッセージ
     * @param conversationHistory 会話履歴
     * @param preferredModel 優先モデル（null=自動選択、MODEL_FLASH=高速、MODEL_PRO=高精度）
     */
    suspend fun chat(
        message: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        preferredModel: String? = null
    ): String = withContext(Dispatchers.IO) {
        val lastUserMessage = conversationHistory.lastOrNull { it.role == "user" }?.content
        val lastCoachMessage = conversationHistory.lastOrNull { it.role == "model" }?.content
        val isQuantitative = isQuantitativeQuestion(message)
        val topicKeyword = detectTopicKeyword(message) ?: detectTopicKeyword(lastUserMessage)

        try {
            if (apiKey.isBlank()) {
                return@withContext "Gemini APIキーが設定されていません。BuildConfig.GEMINI_API_KEY を設定してください。"
            }

            Log.d(TAG, "Starting chat with message: $message")
            
            // Gemini API v1 を使用（Retrofit経由）
            val contents = mutableListOf<Content>()
            contents.add(
                Content(
                    parts = listOf(Part(buildSystemInstruction(lastUserMessage, lastCoachMessage, isQuantitative, topicKeyword))),
                    role = "user"
                )
            )
            
            // 会話履歴を追加
            conversationHistory.forEach { msg ->
                contents.add(
                    Content(
                        parts = listOf(Part(text = msg.content)),
                        role = msg.role
                    )
                )
            }
            
            // 現在のメッセージを追加
            contents.add(
                Content(
                    parts = listOf(Part(text = message)),
                    role = "user"
                )
            )
            
            // モデルを決定（優先モデル指定があればそれを使用、なければ自動選択）
            val modelName = preferredModel ?: getUsableModel()
            
            Log.d(TAG, "Using model: $modelName")
            
            val request = GeminiRequest(contents = contents)
            val response = geminiApi.generateContent(
                model = modelName,
                request = request,
                apiKey = apiKey
            )
            
            val text = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "返答の生成に失敗しました"
            
            Log.d(TAG, "Response received: $text")
            normalizeCoachResponse(text, isQuantitative, topicKeyword, message)
        } catch (e: HttpException) {
            if (e.code() == 503) {
                throw ModelOverloadedException()
            }
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            Log.e(TAG, "Failed to chat (HTTP ${e.code()})", e)
            Log.e(TAG, "Gemini errorBody: ${errorBody ?: "(null)"}")
            Log.e(TAG, "Error details: ${e.stackTraceToString()}")
            "AI接続エラー(${e.code()})。素振りドリルでフォーム維持を確認。"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to chat", e)
            Log.e(TAG, "Error details: ${e.stackTraceToString()}")
            "AI接続で予期せぬエラー。ワンハンド素振りでタイミングを整えましょう。"
        }
    }
    
    /**
     * システムプロンプト付きチャット開始
     */
    suspend fun startCoachingChat(): String = withContext(Dispatchers.IO) {
        val systemPrompt = """
あなたはプロのゴルフコーチです。
ユーザーのスイングに関する質問に、親切で分かりやすく答えてください。
専門用語を使う場合は、必ず説明を加えてください。
具体的な練習方法やドリルを提案してください。
        """.trimIndent()
        
        try {
            val response = generativeModel.generateContent(systemPrompt)
            "こんにちは！ゴルフコーチAIです。スイングについて何でも質問してください。"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start chat", e)
            "チャットの開始に失敗しました"
        }
    }
}

/**
 * スイング分析データ
 */
data class SwingAnalysisData(
    val totalScore: Int,
    val backswingAngle: Double,
    val downswingSpeed: Double,
    val hipRotation: Double,
    val shoulderRotation: Double,
    val headStability: Double,
    val weightShift: Double,
    val swingPlane: String
)

/**
 * チャットメッセージ
 */
data class ChatMessage(
    val role: String,  // "user" or "model"
    val content: String
)

class ModelOverloadedException : Exception("Gemini モデルが過負荷です")

/**
 * コーチングスタイル
 */
enum class CoachingStyle {
    FRIENDLY,      // 親しみやすい
    PROFESSIONAL,  // プロフェッショナル
    STRICT,        // 厳しい
    MOTIVATIONAL   // モチベーション重視
}
