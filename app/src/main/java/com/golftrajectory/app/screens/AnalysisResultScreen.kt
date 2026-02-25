package com.swingtrace.aicoaching.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swingtrace.aicoaching.api.AICoachingResponse
import com.swingtrace.aicoaching.api.AnalysisResult
import kotlinx.coroutines.launch

/**
 * 分析結果表示画面
 */
@Composable
fun AnalysisResultScreen(
    analysisResult: AnalysisResult,
    aiCoaching: AICoachingResponse?,
    videoUri: Uri,
    onBack: () -> Unit,
    onShareResults: () -> Unit,
    onSaveResults: () -> Unit = {},
    onAskAICoach: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 弾道線動画のURL（パスがあれば）
    val trajectoryVideoUrl = analysisResult.trajectory_video_path?.let {
        val filename = it.substringAfterLast("/")
        "https://swingtrace-ai-server.onrender.com/api/trajectory-video/$filename"
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
        // ヘッダー
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color.White
                    )
                }
                Text(
                    text = "分析結果",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row {
                    IconButton(onClick = {
                        onSaveResults()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "分析結果を保存しました",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "保存",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { shareVideo(context, videoUri) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "共有",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 弾道線動画カード（あれば表示）
            if (trajectoryVideoUrl != null) {
                TrajectoryVideoCard(trajectoryVideoUrl, context)
            }
            
            // 弾道データカード
            TrajectoryDataCard(analysisResult)
            
            // AIコーチングカード（有料プラン）
            if (aiCoaching != null) {
                AICoachingCard(aiCoaching)
            } else {
                // 無料プラン：アップグレード促進カード
                UpgradePromotionCard()
            }
            
            // AIコーチに相談ボタン
            Button(
                onClick = onAskAICoach,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // アクションボタン
            ActionButtons(
                onShareResults = onShareResults,
                onShareVideo = { shareVideo(context, videoUri) }
            )
        }
        }
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun TrajectoryVideoCard(videoUrl: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "弾道線動画",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "ボールの軌跡を可視化した動画です",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("動画を再生")
            }
        }
    }
}

@Composable
private fun TrajectoryDataCard(result: AnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "弾道分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            // 飛距離（ヤードをメイン表示）
            DataItem(
                icon = Icons.Default.SportsGolf,
                label = "推定飛距離",
                value = String.format("%.0f", result.carry_distance * 1.09361),
                unit = "yd",
                isMain = true
            )
            
            // 最高到達点
            DataItem(
                icon = Icons.Default.Height,
                label = "最高到達点",
                value = String.format("%.1f", result.max_height),
                unit = "m"
            )
            
            // 滞空時間
            DataItem(
                icon = Icons.Default.Timer,
                label = "滞空時間",
                value = String.format("%.2f", result.flight_time),
                unit = "秒"
            )
            
            // 信頼度
            DataItem(
                icon = Icons.Default.Verified,
                label = "分析信頼度",
                value = String.format("%.0f", result.confidence * 100),
                unit = "%"
            )
        }
    }
}

@Composable
private fun UpgradePromotionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AIコーチング（有料プラン限定）",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider()
            
            Text(
                text = "有料プランにアップグレードすると、以下の機能が利用できます：",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItem("スイングフォーム分析（MediaPipe）")
                FeatureItem("個別改善アドバイス")
                FeatureItem("スコアリング（100点満点）")
                FeatureItem("強み・弱みの診断")
            }
            
            Button(
                onClick = { /* TODO: プラン画面へ遷移 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("プランを見る")
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AICoachingCard(coaching: AICoachingResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Column {
                    Text(
                        text = "AIコーチング",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "スコア: ${coaching.score}点",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Divider()
            
            // アドバイス
            Text(
                text = coaching.advice,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            
            // 改善点
            if (coaching.improvements.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📌 改善点",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    coaching.improvements.forEach { improvement ->
                        Text(
                            text = "• $improvement",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            // 強み
            if (coaching.strengths.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✨ 強み",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    coaching.strengths.forEach { strength ->
                        Text(
                            text = "• $strength",
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    secondaryValue: String? = null,
    secondaryUnit: String? = null,
    isMain: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (isMain) 24.dp else 20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = if (isMain) 16.sp else 14.sp,
                fontWeight = if (isMain) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = if (isMain) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = if (isMain) 14.sp else 12.sp,
                    modifier = Modifier.padding(bottom = if (isMain) 2.dp else 1.dp)
                )
            }
            
            if (secondaryValue != null && secondaryUnit != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = secondaryValue,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = secondaryUnit,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    onShareResults: () -> Unit,
    onShareVideo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onShareVideo,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("動画を共有")
        }
        
        Button(
            onClick = onShareResults,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("結果を共有")
        }
    }
}

private fun shareVideo(context: Context, videoUri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, videoUri)
        type = "video/mp4"
        putExtra(Intent.EXTRA_TEXT, "SwingTrace with AI Coaching で撮影\n#SwingTrace #ゴルフ #AI分析")
    }
    context.startActivity(Intent.createChooser(shareIntent, "動画を共有"))
}
