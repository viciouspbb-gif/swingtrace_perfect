package com.golftrajectory.app.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.golftrajectory.app.UsageManager
import com.golftrajectory.app.plan.Plan

/**
 * MainActionHubScreen - アプリ起動時のエントリポイント
 * 黒を基調としたアスリート向け高級感のあるデザイン
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActionHubScreen(
    navController: NavController,
    userName: String = "Guest",
    onVideoSelected: (Uri) -> Unit,
    onCameraClick: () -> Unit,
    isAthlete: Boolean = false
) {
    val context = LocalContext.current
    val usageManager = remember { UsageManager(context) }
    
    // ギャラリー選択用ランチャー
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // ファイルサイズチェック
            val fileSize = getFileSize(context, selectedUri)
            val fileSizeMB = fileSize / (1024.0 * 1024.0)
            
            if (fileSizeMB > 100) {
                Toast.makeText(
                    context,
                    "ファイルサイズが大きすぎます（${String.format("%.1f", fileSizeMB)}MB > 100MB）",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onVideoSelected(selectedUri)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF1A1A1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // タイトル
            Text(
                text = "SwingTrace AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 撮影ボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2196F3),
                                Color(0xFF1976D2)
                            )
                        )
                    ),
                onClick = {
                    if (!usageManager.canUse()) {
                        Toast.makeText(
                            context,
                            "利用回数が上限に達しました。プランをアップグレードしてください。",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Card
                    }
                    onCameraClick()
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "撮影",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "撮影",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "新規スイングを撮影",
                            fontSize = 14.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // キャッチコピー（撮影ボタン付近）
            Text(
                text = "スマートAI分析：肩・腰・膝の3点を精密解析",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0E0E0),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ギャラリー選択ボタン
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF388E3C)
                            )
                        )
                    ),
                onClick = {
                    if (!usageManager.canUse()) {
                        Toast.makeText(
                            context,
                            "利用回数が上限に達しました。プランをアップグレードしてください。",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Card
                    }
                    galleryLauncher.launch("video/*")
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "ギャラリー",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ギャラリー選択",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "保存済み動画を選択",
                            fontSize = 14.sp,
                            color = Color(0xFFE0E0E0)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // アスリート専用ボタン群
            if (isAthlete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accountボタン
                    AthleteActionButton(
                        icon = Icons.Default.AccountCircle,
                        label = "Account",
                        onClick = { navController.navigate("profile") }
                    )
                    
                    // Historyボタン
                    AthleteActionButton(
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = { navController.navigate("history") }
                    )
                    
                    // MY BAGボタン
                    AthleteActionButton(
                        icon = Icons.Default.SportsGolf,
                        label = "MY BAG",
                        onClick = { navController.navigate("myBag") }
                    )
                }
            } else {
                // Practiceユーザー向けの制限付きボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accountボタン（制限付き）
                    RestrictedAthleteActionButton(
                        icon = Icons.Default.AccountCircle,
                        label = "Account",
                        onClick = { showAthleteUpgradeDialog(context) }
                    )
                    
                    // Historyボタン（制限付き）
                    RestrictedAthleteActionButton(
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = { showAthleteUpgradeDialog(context) }
                    )
                    
                    // MY BAGボタン（制限付き）
                    RestrictedAthleteActionButton(
                        icon = Icons.Default.SportsGolf,
                        label = "MY BAG",
                        onClick = { showAthleteUpgradeDialog(context) }
                    )
                }
            }
        }
    }
}

/**
 * アスリート専用アクションボタン
 */
@Composable
private fun AthleteActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    Color(0xFF2C2C2C),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB0B0B0)
        )
    }
}

/**
 * 制限付きアスリートアクションボタン（Practiceユーザー用）
 */
@Composable
private fun RestrictedAthleteActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color(0xFF1A1A1A),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 鍵アイコンを右下に重ねる
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "制限",
                tint = Color(0xFF888888),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
    }
}

/**
 * アスリート版アップグレードダイアログ表示
 */
private fun showAthleteUpgradeDialog(context: android.content.Context) {
    Toast.makeText(
        context,
        "アスリート版限定です",
        Toast.LENGTH_SHORT
    ).show()
}

/**
 * ファイルサイズを取得する関数
 */
private fun getFileSize(context: android.content.Context, uri: Uri): Long {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.available().toLong()
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}
