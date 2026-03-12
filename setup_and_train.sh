#!/bin/bash
# YOLOv8 クラブヘッド検出モデル トレーニングスクリプト

set -e

echo "🏌️ YOLOv8 クラブヘッド検出モデル トレーニング開始"
echo "================================================"

# ステップ1: 環境チェック
echo ""
echo "📦 ステップ1: 環境チェック"
python --version
if command -v nvidia-smi &> /dev/null; then
    echo "✅ GPU検出"
    nvidia-smi --query-gpu=name --format=csv,noheader
else
    echo "⚠️  GPU未検出（CPUでトレーニング）"
fi

# ステップ2: YOLOv8インストール
echo ""
echo "📦 ステップ2: YOLOv8インストール"
pip install ultralytics opencv-python pillow

# ステップ3: データセット構造確認
echo ""
echo "📂 ステップ3: データセット構造確認"
if [ -d "dataset" ]; then
    echo "✅ dataset/ ディレクトリ存在"
    echo "   画像数: $(find dataset/images -type f | wc -l)"
    echo "   ラベル数: $(find dataset/labels -type f | wc -l)"
else
    echo "❌ dataset/ ディレクトリが見つかりません"
    echo ""
    echo "以下の構造でデータセットを準備してください："
    echo "dataset/"
    echo "├── images/"
    echo "│   ├── img001.jpg"
    echo "│   └── ..."
    echo "└── labels/"
    echo "    ├── img001.txt"
    echo "    └── ..."
    exit 1
fi

# ステップ4: clubhead.yaml作成
echo ""
echo "📝 ステップ4: clubhead.yaml作成"
cat > clubhead.yaml << EOF
path: $(pwd)/dataset
train: images
val: images

nc: 1
names: ['clubhead']
EOF
echo "✅ clubhead.yaml 作成完了"
cat clubhead.yaml

# ステップ5: トレーニング
echo ""
echo "🚀 ステップ5: トレーニング開始"
echo "   モデル: yolov8n.pt (軽量)"
echo "   エポック: 50"
echo "   画像サイズ: 640x640"
echo ""

yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=50 imgsz=640

# ステップ6: 評価
echo ""
echo "📊 ステップ6: モデル評価"
yolo task=detect mode=val model=runs/detect/train/weights/best.pt data=clubhead.yaml

# ステップ7: ONNX変換
echo ""
echo "🔄 ステップ7: ONNX変換"
yolo export model=runs/detect/train/weights/best.pt format=onnx

# ステップ8: アプリに配置
echo ""
echo "📱 ステップ8: Androidアプリに配置"
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"
cp runs/detect/train/weights/best.onnx "$ASSETS_DIR/clubhead_yolov8.onnx"
echo "✅ $ASSETS_DIR/clubhead_yolov8.onnx に配置完了"

# 完了
echo ""
echo "🎉 トレーニング完了！"
echo "================================================"
echo ""
echo "📊 結果:"
echo "   モデルファイル: runs/detect/train/weights/best.pt"
echo "   ONNXファイル: $ASSETS_DIR/clubhead_yolov8.onnx"
echo ""
echo "📱 次のステップ:"
echo "   1. Android Studioでプロジェクトを開く"
echo "   2. ビルド: ./gradlew installDebug"
echo "   3. アプリで「クラブヘッド軌道追跡」をテスト"
echo ""
