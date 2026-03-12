#!/usr/bin/env python3
"""
学習済みモデルをONNX変換
"""

from ultralytics import YOLO
import shutil
import os
from pathlib import Path

def export_model():
    print("🔄 学習済みモデルをONNX変換")
    print("=" * 50)
    
    # 学習済みモデルのパスを探す
    runs_dir = Path("runs/detect")
    
    if not runs_dir.exists():
        print("❌ runsフォルダが見つかりません")
        return
    
    # 最新のトレーニング結果を探す
    training_dirs = sorted(runs_dir.glob("clubhead_detection*"), key=lambda x: x.stat().st_mtime, reverse=True)
    
    if not training_dirs:
        print("❌ トレーニング結果が見つかりません")
        return
    
    latest_dir = training_dirs[0]
    best_model = latest_dir / "weights" / "best.pt"
    
    if not best_model.exists():
        print(f"❌ モデルファイルが見つかりません: {best_model}")
        return
    
    print(f"✅ モデル発見: {best_model}")
    
    # モデルをロード
    print("\n📥 モデルをロード中...")
    model = YOLO(str(best_model))
    
    print("✅ モデルロード完了")
    
    # ONNX変換
    print("\n🔄 ONNX変換中...")
    try:
        onnx_path = model.export(format='onnx', imgsz=640, simplify=True)
        print(f"✅ ONNX変換完了: {onnx_path}")
    except Exception as e:
        print(f"⚠️  ONNX変換エラー: {e}")
        print("\n代替案: PyTorchモデルを直接使用")
        onnx_path = best_model
    
    # assetsフォルダにコピー
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)
    
    if str(onnx_path).endswith('.onnx'):
        dest = os.path.join(assets_dir, "clubhead_yolov8.onnx")
    else:
        dest = os.path.join(assets_dir, "clubhead_yolov8.pt")
    
    shutil.copy(onnx_path, dest)
    
    size_mb = os.path.getsize(dest) / (1024 * 1024)
    
    print(f"\n📱 アプリに配置: {dest}")
    print(f"   サイズ: {size_mb:.2f} MB")
    
    print("\n" + "=" * 50)
    print("🎉 完了！")
    print("=" * 50)
    print("\n📋 次のステップ:")
    print("   .\\gradlew installDebug")

if __name__ == "__main__":
    export_model()
