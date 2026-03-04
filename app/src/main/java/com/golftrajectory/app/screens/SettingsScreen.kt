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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.golftrajectory.app.ai.AppConfig
import com.golftrajectory.app.ai.Plan
import com.golftrajectory.app.ai.UserPlanManager
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val planManager = UserPlanManager.getInstance(context)
    val currentPlan by planManager.currentPlan.collectAsState(initial = Plan.PRACTICE)
    
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

            // プラン情報表示
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "現在のプラン",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentPlan) {
                            com.golftrajectory.app.ai.Plan.PRACTICE -> "PRACTICE (無料版)"
                            com.golftrajectory.app.ai.Plan.ATHLETE -> "ATHLETE (選手版)"
                            com.golftrajectory.app.ai.Plan.PRO -> "PRO (プロ版)"
                        },
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // デベロッパーモード切替（デバッグ時のみ）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "デベロッパーモード",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                val nextMode = when (devMode) {
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
                            Text("現在のモード: ${if (AppConfig.currentMode == AppConfig.Mode.ATHLETE) "PRACTICE" else "ATHLETE"}")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                AppConfig.resetToDefault()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("リセット")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // アップグレードボタン
            if (currentPlan != com.golftrajectory.app.ai.Plan.PRO) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "プランをアップグレード",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                // アップグレード処理（仮）
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

@Composable
fun SettingsItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
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
