#!/usr/bin/env python3
"""
Roboflowデータセットでトレーニング
"""

from ultralytics import YOLO

def train_model():
    print("🏌️ YOLOv8 トレーニング開始")
    print("=" * 50)
    
    # YOLOv8モデルロード
    model = YOLO('yolov8n.pt')
    
    # トレーニング
    results = model.train(
        data='roboflow_dataset/data.yaml',  # Roboflowからダウンロードしたdata.yaml
        epochs=50,
        imgsz=640,
        batch=16,
        name='clubhead_detection'
    )
    
    print("\n✅ トレーニング完了！")
    print(f"モデル保存先: runs/detect/clubhead_detection/weights/best.pt")
    
    # 評価
    print("\n📊 モデル評価中...")
    metrics = model.val()
    
    print(f"\nmAP50: {metrics.box.map50:.3f}")
    print(f"mAP50-95: {metrics.box.map:.3f}")
    
    # ONNX変換
    print("\n🔄 ONNX変換中...")
    model.export(format='onnx', int8=True)
    
    print("\n🎉 すべて完了！")
    print("\n📋 次のステップ:")
    print("  1. cp runs/detect/clubhead_detection/weights/best.onnx app/src/main/assets/clubhead_yolov8.onnx")
    print("  2. ./gradlew installDebug")
    print("  3. アプリでテスト")

if __name__ == "__main__":
    train_model()
