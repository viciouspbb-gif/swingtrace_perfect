package com.golftrajectory.app.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 無料枠超過ダイアログ
 */
@Composable
fun QuotaExceededDialog(
    featureName: String,
    remainingQuota: Int,
    totalQuota: Int,
    nextResetTime: Long,
    onDismiss: () -> Unit,
    onUpgradeToPremium: () -> Unit,
    onWatchAd: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // アイコン
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // タイトル
                Text(
                    text = "無料枠を使い切りました",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // メッセージ
                Text(
                    text = "${featureName}は1日${totalQuota}回まで無料で利用できます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 残り回数表示
                LinearProgressIndicator(
                    progress = remainingQuota.toFloat() / totalQuota,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "残り ${remainingQuota}/${totalQuota} 回",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // プレミアムボタン
                Button(
                    onClick = onUpgradeToPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("プレミアムにアップグレード")
                }
                
                // 広告視聴ボタン（オプション）
                if (onWatchAd != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onWatchAd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("広告を見て+1回")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // キャンセルボタン
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("後で")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // リセット時刻
                Text(
                    text = "次のリセット: ${formatResetTime(nextResetTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * プレミアム機能紹介カード
 */
@Composable
fun PremiumFeatureCard(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA500)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "プレミアム会員",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumFeatureItem(
                    icon = Icons.Default.AllInclusive,
                    text = "無制限の分析回数"
                )
                PremiumFeatureItem(
                    icon = Icons.Default.SmartToy,
                    text = "Gemini AI分類が使い放題"
                )
                PremiumFeatureItem(
                    icon = Icons.Default.VideoLibrary,
                    text = "動画分析が無制限"
                )
                PremiumFeatureItem(
                    icon = Icons.Default.CloudUpload,
                    text = "クラウド保存（無制限）"
                )
                PremiumFeatureItem(
                    icon = Icons.Default.Block,
                    text = "広告なし"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFFFA500)
                    )
                ) {
                    Text(
                        text = "月額 ¥980 で始める",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 無料枠残り表示バナー
 */
@Composable
fun QuotaBanner(
    remainingCount: Int,
    totalCount: Int,
    featureName: String,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (remainingCount == 0) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = featureName,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "残り ${remainingCount}/${totalCount} 回",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (remainingCount == 0) {
                TextButton(onClick = onUpgrade) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("アップグレード")
                }
            }
        }
    }
}

/**
 * リセット時刻をフォーマット
 */
private fun formatResetTime(timeMillis: Long): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return String.format(
        "%02d:%02d",
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE)
    )
}
