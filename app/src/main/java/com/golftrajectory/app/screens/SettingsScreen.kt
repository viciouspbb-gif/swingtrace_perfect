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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golftrajectory.app.AppConfig
import com.swingtrace.aicoaching.ai.CoachingStyle

/**
 * 設定画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentCoachingStyle: CoachingStyle,
    onCoachingStyleChange: (CoachingStyle) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
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
            var devMode by remember { mutableStateOf(AppConfig.currentMode) }

            // コーチングスタイル設定
            Text(
                text = "AIコーチの語り口",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "AIコーチのアドバイスの語り口を選択できます",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // スタイル選択
            CoachingStyleOption(
                style = CoachingStyle.FRIENDLY,
                title = "親しみやすい",
                description = "励ましながら、優しくアドバイスします",
                icon = Icons.Default.EmojiEmotions,
                isSelected = currentCoachingStyle == CoachingStyle.FRIENDLY,
                onClick = { onCoachingStyleChange(CoachingStyle.FRIENDLY) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CoachingStyleOption(
                style = CoachingStyle.PROFESSIONAL,
                title = "プロフェッショナル",
                description = "丁寧で専門的なアドバイスをします",
                icon = Icons.Default.Business,
                isSelected = currentCoachingStyle == CoachingStyle.PROFESSIONAL,
                onClick = { onCoachingStyleChange(CoachingStyle.PROFESSIONAL) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CoachingStyleOption(
                style = CoachingStyle.STRICT,
                title = "厳しい",
                description = "的確で厳しいアドバイスをします",
                icon = Icons.Default.Gavel,
                isSelected = currentCoachingStyle == CoachingStyle.STRICT,
                onClick = { onCoachingStyleChange(CoachingStyle.STRICT) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CoachingStyleOption(
                style = CoachingStyle.MOTIVATIONAL,
                title = "モチベーション重視",
                description = "やる気を高めるアドバイスをします",
                icon = Icons.Default.EmojiEvents,
                isSelected = currentCoachingStyle == CoachingStyle.MOTIVATIONAL,
                onClick = { onCoachingStyleChange(CoachingStyle.MOTIVATIONAL) }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // その他の設定（将来的に追加）
            Text(
                text = "その他の設定",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = "通知設定",
                        description = "練習のリマインダーなど"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Language,
                        title = "言語設定",
                        description = "アプリの表示言語"
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = "アプリについて",
                        description = "バージョン情報・利用規約"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 開発者用モード切替（隠しメニュー）
            Text(
                text = "開発者用設定",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "※ リッキー先生専用：Practice / Athlete 切替",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val nextMode = when (AppConfig.currentMode) {
                        AppConfig.Mode.PRACTICE -> AppConfig.Mode.ATHLETE
                        AppConfig.Mode.ATHLETE -> AppConfig.Mode.PRO
                        AppConfig.Mode.PRO -> AppConfig.Mode.PRACTICE
                    }
                    AppConfig.currentMode = nextMode
                    devMode = nextMode
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = when (devMode) {
                        AppConfig.Mode.PRACTICE -> MaterialTheme.colorScheme.tertiary
                        AppConfig.Mode.ATHLETE -> MaterialTheme.colorScheme.primary
                        AppConfig.Mode.PRO -> MaterialTheme.colorScheme.secondary
                    }
                )
            ) {
                val label = when (devMode) {
                    AppConfig.Mode.PRACTICE -> "🔒 PRACTICE"
                    AppConfig.Mode.ATHLETE -> "⚡ ATHLETE"
                    AppConfig.Mode.PRO -> "🧠 PRO"
                }
                Text("開発者モード切替: $label")
            }
        }
    }
}

@Composable
fun CoachingStyleOption(
    style: CoachingStyle,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
