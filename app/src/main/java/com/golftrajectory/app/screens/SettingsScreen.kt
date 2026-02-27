package com.golftrajectory.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.golftrajectory.app.plan.Plan
import com.golftrajectory.app.plan.UserPlanManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val planManager = remember { UserPlanManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    val currentPlan by planManager.planFlow.collectAsState(initial = Plan.PRACTICE)
    val useCloud by planManager.useCloudFlow.collectAsState(initial = false)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
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
            // プラン設定
            Text(
                text = "プラン設定",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "現在のプラン: ${planManager.getPlanDisplayName()}",
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // プラン切り替えボタン（デバッグ用）
            if (BuildConfig.DEBUG) {
                Text(
                    text = "デバッグ: プラン切り替え",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                planManager.setPlan(Plan.PRACTICE)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRACTICE")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                planManager.setPlan(Plan.ATHLETE)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ATHLETE")
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                planManager.setPlan(Plan.PRO)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRO")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // PROプランのクラウド設定
            if (currentPlan.isPro()) {
                Text(
                    text = "クラウド解析",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("クラウド機能を使用")
                    
                    Switch(
                        checked = useCloud,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                planManager.setUseCloud(enabled)
                            }
                        }
                    )
                }
                
                if (useCloud) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "クラウド解析が有効です",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // アップグレード案内
            if (currentPlan.isPractice()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "ATHLETEプランにアップグレード",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("• 3色スコア表示")
                        Text("• 全関節トラッキング")
                        Text("• AIコーチフル機能")
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                // TODO: アップグレード画面へ遷移
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("アップグレード")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            } else if (currentPlan.isAthlete()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "PROプランにアップグレード",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("• クラウド解析")
                        Text("• ヘッド軌道分析")
                        Text("• シャフトプレーン")
                        Text("• 専属AIコーチ")
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                // TODO: アップグレード画面へ遷移
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("アップグレード")
                        }
                    }
                }
            }
        }
    }
}
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
