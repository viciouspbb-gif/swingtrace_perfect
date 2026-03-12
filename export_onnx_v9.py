#!/usr/bin/env python3
"""
ONNX IR version 9でエクスポート（Android互換）
"""

from ultralytics import YOLO
import shutil
import os

def export_model():
    print("🔄 ONNX IR version 9でエクスポート")
    print("=" * 50)
    
    # 学習済みモデルをロード
    model_path = "runs/detect/clubhead_detection/weights/best.pt"
    
    if not os.path.exists(model_path):
        print(f"❌ モデルファイルが見つかりません: {model_path}")
        return
    
    print(f"✅ モデル発見: {model_path}")
    
    # モデルをロード
    print("\n📥 モデルをロード中...")
    model = YOLO(model_path)
    
    print("✅ モデルロード完了")
    
    # ONNX変換（opset 12でIR version 9）
    print("\n🔄 ONNX変換中（IR version 9）...")
    try:
        onnx_path = model.export(
            format='onnx',
            imgsz=640,
            simplify=True,
            opset=12  # IR version 9に対応
        )
        print(f"✅ ONNX変換完了: {onnx_path}")
    except Exception as e:
        print(f"❌ ONNX変換エラー: {e}")
        return
    
    # assetsフォルダにコピー
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)
    
    dest = os.path.join(assets_dir, "clubhead_yolov8.onnx")
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
