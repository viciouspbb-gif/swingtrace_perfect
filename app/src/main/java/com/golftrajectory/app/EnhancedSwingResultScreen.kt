package com.golftrajectory.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.golftrajectory.app.usecase.CalculateSwingScoreUseCase
import com.golftrajectory.app.usecase.ClassifySwingPhaseUseCase
import com.golftrajectory.app.usecase.GenerateAICommentUseCase

/**
 * 強化版スイング結果画面
 * スコア + AIコメント統合版
 */
@Composable
fun EnhancedSwingResultScreen(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>,
    phases: List<ClassifySwingPhaseUseCase.SwingPhase>,
    swingScore: CalculateSwingScoreUseCase.SwingScore,
    aiComment: GenerateAICommentUseCase.AiComment?,
    isPremium: Boolean,
    onAnalyzeAgain: () -> Unit,
    onWatchAd: () -> Unit,
    onUpgradePremium: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スイング分析結果") },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 総合スコア表示
            TotalScoreCard(swingScore.totalScore)
            
            Spacer(Modifier.height(16.dp))
            
            // 軌道アニメーション
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AnimatedSwingPathCanvas(
                    pathPoints = pathPoints,
                    phaseColors = phaseColors,
                    showImpactMarker = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // スコア詳細
            ScoreBreakdownCard(swingScore)
            
            Spacer(Modifier.height(16.dp))
            
            // AIコメント（プレミアム限定）
            if (isPremium && aiComment != null) {
                AICommentCard(aiComment)
            } else if (!isPremium) {
                EnhancedPremiumFeatureLockedCard(
                    featureName = "AIコーチコメント",
                    onUpgrade = onUpgradePremium
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // フェーズ分類
            PhaseClassificationCard(phases)
            
            Spacer(Modifier.height(24.dp))
            
            // アクションボタン
            ActionButtons(
                onAnalyzeAgain = onAnalyzeAgain,
                onWatchAd = onWatchAd,
                onUpgradePremium = onUpgradePremium,
                isPremium = isPremium
            )
        }
    }
}

/**
 * 総合スコアカード
 */
@Composable
fun TotalScoreCard(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 80 -> Color(0xFF4CAF50)
                score >= 60 -> Color(0xFFFFC107)
                else -> Color(0xFFFF9800)
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
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$score",
                fontSize = 72.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "点",
                fontSize = 24.sp,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    score >= 90 -> "🏆 素晴らしいスイング！"
                    score >= 80 -> "✨ 良いスイングです"
                    score >= 70 -> "👍 まずまずです"
                    score >= 60 -> "📈 改善の余地あり"
                    else -> "💪 練習を続けましょう"
                },
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

/**
 * スコア詳細カード
 */
@Composable
fun ScoreBreakdownCard(score: CalculateSwingScoreUseCase.SwingScore) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "スコア詳細",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            
            ScoreItem(
                label = "軌道の滑らかさ",
                score = score.smoothnessScore,
                icon = Icons.Default.Timeline
            )
            ScoreItem(
                label = "姿勢の安定性",
                score = score.poseStability,
                icon = Icons.Default.Accessibility
            )
            ScoreItem(
                label = "回転スコア",
                score = score.rotationScore,
                icon = Icons.Default.RotateRight
            )
            ScoreItem(
                label = "インパクトタイミング",
                score = score.impactTimingScore,
                icon = Icons.Default.Speed
            )
        }
    }
}

@Composable
fun ScoreItem(
    label: String,
    score: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = score,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    score >= 0.8f -> Color(0xFF4CAF50)
                    score >= 0.6f -> Color(0xFFFFC107)
                    else -> Color(0xFFFF9800)
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${(score * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * AIコメントカード
 */
@Composable
fun AICommentCard(comment: GenerateAICommentUseCase.AiComment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI コーチコメント",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            CommentSection(
                emoji = "🟢",
                title = "良い点",
                content = comment.goodPoint
            )
            
            Spacer(Modifier.height(12.dp))
            
            CommentSection(
                emoji = "🟡",
                title = "改善点",
                content = comment.improvement
            )
            
            Spacer(Modifier.height(12.dp))
            
            CommentSection(
                emoji = "🔧",
                title = "アドバイス",
                content = comment.advice
            )
        }
    }
}

@Composable
fun CommentSection(emoji: String, title: String, content: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = emoji,
                fontSize = 20.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}

/**
 * プレミアム機能ロックカード（強化版）
 */
@Composable
fun EnhancedPremiumFeatureLockedCard(
    featureName: String,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$featureName はプレミアム限定機能です",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onUpgrade) {
                Icon(Icons.Default.Star, null)
                Spacer(Modifier.width(8.dp))
                Text("プレミアムにアップグレード")
            }
        }
    }
}

/**
 * アクションボタン
 */
@Composable
fun ActionButtons(
    onAnalyzeAgain: () -> Unit,
    onWatchAd: () -> Unit,
    onUpgradePremium: () -> Unit,
    isPremium: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onAnalyzeAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("もう一度分析する")
        }
        
        if (!isPremium) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("広告を見て追加分析")
            }
            
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onUpgradePremium,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                )
            ) {
                Icon(Icons.Default.Star, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("プレミアムで無制限", color = Color.Black)
            }
        }
    }
}
