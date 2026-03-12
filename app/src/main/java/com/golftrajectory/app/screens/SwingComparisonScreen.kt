package com.swingtrace.aicoaching.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swingtrace.aicoaching.database.AnalysisHistoryEntity
import com.golftrajectory.app.ai.GeminiAIManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * スイング比較画面
 * 過去の診断結果との比較・成長可視化
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwingComparisonScreen(
    currentAnalysis: AnalysisHistoryEntity,
    pastAnalyses: List<AnalysisHistoryEntity>,
    aiManager: GeminiAIManager? = null,
    onBack: () -> Unit,
    onSelectPastAnalysis: (AnalysisHistoryEntity) -> Unit,
    onAICoachClick: ((current: AnalysisHistoryEntity, past: AnalysisHistoryEntity) -> Unit)? = null
) {
    var selectedPastAnalysis by remember { mutableStateOf<AnalysisHistoryEntity?>(null) }
    var aiAdvice by remember { mutableStateOf<String?>(null) }
    var isLoadingAdvice by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スイング比較") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 現在のスイング
            Text(
                text = "現在のスイング",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            SwingDataCard(currentAnalysis, isCurrentSwing = true)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 過去のスイング選択
            Text(
                text = "比較する過去のスイング",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (pastAnalyses.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "比較できる過去のデータがありません",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                pastAnalyses.forEach { analysis ->
                    SwingDataCard(
                        analysis = analysis,
                        isSelected = selectedPastAnalysis?.id == analysis.id,
                        onClick = {
                            selectedPastAnalysis = analysis
                            onSelectPastAnalysis(analysis)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 比較結果
            if (selectedPastAnalysis != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "成長の記録",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ComparisonResultCard(
                    current = currentAnalysis,
                    past = selectedPastAnalysis!!
                )
                
                // AIアドバイスセクション
                if (aiManager != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (aiAdvice == null && !isLoadingAdvice) {
                        Button(
                            onClick = {
                                isLoadingAdvice = true
                                scope.launch {
                                    try {
                                        val advice = generateComparisonAdvice(
                                            aiManager = aiManager,
                                            current = currentAnalysis,
                                            past = selectedPastAnalysis!!
                                        )
                                        aiAdvice = advice
                                    } catch (e: Exception) {
                                        aiAdvice = "アドバイスの生成に失敗しました: ${e.message}"
                                    } finally {
                                        isLoadingAdvice = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Psychology, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AIコーチのアドバイスを見る")
                        }
                    }
                    
                    if (isLoadingAdvice) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("AIコーチが分析中...")
                            }
                        }
                    }
                    
                    if (aiAdvice != null) {
                        AIAdviceCard(
                            advice = aiAdvice!!,
                            onAICoachClick = if (onAICoachClick != null) {
                                { onAICoachClick(currentAnalysis, selectedPastAnalysis!!) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwingDataCard(
    analysis: AnalysisHistoryEntity,
    isCurrentSwing: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentSwing -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(analysis.timestamp)),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "スコア: ${analysis.totalScore}点",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = getScoreColor(analysis.totalScore)
                    )
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            // 主要指標
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("バックスイング", "${analysis.backswingAngle.toInt()}°")
                MetricItem("ダウンスイング", "${analysis.downswingSpeed.toInt()}")
                MetricItem("頭の安定性", "${analysis.headStability.toInt()}%")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 推定飛距離
            if (analysis.estimatedDistance > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GolfCourse,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "推定飛距離: ${analysis.estimatedDistance.toInt()}ヤード",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonResultCard(
    current: AnalysisHistoryEntity,
    past: AnalysisHistoryEntity
) {
    val scoreDiff = current.totalScore - past.totalScore
    val backswingDiff = current.backswingAngle - past.backswingAngle
    val downswingDiff = current.downswingSpeed - past.downswingSpeed
    val headStabilityDiff = current.headStability - past.headStability
    val distanceDiff = current.estimatedDistance - past.estimatedDistance
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 総合スコアの変化
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "総合スコア",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (scoreDiff > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (scoreDiff > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (scoreDiff > 0) "+" else ""}${scoreDiff}点",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scoreDiff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // 詳細な変化
            if (current.estimatedDistance > 0 && past.estimatedDistance > 0) {
                ComparisonMetricRow("推定飛距離", past.estimatedDistance, current.estimatedDistance, "ヤード", isDistance = true)
                Spacer(modifier = Modifier.height(12.dp))
            }
            ComparisonMetricRow("バックスイング角度", past.backswingAngle, current.backswingAngle, "°")
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonMetricRow("ダウンスイング速度", past.downswingSpeed.toDouble(), current.downswingSpeed.toDouble(), "")
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonMetricRow("頭の安定性", past.headStability.toDouble(), current.headStability.toDouble(), "%")
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonMetricRow("腰の回転", past.hipRotation, current.hipRotation, "°")
            Spacer(modifier = Modifier.height(12.dp))
            ComparisonMetricRow("肩の回転", past.shoulderRotation, current.shoulderRotation, "°")
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // 成長のまとめ
            Text(
                text = "成長のまとめ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (scoreDiff > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "素晴らしい！${scoreDiff}点改善しました！",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (scoreDiff < 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "前回より${-scoreDiff}点下がっています。練習を続けましょう。",
                        color = Color(0xFFFF9800)
                    )
                }
            } else {
                Text(
                    text = "スコアは変わっていません。",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ComparisonMetricRow(
    label: String,
    pastValue: Double,
    currentValue: Double,
    unit: String,
    isDistance: Boolean = false
) {
    val diff = currentValue - pastValue
    val isImprovement = diff > 0
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isDistance) 15.sp else 14.sp,
            fontWeight = if (isDistance) FontWeight.Bold else FontWeight.Normal
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${pastValue}$unit → ${currentValue}$unit",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${if (diff > 0) "+" else ""}${String.format("%.1f", diff)}$unit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isImprovement) Color(0xFF4CAF50) else Color(0xFFE53935)
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFE53935)
    }
}

/**
 * AIコーチのアドバイスカード
 */
@Composable
fun AIAdviceCard(
    advice: String,
    onAICoachClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AIコーチのアドバイス",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = advice,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            
            if (onAICoachClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAICoachClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AIコーチに詳しく相談する")
                }
            }
        }
    }
}

/**
 * 比較結果に基づくAIアドバイスを生成
 */
suspend fun generateComparisonAdvice(
    aiManager: GeminiAIManager,
    current: AnalysisHistoryEntity,
    past: AnalysisHistoryEntity
): String {
    val scoreDiff = current.totalScore - past.totalScore
    val backswingDiff = current.backswingAngle - past.backswingAngle
    val downswingDiff = current.downswingSpeed - past.downswingSpeed
    val headStabilityDiff = current.headStability - past.headStability
    val hipRotationDiff = current.hipRotation - past.hipRotation
    val shoulderRotationDiff = current.shoulderRotation - past.shoulderRotation
    
    val prompt = """
あなたはプロのゴルフコーチです。以下のスイング比較データを分析し、親しみやすく具体的なアドバイスを日本語で提供してください。

【過去のスイング】（${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(past.timestamp))}）
総合スコア: ${past.totalScore}点
バックスイング角度: ${past.backswingAngle}°
ダウンスイング速度: ${past.downswingSpeed}
頭の安定性: ${past.headStability}%
腰の回転: ${past.hipRotation}°
肩の回転: ${past.shoulderRotation}°

【現在のスイング】（${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(current.timestamp))}）
総合スコア: ${current.totalScore}点
バックスイング角度: ${current.backswingAngle}°
ダウンスイング速度: ${current.downswingSpeed}
頭の安定性: ${current.headStability}%
腰の回転: ${current.hipRotation}°
肩の回転: ${current.shoulderRotation}°

【変化】
総合スコア: ${if (scoreDiff > 0) "+" else ""}${scoreDiff}点
バックスイング: ${if (backswingDiff > 0) "+" else ""}${String.format("%.1f", backswingDiff)}°
ダウンスイング: ${if (downswingDiff > 0) "+" else ""}${String.format("%.1f", downswingDiff)}
頭の安定性: ${if (headStabilityDiff > 0) "+" else ""}${String.format("%.1f", headStabilityDiff)}%
腰の回転: ${if (hipRotationDiff > 0) "+" else ""}${String.format("%.1f", hipRotationDiff)}°
肩の回転: ${if (shoulderRotationDiff > 0) "+" else ""}${String.format("%.1f", shoulderRotationDiff)}°

【アドバイス形式】
1. 総合評価（改善・悪化・停滞を明確に）
2. 良くなった点（具体的に褒める）
3. 悪くなった点（原因を推測し、改善策を提案）
   - 悪化の可能性：疲労、フォームの崩れ、過度な意識、練習不足など
4. 次のステップ（具体的な目標と練習方法）

${if (scoreDiff < 0) "※スコアが下がっていますが、落ち込まず前向きに改善策を伝えてください。" else ""}

簡潔で分かりやすく、励ましの言葉も含めてください。マークダウン記号は使わないでください。
    """.trimIndent()
    
    return aiManager.chat(prompt)
}
