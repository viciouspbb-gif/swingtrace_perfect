package com.golftrajectory.app.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.golftrajectory.app.plan.Plan
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import android.net.Uri
import com.golftrajectory.app.plan.LitePlanAdBanner
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.utils.LockScreenOrientation
import android.content.pm.ActivityInfo

/**
 * モダンなトップ画面
 * AI分析機能を強調したデザイン
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopScreen(
    userName: String,
    latestScore: Int? = null,
    planTier: Plan,
    userPlanManager: UserPlanManager,
    onCameraClick: () -> Unit,
    onSmartAIAnalysisClick: () -> Unit,
    onRearSwingClick: (Uri) -> Unit = {},
    onFrontSwingClick: (Uri) -> Unit = {},
    onClubHeadTrackingClick: () -> Unit = {},
    onAICoachClick: () -> Unit = {},
    onComparisonClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRequestPlanSwitch: (Plan) -> Unit = {},
    onPlanBadgeLongPress: () -> Unit
) {
    // 縦画面に固定
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var hasAnnouncedInitialPlan by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    val provider = remember {
        GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = com.swingtrace.aicoaching.R.array.com_google_android_gms_fonts_certs
        )
    }
    val montserrat = remember {
        FontFamily(
            Font(
                googleFont = GoogleFont("Montserrat"),
                fontProvider = provider,
                weight = FontWeight.Bold
            )
        )
    }
    val gradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1A237E), Color(0xFF0D1550))
        )
    }
    val statusBarColor = Color(0xFF0D1550)
    SideEffect {
        val window = (view.context as? Activity)?.window
        window?.statusBarColor = statusBarColor.toArgb()
        window?.let {
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = false
        }
    }
    val isLite = planTier == Plan.PRACTICE
    val isPro = planTier == Plan.PRO
    val isCloud = planTier == Plan.PRO
    LaunchedEffect(planTier) {
        if (hasAnnouncedInitialPlan) {
            Toast.makeText(
                context,
                "プランを ${planTier.name} に切り替えました",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            hasAnnouncedInitialPlan = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { onRequestPlanSwitch(Plan.PRACTICE) })
                    }
            ) {
                Text(
                    text = "SwingTrace",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontFamily = montserrat
                )
            }

            Spacer(Modifier.height(12.dp))

            PlanBadge(
                planTier = planTier,
                onLongPress = onPlanBadgeLongPress,
                onSettingsClick = {
                    if (isLite) {
                        Toast
                            .makeText(context, "PROプラン限定機能です", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        onSettingsClick()
                    }
                },
                onLogoutClick = { showLogoutDialog = true }
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "こんにちは、$userName さん",
                color = Color.White,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            FeatureHighlightCard(
                title = "スマートAI解析",
                description = "自動でバックスイング・腰肩回転をスコア化して改善点を提示します。",
                primaryActionLabel = "カメラで新しく撮影",
                secondaryActionLabel = "ギャラリーから選択",
                onPrimaryAction = onCameraClick,
                onSecondaryAction = onSmartAIAnalysisClick
            )

            Spacer(Modifier.height(24.dp))

            SecondaryActionsRow(
                onHistoryClick = onHistoryClick,
                onComparisonClick = onComparisonClick
            )

            Spacer(Modifier.height(24.dp))

            if (latestScore != null) {
                ScoreCard(score = latestScore)
                Spacer(Modifier.height(16.dp))
            }

            if (isLite) {
                UpgradePrompt(
                    onUpgradeClick = onPremiumClick,
                    title = "Liteプランでは機能が一部制限されています",
                    description = "AIコーチとの対話・広告非表示・履歴比較を利用するにはPROプランへのアップグレードが必要です。"
                )
                Spacer(Modifier.height(16.dp))
                LitePlanAdBanner()
            }
            Spacer(Modifier.height(20.dp))
            CloudBadge(
                isCloudPlan = isCloud,
                onClick = { showCloudDialog = true }
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウトしますか？") },
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

    if (showCloudDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDialog = false },
            title = { Text("PRO Cloud (Coming Soon)") },
            text = {
                Text(
                    "サーバー解析による精密スイング診断に加え、練習ラウンドを変える「AIキャディー機能」を開発中。\nあなただけの専属キャディーが、まもなく到着します。",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showCloudDialog = false }) {
                    Text("楽しみに待つ")
                }
            }
        )
    }
}

/**
 * セクションヘッダー
 */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 機能カード
 */
@Composable
fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    isPremium: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) 
                MaterialTheme.colorScheme.surfaceVariant 
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
            // アイコン
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // テキスト
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // バッジ
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFF6B35),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            // プレミアムロック
            if (isPremium) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "プレミアム限定",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PlanBadge(
    planTier: Plan,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val (label, caption, colors) = when (planTier) {
        Plan.PRACTICE -> Triple(
            "PRACTICE",
            "無料版・基本機能",
            listOf(Color(0xFF5C6BC0), Color(0xFF303F9F))
        )
        Plan.ATHLETE -> Triple(
            "ATHLETE",
            "選手版・AI分析",
            listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
        )
        Plan.PRO -> Triple(
            "PRO",
            "プロ版・全機能",
            listOf(Color(0xFFB388FF), Color(0xFF7C4DFF))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "現在のプラン",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = caption,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "設定", tint = Color.White)
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.Default.Logout, contentDescription = "ログアウト", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun FeatureHighlightCard(
    title: String,
    description: String,
    primaryActionLabel: String? = null,
    secondaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val hasActions = primaryActionLabel != null || secondaryActionLabel != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(text = description, color = Color(0xFFB0C4FF), fontSize = 14.sp)

            if (hasActions) {
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(primaryActionLabel, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(secondaryActionLabel, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecondaryActionsRow(
    onHistoryClick: () -> Unit,
    onComparisonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SecondaryActionCard(
            title = "履歴アーカイブ",
            description = "過去のスイングから学ぶ",
            icon = Icons.Default.History,
            onClick = onHistoryClick
        )
        SecondaryActionCard(
            title = "比較モード",
            description = "ベストスイングと比較",
            icon = Icons.Default.Compare,
            onClick = onComparisonClick
        )
    }
}

@Composable
private fun SecondaryActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFB0C4FF))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Color(0xFFB0C4FF), fontSize = 12.sp)
        }
    }
}

@Composable
fun UpgradePrompt(
    title: String,
    description: String,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26336C)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Color(0xFFB0C4FF), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B8CFF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("PROプランを見る", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CloudBadge(
    isCloudPlan: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A3C)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PRO Cloud (Coming Soon)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isCloudPlan) "クラウド解析ベータに参加中" else "クラウド解析 + AIキャディーの先行案内を受け取る",
                    color = Color(0xFFB0C4FF),
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Default.CloudQueue,
                contentDescription = null,
                tint = Color(0xFFB0C4FF),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * スコアカード
 */
@Composable
fun ScoreCard(score: Int) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "📊 最新スコア",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        score >= 90 -> "🏆 素晴らしい！"
                        score >= 80 -> "✨ 良好です"
                        score >= 70 -> "👍 まずまず"
                        else -> "💪 改善中"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Text(
                text = "$score",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
