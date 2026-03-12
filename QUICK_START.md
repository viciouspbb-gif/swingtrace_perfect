# 🚀 クラブヘッド軌道追跡 クイックスタート

## 📋 必要なもの

- Python 3.8以上
- 100枚以上のクラブヘッド画像
- GPU（推奨、なくてもOK）

---

## ⚡ 最速5ステップ

### 1️⃣ データセット準備

```bash
# ディレクトリ作成
mkdir -p dataset/images dataset/labels

# 画像を撮影してdataset/images/に配置
# スマホでスイング動画を撮影 → FFmpegでフレーム抽出
ffmpeg -i swing.mp4 -vf fps=5 dataset/images/img%04d.jpg
```

### 2️⃣ ラベリング

```bash
# LabelImgインストール
pip install labelImg

# 起動
labelImg dataset/images dataset/labels

# 各画像でクラブヘッドを囲む
# 保存形式: YOLO
# クラス名: clubhead
```

### 3️⃣ トレーニング実行

**Windows:**
```cmd
setup_and_train.bat
```

**Mac/Linux:**
```bash
chmod +x setup_and_train.sh
./setup_and_train.sh
```

### 4️⃣ ビルド＆実行

```bash
# Android Studio でプロジェクトを開く
# または
./gradlew installDebug
```

### 5️⃣ テスト

1. アプリ起動
2. 「クラブヘッド軌道追跡」タップ
3. カメラでクラブを映す
4. リアルタイムで軌道が表示される！🎉

---

## 🎯 データ収集のコツ

### 撮影条件

✅ **良い例:**
- 明るい屋外
- クラブヘッドが明確に見える
- 様々な角度・距離
- 背景がシンプル

❌ **悪い例:**
- 暗い室内
- ブレている
- クラブヘッドが小さすぎる
- 背景が複雑

### 画像枚数の目安

- **最低**: 100枚
- **推奨**: 300-500枚
- **理想**: 1000枚以上

### バリエーション

- アドレス
- テイクバック
- トップ
- ダウンスイング
- インパクト
- フォロースルー

---

## 🔧 トラブルシューティング

### Q: GPUが使えない

```bash
# CPU版でトレーニング（遅いけどOK）
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=30 device=cpu
```

### Q: メモリ不足

```bash
# バッチサイズを減らす
yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml batch=4
```

### Q: 精度が低い

1. データ量を増やす（500枚以上）
2. より大きなモデルを使用
   ```bash
   yolo task=detect mode=train model=yolov8s.pt data=clubhead.yaml epochs=100
   ```
3. データ拡張を有効化（自動）

### Q: トレーニングが進まない

- データセット構造を確認
- clubhead.yaml のパスを確認
- ラベルファイルの形式を確認（YOLO形式）

---

## 📊 期待される結果

### トレーニング後

```
runs/detect/train/
├── weights/
│   ├── best.pt       # 最良モデル
│   └── last.pt       # 最終モデル
├── results.png       # 学習曲線
└── confusion_matrix.png
```

### 精度の目安

- **mAP50**: 0.8以上（良好）
- **mAP50-95**: 0.5以上（合格）

### アプリでの動作

- **検出速度**: 30fps以上
- **精度**: 80%以上
- **レイテンシ**: 50ms以下

---

## 🎉 完成後の機能

✅ リアルタイムクラブヘッド検出  
✅ 軌道の色分け表示（青→赤→緑）  
✅ インパクト位置検出（黄色★）  
✅ スコア計算（0-100点）  
✅ Gemini AIコメント生成  

---

## 📚 参考リンク

- [YOLOv8 公式ドキュメント](https://docs.ultralytics.com/)
- [LabelImg GitHub](https://github.com/heartexlabs/labelImg)
- [Roboflow チュートリアル](https://roboflow.com/learn)

---

## 💡 次のステップ

1. ✅ データセット準備
2. ✅ トレーニング実行
3. ✅ アプリ統合
4. 📈 精度改善（データ追加・再トレーニング）
5. 🚀 本番リリース

**頑張ってください！** 🏌️‍♂️✨
