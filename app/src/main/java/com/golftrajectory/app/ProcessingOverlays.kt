package com.golftrajectory.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 処理中オーバーレイ
 */
@Composable
fun ProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF4CAF50),
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "動画を処理中...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "クラブヘッドを検出しています",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 成功オーバーレイ
 */
@Composable
fun SuccessOverlay(
    stats: VideoProcessor.DetectionStats,
    qualityLevel: QualityLevel,
    onDismiss: () -> Unit
) {
    val (backgroundColor, icon, title, hint) = when (qualityLevel) {
        QualityLevel.EXCELLENT -> Quadruple(
            Color(0xFF2E7D32),
            "✅",
            "検出成功！",
            "素晴らしい検出率です"
        )
        QualityLevel.GOOD -> Quadruple(
            Color(0xFFF57C00),
            "⚠️",
            "検出完了",
            "まずまずの検出率です"
        )
        QualityLevel.POOR -> Quadruple(
            Color(0xFFE64A19),
            "⚠️",
            "検出率が低い",
            "クラブヘッドがカメラに映っているか確認してください"
        )
        QualityLevel.FAILED -> Quadruple(
            Color(0xFFC62828),
            "❌",
            "検出失敗",
            "クラブヘッドが検出できませんでした。別の動画で試してください"
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    fontSize = 56.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 統計情報
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StatRow("総フレーム数", "${stats.totalFrames}")
                        StatRow("検出数", "${stats.detectedFrames} (${(stats.detectionRate * 100).toInt()}%)")
                        StatRow("採用数", "${stats.adoptedFrames} (${(stats.adoptionRate * 100).toInt()}%)")
                        StatRow("平均信頼度", "${(stats.averageConfidence * 100).toInt()}%")
                        StatRow("最大信頼度", "${(stats.maxConfidence * 100).toInt()}%")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 信頼度分布
                Text(
                    text = "信頼度分布",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DistributionRow("75-100%", stats.confidenceDistribution["75-100%"] ?: 0, Color(0xFF4CAF50))
                        DistributionRow("50-75%", stats.confidenceDistribution["50-75%"] ?: 0, Color(0xFF8BC34A))
                        DistributionRow("25-50%", stats.confidenceDistribution["25-50%"] ?: 0, Color(0xFFFFC107))
                        DistributionRow("10-25%", stats.confidenceDistribution["10-25%"] ?: 0, Color(0xFFFF9800))
                        DistributionRow("0-10%", stats.confidenceDistribution["0-10%"] ?: 0, Color(0xFF9E9E9E))
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // ヒント
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💡 $hint",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("閉じる", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 失敗オーバーレイ
 */
@Composable
fun FailedOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "❌", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "エラー",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("閉じる", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * 統計行
 */
@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 信頼度分布行
 */
@Composable
fun DistributionRow(range: String, count: Int, color: Color) {
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
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = range,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * ヘルパークラス
 */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
