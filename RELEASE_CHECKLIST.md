# 🚀 SwingTrace リリースチェックリスト

## ✅ 1. 課金設計とUI構成

### プレミアム機能定義
- [x] **無制限解析** - 1日の制限なし
- [x] **軌道保存** - クラウド保存無制限
- [x] **Geminiコメント** - AI分析コメント
- [x] **比較機能** - 過去のスイングと比較
- [x] **インパクトスコア** - 詳細分析
- [x] **広告なし** - ストレスフリー

### 無料枠制御
- [x] `FreeQuotaManager.kt` 実装完了
  - 無料分析: 1日1回
  - リワード広告: 1日3回
  - DataStore永続化
  - 自動リセット（毎日0時）

### 課金誘導UI
- [x] `PremiumPromptUI.kt` 実装完了
  - QuotaExceededDialog
  - PremiumFeatureCard
  - QuotaBanner
  - PremiumBenefitsDialog

### 実装状況
```
✅ FreeQuotaManager.kt
✅ PremiumPromptUI.kt
✅ SwingResultScreen.kt
✅ 統合ViewModel
```

---

## ✅ 2. AdMob設定

### 広告ユニットID取得手順

#### Step 1: AdMobアカウント作成
1. https://admob.google.com にアクセス
2. Googleアカウントでログイン
3. 「アプリを追加」をクリック

#### Step 2: アプリ登録
```
アプリ名: SwingTrace - Golf Swing Analyzer
プラットフォーム: Android
パッケージ名: com.golftrajectory.app
```

#### Step 3: 広告ユニット作成

**バナー広告**
```
名前: SwingTrace Banner
広告フォーマット: バナー
サイズ: 320x50 (標準バナー)
```

**インタースティシャル広告**
```
名前: SwingTrace Interstitial
広告フォーマット: インタースティシャル
```

**リワード広告**
```
名前: SwingTrace Rewarded
広告フォーマット: リワード
報酬: +1回の分析
```

#### Step 4: ユニットIDを取得

取得後、以下のファイルを更新：

```kotlin
// SwingTraceAdManager.kt
companion object {
    // 本番環境のID（AdMobから取得）
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
}
```

### 広告表示タイミング

**インタースティシャル広告**
- 分析完了時（3回に1回）
- 最低5分間隔

**リワード広告**
- 無料枠超過時
- ユーザーが「広告を見て+1回」を選択

**バナー広告**
- ホーム画面下部
- 結果画面下部

### 実装状況
```
✅ SwingTraceAdManager.kt
✅ AdDisplayStrategy
⏳ AdMobアカウント作成
⏳ 広告ユニットID取得
⏳ 本番IDに置き換え
```

---

## ✅ 3. Gemini API連携

### APIキー取得
1. https://makersuite.google.com/app/apikey にアクセス
2. 「Create API Key」をクリック
3. APIキーをコピー

### プロンプトテンプレート

```kotlin
// ClassifySwingPhaseUseCase.kt
private fun buildPrompt(trajectory: List<FrameData>): String {
    return buildString {
        append("以下はゴルフスイングのクラブヘッドの座標と時間です。\n")
        append("各行は「時刻(ms), x座標, y座標」を表します。\n\n")
        
        trajectory.forEachIndexed { index, frame ->
            append("Frame ${index + 1}: ${frame.timeMs}ms, x=${frame.x}, y=${frame.y}\n")
        }
        
        append("\nこのデータをもとに、各フレームのスイングフェーズ（TAKEBACK、DOWNSWING、FOLLOW）を判定してください。\n")
        append("\nフェーズの定義:\n")
        append("- TAKEBACK: テイクバック（クラブを後ろに引く動作、x座標が減少）\n")
        append("- DOWNSWING: ダウンスイング（クラブを振り下ろす動作、x座標が増加＋y座標が増加）\n")
        append("- FOLLOW: フォロースルー（インパクト後の動作、x座標が増加＋y座標が減少）\n")
        append("\n出力形式は「Frame 1: TAKEBACK」のようにお願いします。")
    }
}
```

### テスト手順

```bash
# テストデータ作成
val testTrajectory = listOf(
    FrameData(0, 100f, 300f, 0.9f),
    FrameData(100, 90f, 290f, 0.9f),   // TAKEBACK
    FrameData(200, 80f, 280f, 0.9f),   // TAKEBACK
    FrameData(300, 90f, 290f, 0.9f),   // DOWNSWING
    FrameData(400, 100f, 300f, 0.9f),  // DOWNSWING
    FrameData(500, 110f, 290f, 0.9f)   // FOLLOW
)

# 分類テスト
val result = classifySwingPhaseUseCase.classifyWithGemini(testTrajectory)
println(result)
```

### 実装状況
```
✅ ClassifySwingPhaseUseCase.kt
✅ プロンプトテンプレート
✅ レスポンスパーサー
⏳ APIキー取得
⏳ 本番APIキーに置き換え
⏳ 精度テスト
```

---

## ✅ 4. ONNXモデル組み込み

### YOLOv8モデルのONNX化

```bash
# トレーニング
python train_clubhead_yolov8.py

# ONNX変換（自動）
# → runs/detect/clubhead_detector/weights/best.onnx

# モデルを配置
cp runs/detect/clubhead_detector/weights/best.onnx \
   app/src/main/assets/clubhead_yolov8.onnx
```

### 推論テスト

```kotlin
// ClubHeadDetector.kt
val detector = ClubHeadDetector(context)

// テスト画像で推論
val testBitmap = BitmapFactory.decodeResource(resources, R.drawable.test_swing)
val detection = detector.detect(testBitmap)

println("検出結果:")
println("  位置: (${detection?.position?.x}, ${detection?.position?.y})")
println("  信頼度: ${detection?.confidence}")
```

### フローテスト

```kotlin
// 1. 検出
val detection = detector.detect(bitmap)

// 2. 記録
recorder.recordFrame(detection)

// 3. フェーズ判定
val phases = phaseDetector.detectPhase(point)

// 4. 描画
SwingPathCanvas(pathPoints, phaseColors)
```

### 実装状況
```
✅ ClubHeadDetector.kt
✅ TrajectoryRecorder.kt
✅ SwingPhaseDetector.kt
✅ SwingPathCanvas.kt
⏳ データセット作成
⏳ モデルトレーニング
⏳ ONNX変換
⏳ 精度テスト
```

---

## 📱 5. ストア掲載情報

### アプリ名
- **日本語**: SwingTrace - ゴルフスイング分析
- **英語**: SwingTrace - Golf Swing Analyzer

### 短い説明（80文字以内）
- **日本語**: AIでゴルフスイングを分析。クラブヘッドの軌道を可視化してスコアアップ！
- **英語**: AI-powered golf swing analyzer. Visualize club head trajectory!

### 詳細説明

#### 日本語
```
SwingTraceは、AIを活用したゴルフスイング分析アプリです。

【主な機能】
✓ リアルタイムクラブヘッド追跡
✓ スイング軌道の可視化（色分け表示）
✓ AIによるスイングフェーズ分析
✓ 動画分析機能
✓ インパクトスコア評価（プレミアム）
✓ スイング比較機能（プレミアム）

【使い方】
1. スマホでスイング動画を撮影
2. AIが自動でクラブヘッドを検出
3. 軌道を色分けして表示
4. スイングフェーズを分析

【プレミアム機能】
・無制限の分析回数
・軌道データの保存
・Gemini AIによる詳細分析
・スイング比較機能
・広告なし

無料版でも1日1回の分析が可能です。
広告視聴で追加分析もできます。

ゴルフの上達をAIがサポートします！
```

#### 英語
```
SwingTrace is an AI-powered golf swing analyzer app.

【Key Features】
✓ Real-time club head tracking
✓ Swing trajectory visualization (color-coded)
✓ AI swing phase analysis
✓ Video analysis
✓ Impact score evaluation (Premium)
✓ Swing comparison (Premium)

【How to Use】
1. Record your swing with smartphone
2. AI automatically detects club head
3. Visualize trajectory with color coding
4. Analyze swing phases

【Premium Features】
・Unlimited analysis
・Save trajectory data
・Detailed analysis by Gemini AI
・Swing comparison
・Ad-free

Free version includes 1 analysis per day.
Watch ads for additional analysis.

Improve your golf game with AI!
```

### スクリーンショット（必要枚数: 最低2枚、推奨8枚）

1. **ホーム画面** - アプリのメイン画面
2. **リアルタイム分析** - カメラで撮影中の画面
3. **軌道表示** - 色分けされた軌道
4. **分析結果** - フェーズ分類結果
5. **インパクトスコア** - スコア表示（プレミアム）
6. **動画分析** - 既存動画の分析
7. **スイング比較** - 2つのスイングを比較（プレミアム）
8. **プレミアム機能** - 機能一覧

### カテゴリ
- **メインカテゴリ**: スポーツ
- **サブカテゴリ**: ゴルフ

### コンテンツレーティング
- **対象年齢**: 3歳以上

### 価格
- **無料版**: 無料（広告あり）
- **プレミアム**: ¥980/月

---

## 🔧 6. 最終チェック

### ビルド設定

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        applicationId = "com.golftrajectory.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 権限確認

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### ProGuard設定

```
# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Gemini AI
-keep class com.google.ai.client.generativeai.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }
```

### テスト項目

- [ ] カメラ撮影
- [ ] クラブヘッド検出
- [ ] 軌道描画
- [ ] フェーズ分類
- [ ] 動画分析
- [ ] 無料枠管理
- [ ] 広告表示
- [ ] 課金処理
- [ ] データ保存
- [ ] 比較機能

---

## 📦 7. リリース手順

### Step 1: ビルド
```bash
./gradlew clean
./gradlew assembleRelease
```

### Step 2: 署名
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  alias_name
```

### Step 3: 最適化
```bash
zipalign -v 4 app-release-unsigned.apk SwingTrace-v1.0.0.apk
```

### Step 4: Google Play Console
1. https://play.google.com/console にアクセス
2. 「アプリを作成」
3. APKをアップロード
4. ストア掲載情報を入力
5. 審査に提出

---

## 📊 8. KPI設定

### 初月目標
- **ダウンロード数**: 100
- **DAU**: 30
- **無料→プレミアム転換率**: 5%
- **広告視聴率**: 70%

### 3ヶ月目標
- **ダウンロード数**: 1,000
- **DAU**: 300
- **無料→プレミアム転換率**: 10%
- **月間収益**: ¥100,000

---

## ✅ 完了チェックリスト

### 開発
- [x] FreeQuotaManager実装
- [x] 課金誘導UI実装
- [x] Gemini API連携
- [x] ONNX推論実装
- [x] 広告統合

### テスト
- [ ] 機能テスト
- [ ] 広告テスト
- [ ] 課金テスト
- [ ] パフォーマンステスト

### リリース準備
- [ ] AdMobアカウント作成
- [ ] 広告ユニットID取得
- [ ] Gemini APIキー取得
- [ ] ストア掲載情報作成
- [ ] スクリーンショット作成
- [ ] プライバシーポリシー作成

### リリース
- [ ] APKビルド
- [ ] Google Play Console登録
- [ ] 審査提出
- [ ] リリース！🚀
