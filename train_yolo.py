#!/usr/bin/env python3
"""
YOLOv8トレーニング
"""

from ultralytics import YOLO
import shutil
import os

def train_model():
    print("🏌️ YOLOv8 クラブヘッド検出 トレーニング")
    print("=" * 50)
    
    # YOLOv8nモデルをロード
    print("\n📥 YOLOv8nモデルをロード...")
    model = YOLO('yolov8n.pt')
    
    print("✅ モデルロード完了")
    
    # トレーニング
    print("\n🚀 トレーニング開始...")
    print("   エポック: 50")
    print("   画像サイズ: 640")
    print("   バッチサイズ: 16")
    print()
    
    results = model.train(
        data='dataset/data.yaml',
        epochs=50,
        imgsz=640,
        batch=16,
        name='clubhead_detection',
        patience=10
    )
    
    print("\n✅ トレーニング完了！")
    
    # 評価
    print("\n📊 モデル評価中...")
    metrics = model.val()
    
    print(f"\nmAP50: {metrics.box.map50:.3f}")
    print(f"mAP50-95: {metrics.box.map:.3f}")
    
    # ONNX変換
    print("\n🔄 ONNX変換中...")
    onnx_path = model.export(format='onnx', imgsz=640)
    
    print(f"✅ ONNX変換完了: {onnx_path}")
    
    # assetsフォルダにコピー
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)
    
    dest = os.path.join(assets_dir, "clubhead_yolov8.onnx")
    shutil.copy(onnx_path, dest)
    
    size_mb = os.path.getsize(dest) / (1024 * 1024)
    
    print(f"\n📱 アプリに配置: {dest}")
    print(f"   サイズ: {size_mb:.2f} MB")
    
    print("\n" + "=" * 50)
    print("🎉 すべて完了！")
    print("=" * 50)
    print("\n📋 次のステップ:")
    print("   .\\gradlew installDebug")
    print("   アプリで「クラブヘッド軌道追跡」をテスト")

if __name__ == "__main__":
    train_model()
