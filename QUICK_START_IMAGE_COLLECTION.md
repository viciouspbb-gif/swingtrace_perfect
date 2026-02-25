# 🚀 クイックスタート: 画像収集からモデル改善まで

## ⏱️ 所要時間: 約1.5〜3時間

---

## 📋 準備（5分）

### 1. APIキーを取得

**Pixabay**:
```
1. https://pixabay.com/ でアカウント作成
2. https://pixabay.com/api/docs/ でAPIキー取得
3. コマンドプロンプトで実行:
   setx PIXABAY_API_KEY "your_key_here"
```

**Unsplash**:
```
1. https://unsplash.com/developers でアプリ登録
2. Access Key取得
3. コマンドプロンプトで実行:
   setx UNSPLASH_ACCESS_KEY "your_key_here"
```

**Pexels**:
```
1. https://www.pexels.com/api/ でAPIキー取得
2. コマンドプロンプトで実行:
   setx PEXELS_API_KEY "your_key_here"
```

💡 **重要**: 環境変数設定後、コマンドプロンプトを再起動してください。

---

## 🎯 ステップ1: 画像収集（10-20分）

```batch
download_images.bat
```

**実行内容**:
- Pixabay, Unsplash, Pexelsから画像をダウンロード
- 約180-200枚の著作権フリー画像を収集
- `downloaded_images/` フォルダに保存

**結果**:
```
✅ 合計: 180枚
📁 保存先: downloaded_images/
```

---

## 🏷️ ステップ2: ラベル付け（30-50分）

### 2-1. Roboflowにアップロード
```
1. https://roboflow.com/ にアクセス
2. 「Create New Project」→ Object Detection
3. プロジェクト名: Golf Club Head Detection
4. クラス名: clubhead
5. 「Upload」→ downloaded_images/ の画像をドラッグ&ドロップ
```

### 2-2. ラベル付け
```
1. 「Annotate」タブをクリック
2. クラブヘッドを矩形で囲む
3. Enterキーで次の画像へ
4. 全画像にラベル付け（1枚10-30秒）
```

**キーボードショートカット**:
- `B`: ボックスモード
- `1`: クラス1（clubhead）
- `Enter`: 次の画像
- `Ctrl+Z`: 元に戻す

### 2-3. エクスポート
```
1. 「Generate」→ 「Generate New Version」
2. 「Export」→ Format: YOLOv8
3. 「Download ZIP to Computer」
```

### 2-4. データセットの配置
```batch
# ZIPを展開後
xcopy roboflow-export\train\images\*.jpg dataset\images\ /Y
xcopy roboflow-export\train\labels\*.txt dataset\labels\ /Y
xcopy roboflow-export\valid\images\*.jpg dataset\images\ /Y
xcopy roboflow-export\valid\labels\*.txt dataset\labels\ /Y
```

---

## 🔄 ステップ3: データ拡張 & トレーニング（30分〜2時間）

### 3-1. 環境セットアップ（初回のみ）
```batch
setup_training_env.bat
```

### 3-2. 自動トレーニング実行
```batch
run_auto_training.bat
```

**実行内容**:
1. データ拡張（200枚追加）
2. YOLOv8トレーニング（100エポック）
3. モデル評価
4. ONNX変換
5. アプリへの配置

**結果**:
```
✅ トレーニング完了
📊 最終結果:
   mAP50: 0.823
   平均信頼度: 0.42
✅ ONNX変換完了
✅ アプリに配置完了
```

---

## 🧪 ステップ4: テスト（5-10分）

```batch
test_model.bat
```

**テストメニュー**:
```
1. 画像でテスト
2. 動画でテスト
3. Webカメラでテスト
```

**確認項目**:
- ✅ 信頼度が30%以上
- ✅ クラブヘッドが正確に検出される
- ✅ バウンディングボックスが適切

---

## 📱 ステップ5: アプリで確認（5分）

### 5-1. アプリをビルド
```batch
cd app
gradlew installDebug
```

### 5-2. 動画を処理
```
1. アプリを起動
2. 「📹 動画」ボタンをタップ
3. 動画を選択
4. 処理完了を待つ
```

### 5-3. 結果を確認
```
✅ 良好: 65/90フレーム検出 (72%)
平均信頼度: 42%

📊 検出統計:
  総フレーム数: 90
  検出数: 75 (83%)
  採用数: 65 (72%)
  平均信頼度: 42%
  最大信頼度: 68%
```

---

## 📊 期待される改善

### トレーニング前
```
採用率: 20-40% ❌
平均信頼度: 15-25%
UI表示: 不安定
```

### トレーニング後
```
採用率: 40-70% ✅
平均信頼度: 30-45% ✅
UI表示: 安定 ✅
```

---

## 🔧 トラブルシューティング

### APIキーが認識されない
```batch
# 環境変数を確認
echo %PIXABAY_API_KEY%

# 再設定
setx PIXABAY_API_KEY "your_key_here"

# コマンドプロンプトを再起動
```

### 画像ダウンロードが失敗する
```
- インターネット接続を確認
- APIキーの有効性を確認
- レート制限を確認
- 時間をおいて再実行
```

### トレーニングが遅い
```python
# auto_augment_and_train.py
# バッチサイズを下げる
trainer.train_model(epochs=100, batch=8)  # 16 → 8
```

### メモリ不足エラー
```python
# バッチサイズを下げる
trainer.train_model(epochs=100, batch=4)  # 16 → 4
```

---

## 📚 詳細ガイド

- **画像収集**: `download_images.py` のコメント参照
- **ラベル付け**: `ROBOFLOW_GUIDE.md` 参照
- **トレーニング**: `AUTO_TRAINING_GUIDE.md` 参照
- **完全ワークフロー**: `COMPLETE_WORKFLOW_GUIDE.md` 参照

---

## 🎉 まとめ

### 4ステップで完了
1. **画像収集**: `download_images.bat`（10-20分）
2. **ラベル付け**: Roboflow（30-50分）
3. **トレーニング**: `run_auto_training.bat`（30分〜2時間）
4. **テスト**: `test_model.bat`（5-10分）

### 合計所要時間
約1.5〜3時間

### 期待される結果
- ✅ 平均信頼度: 30%以上
- ✅ 採用率: 40-70%
- ✅ UI表示: 安定した軌道描画

**さっそく始めましょう！** 🚀
