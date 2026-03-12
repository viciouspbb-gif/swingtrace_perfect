from ultralytics import YOLO
import shutil
import os

print("🏌️ YOLOv8モデルをダウンロード＆変換")
print("=" * 50)

# YOLOv8nモデルをロード（自動ダウンロード）
print("\n📥 モデルダウンロード中...")
model = YOLO('yolov8n.pt')

print("✅ ダウンロード完了")

# ONNX変換
print("\n🔄 ONNX変換中...")
onnx_file = model.export(format='onnx')

print(f"✅ ONNX変換完了: {onnx_file}")

# assetsフォルダにコピー
assets_dir = "app/src/main/assets"
os.makedirs(assets_dir, exist_ok=True)

dest = os.path.join(assets_dir, "clubhead_yolov8.onnx")
shutil.copy(onnx_file, dest)

size_mb = os.path.getsize(dest) / (1024 * 1024)

print(f"\n📱 アプリに配置完了: {dest}")
print(f"   サイズ: {size_mb:.2f} MB")

print("\n" + "=" * 50)
print("🎉 完了！")
print("=" * 50)
print("\n📋 次のステップ:")
print("   .\\gradlew installDebug")
