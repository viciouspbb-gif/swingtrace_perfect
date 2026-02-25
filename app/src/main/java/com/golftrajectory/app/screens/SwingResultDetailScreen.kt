package com.swingtrace.aicoaching.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * スイング分析結果詳細画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwingResultDetailScreen(
    result: com.golftrajectory.app.SwingAnalysisResult,
    adviceList: List<String>,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onSave: () -> Unit = {},
    isPremium: Boolean = false
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分析結果詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "分析結果を保存しました",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    IconButton(onClick = onShare) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 総合スコア
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        result.score >= 80 -> Color(0xFF4CAF50)
                        result.score >= 60 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "総合スコア",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${result.score}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            result.score >= 80 -> "優秀"
                            result.score >= 60 -> "良好"
                            else -> "要改善"
                        },
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 推定飛距離（プレミアム機能）
            if (isPremium && result.estimatedDistance > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏆 推定飛距離（プレミアム）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${result.estimatedDistance.toInt()}ヤード",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "※スイングからの理論値",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ドライバー想定・実測値ではありません",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            } else if (!isPremium && result.estimatedDistance > 0) {
                // 無料ユーザー向けアップグレード誘導
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFFF6F00)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "推定飛距離を見る",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6F00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "スイングデータから飛距離を推定",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "※理論値・ドライバー想定",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* TODO: プレミアムへ */ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Star, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("プレミアムで解放", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 詳細データ
            Text(
                text = "詳細データ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailCard(
                title = "バックスイング角度",
                value = "${result.backswingAngle.toInt()}°",
                ideal = "60-85°",
                isGood = result.backswingAngle in 60f..85f
            )
            
            DetailCard(
                title = "ダウンスイング速度",
                value = "${result.downswingSpeed.toInt()}",
                ideal = "40以上",
                isGood = result.downswingSpeed >= 40
            )
            
            DetailCard(
                title = "腰の回転",
                value = "${result.hipRotation.toInt()}°",
                ideal = "30-45°",
                isGood = result.hipRotation in 30f..45f
            )
            
            DetailCard(
                title = "肩の回転",
                value = "${result.shoulderRotation.toInt()}°",
                ideal = "45-60°",
                isGood = result.shoulderRotation in 45f..60f
            )
            
            DetailCard(
                title = "頭の安定性",
                value = "${result.headStability.toInt()}%",
                ideal = "70%以上",
                isGood = result.headStability >= 70
            )
            
            DetailCard(
                title = "体重移動",
                value = "${result.weightTransfer.toInt()}",
                ideal = "20-60",
                isGood = result.weightTransfer in 20f..60f
            )
            
            DetailCard(
                title = "スイングプレーン",
                value = result.swingPlane,
                ideal = "正常",
                isGood = result.swingPlane == "正常"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // アドバイス
            Text(
                text = "💬 アドバイス",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    adviceList.forEach { advice ->
                        Text(
                            text = advice,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCard(
    title: String,
    value: String,
    ideal: String,
    isGood: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGood) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
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
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "理想値: $ideal",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGood) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = if (isGood) "✓ 良好" else "✗ 要改善",
                    fontSize = 12.sp,
                    color = if (isGood) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
