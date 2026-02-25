# YOLOv8 クラブヘッド検出モデル トレーニングガイド

## 🎯 目標
ゴルフクラブヘッドをリアルタイムで検出するYOLOv8モデルを作成し、Android アプリで使用する。

---

## 📦 必要なもの

### ソフトウェア
- Python 3.8以上
- CUDA対応GPU（推奨）
- 10GB以上のストレージ

### データ
- クラブヘッド画像: 500-1000枚（最低100枚）
- アノテーション（バウンディングボックス）

---

## 🚀 オプション1: ゼロからトレーニング

### ステップ1: 環境構築

```bash
# 仮想環境作成
python -m venv yolo_env
source yolo_env/bin/activate  # Windows: yolo_env\Scripts\activate

# Ultralytics YOLOv8 インストール
pip install ultralytics opencv-python pillow
```

### ステップ2: データセット準備

#### ディレクトリ構造
```
clubhead_dataset/
├── images/
│   ├── train/
│   │   ├── img001.jpg
│   │   ├── img002.jpg
│   │   └── ...
│   └── val/
│       ├── img101.jpg
│       └── ...
└── labels/
    ├── train/
    │   ├── img001.txt
    │   ├── img002.txt
    │   └── ...
    └── val/
        ├── img101.txt
        └── ...
```

#### ラベルフォーマット（YOLO形式）
```
# img001.txt
0 0.5 0.5 0.1 0.1
# クラスID x_center y_center width height（すべて0-1で正規化）
```

#### データセット設定ファイル（clubhead.yaml）
```yaml
# clubhead.yaml
path: /path/to/clubhead_dataset
train: images/train
val: images/val

nc: 1  # クラス数
names: ['clubhead']  # クラス名
```

### ステップ3: トレーニング

```bash
# 基本トレーニング
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=100 imgsz=640

# GPU使用（推奨）
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=100 imgsz=640 device=0

# より高精度（時間かかる）
yolo task=detect mode=train model=yolov8s.pt data=clubhead.yaml epochs=200 imgsz=640
```

#### パラメータ説明
- `model=yolov8n.pt`: ナノモデル（軽量・高速）
- `model=yolov8s.pt`: スモールモデル（精度↑、速度↓）
- `epochs=100`: トレーニング回数
- `imgsz=640`: 入力画像サイズ

### ステップ4: 評価

```bash
# 検証データで評価
yolo task=detect mode=val model=runs/detect/train/weights/best.pt data=clubhead.yaml

# テスト画像で推論
yolo task=detect mode=predict model=runs/detect/train/weights/best.pt source=test.jpg
```

### ステップ5: ONNX変換

```bash
# ONNX形式にエクスポート
yolo export model=runs/detect/train/weights/best.pt format=onnx

# 出力: best.onnx
```

### ステップ6: Androidアプリに組み込み

```bash
# ONNXファイルをリネーム＆配置
cp runs/detect/train/weights/best.onnx clubhead_yolov8.onnx
mv clubhead_yolov8.onnx /path/to/SwingTraceWithAICoaching/app/src/main/assets/
```

---

## 🔄 オプション2: 既存モデルを探す

### 公開データセット・モデル

1. **Roboflow Universe**
   - https://universe.roboflow.com/
   - 検索: "golf club" "golf ball" "sports equipment"

2. **Kaggle**
   - https://www.kaggle.com/datasets
   - 検索: "golf swing" "golf detection"

3. **GitHub**
   - YOLOv8 golf projects
   - スポーツ用具検出プロジェクト

### 既存モデルの使用方法

```bash
# ダウンロードしたモデルをONNXに変換
yolo export model=downloaded_model.pt format=onnx

# assetsに配置
cp downloaded_model.onnx app/src/main/assets/clubhead_yolov8.onnx
```

---

## 🧪 オプション3: 簡易テスト（汎用モデル）

YOLOv8の汎用物体検出モデルで一時的にテスト可能。

```bash
# YOLOv8ナノモデルをダウンロード＆ONNX変換
yolo export model=yolov8n.pt format=onnx

# assetsに配置
cp yolov8n.onnx app/src/main/assets/clubhead_yolov8.onnx
```

**注意**: 汎用モデルはクラブヘッド専用ではないため、精度は低いです。

---

## 📊 データ収集のヒント

### 画像収集方法

1. **自分で撮影**
   - スマホでスイング動画を撮影
   - FFmpegでフレーム抽出: `ffmpeg -i swing.mp4 -vf fps=5 img%04d.jpg`

2. **YouTube動画**
   - ゴルフレッスン動画
   - プロのスイング動画
   - フレーム抽出して使用

3. **公開データセット**
   - Roboflow
   - Kaggle

### アノテーション（ラベリング）ツール

1. **LabelImg** (推奨)
   ```bash
   pip install labelImg
   labelImg
   ```

2. **Roboflow** (オンライン)
   - https://roboflow.com/
   - 自動アノテーション機能あり

3. **CVAT**
   - https://www.cvat.ai/
   - チーム作業向け

---

## 🎯 推奨ワークフロー

### 初心者向け（最短ルート）

```
1. Roboflowで既存データセット検索
   ↓
2. 見つかれば → ダウンロード → ONNX変換
   ↓
3. 見つからなければ → 自分で100枚撮影
   ↓
4. LabelImgでアノテーション
   ↓
5. YOLOv8nでトレーニング（epochs=50）
   ↓
6. ONNX変換 → アプリに組み込み
```

### 本格派向け（高精度）

```
1. 1000枚以上の画像を収集
   ↓
2. 多様な環境（室内・屋外・照明）
   ↓
3. データ拡張（回転・反転・明度変更）
   ↓
4. YOLOv8sでトレーニング（epochs=200）
   ↓
5. 精度評価 → 改善 → 再トレーニング
   ↓
6. ONNX変換 → アプリに組み込み
```

---

## 🔧 トラブルシューティング

### Q: GPUが使えない
```bash
# CPU版でトレーニング（遅い）
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=50 device=cpu
```

### Q: メモリ不足
```bash
# バッチサイズを減らす
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml batch=8
```

### Q: 精度が低い
- データ量を増やす（500枚以上）
- データ拡張を使用
- より大きなモデル（yolov8s, yolov8m）
- epochs数を増やす

### Q: ONNX変換エラー
```bash
# opset指定
yolo export model=best.pt format=onnx opset=12
```

---

## 📱 Androidアプリでの使用

### 1. モデルファイル配置
```
app/src/main/assets/clubhead_yolov8.onnx
```

### 2. ビルド＆実行
```bash
./gradlew installDebug
```

### 3. 動作確認
- 「クラブヘッド軌道追跡」ボタンをタップ
- カメラでクラブを映す
- リアルタイムで軌道が表示される

---

## 🎉 完成後の機能

- ✅ リアルタイムクラブヘッド検出
- ✅ 軌道の色分け表示（青→赤→緑）
- ✅ インパクト位置検出（黄色★）
- ✅ スコア計算（0-100点）
- ✅ Gemini AIコメント生成

---

## 📚 参考リンク

- [Ultralytics YOLOv8 公式](https://docs.ultralytics.com/)
- [YOLOv8 トレーニングガイド](https://docs.ultralytics.com/modes/train/)
- [Roboflow Universe](https://universe.roboflow.com/)
- [LabelImg GitHub](https://github.com/heartexlabs/labelImg)

---

## 💡 次のステップ

1. **データ収集開始** - 100枚の画像を撮影
2. **アノテーション** - LabelImgでラベリング
3. **トレーニング** - YOLOv8でモデル作成
4. **ONNX変換** - アプリで使用可能な形式に
5. **統合テスト** - 実機でテスト

**頑張ってください！** 🚀
