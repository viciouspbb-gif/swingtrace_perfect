#!/usr/bin/env python3
"""
YOLOv8汎用モデルをダウンロードしてONNX変換
"""

from ultralytics import YOLO

def download_and_convert():
    print("🏌️ YOLOv8汎用モデルをダウンロード")
    print("=" * 50)
    
    # YOLOv8nモデルをダウンロード（自動）
    model = YOLO('yolov8n.pt')
    
    print("\n✅ モデルダウンロード完了")
    
    # ONNX変換
    print("\n🔄 ONNX変換中...")
    onnx_path = model.export(format='onnx', int8=True, imgsz=640)
    
    print(f"\n✅ ONNX変換完了: {onnx_path}")
    
    # assetsフォルダにコピー
    import shutil
    import os
    
    assets_dir = "app/src/main/assets"
    os.makedirs(assets_dir, exist_ok=True)
    
    dest_path = os.path.join(assets_dir, "clubhead_yolov8.onnx")
    shutil.copy(onnx_path, dest_path)
    
    print(f"\n📱 アプリに配置: {dest_path}")
    print("\n⚠️  注意:")
    print("   このモデルはクラブヘッド専用ではありません。")
    print("   精度は低いですが、動作確認には使えます。")
    print("\n📋 次のステップ:")
    print("   1. ./gradlew installDebug")
    print("   2. アプリで動作確認")
    print("   3. 後で専用モデルに置き換え")

if __name__ == "__main__":
    download_and_convert()
