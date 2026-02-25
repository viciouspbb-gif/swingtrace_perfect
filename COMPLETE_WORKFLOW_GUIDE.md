# 🎯 完全自動化ワークフロー: 画像収集からモデル改善まで

## 📋 目次
1. [概要](#概要)
2. [4ステップワークフロー](#4ステップワークフロー)
3. [詳細手順](#詳細手順)
4. [期待される結果](#期待される結果)
5. [トラブルシューティング](#トラブルシューティング)

---

## 🎯 概要

### 目的
クラブヘッド検出モデルの信頼度を向上させ、UI表示を安定化させる。

### 目標値
- ✅ **最大信頼度**: 45%以上
- ✅ **平均信頼度**: 30%以上
- ✅ **採用率**: 40-70%（UI閾値0.25f）
- ✅ **UI表示**: 安定した軌道描画

### 使用する画像
- ✅ **著作権フリー**（CC0ライセンス）
- ✅ **商用利用可能**
- ✅ Pixabay, Unsplash, Pexels

---

## 🔄 4ステップワークフロー

```
┌─────────────────────────────────────────────────────────┐
│ 1️⃣ 画像収集（著作権フリー）                              │
│    download_images.py                                   │
│    ↓ 200枚の画像をダウンロード                           │
├─────────────────────────────────────────────────────────┤
│ 2️⃣ ラベル付け（Roboflow）                               │
│    https://roboflow.com/                                │
│    ↓ クラブヘッドの位置を矩形で指定                       │
├─────────────────────────────────────────────────────────┤
│ 3️⃣ データ拡張（自動）                                    │
│    auto_augment_and_train.py                            │
│    ↓ 200枚追加（Flip, Blur, Rotation等）                │
├─────────────────────────────────────────────────────────┤
│ 4️⃣ 再トレーニング（自動）                                │
│    run_auto_training.bat                                │
│    ↓ YOLOv8で100エポック学習 → ONNX変換                 │
└─────────────────────────────────────────────────────────┘
```

---

## 📝 詳細手順

### ステップ1️⃣: 画像収集（著作権フリー）

#### 1-1. APIキーの取得

**Pixabay**:
1. https://pixabay.com/ にアクセス
2. アカウント作成（無料）
3. https://pixabay.com/api/docs/ でAPIキーを取得
4. 環境変数に設定:
   ```batch
   setx PIXABAY_API_KEY "your_api_key_here"
   ```

**Unsplash**:
1. https://unsplash.com/developers にアクセス
2. アプリケーション登録
3. Access Keyを取得
4. 環境変数に設定:
   ```batch
   setx UNSPLASH_ACCESS_KEY "your_access_key_here"
   ```

**Pexels**:
1. https://www.pexels.com/api/ にアクセス
2. APIキーを取得
3. 環境変数に設定:
   ```batch
   setx PEXELS_API_KEY "your_api_key_here"
   ```

💡 **ヒント**: 環境変数設定後、コマンドプロンプトを再起動してください。

#### 1-2. 画像ダウンロード

```batch
# Python環境を確認
python --version

# 必要なライブラリをインストール
pip install requests

# 画像ダウンロードスクリプトを実行
python download_images.py
```

**実行結果**:
```
🔍 Pixabayから検索: 'golf club head'
✅ 15件見つかりました
✅ ダウンロード: pixabay_golf_club_head_001.jpg
✅ ダウンロード: pixabay_golf_club_head_002.jpg
...

📊 ダウンロード完了
✅ 合計: 180枚
📁 保存先: downloaded_images/
```

#### 1-3. 画像の確認

```
downloaded_images/
├── pixabay_golf_club_head_001.jpg
├── pixabay_golf_club_head_002.jpg
├── unsplash_golf_driver_001.jpg
├── pexels_golf_iron_001.jpg
└── ...
```

**チェックポイント**:
- ✅ クラブヘッドが明瞭に映っている
- ✅ 様々な角度・照明条件
- ✅ 異なるクラブ種別
- ❌ ブレが激しい画像は削除

---

### ステップ2️⃣: ラベル付け（Roboflow）

#### 2-1. Roboflowプロジェクト作成

1. https://roboflow.com/ にアクセス
2. 「Create New Project」をクリック
3. プロジェクト名: `Golf Club Head Detection`
4. タイプ: **Object Detection**
5. クラス名: `clubhead`

#### 2-2. 画像アップロード

1. 「Upload」をクリック
2. `downloaded_images/` フォルダの画像をドラッグ&ドロップ
3. 「Finish Uploading」をクリック

💡 **ヒント**: 一度に50-100枚ずつアップロードすると安定します。

#### 2-3. ラベル付け

1. 「Annotate」タブをクリック
2. 画像を選択
3. クラブヘッドを矩形で囲む:
   - マウスでドラッグ
   - クラス「clubhead」を選択
4. `Enter`キーで次の画像へ

**効率化のコツ**:
- キーボードショートカット使用（`B`: ボックスモード、`1`: クラス1）
- スマートポリゴン機能で自動検出
- 似た画像は「Copy Annotations」でコピー

**所要時間**: 100枚で約30-50分

#### 2-4. データセットエクスポート

1. 「Generate」タブをクリック
2. 「Generate New Version」をクリック
3. データ拡張設定（オプション）:
   - ✅ Flip: Horizontal
   - ✅ Rotation: ±15°
   - ✅ Brightness: ±15%
4. 「Generate」をクリック
5. 「Export」タブをクリック
6. フォーマット: **YOLOv8**
7. 「Download ZIP to Computer」をクリック

#### 2-5. データセットの配置

```batch
# ZIPファイルを展開
# roboflow-export.zip → roboflow-export/

# train/images/ の内容を dataset/images/ にコピー
xcopy roboflow-export\train\images\*.jpg dataset\images\ /Y

# train/labels/ の内容を dataset/labels/ にコピー
xcopy roboflow-export\train\labels\*.txt dataset\labels\ /Y

# valid/ も同様にコピー（オプション）
xcopy roboflow-export\valid\images\*.jpg dataset\images\ /Y
xcopy roboflow-export\valid\labels\*.txt dataset\labels\ /Y
```

**確認**:
```
dataset/
├── images/
│   ├── 251024_073520_0001.jpg (既存)
│   ├── pixabay_golf_club_head_001.jpg (新規)
│   └── ...
└── labels/
    ├── 251024_073520_0001.txt (既存)
    ├── pixabay_golf_club_head_001.txt (新規)
    └── ...
```

---

### ステップ3️⃣: データ拡張（自動）

#### 3-1. 環境セットアップ（初回のみ）

```batch
setup_training_env.bat
```

**実行内容**:
- Python仮想環境作成
- ultralytics, albumentations等をインストール
- 所要時間: 5-10分

#### 3-2. データ拡張の確認

`auto_augment_and_train.py`は自動的に以下を実行します:

```python
# データ拡張設定
augmenter.augment_dataset(target_count=200)
```

**拡張内容**:
- Flip（左右反転）
- Motion Blur（モーションブレ）
- Gaussian Blur（ガウシアンブラー）
- Brightness/Contrast（明暗調整）
- Rotation（±15°）
- Hue/Saturation（色相・彩度）
- RGB Shift（RGB調整）

**結果**:
```
dataset/augmented/
├── images/
│   ├── 251024_073520_0001.jpg (オリジナル)
│   ├── 251024_073520_0001_aug_1.jpg (拡張1)
│   ├── 251024_073520_0001_aug_2.jpg (拡張2)
│   └── ...
└── labels/
    ├── 251024_073520_0001.txt
    ├── 251024_073520_0001_aug_1.txt
    └── ...
```

---

### ステップ4️⃣: 再トレーニング（自動）

#### 4-1. トレーニング実行

```batch
run_auto_training.bat
```

**実行内容**:
1. データ拡張（200枚追加）
2. YOLOv8トレーニング
   - エポック: 100
   - バッチサイズ: 16
   - 画像サイズ: 640x640
   - 早期終了: patience=20
3. モデル評価
   - mAP50, mAP50-95
   - Precision, Recall
4. ONNX変換
   - `best.pt` → `best.onnx`
5. アプリへの配置
   - `app/src/main/assets/clubhead_yolov8.onnx`

**所要時間**: 30分〜2時間（GPU使用時）

#### 4-2. トレーニングログの確認

```
Epoch 1/100: 100%|██████████| 10/10 [00:15<00:00,  1.56s/it]
      Class     Images  Instances      Box(P          R      mAP50  mAP50-95)
        all        266        266      0.654      0.721      0.698      0.432

Epoch 50/100: 100%|██████████| 10/10 [00:12<00:00,  1.23s/it]
      Class     Images  Instances      Box(P          R      mAP50  mAP50-95)
        all        266        266      0.782      0.845      0.823      0.567

✅ トレーニング完了
📊 最終結果:
   mAP50: 0.823
   Precision: 0.782
   Recall: 0.845
   平均信頼度: 0.42

✅ ONNX変換完了
✅ アプリに配置完了
```

#### 4-3. モデルテスト

```batch
test_model.bat
```

**テストメニュー**:
```
1. 画像でテスト
2. 動画でテスト
3. Webカメラでテスト
4. 一括テスト
```

**テスト結果例**:
```
📸 画像: test_image.jpg
✅ 検出: 信頼度=0.67
   位置: (320, 240)
   バウンディングボックス: [280, 200, 360, 280]
```

---

## 📊 期待される結果

### トレーニング前
```
現在のデータセット: 42枚
検出率: 80-90%
採用率: 20-40%（UI閾値0.25f）
平均信頼度: 15-25%
最大信頼度: 30-45%
```

### トレーニング後
```
拡張後のデータセット: 242-442枚
検出率: 85-95% ✅
採用率: 40-70%（UI閾値0.25f）✅
平均信頼度: 30-45% ✅
最大信頼度: 50-70% ✅
```

### UI表示の改善
```
トレーニング前:
  ⚠️ 除外: 信頼度=0.123 < 0.25
  ⚠️ 除外: 信頼度=0.187 < 0.25
  ✅ 採用: 信頼度=0.267
  採用率: 25% ❌

トレーニング後:
  ✅ 採用: 信頼度=0.456
  ✅ 採用: 信頼度=0.523
  ✅ 採用: 信頼度=0.389
  採用率: 65% ✅
```

---

## 🔧 トラブルシューティング

### 問題1: APIキーが認識されない

**症状**:
```
⚠️ PIXABAY_API_KEYが設定されていません
```

**解決策**:
```batch
# 環境変数を設定
setx PIXABAY_API_KEY "your_key_here"

# コマンドプロンプトを再起動
# 確認
echo %PIXABAY_API_KEY%
```

### 問題2: 画像ダウンロードが失敗する

**症状**:
```
❌ エラー: pixabay_golf_club_head_001.jpg - Connection timeout
```

**解決策**:
- インターネット接続を確認
- APIキーの有効性を確認
- レート制限を確認（Pixabay: 5,000リクエスト/時間）
- 時間をおいて再実行

### 問題3: Roboflowでエクスポートできない

**症状**:
- エクスポートボタンが押せない
- ZIPファイルがダウンロードされない

**解決策**:
- 全ての画像にラベルが付いているか確認
- ブラウザを再読み込み
- 別のブラウザで試す（Chrome推奨）
- 無料プランの制限を確認（1,000枚まで）

### 問題4: トレーニングが遅い

**症状**:
```
Epoch 1/100: 100%|██████████| 10/10 [05:00<00:00, 30.0s/it]
```

**解決策**:
```python
# auto_augment_and_train.py
# バッチサイズを下げる
trainer.train_model(epochs=100, batch=8)  # 16 → 8

# 画像サイズを下げる
trainer.train_model(epochs=100, batch=16, imgsz=416)  # 640 → 416
```

### 問題5: メモリ不足エラー

**症状**:
```
RuntimeError: CUDA out of memory
```

**解決策**:
```python
# バッチサイズを下げる
trainer.train_model(epochs=100, batch=4)  # 16 → 4

# または、CPUで実行
trainer.train_model(epochs=100, batch=16, device='cpu')
```

### 問題6: 信頼度が向上しない

**症状**:
```
平均信頼度: 18% (目標: 30%以上)
```

**解決策**:
1. **データの質を確認**:
   - ラベルが正確か
   - クラブヘッドが明瞭に映っているか
   - 多様性があるか

2. **データ量を増やす**:
   ```python
   # 拡張枚数を増やす
   augmenter.augment_dataset(target_count=300)  # 200 → 300
   ```

3. **エポック数を増やす**:
   ```python
   trainer.train_model(epochs=150, batch=16)  # 100 → 150
   ```

4. **追加の実画像を収集**:
   - 自分で撮影した画像を追加
   - より多様なシーンを含める

---

## 💡 カスタマイズオプション

### データ拡張枚数の変更

```python
# auto_augment_and_train.py の main() 内
augmenter.augment_dataset(target_count=300)  # 200 → 300
```

### エポック数の変更

```python
# auto_augment_and_train.py の main() 内
trainer.train_model(epochs=150, batch=16)  # 100 → 150
```

### バッチサイズの変更

```python
# メモリ不足時
trainer.train_model(epochs=100, batch=8)  # 16 → 8

# メモリに余裕がある時
trainer.train_model(epochs=100, batch=32)  # 16 → 32
```

### UI閾値の調整

```kotlin
// ClubHeadTrackingScreen.kt
val uiConfidenceThreshold = 0.25f  // 0.1f, 0.25f, 0.5f
```

---

## 📋 チェックリスト

### ステップ1: 画像収集
- [ ] APIキーを取得（Pixabay, Unsplash, Pexels）
- [ ] 環境変数に設定
- [ ] `download_images.py` を実行
- [ ] 180-200枚の画像をダウンロード
- [ ] 画像の品質を確認

### ステップ2: ラベル付け
- [ ] Roboflowアカウント作成
- [ ] プロジェクト作成
- [ ] 画像をアップロード
- [ ] 全画像にラベル付け（30-50分）
- [ ] YOLOv8形式でエクスポート
- [ ] `dataset/images/` と `dataset/labels/` に配置

### ステップ3: データ拡張
- [ ] `setup_training_env.bat` を実行（初回のみ）
- [ ] `auto_augment_and_train.py` の設定を確認
- [ ] データ拡張が自動で実行されることを確認

### ステップ4: トレーニング
- [ ] `run_auto_training.bat` を実行
- [ ] トレーニングログを確認
- [ ] mAP50, Precision, Recallを確認
- [ ] ONNX変換を確認
- [ ] アプリへの配置を確認

### ステップ5: テスト
- [ ] `test_model.bat` を実行
- [ ] 画像でテスト
- [ ] 動画でテスト
- [ ] Webカメラでテスト
- [ ] 信頼度と検出率を確認

### ステップ6: アプリ確認
- [ ] アプリをビルド
- [ ] 動画を処理
- [ ] Logcatで統計を確認
- [ ] UI表示を確認（「✅ 良好」表示）
- [ ] 軌道描画の安定性を確認

---

## 🎉 まとめ

### 4ステップで完了
1. **画像収集**: `download_images.py` で著作権フリー画像を収集
2. **ラベル付け**: Roboflowで30-50分
3. **データ拡張**: 自動（200枚追加）
4. **トレーニング**: `run_auto_training.bat` で自動

### 所要時間
- 画像収集: 10-20分
- ラベル付け: 30-50分
- トレーニング: 30分〜2時間
- **合計**: 約1.5〜3時間

### 期待される改善
- ✅ 平均信頼度: 30%以上
- ✅ 採用率: 40-70%
- ✅ UI表示: 安定した軌道描画

**準備完了！さっそく始めましょう！** 🚀
