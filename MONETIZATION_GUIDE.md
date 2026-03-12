# 収益化システム完全ガイド

## 📊 収益化戦略

### 無料プラン
- **リアルタイム分析**: 1日3回
- **動画分析**: 1日1回
- **Gemini AI分類**: 1日2回
- **広告表示**: あり（3回に1回）

### プレミアムプラン（月額¥980）
- **すべての機能**: 無制限
- **広告**: なし
- **クラウド保存**: 無制限
- **優先サポート**: あり

## 🎯 広告表示タイミング

### 1. インタースティシャル広告
```kotlin
// 分析完了時（3回に1回、最低5分間隔）
if (adDisplayStrategy.shouldShowAdAfterAnalysis()) {
    adManager.showInterstitialAd(
        activity = activity,
        onAdClosed = {
            adDisplayStrategy.recordAdShown()
        }
    )
}
```

### 2. リワード広告
```kotlin
// 無料枠超過時
QuotaExceededDialog(
    featureName = "リアルタイム分析",
    remainingQuota = 0,
    totalQuota = 3,
    onWatchAd = {
        adManager.showRewardedAd(
            activity = activity,
            onRewarded = {
                // +1回付与
                quotaManager.grantExtraQuota()
            }
        )
    }
)
```

### 3. バナー広告
```kotlin
// ホーム画面下部
@Composable
fun HomeScreen() {
    Column {
        // コンテンツ
        
        // バナー広告（無料ユーザーのみ）
        if (!isPremium) {
            AndroidView(
                factory = { context ->
                    adManager.createBannerAdView()
                }
            )
        }
    }
}
```

## 💰 使用例

### ViewModelでの統合

```kotlin
class SwingTraceViewModel(
    private val quotaManager: FreeQuotaManager,
    private val adManager: SwingTraceAdManager,
    private val billingManager: BillingManager
) : ViewModel() {
    
    val isPremium = billingManager.isPremium
    val remainingRealtimeAnalysis = quotaManager.remainingRealtimeAnalysis
    
    fun startRecording() {
        viewModelScope.launch {
            // プレミアムユーザーはチェック不要
            if (isPremium.value) {
                startAnalysis()
                return@launch
            }
            
            // 無料枠チェック
            val canUse = quotaManager.useRealtimeAnalysis()
            if (canUse) {
                startAnalysis()
            } else {
                // 無料枠超過ダイアログ表示
                _showQuotaDialog.value = true
            }
        }
    }
    
    fun onAnalysisCompleted() {
        // 広告表示判定
        if (!isPremium.value && adDisplayStrategy.shouldShowAdAfterAnalysis()) {
            _showInterstitialAd.value = true
        }
    }
}
```

### 画面での使用

```kotlin
@Composable
fun SwingTraceScreen(viewModel: SwingTraceViewModel) {
    val isPremium by viewModel.isPremium.collectAsState()
    val remainingQuota by viewModel.remainingRealtimeAnalysis.collectAsState()
    val showQuotaDialog by viewModel.showQuotaDialog.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("スイング分析") },
                actions = {
                    // プレミアムバッジ or アップグレードボタン
                    if (isPremium) {
                        Icon(Icons.Default.Star, "Premium")
                    } else {
                        IconButton(onClick = { /* プレミアム画面へ */ }) {
                            Icon(Icons.Default.Star, "Upgrade")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 無料枠残り表示（無料ユーザーのみ）
            if (!isPremium) {
                QuotaBanner(
                    remainingCount = remainingQuota,
                    totalCount = 3,
                    featureName = "リアルタイム分析",
                    onUpgrade = { /* プレミアム画面へ */ }
                )
            }
            
            // メインコンテンツ
            // ...
            
            // プレミアム機能紹介カード（無料ユーザーのみ）
            if (!isPremium) {
                PremiumFeatureCard(
                    onUpgrade = { /* プレミアム画面へ */ }
                )
            }
        }
    }
    
    // 無料枠超過ダイアログ
    if (showQuotaDialog) {
        QuotaExceededDialog(
            featureName = "リアルタイム分析",
            remainingQuota = 0,
            totalQuota = 3,
            nextResetTime = quotaManager.getNextResetTime(),
            onDismiss = { viewModel.dismissQuotaDialog() },
            onUpgradeToPremium = { /* プレミアム画面へ */ },
            onWatchAd = {
                // リワード広告表示
                adManager.showRewardedAd(
                    activity = activity,
                    onRewarded = {
                        viewModel.grantExtraQuota()
                    }
                )
            }
        )
    }
}
```

## 📈 収益予測

### 月間アクティブユーザー: 1,000人

#### 無料ユーザー（800人）
- インタースティシャル広告: 800人 × 10回/月 × $0.50 = **$4,000**
- リワード広告: 800人 × 5回/月 × $1.00 = **$4,000**
- バナー広告: 800人 × 30日 × $0.10 = **$2,400**

#### プレミアムユーザー（200人）
- サブスクリプション: 200人 × ¥980 = **¥196,000** ($1,400)

### 合計月間収益
- 広告: **$10,400** (約¥1,560,000)
- サブスク: **$1,400** (約¥196,000)
- **総計: $11,800** (約¥1,756,000)

## 🎨 UI/UX ベストプラクティス

### 1. 無料枠残り表示
- 常に見える位置に配置
- 残り0回になったら目立つ色に変更
- 次のリセット時刻を表示

### 2. プレミアム誘導
- 押し付けがましくない
- 価値を明確に伝える
- タイミングを考慮（無料枠使い切った時など）

### 3. 広告表示
- ユーザー体験を損なわない
- 適切な間隔を保つ
- スキップ可能にする

## 🔧 実装チェックリスト

- [ ] FreeQuotaManager実装
- [ ] AdManager実装
- [ ] BillingManager実装
- [ ] QuotaExceededDialog実装
- [ ] PremiumFeatureCard実装
- [ ] QuotaBanner実装
- [ ] ViewModelに統合
- [ ] 画面に統合
- [ ] テスト
- [ ] 本番広告IDに変更
