# 🏌️ クラブヘッド検出 - 自動トレーニングガイド

## 🎯 目標

- **データ拡張**: 66枚 → 266枚（200枚追加）
- **トレーニング**: 100エポック
- **目標信頼度**: 50%以上

---

## 🚀 クイックスタート（3ステップ）

### ステップ1: 環境セットアップ

```batch
setup_training_env.bat
```

**実行内容**:
- Python仮想環境作成
- 必要なライブラリインストール
  - ultralytics（YOLOv8）
  - opencv-python
  - albumentations（データ拡張）
  - tqdm, numpy, pillow

**所要時間**: 5〜10分

---

### ステップ2: 自動トレーニング実行

```batch
run_auto_training.bat
```

**実行内容**:
1. **データ拡張**（Roboflow風）
   - 左右反転（Flip）
   - モーションブレ（Blur）
   - 明暗調整（Exposure）
   - 回転（±15度）
   - 色相・彩度調整

2. **YOLOv8トレーニング**
   - エポック数: 100
   - バッチサイズ: 16
   - 画像サイズ: 640x640
   - 早期終了: patience=20

3. **モデル評価**
   - mAP50, mAP50-95
   - Precision, Recall

4. **ONNX変換**
   - Androidアプリ用に自動配置

**所要時間**: 30分〜2時間（GPU使用時）

---

### ステップ3: 結果確認

トレーニング完了後、以下のファイルが生成されます：

```
runs/detect/clubhead_high_confidence/
├── weights/
│   ├── best.pt          # 最良モデル（PyTorch）
│   ├── best.onnx        # ONNX形式
│   └── last.pt          # 最終エポックモデル
├── results.png          # 学習曲線
├── confusion_matrix.png # 混同行列
└── val_batch0_pred.jpg  # 検証結果サンプル
```

**自動配置**:
```
app/src/main/assets/clubhead_yolov8.onnx  ← Androidアプリで使用
```

---

## 📊 データ拡張の詳細

### 拡張手法（Roboflow準拠）

| 手法 | 確率 | 効果 |
|------|------|------|
| **左右反転** | 70% | クラブの向きバリエーション |
| **回転（±15°）** | 70% | スイング角度の多様化 |
| **モーションブレ** | 50% | 高速スイング時のブレ再現 |
| **明暗調整** | 60% | 屋内・屋外の照明差対応 |
| **色相・彩度調整** | 40% | カメラ特性の違いに対応 |

### 拡張例

元画像1枚から3〜5バリエーション生成：

```
元画像: 251024_073520_0001.jpg
  ↓
拡張後:
  - 251024_073520_0001_aug1.jpg（左右反転）
  - 251024_073520_0001_aug2.jpg（回転+ブレ）
  - 251024_073520_0001_aug3.jpg（明暗調整）
  - 251024_073520_0001_aug4.jpg（複合）
```

---

## 🎓 トレーニングパラメータ

### 基本設定

```python
epochs = 100          # トレーニング回数
batch = 16            # バッチサイズ
imgsz = 640           # 入力画像サイズ
patience = 20         # 早期終了（改善なし20エポックで停止）
conf = 0.5            # 信頼度閾値（50%）
```

### GPU vs CPU

| 環境 | 所要時間 | 推奨 |
|------|----------|------|
| **GPU（CUDA）** | 30分〜1時間 | ✅ 推奨 |
| **CPU** | 2〜4時間 | ⚠️ 遅い |

**GPU確認方法**:
```bash
python -c "import torch; print(torch.cuda.is_available())"
# True → GPU利用可能
# False → CPU使用
```

---

## 📈 評価指標の見方

### mAP（Mean Average Precision）

- **mAP50**: IoU 0.5での平均精度
  - 0.5以上 → 良好 ✅
  - 0.3〜0.5 → 改善の余地 ⚠️
  - 0.3未満 → 要改善 ❌

- **mAP50-95**: IoU 0.5〜0.95での平均精度
  - より厳密な評価指標

### Precision（精度）

- 検出した中で正解の割合
- 高いほど誤検出が少ない

### Recall（再現率）

- 正解の中で検出できた割合
- 高いほど見逃しが少ない

---

## 🔧 トラブルシューティング

### Q1: メモリ不足エラー

**症状**:
```
CUDA out of memory
```

**解決策**:
```python
# auto_augment_and_train.py の batch を減らす
batch = 8  # 16 → 8 に変更
```

---

### Q2: 精度が50%に届かない

**原因**:
- データ量不足
- データの多様性不足

**解決策**:
1. **データ追加**
   ```python
   # auto_augment_and_train.py
   augmenter.augment_dataset(target_count=300)  # 200 → 300
   ```

2. **エポック数増加**
   ```python
   trainer.train_model(epochs=150, batch=16)  # 100 → 150
   ```

3. **より大きなモデル使用**
   ```python
   model = YOLO('yolov8s.pt')  # yolov8n → yolov8s
   ```

---

### Q3: トレーニングが途中で止まる

**症状**:
- プログレスバーが進まない
- エラーなく停止

**解決策**:
1. **データセット確認**
   ```bash
   # ラベルファイルの整合性チェック
   python -c "from pathlib import Path; print(len(list(Path('dataset/images').glob('*.jpg'))), len(list(Path('dataset/labels').glob('*.txt'))))"
   ```

2. **ログ確認**
   ```
   runs/detect/clubhead_high_confidence/train.log
   ```

---

### Q4: ONNX変換エラー

**症状**:
```
Export failure: ONNX export requires...
```

**解決策**:
```bash
# ONNX関連ライブラリを再インストール
pip install --upgrade onnx onnxruntime
```

---

## 🧪 テスト方法

### 1. 単一画像でテスト

```bash
yolo predict model=runs/detect/clubhead_high_confidence/weights/best.pt source=test.jpg conf=0.5
```

### 2. 動画でテスト

```bash
yolo predict model=runs/detect/clubhead_high_confidence/weights/best.pt source=swing.mp4 conf=0.5
```

### 3. Webカメラでリアルタイムテスト

```bash
yolo predict model=runs/detect/clubhead_high_confidence/weights/best.pt source=0 conf=0.5
```

---

## 📱 Androidアプリへの統合

### 1. ONNXファイル確認

```
app/src/main/assets/clubhead_yolov8.onnx
```

### 2. アプリビルド

```bash
./gradlew assembleDebug
```

### 3. インストール

```bash
./gradlew installDebug
```

### 4. 動作確認

1. アプリ起動
2. 「クラブヘッド軌道追跡」タップ
3. カメラでクラブを映す
4. リアルタイムで軌道が表示される

---

## 📚 参考資料

### 公式ドキュメント

- [Ultralytics YOLOv8](https://docs.ultralytics.com/)
- [Albumentations](https://albumentations.ai/)
- [Roboflow](https://roboflow.com/)

### データ拡張のベストプラクティス

1. **多様性を重視**
   - 異なる照明条件
   - 異なるクラブ種別
   - 異なる背景

2. **現実的な拡張**
   - 過度な変形は避ける
   - スイング動作に即した拡張

3. **バランス**
   - 元データと拡張データの比率 1:3〜1:5

---

## 🎉 期待される結果

### トレーニング前

- 信頼度: 20〜30%
- 検出率: 不安定
- 誤検出: 多い

### トレーニング後（目標）

- **信頼度: 50%以上** ✅
- **検出率: 安定** ✅
- **誤検出: 少ない** ✅

---

## 💡 次のステップ

1. ✅ 自動トレーニング実行
2. ✅ 精度評価
3. ✅ Androidアプリでテスト
4. 🔄 必要に応じて再トレーニング
5. 🚀 本番環境へデプロイ

---

## 📞 サポート

問題が発生した場合:

1. **ログ確認**
   ```
   runs/detect/clubhead_high_confidence/train.log
   ```

2. **環境確認**
   ```bash
   pip list
   python --version
   ```

3. **データセット確認**
   ```bash
   python -c "from pathlib import Path; print('Images:', len(list(Path('dataset/images').glob('*.jpg')))); print('Labels:', len(list(Path('dataset/labels').glob('*.txt'))))"
   ```

---

**頑張ってください！** 🚀
