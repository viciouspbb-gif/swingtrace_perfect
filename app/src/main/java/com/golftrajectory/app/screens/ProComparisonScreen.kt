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

/**
 * プロとの類似度診断画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProComparisonScreen(
    currentAnalysis: AnalysisHistoryEntity,
    onBack: () -> Unit,
    onSelectPro: (ProGolfer) -> Unit
) {
    var selectedPro by remember { mutableStateOf<ProGolfer?>(null) }
    var targetPro by remember { mutableStateOf<ProGolfer?>(null) }
    
    val proGolfers = remember {
        listOf(
            ProGolfer(
                name = "タイガー・ウッズ",
                style = "パワフル",
                backswingAngle = 85.0,
                downswingSpeed = 95,
                hipRotation = 45.0,
                shoulderRotation = 60.0,
                headStability = 92,
                description = "パワフルで正確なスイング。飛距離と精度を両立。"
            ),
            ProGolfer(
                name = "ローリー・マキロイ",
                style = "スムーズ",
                backswingAngle = 80.0,
                downswingSpeed = 92,
                hipRotation = 42.0,
                shoulderRotation = 58.0,
                headStability = 90,
                description = "流れるようなスムーズなスイング。リズムが重要。"
            ),
            ProGolfer(
                name = "松山英樹",
                style = "安定",
                backswingAngle = 75.0,
                downswingSpeed = 88,
                hipRotation = 40.0,
                shoulderRotation = 55.0,
                headStability = 95,
                description = "安定性重視のスイング。再現性が高い。"
            ),
            ProGolfer(
                name = "ダスティン・ジョンソン",
                style = "シンプル",
                backswingAngle = 78.0,
                downswingSpeed = 90,
                hipRotation = 38.0,
                shoulderRotation = 52.0,
                headStability = 88,
                description = "シンプルで効率的なスイング。無駄がない。"
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プロとの類似度診断") },
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
            // 説明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "あなたのスイングと似ているプロゴルファーを見つけましょう。目指すプロを選ぶと、そのスイングに近づくためのアドバイスが受けられます。",
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // プロゴルファー一覧
            Text(
                text = "プロゴルファーを選択",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            proGolfers.forEach { pro ->
                val similarity = calculateSimilarity(currentAnalysis, pro)
                
                ProGolferCard(
                    pro = pro,
                    similarity = similarity,
                    isSelected = selectedPro == pro,
                    isTarget = targetPro == pro,
                    onClick = {
                        selectedPro = pro
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 選択されたプロの詳細
            if (selectedPro != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                ProDetailCard(
                    pro = selectedPro!!,
                    currentAnalysis = currentAnalysis,
                    onSetAsTarget = {
                        targetPro = selectedPro
                        onSelectPro(selectedPro!!)
                    },
                    isTarget = targetPro == selectedPro
                )
            }
        }
    }
}

@Composable
fun ProGolferCard(
    pro: ProGolfer,
    similarity: Int,
    isSelected: Boolean,
    isTarget: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isTarget -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pro.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isTarget) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "目標",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = pro.style,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "類似度",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "$similarity%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = getSimilarityColor(similarity)
                )
            }
        }
    }
}

@Composable
fun ProDetailCard(
    pro: ProGolfer,
    currentAnalysis: AnalysisHistoryEntity,
    onSetAsTarget: () -> Unit,
    isTarget: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "${pro.name}のスイング",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = pro.description,
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // プロのデータ
            Text(
                text = "スイングデータ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            ProMetricRow("バックスイング角度", pro.backswingAngle, currentAnalysis.backswingAngle, "°")
            Spacer(modifier = Modifier.height(8.dp))
            ProMetricRow("ダウンスイング速度", pro.downswingSpeed.toDouble(), currentAnalysis.downswingSpeed.toDouble(), "")
            Spacer(modifier = Modifier.height(8.dp))
            ProMetricRow("腰の回転", pro.hipRotation, currentAnalysis.hipRotation, "°")
            Spacer(modifier = Modifier.height(8.dp))
            ProMetricRow("肩の回転", pro.shoulderRotation, currentAnalysis.shoulderRotation, "°")
            Spacer(modifier = Modifier.height(8.dp))
            ProMetricRow("頭の安定性", pro.headStability.toDouble(), currentAnalysis.headStability.toDouble(), "%")
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 目標設定ボタン
            Button(
                onClick = onSetAsTarget,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTarget
            ) {
                Icon(
                    imageVector = if (isTarget) Icons.Default.CheckCircle else Icons.Default.Flag,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTarget) "目標に設定済み" else "このプロを目指す"
                )
            }
            
            if (isTarget) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AIコーチが${pro.name}のスイングに近づくためのアドバイスをします",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ProMetricRow(
    label: String,
    proValue: Double,
    yourValue: Double,
    unit: String
) {
    val diff = yourValue - proValue
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
        Row {
            Text(
                text = "プロ: ${proValue}$unit",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "あなた: ${yourValue}$unit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${if (diff > 0) "+" else ""}${String.format("%.1f", diff)})",
                fontSize = 14.sp,
                color = if (kotlin.math.abs(diff) < 5) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        }
    }
}

fun calculateSimilarity(analysis: AnalysisHistoryEntity, pro: ProGolfer): Int {
    val backswingDiff = kotlin.math.abs(analysis.backswingAngle - pro.backswingAngle) / 90.0
    val downswingDiff = kotlin.math.abs(analysis.downswingSpeed - pro.downswingSpeed) / 100.0
    val hipDiff = kotlin.math.abs(analysis.hipRotation - pro.hipRotation) / 45.0
    val shoulderDiff = kotlin.math.abs(analysis.shoulderRotation - pro.shoulderRotation) / 60.0
    val headDiff = kotlin.math.abs(analysis.headStability - pro.headStability) / 100.0
    
    val avgDiff = (backswingDiff + downswingDiff + hipDiff + shoulderDiff + headDiff) / 5.0
    val similarity = ((1.0 - avgDiff) * 100).toInt().coerceIn(0, 100)
    
    return similarity
}

fun getSimilarityColor(similarity: Int): Color {
    return when {
        similarity >= 80 -> Color(0xFF4CAF50)
        similarity >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFE53935)
    }
}

/**
 * プロゴルファーデータ
 */
data class ProGolfer(
    val name: String,
    val style: String,
    val backswingAngle: Double,
    val downswingSpeed: Int,
    val hipRotation: Double,
    val shoulderRotation: Double,
    val headStability: Int,
    val description: String
)
