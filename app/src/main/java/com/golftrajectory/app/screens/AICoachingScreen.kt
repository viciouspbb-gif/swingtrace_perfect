package com.swingtrace.aicoaching.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.golftrajectory.app.plan.Plan
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golftrajectory.app.plan.LitePlanAdBanner
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.ai.AICoachConversationManager
import com.golftrajectory.app.ai.AICoachConversationManager.ConversationOption
import com.golftrajectory.app.ai.ChatMessage
import com.golftrajectory.app.ai.GeminiAIManager
import com.golftrajectory.app.ai.ModelOverloadedException
import com.golftrajectory.app.utils.LockScreenOrientation
import android.content.pm.ActivityInfo
import com.swingtrace.aicoaching.analysis.ProSimilarityCalculator
import com.swingtrace.aicoaching.domain.usecase.SwingData
import com.swingtrace.aicoaching.voice.VoiceManager
import com.golftrajectory.app.logic.BiomechanicsFrame
import kotlinx.coroutines.launch

/**
 * マークダウン記号を削除してプレーンテキストに変換
 */
fun String.removeMarkdown(): String {
    return this
        // 見出し記号を削除 (###, ##, # など)
        .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
        // 太字・斜体記号を削除 (***, **, * など)
        .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        // アンダースコアの太字・斜体を削除
        .replace(Regex("_{1,3}([^_]+)_{1,3}"), "$1")
        // 水平線を削除 (---, ***, ___)
        .replace(Regex("^[-*_]{3,}$", RegexOption.MULTILINE), "")
        // コードブロックのバッククォートを削除
        .replace(Regex("```[^`]*```"), "")
        .replace(Regex("`([^`]+)`"), "$1")
        // リンク記号を削除 [text](url) -> text
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        // 画像記号を削除
        .replace(Regex("!\\[([^\\]]*)\\]\\([^)]+\\)"), "$1")
        // 引用記号を削除
        .replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
        // 余分な空白行を整理
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

@Composable
private fun LitePlanCoachGate(
    summaryText: String,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AIコーチング（Lite）")
                        Text(
                            "詳細な対話はPROプラン限定",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Liteプランのプレビュー", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(summaryText, lineHeight = 20.sp)
                }
            }
            
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PROプランでできること", fontWeight = FontWeight.Bold)
                    Text("・AIコーチとのリアルタイム対話\n・ボイスモードでのコーチング\n・履歴に応じた深堀りアドバイス")
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("PROプランにアップグレード")
                    }
                }
            }
            
            LitePlanAdBanner()
        }
    }
}

/**
 * 対話式AIコーチング画面（プレミアム版）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachingScreen(
    aiManager: GeminiAIManager,
    swingData: SwingData? = null,
    proSimilarity: ProSimilarityCalculator.SimilarityResult? = null,
    previousScore: Int? = null,
    biomechanicsHistory: List<BiomechanicsFrame> = emptyList(),
    planTier: Plan,
    isPremium: Boolean = false,
    userPlanManager: UserPlanManager,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onVideoPickClick: () -> Unit = {}
) {
    // 縦画面に固定
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val voiceManager = remember { VoiceManager(context) }
    val conversationManager = remember { AICoachConversationManager(aiManager, userPlanManager) }
    
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isVoiceMode by remember { mutableStateOf(false) }
    var currentOptions by remember { mutableStateOf<List<AICoachConversationManager.ConversationOption>?>(null) }
    var showUpgrade by remember { mutableStateOf(false) }
    var upgradeMessage by remember { mutableStateOf<String?>(null) }
    val isLitePlan = planTier == Plan.PRACTICE
    val liteSummary = remember(swingData, proSimilarity, previousScore) {
        buildString {
            append("最新のスイング診断まとめ\n\n")
            swingData?.let {
                append("・総合スコア: ${it.totalScore}\n")
                append("・バックスイング角: ${"%.1f".format(it.backswingAngle)}°\n")
                append("・腰/肩の回転: ${"%.1f".format(it.hipRotation)}° / ${"%.1f".format(it.shoulderRotation)}°\n")
                append("・体重移動: ${"%.1f".format(it.weightShift)}%\n")
            } ?: append("・スイングデータはまだ記録されていません\n")
            proSimilarity?.let {
                append("・プロ比較: ${it.pro.name} に近い動き（類似度 ${"%.0f".format(it.similarity * 100)}%）\n")
            }
            previousScore?.let {
                append("・前回スコアとの差分: ${previousScore?.let { prev -> swingData?.totalScore?.minus(prev) ?: 0 } ?: 0} 点\n")
            }
            append("\nAIコーチとの対話を開始するにはPROプランへアップグレードしてください。")
        }
    }
    
    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.release()
        }
    }
    
    if (isLitePlan) {
        LitePlanCoachGate(
            summaryText = liteSummary,
            onBack = onBack,
            onUpgradeClick = onUpgradeClick
        )
        return
    }
    
    // 初回メッセージ（スイングデータがあれば自動会話開始）
    LaunchedEffect(Unit) {
        try {
            if (swingData != null) {
                // スイングデータを基に会話開始
                val response = conversationManager.startConversationWithSwingData(
                    swingData = swingData!!,
                    proSimilarity = proSimilarity,
                    previousScore = previousScore,
                    biomechanicsHistory = biomechanicsHistory
                )
                messages = listOf(ChatMessage(id = "model", text = response.message, isUser = false))
                currentOptions = response.options
            } else {
                // 通常の挨拶
                val greeting = "こんにちは！ゴルフコーチAIです。🏌️\n\nスイングについて何でも質問してください。\n\n例えば：\n• バックスイングの改善方法\n• 飛距離を伸ばすコツ\n• スイングの基本\n\nなど、お気軽にどうぞ！"
                messages = listOf(ChatMessage(id = "model", text = greeting, isUser = false))
            }
        } catch (e: Exception) {
            android.util.Log.e("AICoachingScreen", "Error initializing chat", e)
            messages = listOf(ChatMessage(id = "model", text = "チャットの初期化に失敗しました。もう一度お試しください。", isUser = false))
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AIコーチング")
                        Text(
                            "何でも質問してください",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // メッセージリスト
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                state = listState
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 選択肢ボタン
                if (currentOptions != null && !isLoading) {
                    item {
                        ConversationOptions(
                            options = currentOptions!!,
                            onOptionClick = { option ->
                                currentOptions = null
                                messages = messages + ChatMessage(id = "user_${System.currentTimeMillis()}", text = option.text, isUser = true)
                                isLoading = true
                                
                                scope.launch {
                                    listState.animateScrollToItem(messages.size)
                                    
                                    try {
                                        val response = conversationManager.respondToOption(
                                            optionId = option.id,
                                            swingData = swingData!!,
                                            proSimilarity = proSimilarity,
                                            isPremium = isPremium
                                        )
                                        messages = messages + ChatMessage(id = "model_${System.currentTimeMillis()}", text = response.message, isUser = false)
                                        showUpgrade = response.showUpgrade
                                        upgradeMessage = response.upgradeMessage
                                        
                                        listState.animateScrollToItem(messages.size)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.util.Log.e("AICoachingScreen", "Error in conversation", e)
                                        messages = messages + ChatMessage(
                                            id = "model_error_${System.currentTimeMillis()}",
                                            text = "エラーが発生しました: ${e.message}\n\n${e.cause?.message ?: ""}",
                                            isUser = false
                                        )
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        )
                    }
                }
                
                // アップグレード誘導
                if (showUpgrade && upgradeMessage != null) {
                    item {
                        UpgradePromptCard(
                            message = upgradeMessage!!,
                            onUpgradeClick = onUpgradeClick
                        )
                    }
                }
                
                // ローディング表示
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("考え中...")
                                }
                            }
                        }
                    }
                }
            }
            
            // 入力エリア
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // カメラボタン
                    IconButton(
                        onClick = onCameraClick,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "動画撮影",
                            tint = if (!isLoading) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    
                    // 動画添付ボタン
                    IconButton(
                        onClick = onVideoPickClick,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "動画を添付",
                            tint = if (!isLoading) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    
                    // 音声モード切り替えボタン
                    IconButton(
                        onClick = { isVoiceMode = !isVoiceMode }
                    ) {
                        Icon(
                            imageVector = if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                            contentDescription = if (isVoiceMode) "テキスト入力" else "音声入力",
                            tint = if (isVoiceMode) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    
                    if (isVoiceMode) {
                        // 音声入力ボタン
                        FilledTonalButton(
                            onClick = {
                                if (isListening) {
                                    voiceManager.stopListening()
                                } else {
                                    voiceManager.startListening { recognizedText ->
                                        if (recognizedText.isNotBlank()) {
                                            val userMessage = recognizedText
                                            messages = messages + ChatMessage(id = "voice_${System.currentTimeMillis()}", text = userMessage, isUser = true)
                                            isLoading = true
                                            
                                            scope.launch {
                                                listState.animateScrollToItem(messages.size)
                                                
                                                try {
                                                    // 会話履歴から最後のユーザーメッセージを除外
                                                    val historyForAI = messages.dropLast(1)
                                                    val response = aiManager.chat(
                                                        message = userMessage,
                                                        conversationHistory = historyForAI
                                                    )
                                                    messages = messages + ChatMessage(id = "model_voice_${System.currentTimeMillis()}", text = response, isUser = false)
                                                    
                                                    // 音声で読み上げ（マークダウン記号を除去）
                                                    voiceManager.speak(response.removeMarkdown())
                                                    
                                                    listState.animateScrollToItem(messages.size)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("AICoachingScreen", "Voice chat error", e)
                                                    if (e is ModelOverloadedException) {
                                                        Toast.makeText(
                                                            context,
                                                            "AIコーチが混雑中です。少し時間をおいて再度お試しください。",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        val errorMsg = "申し訳ございません。エラーが発生しました: ${e.message}"
                                                        messages = messages + ChatMessage(id = "model_error_${System.currentTimeMillis()}", text = errorMsg, isUser = false)
                                                        voiceManager.speak("申し訳ございません。エラーが発生しました。")
                                                    }
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            if (isListening) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("聞いています...")
                            } else {
                                Icon(Icons.Default.Mic, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("タップして話す")
                            }
                        }
                    } else {
                        // テキスト入力
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("質問を入力...") },
                            enabled = !isLoading,
                            maxLines = 3
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val userMessage = inputText
                                    messages = messages + ChatMessage(id = "text_${System.currentTimeMillis()}", text = userMessage, isUser = true)
                                    inputText = ""
                                    isLoading = true
                                    
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size)
                                        
                                        try {
                                            // 会話履歴から最後のユーザーメッセージを除外
                                            val historyForAI = messages.dropLast(1)
                                            val response = aiManager.chat(
                                                message = userMessage,
                                                conversationHistory = historyForAI
                                            )
                                            messages = messages + ChatMessage(id = "model_text_${System.currentTimeMillis()}", text = response, isUser = false)
                                            listState.animateScrollToItem(messages.size)
                                        } catch (e: Exception) {
                                            android.util.Log.e("AICoachingScreen", "Text chat error", e)
                                            if (e is ModelOverloadedException) {
                                                Toast.makeText(
                                                    context,
                                                    "AIコーチが混雑中です。少し時間をおいて再度お試しください。",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                messages = messages + ChatMessage(
                                                    id = "model_error_${System.currentTimeMillis()}",
                                                    text = "申し訳ございません。エラーが発生しました: ${e.message}\n\nもう一度お試しください。",
                                                    isUser = false
                                                )
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            enabled = inputText.isNotBlank() && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "送信",
                                tint = if (inputText.isNotBlank() && !isLoading) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsGolf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AIコーチ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Text(
                    text = message.text.removeMarkdown(),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * 会話選択肢ボタン
 */
@Composable
fun ConversationOptions(
    options: List<AICoachConversationManager.ConversationOption>,
    onOptionClick: (AICoachConversationManager.ConversationOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            OutlinedButton(
                onClick = { onOptionClick(option) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * アップグレード誘導カード
 */
@Composable
fun UpgradePromptCard(
    message: String,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "プレミアム機能",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "プレミアムを見る",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * サンプル質問ボタン
 */
@Composable
fun SampleQuestions(onQuestionClick: (String) -> Unit) {
    val questions = listOf(
        "バックスイングの角度を改善するには？",
        "ダウンスイングの速度を上げる方法は？",
        "頭の安定性を保つコツは？",
        "体重移動のタイミングは？"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "よくある質問",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        questions.forEach { question ->
            OutlinedButton(
                onClick = { onQuestionClick(question) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = question,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
