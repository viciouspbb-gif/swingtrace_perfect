#!/usr/bin/env python3
"""
YOLOv8汎用モデルをダウンロードしてONNX変換（修正版）
"""

from ultralytics import YOLO
import torch

# PyTorch 2.6対応
torch.serialization.add_safe_globals([
    'ultralytics.nn.tasks.DetectionModel'
])

def download_and_convert():
    print("🏌️ YOLOv8汎用モデルをダウンロード")
    print("=" * 50)
    
    try:
        # YOLOv8nモデルをダウンロード
        print("\n📥 モデルダウンロード中...")
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
        
        # ファイルサイズ確認
        size_mb = os.path.getsize(dest_path) / (1024 * 1024)
        print(f"   ファイルサイズ: {size_mb:.2f} MB")
        
        print("\n" + "=" * 50)
        print("🎉 完了！")
        print("=" * 50)
        print("\n⚠️  注意:")
        print("   このモデルはクラブヘッド専用ではありません。")
        print("   汎用物体検出モデルなので、精度は低いです。")
        print("   動作確認用として使用してください。")
        print("\n📋 次のステップ:")
        print("   1. ./gradlew installDebug")
        print("   2. アプリで「クラブヘッド軌道追跡」をテスト")
        print("   3. 動作確認後、専用モデルに置き換え")
        
    except Exception as e:
        print(f"\n❌ エラー: {e}")
        print("\n代替案: 手動ダウンロード")
        print("   1. https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.pt")
        print("   2. ダウンロードしたファイルをプロジェクトルートに配置")
        print("   3. 再実行")

if __name__ == "__main__":
    download_and_convert()
