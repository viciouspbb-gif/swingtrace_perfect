package com.golftrajectory.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 弾道・飛距離の結果表示カード
 */
@Composable
fun TrajectoryResultCard(
    carryDistance: Double,
    maxHeight: Double,
    flightTime: Double,
    onAIDiagnosisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // アニメーション用の状態
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "card_animation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // タイトル
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "弾道分析結果",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider()
            
            // 飛距離（メイン表示）
            TrajectoryDataItem(
                icon = Icons.Default.SportsGolf,
                label = "推定飛距離",
                value = String.format("%.1f", carryDistance * animatedProgress),
                unit = "m",
                secondaryValue = String.format("%.1f", carryDistance * animatedProgress * 1.09361),
                secondaryUnit = "yd",
                isMain = true
            )
            
            // 最高到達点
            TrajectoryDataItem(
                icon = Icons.Default.Height,
                label = "最高到達点",
                value = String.format("%.1f", maxHeight * animatedProgress),
                unit = "m",
                secondaryValue = String.format("%.1f", maxHeight * animatedProgress * 3.28084),
                secondaryUnit = "ft"
            )
            
            // 滞空時間
            TrajectoryDataItem(
                icon = Icons.Default.Timer,
                label = "滞空時間",
                value = String.format("%.2f", flightTime * animatedProgress),
                unit = "秒"
            )
            
            Divider()
            
            // AI診断ボタン
            Button(
                onClick = onAIDiagnosisClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI診断を受ける",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TrajectoryDataItem(
    icon: ImageVector,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (isMain) 24.dp else 20.dp)
            )
            Text(
                text = label,
                fontSize = if (isMain) 16.sp else 14.sp,
                fontWeight = if (isMain) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = if (isMain) 24.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = unit,
                    fontSize = if (isMain) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = if (isMain) 2.dp else 1.dp)
                )
            }
            
            if (secondaryValue != null && secondaryUnit != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = secondaryValue,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = secondaryUnit,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
