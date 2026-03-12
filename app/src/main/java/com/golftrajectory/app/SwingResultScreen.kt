package com.golftrajectory.app

import androidx.compose.foundation.background
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
import com.golftrajectory.app.billing.FreeQuotaManager
import com.golftrajectory.app.billing.PremiumFeatureCard
import com.golftrajectory.app.usecase.ClassifySwingPhaseUseCase

/**
 * スイング結果画面
 */
@Composable
fun SwingResultScreen(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>,
    phases: List<ClassifySwingPhaseUseCase.SwingPhase>,
    impactScore: Float? = null,
    isPremium: Boolean,
    remainingFreeAnalysis: Int,
    remainingRewardedAds: Int,
    onAnalyzeAgain: () -> Unit,
    onWatchAdForAnalysis: () -> Unit,
    onUpgradeToPremium: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showPremiumDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分析結果") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    if (isPremium) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Premium",
                            tint = Color(0xFFFFD700)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 軌道描画Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                AnimatedSwingPathCanvas(
                    pathPoints = pathPoints,
                    phaseColors = phaseColors,
                    showImpactMarker = true,
                    showScore = isPremium,
                    score = impactScore
                )
            }
            
            // フェーズ分類結果
            PhaseClassificationCard(phases = phases)
            
            // インパクトスコア（プレミアム限定）
            if (isPremium && impactScore != null) {
                ImpactScoreCard(score = impactScore)
            } else if (!isPremium) {
                PremiumFeatureLockedCard(
                    featureName = "インパクトスコア分析",
                    onUpgrade = onUpgradeToPremium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // アクションボタン
            ActionButtons(
                isPremium = isPremium,
                remainingFreeAnalysis = remainingFreeAnalysis,
                remainingRewardedAds = remainingRewardedAds,
                onAnalyzeAgain = onAnalyzeAgain,
                onWatchAdForAnalysis = onWatchAdForAnalysis,
                onUpgradeToPremium = onUpgradeToPremium,
                onShowPremiumDialog = { showPremiumDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // プレミアム機能紹介（無料ユーザーのみ）
            if (!isPremium) {
                PremiumFeatureCard(onUpgrade = onUpgradeToPremium)
            }
        }
    }
    
    // プレミアム特典ダイアログ
    if (showPremiumDialog) {
        PremiumBenefitsDialog(
            onDismiss = { showPremiumDialog = false },
            onUpgrade = {
                showPremiumDialog = false
                onUpgradeToPremium()
            }
        )
    }
}

/**
 * フェーズ分類結果カード
 */
@Composable
fun PhaseClassificationCard(phases: List<ClassifySwingPhaseUseCase.SwingPhase>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "スイングフェーズ分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // フェーズ統計
            val phaseStats = phases.groupingBy { it }.eachCount()
            
            PhaseStatRow(
                phase = "テイクバック",
                count = phaseStats[ClassifySwingPhaseUseCase.SwingPhase.TAKEBACK] ?: 0,
                color = Color.Blue
            )
            PhaseStatRow(
                phase = "ダウンスイング",
                count = phaseStats[ClassifySwingPhaseUseCase.SwingPhase.DOWNSWING] ?: 0,
                color = Color.Red
            )
            PhaseStatRow(
                phase = "フォロー",
                count = phaseStats[ClassifySwingPhaseUseCase.SwingPhase.FOLLOW] ?: 0,
                color = Color.Green
            )
        }
    }
}

@Composable
fun PhaseStatRow(phase: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(phase)
        }
        Text("${count}フレーム", style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * インパクトスコアカード
 */
@Composable
fun ImpactScoreCard(score: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFFD700)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "インパクトスコア",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${score.toInt()}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when {
                    score >= 90 -> "素晴らしい！"
                    score >= 75 -> "良好です"
                    score >= 60 -> "改善の余地あり"
                    else -> "要練習"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * プレミアム機能ロックカード
 */
@Composable
fun PremiumFeatureLockedCard(
    featureName: String,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = featureName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "プレミアム会員限定",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(onClick = onUpgrade) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("プレミアムで解除")
            }
        }
    }
}

/**
 * アクションボタン
 */
@Composable
fun ActionButtons(
    isPremium: Boolean,
    remainingFreeAnalysis: Int,
    remainingRewardedAds: Int,
    onAnalyzeAgain: () -> Unit,
    onWatchAdForAnalysis: () -> Unit,
    onUpgradeToPremium: () -> Unit,
    onShowPremiumDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // もう1回解析するボタン
        Button(
            onClick = onAnalyzeAgain,
            modifier = Modifier.fillMaxWidth(),
            enabled = isPremium || remainingFreeAnalysis > 0
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPremium) {
                    "もう1回解析する"
                } else {
                    "もう1回解析する（残り${remainingFreeAnalysis}回）"
                }
            )
        }
        
        // 無料ユーザー専用ボタン
        if (!isPremium) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 広告を見て追加解析
            OutlinedButton(
                onClick = onWatchAdForAnalysis,
                modifier = Modifier.fillMaxWidth(),
                enabled = remainingRewardedAds > 0
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("広告を見て追加解析（残り${remainingRewardedAds}回）")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // プレミアムで無制限に使う
            Button(
                onClick = onUpgradeToPremium,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("プレミアムで無制限に使う")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // プレミアム特典一覧
            TextButton(
                onClick = onShowPremiumDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("プレミアム特典一覧を見る")
            }
        }
    }
}

/**
 * プレミアム特典ダイアログ
 */
@Composable
fun PremiumBenefitsDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("プレミアム会員特典")
        },
        text = {
            Column {
                BenefitItem("✓ 無制限の分析回数")
                BenefitItem("✓ インパクトスコア分析")
                BenefitItem("✓ Gemini AI分類が使い放題")
                BenefitItem("✓ 動画分析が無制限")
                BenefitItem("✓ クラウド保存（無制限）")
                BenefitItem("✓ 広告なし")
                BenefitItem("✓ 優先サポート")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "月額 ¥980",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text("今すぐ始める")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("後で")
            }
        }
    )
}

@Composable
fun BenefitItem(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}
