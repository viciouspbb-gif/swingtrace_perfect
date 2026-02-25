# YOLOv8モデルの取得方法

## ❌ 問題
公式のダウンロードURLが404エラーになっています。

## ✅ 解決方法

### オプション1: Ultralytics公式から変換（推奨）

#### 1. Pythonをインストール（既にある場合はスキップ）

#### 2. Ultralyticsをインストール
```bash
pip install ultralytics
```

#### 3. モデルをダウンロード＆TFLite形式に変換
```bash
yolo export model=yolov8n.pt format=tflite
```

#### 4. 生成されたファイルを移動
```
yolov8n_saved_model/yolov8n_float32.tflite
  ↓
C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching\app\src\main\assets\yolov8n.tflite
```

---

### オプション2: 別のダウンロードソース

#### Hugging Faceから取得
```
https://huggingface.co/Ultralytics/YOLOv8/resolve/main/yolov8n.tflite
```

ブラウザで開いてダウンロード

---

### オプション3: 事前変換済みモデル（Google Colab）

#### 1. Google Colabを開く
https://colab.research.google.com/

#### 2. 以下のコードを実行
```python
!pip install ultralytics

from ultralytics import YOLO

# モデルをロード
model = YOLO('yolov8n.pt')

# TFLite形式にエクスポート
model.export(format='tflite')

# ダウンロード
from google.colab import files
files.download('yolov8n_saved_model/yolov8n_float32.tflite')
```

#### 3. ダウンロードしたファイルを配置
```
yolov8n_float32.tflite
  ↓ リネーム
yolov8n.tflite
  ↓ 移動
app/src/main/assets/yolov8n.tflite
```

---

### オプション4: カスタムモデルを学習（高度）

ゴルフボール専用のYOLOv8モデルを学習する
- より高精度
- 実装時間: 1週間

---

## 🎯 推奨

**オプション2（Hugging Face）が最も簡単です**

1. https://huggingface.co/Ultralytics/YOLOv8/resolve/main/yolov8n.tflite
2. ダウンロード
3. `app/src/main/assets/yolov8n.tflite`に配置

---

## ⚠️ モデルなしでも動作します

YOLOv8モデルがなくても：
- ✅ アプリは起動する
- ✅ 他の機能は使える
- ❌ 自動ボール検出のみ使えない

後で追加することも可能です。
