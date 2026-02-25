# 完全統合実装例

## 🎯 ViewModelの使用

```kotlin
// MainActivity.kt または DI Container

val viewModel = SwingTraceViewModel(
    detectClubHeadUseCase = DetectClubHeadUseCase(detector),
    recordTrajectoryUseCase = RecordTrajectoryUseCase(),
    classifySwingPhaseUseCase = ClassifySwingPhaseUseCase(geminiApiKey),
    drawTrajectoryUseCase = DrawTrajectoryUseCase(),
    quotaManager = FreeQuotaManager(context),
    billingManager = BillingManager(context)
)
```

## 📱 画面での使用

```kotlin
@Composable
fun SwingTraceScreenIntegrated(
    viewModel: SwingTraceViewModel,
    adManager: SwingTraceAdManager,
    activity: Activity
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val remainingFree by viewModel.remainingFreeAnalysis.collectAsState()
    val remainingAds by viewModel.remainingRewardedAds.collectAsState()
    val showQuotaDialog by viewModel.showQuotaDialog.collectAsState()
    val showRewardedAd by viewModel.showRewardedAd.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スイング分析") },
                actions = {
                    // プレミアムバッジ or アップグレードボタン
                    if (isPremium) {
                        Icon(
                            Icons.Default.Star,
                            "Premium",
                            tint = Color(0xFFFFD700)
                        )
                    } else {
                        IconButton(onClick = { /* プレミアム画面へ */ }) {
                            Icon(Icons.Default.Star, "Upgrade")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 残り回数表示（無料ユーザーのみ）
                    if (!isPremium) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("無料分析: 残り${remainingFree}回")
                            Text("広告視聴: 残り${remainingAds}回")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 分析開始ボタン
                    Button(
                        onClick = { viewModel.onAnalyzeSwing() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is SwingTraceViewModel.UiState.Recording
                    ) {
                        Text(
                            if (isPremium) {
                                "分析開始"
                            } else {
                                "分析開始（残り${remainingFree}回）"
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // メインコンテンツ
            // ...
        }
    }
    
    // 無料枠超過ダイアログ
    if (showQuotaDialog) {
        QuotaExceededDialog(
            featureName = "スイング分析",
            remainingQuota = 0,
            totalQuota = 1,
            nextResetTime = quotaManager.getNextResetTime(),
            onDismiss = { viewModel.dismissQuotaDialog() },
            onUpgradeToPremium = { /* プレミアム画面へ */ },
            onWatchAd = null // 広告枠も使い切った場合
        )
    }
    
    // リワード広告表示
    if (showRewardedAd) {
        LaunchedEffect(Unit) {
            adManager.showRewardedAd(
                activity = activity,
                onRewarded = {
                    viewModel.onRewardedAdWatched()
                },
                onAdFailed = {
                    viewModel.dismissRewardedAdDialog()
                    // エラー表示
                }
            )
        }
    }
}
```

## 🎨 プレミアム差別化機能

### 1. 軌道保存（プレミアム限定）

```kotlin
@Composable
fun SaveTrajectoryButton(
    isPremium: Boolean,
    onSave: () -> Unit,
    onUpgrade: () -> Unit
) {
    if (isPremium) {
        Button(onClick = onSave) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text("軌道を保存")
        }
    } else {
        OutlinedButton(onClick = onUpgrade) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("軌道保存（プレミアム限定）")
        }
    }
}
```

### 2. Geminiコメント（プレミアム限定）

```kotlin
@Composable
fun GeminiCommentCard(
    isPremium: Boolean,
    comment: String?,
    onUpgrade: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, null)
                Spacer(Modifier.width(8.dp))
                Text("AI コーチコメント")
                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Lock, null, tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (isPremium && comment != null) {
                Text(comment)
            } else {
                Text(
                    "プレミアム会員限定機能",
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onUpgrade) {
                    Text("プレミアムで解除")
                }
            }
        }
    }
}
```

### 3. 比較機能（プレミアム限定）

```kotlin
@Composable
fun CompareSwingsButton(
    isPremium: Boolean,
    savedSwings: List<SwingData>,
    onCompare: () -> Unit,
    onUpgrade: () -> Unit
) {
    if (isPremium) {
        Button(
            onClick = onCompare,
            enabled = savedSwings.size >= 2
        ) {
            Icon(Icons.Default.Compare, null)
            Spacer(Modifier.width(8.dp))
            Text("スイング比較")
        }
    } else {
        OutlinedButton(onClick = onUpgrade) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("スイング比較（プレミアム限定）")
        }
    }
}
```

## 📊 フロー図

```
ユーザーが「分析開始」をタップ
    ↓
onAnalyzeSwing()
    ↓
┌─────────────────────────┐
│ プレミアムユーザー？    │
└─────────────────────────┘
    ↓ YES              ↓ NO
分析開始          無料枠残あり？
                      ↓ YES      ↓ NO
                  分析開始    広告枠残あり？
                              ↓ YES      ↓ NO
                          広告表示    課金誘導
                              ↓
                          広告視聴
                              ↓
                          分析開始
```

## 💰 収益最大化のポイント

### 1. 無料枠設定
- **1日1回**: 少なすぎず多すぎず
- **リワード広告3回**: ユーザーに選択肢を与える
- **毎日リセット**: 継続利用を促す

### 2. プレミアム価値訴求
- **軌道保存**: データ蓄積の価値
- **Geminiコメント**: AI分析の価値
- **比較機能**: 上達の可視化
- **広告なし**: ストレスフリー

### 3. 広告配置
- **リワード広告**: 無料枠超過時
- **インタースティシャル**: 分析完了時（3回に1回）
- **バナー広告**: ホーム画面下部

## 🎯 KPI目標

- **無料→プレミアム転換率**: 5%
- **広告視聴率**: 70%
- **継続率（7日）**: 40%
- **継続率（30日）**: 20%
