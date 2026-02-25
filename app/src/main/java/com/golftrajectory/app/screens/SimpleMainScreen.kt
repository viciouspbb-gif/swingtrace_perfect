package com.swingtrace.aicoaching.screens

import androidx.compose.foundation.layout.*
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
 * ログアウト機能付きメイン画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleMainScreenWithLogout(
    userName: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit = {},
    onRearSwingClick: () -> Unit = {},
    onFrontSwingClick: () -> Unit = {},
    onClubHeadTrackingClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onAICoachClick: () -> Unit = {},
    onComparisonClick: () -> Unit = {},
    onProComparisonClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isPremium: Boolean = false,
    isProPlan: Boolean = false,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                actions = {
                    // AIコーチボタン（プレミアム版のみ）- 目立つデザイン
                    if (isProPlan) {
                        FilledTonalButton(
                            onClick = onAICoachClick,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AIコーチと会話",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // プレミアムプランボタン - 目立つデザイン
                    FilledTonalButton(
                        onClick = onPremiumClick,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPremium || isProPlan) Color(0xFFFFD700) else Color(0xFF9E9E9E),
                            contentColor = if (isPremium || isProPlan) Color.Black else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isPremium || isProPlan) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPremium || isProPlan) "プレミアム" else "プラン選び",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // 設定ボタン
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定"
                        )
                    }
                    // 履歴ボタン
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "履歴"
                        )
                    }
                    // ユーザー名表示
                    Text(
                        text = userName,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // ログアウトボタン
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "ログアウト"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        SimpleMainScreen(
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick,
            onRearSwingClick = onRearSwingClick,
            onFrontSwingClick = onFrontSwingClick,
            onClubHeadTrackingClick = onClubHeadTrackingClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
    
    // ログアウト確認ダイアログ
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("ログアウト")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

/**
 * シンプルなメイン画面
 * カメラ撮影のみ
 */
@Composable
fun SimpleMainScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit = {},
    onRearSwingClick: () -> Unit = {},
    onFrontSwingClick: () -> Unit = {},
    onClubHeadTrackingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // ヘッダー
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SportsGolf,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SwingTrace",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "with AI Coaching",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // メインコンテンツ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // アイコン
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // タイトル
            Text(
                text = "スイングを撮影",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 説明
            Text(
                text = "AIが自動でスイングを分析し、\n改善点をアドバイスします",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 撮影ボタン
            Button(
                onClick = onCameraClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "撮影を開始",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "スイング分析",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // スイング分析ボタン
            Button(
                onClick = onRearSwingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "後方スイング分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onFrontSwingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "正面スイング分析",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // クラブヘッド軌道追跡ボタン（NEW!）
            Button(
                onClick = onClubHeadTrackingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)  // オレンジ色で目立たせる
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "クラブヘッド軌道追跡",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "NEW! リアルタイム軌道表示",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 機能説明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "姿勢検出",
                        description = "MediaPipe AIで骨格を検出"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureItem(
                        icon = Icons.Default.Timeline,
                        title = "スイング分析",
                        description = "角度・速度・バランスを計算"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FeatureItem(
                        icon = Icons.Default.Psychology,
                        title = "AIコーチング",
                        description = "改善点をアドバイス"
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
