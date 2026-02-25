# クラブヘッド軌道追跡 UI統合ガイド

## 📱 現在の状態

✅ **実装済み**
- ClubHeadDetector (YOLOv8 ONNX推論)
- SwingTraceViewModel (軌道記録・分析)
- SwingTraceScreen (カメラ+軌道表示)
- SwingResultScreen (結果表示)
- FreeQuotaManager (無料枠管理)
- BillingManager (課金管理)

❌ **未統合**
- メイン画面からのナビゲーション
- 既存UIとの統合

## 🎯 軌道の見え方

### リアルタイム表示
```
┌─────────────────────────┐
│  カメラプレビュー        │
│                         │
│    ●━━━━━━━●          │ ← 青線（テイクバック）
│           ╱             │
│          ●              │ ← 赤線（ダウンスイング）
│         ╱               │
│        ● ⚡             │ ← 黄色★（インパクト）
│         ╲               │
│          ●━━━●         │ ← 緑線（フォロー）
│                         │
│  [記録中... 45 points]  │
└─────────────────────────┘
```

### 結果画面
```
┌─────────────────────────┐
│  軌道アニメーション       │
│  （色分け+マーカー）      │
├─────────────────────────┤
│  📊 フェーズ分析         │
│  ・テイクバック: 15フレーム│
│  ・ダウンスイング: 20フレーム│
│  ・フォロー: 10フレーム    │
├─────────────────────────┤
│  ⭐ インパクトスコア      │
│  85点（プレミアム限定）   │
├─────────────────────────┤
│  [もう1回解析する]        │
│  [広告を見て追加解析]     │
│  [プレミアムで無制限]     │
└─────────────────────────┘
```

## 🔧 メイン画面への統合方法

### オプション1: 新しいメニュー項目を追加

```kotlin
// SimpleMainScreen.kt に追加
Button(
    onClick = { navController.navigate("club_head_tracking") },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Timeline, null)
    Spacer(Modifier.width(8.dp))
    Text("クラブヘッド軌道追跡")
}
```

### オプション2: タブで切り替え

```kotlin
TabRow(selectedTabIndex = selectedTab) {
    Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("通常分析") }
    )
    Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("軌道追跡") }
    )
}
```

### オプション3: フローティングアクションボタン

```kotlin
Scaffold(
    floatingActionButton = {
        FloatingActionButton(
            onClick = { navController.navigate("club_head_tracking") }
        ) {
            Icon(Icons.Default.Timeline, "軌道追跡")
        }
    }
) { ... }
```

## 📝 統合コード例

### NewMainActivity.kt に追加

```kotlin
// NavHost内に追加
composable("club_head_tracking") {
    val viewModel = remember {
        SwingTraceViewModel(
            detectClubHeadUseCase = DetectClubHeadUseCase(
                ClubHeadDetector(this@NewMainActivity)
            ),
            recordTrajectoryUseCase = RecordTrajectoryUseCase(),
            classifySwingPhaseUseCase = ClassifySwingPhaseUseCase(
                geminiApiKey = "YOUR_GEMINI_API_KEY"
            ),
            drawTrajectoryUseCase = DrawTrajectoryUseCase(),
            quotaManager = FreeQuotaManager(this@NewMainActivity),
            billingManager = billingManager
        )
    }
    
    SwingTraceScreen(
        viewModel = viewModel,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### SimpleMainScreen.kt に追加

```kotlin
@Composable
fun SimpleMainScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToClubHeadTracking: () -> Unit, // ← 追加
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 既存のボタン
        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("スイング撮影")
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 新しいボタン
        Button(
            onClick = onNavigateToClubHeadTracking,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.Timeline, null)
            Spacer(Modifier.width(8.dp))
            Text("クラブヘッド軌道追跡")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onNavigateToHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("履歴")
        }
    }
}
```

## 🎨 UI比較

### 従来の分析（既存）
- 動画撮影 → 後処理 → 結果表示
- 軌道は事後に描画
- フレーム単位の分析

### クラブヘッド軌道追跡（新機能）
- リアルタイムカメラ → 即座に軌道描画
- 録画中に軌道が見える
- YOLOv8による高精度検出

## 🚀 クイックスタート

### 1. メイン画面にボタン追加
```kotlin
// SimpleMainScreen.kt
Button(onClick = { navController.navigate("club_head_tracking") }) {
    Text("クラブヘッド軌道追跡")
}
```

### 2. ナビゲーション追加
```kotlin
// NewMainActivity.kt
composable("club_head_tracking") {
    SwingTraceScreen(...)
}
```

### 3. 実行
```bash
./gradlew installDebug
```

### 4. 使い方
1. メイン画面で「クラブヘッド軌道追跡」をタップ
2. カメラが起動
3. 「記録開始」をタップ
4. スイングする
5. リアルタイムで軌道が描画される
6. 「停止」をタップ
7. 結果画面で詳細を確認

## 💡 おすすめの統合方法

### フェーズ1: 開発・テスト
- フローティングアクションボタンで追加
- 既存UIに影響なし
- すぐにテスト可能

### フェーズ2: ベータ版
- メニューに「軌道追跡（ベータ）」として追加
- ユーザーフィードバック収集

### フェーズ3: 正式版
- タブで「通常分析」と「軌道追跡」を切り替え
- メイン機能として統合

## 🎯 次のステップ

1. **メイン画面にボタン追加** ← まずここから！
2. ナビゲーション設定
3. 実機でテスト
4. UIの微調整
5. ユーザーテスト
6. 正式リリース

統合は簡単です！まずはボタン1つ追加するだけでOKです 🎉
