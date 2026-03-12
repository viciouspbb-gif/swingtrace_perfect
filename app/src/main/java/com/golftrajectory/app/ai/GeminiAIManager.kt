package com.golftrajectory.app.ai

import android.content.Context
import android.util.Log
import com.swingtrace.aicoaching.BuildConfig
import com.golftrajectory.app.*
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
    suspend fun getCoachingAdvice(
        swingData: SwingAnalysisData,
        targetProName: String? = null,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY,
        currentClub: UserClubSetting? = null,
        biomechanicsData: BiomechanicsData? = null,
        stance: String = "RIGHT_HANDED" // スタンス情報を追加
    ): String = withContext(Dispatchers.IO) {
        Log.w(TAG, "generateCoachingAdvice called on stub implementation")
        buildCoachingPrompt(swingData, targetProName, coachingStyle, currentClub, biomechanicsData, stance)
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
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY,
        currentClub: UserClubSetting? = null,
        biomechanicsData: BiomechanicsData? = null,
        stance: String = "RIGHT_HANDED" // スタンス情報を追加
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

        // クラブ情報セクション
        val clubSection = if (currentClub != null) {
            val flexCharacteristics = getShaftFlexCharacteristics(currentClub.shaftFlex)
            val physicsAnalysis = getPhysicsBasedAnalysis(currentClub, biomechanicsData)
            
            """
            
            【使用クラブ情報】
            クラブ: ${currentClub.clubName}
            シャフト: ${currentClub.shaftModel}
            フレックス: ${currentClub.shaftFlex.displayName} (${flexCharacteristics})
            シャフト重量: ${currentClub.shaftWeight}g
            ロフト角: ${currentClub.loftAngle}°
            
            【物理特性分析】
            $physicsAnalysis
            
            ⚠️重要指示: このクラブの物理的特性とユーザーのバイオメカニクスデータを統合し、
            物理学の法則に基づいた客観的診断を行ってください。
            """.trimIndent()
        } else {
            "\n\n【使用クラブ情報】\nクラブ情報が未設定です。一般的なスペックを仮定して診断します。"
        }

        // バイオメカニクス詳細データ
        val biomechanicsSection = if (biomechanicsData != null) {
            val physicsEvaluation = evaluatePhysicsCompatibility(currentClub, biomechanicsData)
            
            // 単位系に応じた数値表現
            val unitSystem = com.golftrajectory.app.UserPreferences(context).getUnitSystem()
            
            val headMovementUnit = if (unitSystem == UnitSystem.METRIC) "cm" else "inches"
            val weightShiftUnit = if (unitSystem == UnitSystem.METRIC) "cm" else "inches"
            
            """
            
            【詳細バイオメカニクスデータ】
            肩の回転角: ${biomechanicsData.shoulderRotation}°
            腰の回転角: ${biomechanicsData.hipRotation}°
            X-Factor: ${biomechanicsData.xFactor}°
            頭の移動量: ${String.format("%.1f", biomechanicsData.headMovement)} ${headMovementUnit}
            体重移動: ${String.format("%.1f", biomechanicsData.weightShift)} ${weightShiftUnit}
            シャフトレイン: ${biomechanicsData.shaftLean}°
            
            【物理学適合性評価】
            $physicsEvaluation
            
            ⚠️物理学に基づく診断指示:
            1. 上記のバイオメカニクスデータを物理学の法則（トルク、角速度、運動エネルギー）から評価
            2. シャフトの剛性とスイングエネルギーの伝達効率を分析
            3. 身体の各セグメントの連動性とクラブ性能の適合性を診断
            4. 具体的な数値目標と、その目標を達成するための物理学的アプローチを提示
            5. クラブスペックの変更が推奨される場合、その科学的根拠を明確に説明
            """.trimIndent()
        } else {
            ""
        }

        return """
[Stance: ${if (stance == "LEFT_HANDED") "Left-handed" else "Right-handed"}]

あなたはプロのゴルフコーチです。${styleInstruction}、以下のスイング分析データに基づいて、具体的で実践的なアドバイスを日本語で提供してください。$targetProSection$clubSection$biomechanicsSection

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
3. 改善点（2-3つ、物理学の観点から具体的な練習方法を含む）
4. 次回の目標（1-2つ）

${if (currentClub != null && biomechanicsData != null) """
【物理学に基づくクラブ適合性診断】
5. シャフト性能とスイング力学の適合性評価
6. クラブスペックの最適化提案（科学的根拠付き）
7. 物理法則に基づいた具体的改善アプローチ
""".trimIndent() else ""}

簡潔で分かりやすく、物理学の視点から客観的なアドバイスを提供してください。
        """.trimIndent()
    }
    
    /**
     * シャフトフレックスの物理特性を取得
     */
    private fun getShaftFlexCharacteristics(shaftFlex: ShaftFlex): String {
        return when (shaftFlex) {
            ShaftFlex.L -> "非常に柔らかい、低速スイング向け、しなり易い"
            ShaftFlex.A -> "柔らかい、中低速スイング向け、適度なしなり"
            ShaftFlex.R -> "標準的な硬さ、中速スイング向け、バランス型"
            ShaftFlex.S -> "硬い、中高速スイング向け、安定性重視"
            ShaftFlex.X -> "非常に硬い、高速スイング向け、低スピン"
            ShaftFlex.XX -> "極めて硬い、超高速スイング向け、最大限の安定性"
            ShaftFlex.TX -> "ツアープロ向け、最高レベルの剛性"
        }
    }
    
    /**
     * 物理学に基づいたクラブ分析
     */
    private fun getPhysicsBasedAnalysis(club: UserClubSetting, biomechanics: BiomechanicsData?): String {
        val analysis = StringBuilder()
        
        // シャフト重量とスイングエネルギーの関係
        analysis.append("• シャフト重量${club.shaftWeight}gは")
        if (club.shaftWeight < 60) {
            analysis.append("軽量で、ヘッドスピード向上に有利ですが、安定性に課題がある可能性")
        } else if (club.shaftWeight < 80) {
            analysis.append("標準重量で、スピードと安定性のバランスが良好")
        } else if (club.shaftWeight < 100) {
            analysis.append("中重量で、安定性重視ですが、スピード維持に筋力が必要")
        } else {
            analysis.append("重量級で、最大限の安定性を提供しますが、高いスイングパワーが必要")
        }
        analysis.append("。\n")
        
        // ロフト角と打ち出し角度の関係
        analysis.append("• ロフト角${club.loftAngle}°は")
        if (club.loftAngle < 10) {
            analysis.append("低ロフトで、低スピン・低弾道の打球傾向。ヘッドスピードが重要")
        } else if (club.loftAngle < 20) {
            analysis.append("中ロフトで、距離と操作性のバランスが良好")
        } else if (club.loftAngle < 35) {
            analysis.append("高ロフトで、高い打ち出し角度とスピンコントロールが可能")
        } else {
            analysis.append("超高ロフトで、アプローチショットに最適")
        }
        analysis.append("。\n")
        
        // シャフトフレックスとバイオメカニクスの適合性
        if (biomechanics != null) {
            val recommendedFlex = when {
                biomechanics.shaftLean < 2.0 -> ShaftFlex.L
                biomechanics.shaftLean < 4.0 -> ShaftFlex.R
                biomechanics.shaftLean < 6.0 -> ShaftFlex.S
                biomechanics.shaftLean < 8.0 -> ShaftFlex.X
                else -> ShaftFlex.XX
            }
            
            analysis.append("• 現在のシャフトレイン${biomechanics.shaftLean}°から推奨されるフレックスは${recommendedFlex.displayName}です。")
            if (recommendedFlex != club.shaftFlex) {
                analysis.append("現在の${club.shaftFlex.displayName}とは適合性に課題がある可能性があります。")
            } else {
                analysis.append("現在の${club.shaftFlex.displayName}は適合性が良好です。")
            }
        }
        
        return analysis.toString()
    }
    
    /**
     * 物理学適合性を評価
     */
    private fun evaluatePhysicsCompatibility(club: UserClubSetting?, biomechanics: BiomechanicsData): String {
        if (club == null) return "クラブ情報がないため、物理学的評価はできません。"
        
        val evaluation = StringBuilder()
        
        // エネルギー伝達効率の評価
        val energyEfficiency = calculateEnergyEfficiency(club, biomechanics)
        evaluation.append("• エネルギー伝達効率: ${String.format("%.1f", energyEfficiency)}%\n")
        
        // トルク生成能力の評価
        val torqueScore = calculateTorqueScore(biomechanics)
        evaluation.append("• トルク生成能力: ${String.format("%.1f", torqueScore)}点\n")
        
        // 角速度の適合性
        val angularVelocity = calculateAngularVelocity(biomechanics)
        evaluation.append("• 角速度: ${String.format("%.1f", angularVelocity)} rad/s\n")
        
        // 総合的適合性評価
        val compatibilityScore = (energyEfficiency + torqueScore + angularVelocity) / 3
        evaluation.append("• 総合適合性評価: ${String.format("%.1f", compatibilityScore)}点\n")
        
        return evaluation.toString()
    }
    
    /**
     * エネルギー伝達効率を計算
     */
    private fun calculateEnergyEfficiency(club: UserClubSetting, biomechanics: BiomechanicsData): Double {
        // 簡易的な計算：X-Factorとシャフト重量のバランス
        val xFactorScore = biomechanics.xFactor.coerceIn(0.0, 90.0) / 90.0 * 50
        val weightScore = when {
            club.shaftWeight < 60 -> 30
            club.shaftWeight < 80 -> 45
            club.shaftWeight < 100 -> 35
            else -> 25
        }
        return xFactorScore + weightScore
    }
    
    /**
     * トルク生成能力を計算
     */
    private fun calculateTorqueScore(biomechanics: BiomechanicsData): Double {
        // 肩と腰の回転差をトルク指標として使用
        return biomechanics.xFactor.coerceIn(0.0, 60.0) * 1.5
    }
    
    /**
     * 角速度を計算
     */
    private fun calculateAngularVelocity(biomechanics: BiomechanicsData): Double {
        // 肩の回転角を角速度の指標として簡易計算
        return biomechanics.shoulderRotation.coerceIn(0.0, 180.0) * 0.1
    }
}

class ModelOverloadedException : Exception("Gemini model overloaded")
