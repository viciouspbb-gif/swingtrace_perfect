package com.swingtrace.aicoaching.ai

import com.golftrajectory.app.AppConfig
import com.swingtrace.aicoaching.analysis.ProSimilarityCalculator
import com.swingtrace.aicoaching.domain.usecase.SwingData

/**
 * AIコーチ会話管理
 */
class AICoachConversationManager(
    private val geminiAIManager: GeminiAIManager
) {
    
    /**
     * 会話の選択肢
     */
    data class ConversationOption(
        val id: String,
        val text: String,
        val icon: String
    )
    
    /**
     * AIコーチの応答
     */
    data class CoachResponse(
        val message: String,
        val options: List<ConversationOption>? = null,
        val showUpgrade: Boolean = false,
        val upgradeMessage: String? = null
    )
    
    /**
     * スイングデータを基に会話を開始
     */
    suspend fun startConversationWithSwingData(
        swingData: SwingData,
        proSimilarity: ProSimilarityCalculator.SimilarityResult?,
        previousScore: Int?,
        coachingStyle: CoachingStyle = CoachingStyle.FRIENDLY
    ): CoachResponse {
        val greeting = buildGreeting(swingData, proSimilarity, previousScore, coachingStyle)
        val options = limitOptionsForMode(generateInitialOptions(swingData, proSimilarity))
        
        return CoachResponse(
            message = greeting,
            options = options
        )
            .applyModeLimit()
    }
    
    /**
     * 挨拶メッセージを生成
     */
    private fun buildGreeting(
        swingData: SwingData,
        proSimilarity: ProSimilarityCalculator.SimilarityResult?,
        previousScore: Int?,
        coachingStyle: CoachingStyle
    ): String {
        val score = swingData.totalScore
        val proName = proSimilarity?.pro?.name ?: "プロゴルファー"
        val similarity = proSimilarity?.similarity?.toInt() ?: 0
        
        val greeting = when (coachingStyle) {
            CoachingStyle.FRIENDLY -> {
                buildString {
                    append("お疲れ様です！今回のスイング、拝見しました👏\n\n")
                    append("📊 総合スコア：${score}点\n")
                    
                    if (previousScore != null) {
                        val diff = score - previousScore
                        if (diff > 0) {
                            append("🎉 前回より${diff}点アップです！素晴らしい成長ですね！\n")
                        } else if (diff < 0) {
                            append("💪 前回より${-diff}点下がりましたが、大丈夫！一緒に改善していきましょう！\n")
                        } else {
                            append("📈 前回と同じスコアですね。安定感がありますよ！\n")
                        }
                    }
                    
                    append("\nどんなアドバイスが欲しいですか？下の選択肢から選んでくださいね！💪")
                }
            }
            CoachingStyle.PROFESSIONAL -> {
                buildString {
                    append("分析結果をご報告します。\n\n")
                    append("総合スコア：${score}点\n")
                    
                    if (previousScore != null) {
                        val diff = score - previousScore
                        append("前回比：${if (diff > 0) "+" else ""}${diff}点\n")
                    }
                    
                    append("\n改善の余地がある項目について、具体的なアドバイスをご提供できます。")
                }
            }
            CoachingStyle.STRICT -> {
                buildString {
                    append("スイングを確認しました。\n\n")
                    append("スコア：${score}点\n")
                    
                    if (score < 70) {
                        append("\nまだまだです。基本から見直す必要があります。\n")
                    } else if (score < 85) {
                        append("\n悪くはないですが、改善の余地は大いにあります。\n")
                    } else {
                        append("\n悪くはないですが、改善の余地は大いにあります。\n")
                    }
                    
                    append("\n何から始めますか？")
                }
            }
            CoachingStyle.MOTIVATIONAL -> {
                buildString {
                    append("素晴らしいスイングでした！🔥\n\n")
                    append("あなたのスコア：${score}点！\n")
                    
                    if (previousScore != null) {
                        val diff = score - previousScore
                        if (diff > 0) {
                            append("前回から${diff}点も成長！この調子です！💪\n")
                        } else {
                            append("今日は調子が出なかったかもしれませんが、次は必ずもっと良くなります！🌟\n")
                        }
                    }
                    
                    append("\nあなたには無限の可能性があります！\n")
                    append("一緒に夢を叶えましょう！✨")
                }
            }
        }
        
        return greeting
    }
    
    /**
     * 初期選択肢を生成
     */
    private fun generateInitialOptions(
        swingData: SwingData,
        proSimilarity: ProSimilarityCalculator.SimilarityResult?
    ): List<ConversationOption> {
        val options = mutableListOf<ConversationOption>()
        
        // 改善ポイントに基づく選択肢
        if (swingData.backswingAngle < 60 || swingData.backswingAngle > 90) {
            options.add(
                ConversationOption(
                    id = "improve_backswing",
                    text = "バックスイングを改善したい",
                    icon = "🏌️"
                )
            )
        }
        
        if (swingData.downswingSpeed < 40) {
            options.add(
                ConversationOption(
                    id = "improve_speed",
                    text = "スイングスピードを上げたい",
                    icon = "⚡"
                )
            )
        }
        
        if (swingData.headStability < 70) {
            options.add(
                ConversationOption(
                    id = "improve_stability",
                    text = "頭の安定性を高めたい",
                    icon = "🎯"
                )
            )
        }
        
        // プロのようなスイングになりたい（スコア60点以上の場合のみ）
        if (swingData.totalScore >= 60) {
            options.add(
                ConversationOption(
                    id = "choose_pro",
                    text = "プロのようなスイングになりたい",
                    icon = "🏆"
                )
            )
        }
        
        // 総合的なアドバイス
        options.add(
            ConversationOption(
                id = "overall_advice",
                text = "総合的なアドバイスが欲しい",
                icon = "💡"
            )
        )
        
        // 練習メニュー
        options.add(
            ConversationOption(
                id = "practice_menu",
                text = "おすすめの練習メニューは？",
                icon = "📋"
            )
        )
        
        return options.take(4) // 最大4つ
    }
    
    /**
     * 選択肢に基づいて応答を生成
     */
    suspend fun respondToOption(
        optionId: String,
        swingData: SwingData,
        proSimilarity: ProSimilarityCalculator.SimilarityResult?,
        isPremium: Boolean
    ): CoachResponse {
        val response = when (optionId) {
            "improve_backswing" -> generateBackswingAdvice(swingData, isPremium)
            "improve_speed" -> generateSpeedAdvice(swingData, isPremium)
            "improve_stability" -> generateStabilityAdvice(swingData, isPremium)
            "choose_pro" -> generateProSelectionOptions(swingData)
            "compare_pro" -> generateProComparisonAdvice(swingData, proSimilarity, isPremium)
            "overall_advice" -> generateOverallAdvice(swingData, isPremium)
            "practice_menu" -> generatePracticeMenu(swingData, isPremium)
            else -> {
                // プロ選択の場合（pro_matsuyama, pro_tiger など）
                if (optionId.startsWith("pro_")) {
                    val proName = when (optionId) {
                        "pro_matsuyama" -> "松山英樹"
                        "pro_tiger" -> "タイガー・ウッズ"
                        "pro_rory" -> "ローリー・マキロイ"
                        "pro_dustin" -> "ダスティン・ジョンソン"
                        else -> null
                    }
                    if (proName != null) {
                        val pro = ProSimilarityCalculator.getProByName(proName)
                        if (pro != null) {
                            generateSpecificProAdvice(swingData, pro, isPremium)
                        } else {
                            CoachResponse("申し訳ございません。もう一度お試しください。")
                        }
                    } else {
                        CoachResponse("申し訳ございません。もう一度お試しください。")
                    }
                } else {
                    CoachResponse("申し訳ございません。もう一度お試しください。")
                }
            }
        }
        
        return response.applyModeLimit()
    }
    
    /**
     * プロ選択の選択肢を生成
     */
    private fun generateProSelectionOptions(swingData: SwingData): CoachResponse {
        val message = "どのプロゴルファーのスイングを目指しますか？🏌️\n\n各プロの特徴を参考に選んでください！"
        
        val options = listOf(
            ConversationOption(
                id = "pro_matsuyama",
                text = "🇯🇵 松山英樹（安定したテンポと正確性）",
                icon = ""
            ),
            ConversationOption(
                id = "pro_tiger",
                text = "🇺🇸 タイガー・ウッズ（パワフルで攻撃的）",
                icon = ""
            ),
            ConversationOption(
                id = "pro_rory",
                text = "🇮🇪 ローリー・マキロイ（スムーズで流れるような）",
                icon = ""
            ),
            ConversationOption(
                id = "pro_dustin",
                text = "🇺🇸 ダスティン・ジョンソン（シンプルで力強い）",
                icon = ""
            )
        )
        
        return CoachResponse(
            message = message,
            options = options
        ).applyModeLimit()
    }
    
    /**
     * 選択されたプロとの比較アドバイスを生成
     */
    private suspend fun generateSpecificProAdvice(
        swingData: SwingData,
        pro: ProSimilarityCalculator.ProGolferData,
        isPremium: Boolean
    ): CoachResponse {
        // プロとの類似度を計算
        val similarities = ProSimilarityCalculator.calculateSimilarities(
            backswingAngle = swingData.backswingAngle,
            downswingSpeed = swingData.downswingSpeed,
            hipRotation = swingData.hipRotation,
            shoulderRotation = swingData.shoulderRotation,
            headStability = swingData.headStability,
            weightTransfer = swingData.weightShift
        )
        
        val similarity = similarities.find { it.pro.name == pro.name }
        
        val message = buildString {
            append("${pro.emoji} ${pro.name}のスイングについて！\n\n")
            append("特徴：${pro.characteristics}\n\n")
            
            if (similarity != null) {
                append("あなたとの類似度：${similarity.similarity.toInt()}%\n\n")
                
                append("📊 詳細比較：\n")
                similarity.breakdown.forEach { (key, value) ->
                    val icon = if (value >= 80) "✅" else if (value >= 60) "⚠️" else "❌"
                    append("$icon $key: ${value.toInt()}%\n")
                }
                
                append("\n💡 ${pro.name}に近づくためのアドバイス：\n")
                
                // 最も改善が必要な項目を特定
                val weakestArea = similarity.breakdown.minByOrNull { it.value }
                if (weakestArea != null && weakestArea.value < 70) {
                    append("\n特に「${weakestArea.key}」を改善しましょう！\n")
                    
                    when (weakestArea.key) {
                        "バックスイング" -> append("• ${pro.name}は${pro.backswingAngle.toInt()}°のバックスイングです\n")
                        "ダウンスイング" -> append("• ${pro.name}は${pro.downswingSpeed.toInt()}のスピードです\n")
                        "腰の回転" -> append("• ${pro.name}は${pro.hipRotation.toInt()}°の腰の回転です\n")
                        "肩の回転" -> append("• ${pro.name}は${pro.shoulderRotation.toInt()}°の肩の回転です\n")
                        "頭の安定性" -> append("• ${pro.name}は${pro.headStability.toInt()}%の頭の安定性です\n")
                        "体重移動" -> append("• ${pro.name}は${pro.weightTransfer.toInt()}の体重移動です\n")
                    }
                }
            }
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で${pro.name}のスイング動画と比較できます！" else null
        ).applyModeLimit()
    }
    
    /**
     * バックスイング改善アドバイス
     */
    private fun generateBackswingAdvice(swingData: SwingData, isPremium: Boolean): CoachResponse {
        val angle = swingData.backswingAngle
        val message = buildString {
            append("バックスイング角度について解説します！\n\n")
            append("現在の角度：${angle.toInt()}°\n")
            append("理想の範囲：60-85°\n\n")
            
            if (angle < 60) {
                append("📌 バックスイングが浅いですね。\n\n")
                append("改善ポイント：\n")
                append("1️⃣ 肩をしっかり回す\n")
                append("2️⃣ 左腕を伸ばす\n")
                append("3️⃣ 体重を右足に移動\n\n")
            } else if (angle > 85) {
                append("📌 バックスイングが深すぎます。\n\n")
                append("改善ポイント：\n")
                append("1️⃣ トップの位置を低めに\n")
                append("2️⃣ 肩の回転を90°程度に\n")
                append("3️⃣ オーバースイングに注意\n\n")
            }
            
            append("💪 次回のスイングで意識してみてください！")
        }
        
        val showUpgrade = !isPremium
        val upgradeMessage = if (!isPremium) {
            "📹 プレミアム版なら、あなたのスイング動画とプロの動画を並べて比較できます！"
        } else null
        
        return CoachResponse(
            message = message,
            showUpgrade = showUpgrade,
            upgradeMessage = upgradeMessage
        ).applyModeLimit()
    }
    
    /**
     * スピード改善アドバイス
     */
    private fun generateSpeedAdvice(swingData: SwingData, isPremium: Boolean): CoachResponse {
        val speed = swingData.downswingSpeed
        val message = buildString {
            append("ダウンスイングスピードについて！⚡\n\n")
            append("現在のスピード：${speed.toInt()}\n")
            append("理想の範囲：40-60\n\n")
            
            append("スピードアップのコツ：\n")
            append("1️⃣ 腰の回転を速く\n")
            append("2️⃣ 手首のリリースタイミング\n")
            append("3️⃣ 下半身の力を使う\n\n")
            
            append("⚠️ ただし、スピードだけでなく正確性も大切です！")
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で、スピード強化の特別練習メニューをご提供！" else null
        ).applyModeLimit()
    }
    
    /**
     * 安定性改善アドバイス
     */
    private fun generateStabilityAdvice(swingData: SwingData, isPremium: Boolean): CoachResponse {
        val stability = swingData.headStability
        val message = buildString {
            append("頭の安定性について！🎯\n\n")
            append("現在の安定性：${stability.toInt()}%\n")
            append("理想：70%以上\n\n")
            
            append("安定性を高めるポイント：\n")
            append("1️⃣ 目線をボールに固定\n")
            append("2️⃣ 頭の位置を動かさない\n")
            append("3️⃣ 体の軸を意識\n\n")
            
            append("💡 鏡の前で練習すると効果的です！")
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で、頭の動きを可視化した分析をご提供！" else null
        ).applyModeLimit()
    }
    
    /**
     * プロ比較アドバイス
     */
    private fun generateProComparisonAdvice(
        swingData: SwingData,
        proSimilarity: ProSimilarityCalculator.SimilarityResult?,
        isPremium: Boolean
    ): CoachResponse {
        if (proSimilarity == null) {
            return CoachResponse("プロ類似度データがありません。")
        }
        
        val pro = proSimilarity.pro
        val message = buildString {
            append("${pro.emoji} ${pro.name}に近づくために！\n\n")
            append("現在の類似度：${proSimilarity.similarity.toInt()}%\n\n")
            
            append("${pro.name}の特徴：\n")
            append("${pro.characteristics}\n\n")
            
            append("あなたが特に近づいている点：\n")
            val topItems = proSimilarity.breakdown.entries.sortedByDescending { it.value }.take(2)
            topItems.forEach { (item, value) ->
                append("✅ ${item}：${value.toInt()}%\n")
            }
            
            append("\n改善すべき点：\n")
            val bottomItems = proSimilarity.breakdown.entries.sortedBy { it.value }.take(2)
            bottomItems.forEach { (item, value) ->
                append("📌 ${item}：${value.toInt()}%\n")
            }
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で、${pro.name}のスイング動画と並べて比較できます！" else null
        ).applyModeLimit()
    }
    
    /**
     * 総合アドバイス
     */
    private fun generateOverallAdvice(swingData: SwingData, isPremium: Boolean): CoachResponse {
        val score = swingData.totalScore
        val message = buildString {
            append("総合的なアドバイスです！💡\n\n")
            
            when {
                score >= 85 -> {
                    append("素晴らしいスイングです！✨\n")
                    append("このレベルを維持しつつ、さらに細かい調整で完璧を目指しましょう。")
                }
                score >= 70 -> {
                    append("良いスイングです！👍\n")
                    append("あと少しの改善で、プロレベルに到達できます。")
                }
                else -> {
                    append("基本をしっかり固めましょう！💪\n")
                    append("焦らず、一つずつ改善していけば必ず上達します。")
                }
            }
            
            append("\n\n次回のスイングで意識すること：\n")
            append("1️⃣ リラックスして構える\n")
            append("2️⃣ リズムを大切に\n")
            append("3️⃣ フィニッシュまで振り切る\n\n")
            
            append("継続は力なり！一緒に頑張りましょう！🔥")
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で、毎回のスイングに個別アドバイスが届きます！月額¥1,480" else null
        ).applyModeLimit()
    }
    
    /**
     * 練習メニュー
     */
    private fun generatePracticeMenu(swingData: SwingData, isPremium: Boolean): CoachResponse {
        val message = buildString {
            append("おすすめの練習メニュー📋\n\n")
            
            append("【基礎練習】\n")
            append("1️⃣ ハーフスイング（10分）\n")
            append("2️⃣ 素振り（20回）\n")
            append("3️⃣ アプローチ練習（15分）\n\n")
            
            if (swingData.totalScore < 70) {
                append("【重点項目】\n")
                append("• グリップの確認\n")
                append("• アドレスの姿勢\n")
                append("• バックスイングの軌道\n\n")
            }
            
            append("毎日少しずつ練習することが大切です！")
        }
        
        return CoachResponse(
            message = message,
            showUpgrade = !isPremium,
            upgradeMessage = if (!isPremium) "プレミアム版で、あなた専用の練習メニューを毎週お届け！" else null
        ).applyModeLimit()
    }

    private fun limitOptionsForMode(options: List<ConversationOption>?): List<ConversationOption>? {
        if (!AppConfig.isPractice()) return options
        return options?.firstOrNull()?.let { listOf(it) }
    }

    private fun CoachResponse.applyModeLimit(): CoachResponse {
        if (!AppConfig.isPractice()) return this
        return copy(options = options?.firstOrNull()?.let { listOf(it) })
    }
}
