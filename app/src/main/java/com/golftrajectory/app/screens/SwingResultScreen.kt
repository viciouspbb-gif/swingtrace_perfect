package com.swingtrace.aicoaching.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.swingtrace.aicoaching.analysis.ProSimilarityCalculator
import com.swingtrace.aicoaching.domain.usecase.SwingData
import com.swingtrace.aicoaching.utils.DistanceEstimator
import kotlinx.coroutines.launch

/**
 * スイング分析結果画面（ハイブリッド型）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwingResultScreen(
    swingData: SwingData,
    previousScore: Int? = null,
    onAICoachClick: () -> Unit,
    onCompareHistoryClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // プロ類似度を計算
    val similarities = remember(swingData) {
        ProSimilarityCalculator.calculateSimilarities(
            backswingAngle = swingData.backswingAngle,
            downswingSpeed = swingData.downswingSpeed,
            hipRotation = swingData.hipRotation,
            shoulderRotation = swingData.shoulderRotation,
            headStability = swingData.headStability,
            weightTransfer = swingData.weightShift
        )
    }
    
    // 展開状態
    var scoreDetailExpanded by remember { mutableStateOf(false) }
    var proDetailExpanded by remember { mutableStateOf(false) }
    var improvementExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分析結果") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSaveClick()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "分析結果を保存しました",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, "共有")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 総合スコア
            TotalScoreCard(
                score = swingData.totalScore,
                previousScore = previousScore,
                expanded = scoreDetailExpanded,
                onExpandClick = { scoreDetailExpanded = !scoreDetailExpanded },
                swingData = swingData
            )
            
            // プロ類似度
            ProSimilarityCard(
                similarities = similarities.take(3),
                allSimilarities = similarities,
                expanded = proDetailExpanded,
                onExpandClick = { proDetailExpanded = !proDetailExpanded }
            )
            
            // 成長記録
            if (previousScore != null) {
                GrowthCard(
                    currentScore = swingData.totalScore,
                    previousScore = previousScore,
                    onCompareClick = onCompareHistoryClick
                )
            }
            
            // 改善ポイント
            ImprovementCard(
                swingData = swingData,
                expanded = improvementExpanded,
                onExpandClick = { improvementExpanded = !improvementExpanded }
            )
            
            // 詳細分析カード
            DetailedAnalysisCard(swingData = swingData)
            
            // AIコーチに相談ボタン
            Button(
                onClick = onAICoachClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SportsGolf,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AIコーチに相談する",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TotalScoreCard(
    score: Int,
    previousScore: Int?,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    swingData: SwingData
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 総合スコア",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$score",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " / 100点",
                            fontSize = 20.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // 前回との差分
                    if (previousScore != null) {
                        val diff = score - previousScore
                        if (diff > 0) {
                            Text(
                                text = "🎉 前回より${diff}点アップです！素晴らしい成長ですね！",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                        } else if (diff < 0) {
                            Text(
                                text = "💪 前回より${-diff}点下がりましたが、大丈夫！一緒に改善していきましょう！",
                                fontSize = 14.sp,
                                color = Color(0xFFE53935)
                            )
                        } else {
                            Text(
                                text = "📈 前回と同じスコアですね。安定感がありますよ！",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diff > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${if (diff > 0) "+" else ""}${diff}点",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                            )
                        }
                    }
                }
                
                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "閉じる" else "詳細を見る"
                    )
                }
            }
            
            // 詳細（展開式）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider()
                    
                    // 推定飛距離を表示（クラブタイプ選択可能）
                    EstimatedDistanceSection(swingData = swingData)
                    
                    Divider()
                    
                    ScoreDetailItem("バックスイング角度", swingData.backswingAngle, "°", 60.0..85.0)
                    ScoreDetailItem("ダウンスイング速度", swingData.downswingSpeed, "", 40.0..60.0)
                    ScoreDetailItem("腰の回転", swingData.hipRotation, "°", 30.0..45.0)
                    ScoreDetailItem("肩の回転", swingData.shoulderRotation, "°", 45.0..60.0)
                    ScoreDetailItem("頭の安定性", swingData.headStability, "%", 70.0..100.0)
                    ScoreDetailItem("体重移動", swingData.weightShift, "", 20.0..60.0)
                }
            }
        }
    }
}

@Composable
fun ScoreDetailItem(
    label: String,
    value: Double,
    unit: String,
    idealRange: ClosedRange<Double>
) {
    val isGood = value in idealRange
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${value.toInt()}$unit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGood) Color(0xFF4CAF50) else Color(0xFFFFA726),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProSimilarityCard(
    similarities: List<ProSimilarityCalculator.SimilarityResult>,
    allSimilarities: List<ProSimilarityCalculator.SimilarityResult>,
    expanded: Boolean,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 プロ類似度",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "閉じる" else "全て見る"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // TOP3表示
            similarities.forEachIndexed { index, result ->
                ProSimilarityItem(
                    rank = index + 1,
                    pro = result.pro,
                    similarity = result.similarity,
                    showDetails = false
                )
                if (index < similarities.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 全て表示（展開式）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()
                    allSimilarities.forEachIndexed { index, result ->
                        ProSimilarityItem(
                            rank = index + 1,
                            pro = result.pro,
                            similarity = result.similarity,
                            showDetails = true,
                            breakdown = result.breakdown
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProSimilarityItem(
    rank: Int,
    pro: ProSimilarityCalculator.ProGolferData,
    similarity: Double,
    showDetails: Boolean,
    breakdown: Map<String, Double>? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${rank}位",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = "${pro.emoji} ${pro.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${similarity.toInt()}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        if (showDetails && breakdown != null) {
            Column(
                modifier = Modifier.padding(start = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                breakdown.forEach { (item, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item, fontSize = 12.sp, color = Color.Gray)
                        Text(text = "${value.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GrowthCard(
    currentScore: Int,
    previousScore: Int,
    onCompareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📈 成長記録",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            val diff = currentScore - previousScore
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "前回より ",
                    fontSize = 16.sp
                )
                Text(
                    text = "${if (diff > 0) "+" else ""}${diff}点",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onCompareClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CompareArrows, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("過去と比較する")
            }
        }
    }
}

@Composable
fun ImprovementCard(
    swingData: SwingData,
    expanded: Boolean,
    onExpandClick: () -> Unit
) {
    // 改善ポイントを抽出
    val improvements = mutableListOf<Pair<String, String>>()
    
    if (swingData.backswingAngle < 60 || swingData.backswingAngle > 85) {
        improvements.add("バックスイング角度" to "理想は60-85度です")
    }
    if (swingData.downswingSpeed < 40) {
        improvements.add("ダウンスイング速度" to "もう少しスピードを上げましょう")
    }
    if (swingData.headStability < 70) {
        improvements.add("頭の安定性" to "頭の位置を安定させましょう")
    }
    
    if (improvements.isEmpty()) {
        improvements.add("素晴らしい！" to "全ての項目が理想的です")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 改善ポイント",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "閉じる" else "詳しく見る"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // TOP3表示
            improvements.take(3).forEach { (title, description) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", fontSize = 16.sp)
                    Column {
                        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (expanded) {
                            Text(text = description, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 推定飛距離セクション（クラブタイプ選択可能）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimatedDistanceSection(swingData: SwingData) {
    var selectedClubType by remember { mutableStateOf(swingData.clubType) }
    var expanded by remember { mutableStateOf(false) }
    
    // 選択されたクラブタイプで飛距離を再計算
    val estimatedDistance = remember(selectedClubType) {
        DistanceEstimator.estimateDistanceFromSwingData(
            downswingSpeed = swingData.downswingSpeed,
            backswingAngle = swingData.backswingAngle,
            headStability = swingData.headStability,
            weightTransfer = swingData.weightShift
        )
    }
    
    // クラブタイプごとの飛距離を計算
    val distanceForClub = remember(selectedClubType, estimatedDistance) {
        val baseDistance = estimatedDistance
        val ratio = DistanceEstimator.getClubRatio(selectedClubType)
        baseDistance * ratio
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏌️ 推定飛距離",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${distanceForClub.toInt()} ヤード",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // クラブタイプ選択
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = getClubTypeName(selectedClubType),
                onValueChange = {},
                readOnly = true,
                label = { Text("クラブタイプ") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DistanceEstimator.ClubType.values().forEach { clubType ->
                    DropdownMenuItem(
                        text = { Text(getClubTypeName(clubType)) },
                        onClick = {
                            selectedClubType = clubType
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Text(
            text = "※スイングデータから推定した理論値です",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * クラブタイプ名を取得
 */
fun getClubTypeName(clubType: DistanceEstimator.ClubType): String {
    return when (clubType) {
        DistanceEstimator.ClubType.DRIVER -> "ドライバー (1W)"
        DistanceEstimator.ClubType.WOOD_3 -> "3番ウッド (3W)"
        DistanceEstimator.ClubType.WOOD_5 -> "5番ウッド (5W)"
        DistanceEstimator.ClubType.UT_3 -> "3番ユーティリティ (3UT)"
        DistanceEstimator.ClubType.UT_4 -> "4番ユーティリティ (4UT)"
        DistanceEstimator.ClubType.IRON_4 -> "4番アイアン (4I)"
        DistanceEstimator.ClubType.IRON_5 -> "5番アイアン (5I)"
        DistanceEstimator.ClubType.IRON_6 -> "6番アイアン (6I)"
        DistanceEstimator.ClubType.IRON_7 -> "7番アイアン (7I)"
        DistanceEstimator.ClubType.IRON_8 -> "8番アイアン (8I)"
        DistanceEstimator.ClubType.IRON_9 -> "9番アイアン (9I)"
        DistanceEstimator.ClubType.WEDGE_PW -> "ピッチングウェッジ (PW)"
        DistanceEstimator.ClubType.WEDGE_AW -> "アプローチウェッジ (AW)"
        DistanceEstimator.ClubType.WEDGE_SW -> "サンドウェッジ (SW)"
        DistanceEstimator.ClubType.CUSTOM -> "カスタム"
    }
}

/**
 * 詳細分析カード
 */
@Composable
fun DetailedAnalysisCard(swingData: SwingData) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 詳細分析",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MediaPipe全データ分析",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "閉じる" else "詳細を見る"
                    )
                }
            }
            
            // 3つのスコア表示
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreBadge("技術", swingData.technicalScore, Color(0xFF2196F3))
                ScoreBadge("パワー", swingData.powerScore, Color(0xFFFF5722))
                ScoreBadge("一貫性", swingData.consistencyScore, Color(0xFF4CAF50))
            }
            
            // 詳細（展開式）
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()
                    
                    Text(
                        text = "🦵 下半身",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DetailedScoreItem("膝の安定性", swingData.kneeStability)
                    DetailedScoreItem("足首のバランス", swingData.ankleBalance)
                    DetailedScoreItem("下半身のパワー", swingData.lowerBodyPower)
                    
                    Divider()
                    
                    Text(
                        text = "💪 上半身",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DetailedScoreItem("上半身の同期性", swingData.upperBodySync)
                    DetailedScoreItem("リストコック", swingData.wristCocking, unit = "°")
                    DetailedScoreItem("フォロースルー", swingData.followThrough)
                    
                    Divider()
                    
                    Text(
                        text = "👁️ 頭部・姿勢",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    DetailedScoreItem("ボールへの視線", swingData.eyeOnBall)
                    DetailedScoreItem("姿勢", swingData.posture)
                }
            }
        }
    }
}

/**
 * スコアバッジ
 */
@Composable
fun ScoreBadge(label: String, score: Double, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(60.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${score.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

/**
 * 詳細スコア項目
 */
@Composable
fun DetailedScoreItem(label: String, value: Double, unit: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // プログレスバー
            if (unit.isEmpty()) {
                LinearProgressIndicator(
                    progress = (value / 100.0).toFloat(),
                    modifier = Modifier.width(100.dp),
                    color = when {
                        value >= 80 -> Color(0xFF4CAF50)
                        value >= 60 -> Color(0xFFFFA726)
                        else -> Color(0xFFE53935)
                    }
                )
            }
            
            Text(
                text = "${value.toInt()}$unit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    value >= 80 || unit.isNotEmpty() -> Color(0xFF4CAF50)
                    value >= 60 -> Color(0xFFFFA726)
                    else -> Color(0xFFE53935)
                }
            )
        }
    }
}
