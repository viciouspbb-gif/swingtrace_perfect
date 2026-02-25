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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * プレミアム画面（サブスクリプション）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onPurchasePremium: () -> Unit,
    onPurchasePro: () -> Unit,
    isPremium: Boolean = false,
    isProPlan: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレミアムプラン") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 現在のプラン表示
            if (isProPlan) {
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "プレミアムプラン加入中",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else if (isPremium) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "有料プラン加入中",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 無料版の説明
            PlanCard(
                title = "無料版",
                features = listOf(
                    "動画撮影・再生",
                    "基本的なスイング分析",
                    "スコア表示",
                    "簡易アドバイス"
                ),
                limitations = listOf(
                    "広告表示あり",
                    "詳細なAIコーチングなし"
                ),
                isCurrentPlan = !isPremium && !isProPlan,
                onPurchase = null
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 有料版
            PlanCard(
                title = "有料版",
                badge = "人気",
                features = listOf(
                    "✅ 無料版の全機能",
                    "🤖 AIによる詳細コーチング",
                    "📊 改善点の具体的アドバイス",
                    "📈 進捗トラッキング",
                    "🚫 広告なし"
                ),
                isCurrentPlan = isPremium && !isProPlan,
                onPurchase = if (!isPremium) onPurchasePremium else null,
                buttonText = if (isPremium) "加入中" else "今すぐ始める"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // プレミアム版
            PlanCard(
                title = "プレミアム版",
                badge = "おすすめ",
                badgeColor = MaterialTheme.colorScheme.tertiary,
                features = listOf(
                    "✅ 有料版の全機能",
                    "💬 対話式AIコーチング",
                    "🎯 パーソナライズ練習メニュー",
                    "📱 チャットで質問し放題",
                    "⭐ 優先サポート"
                ),
                isCurrentPlan = isProPlan,
                onPurchase = if (!isProPlan) onPurchasePro else null,
                buttonText = if (isProPlan) "加入中" else "プレミアムを始める"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 注意事項
            Text(
                text = "※ サブスクリプションはGoogle Playアカウントに請求されます\n※ いつでもキャンセル可能です",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    features: List<String>,
    limitations: List<String> = emptyList(),
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    isCurrentPlan: Boolean = false,
    onPurchase: (() -> Unit)? = null,
    buttonText: String = "選択"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (badge != null) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // タイトルとバッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (badge != null) {
                    Surface(
                        color = badgeColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 機能リスト
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        fontSize = 14.sp
                    )
                }
            }
            
            // 制限事項
            limitations.forEach { limitation ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = limitation,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // ボタン
            if (onPurchase != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCurrentPlan
                ) {
                    Text(buttonText)
                }
            } else if (isCurrentPlan) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
