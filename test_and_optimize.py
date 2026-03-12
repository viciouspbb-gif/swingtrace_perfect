#!/usr/bin/env python3
"""
YOLOv8モデルの精度確認・軽量化・最適化スクリプト
"""

import os
from pathlib import Path
from ultralytics import YOLO
import cv2
import time

def test_inference(model_path, test_images_dir):
    """
    推論テスト - 精度とスピード確認
    """
    print("\n" + "="*50)
    print("📊 推論テスト開始")
    print("="*50)
    
    # モデルロード
    model = YOLO(model_path)
    print(f"✅ モデルロード: {model_path}")
    
    # テスト画像取得
    test_images = list(Path(test_images_dir).glob("*.jpg"))
    if not test_images:
        print(f"❌ テスト画像が見つかりません: {test_images_dir}")
        return
    
    print(f"📸 テスト画像数: {len(test_images)}")
    
    # 推論実行
    total_time = 0
    detections = 0
    
    for img_path in test_images[:10]:  # 最初の10枚でテスト
        img = cv2.imread(str(img_path))
        
        # 推論時間計測
        start = time.time()
        results = model(img, verbose=False)
        inference_time = (time.time() - start) * 1000  # ms
        
        total_time += inference_time
        
        # 検出数カウント
        for result in results:
            detections += len(result.boxes)
        
        print(f"  {img_path.name}: {inference_time:.1f}ms, 検出数: {len(results[0].boxes)}")
    
    # 統計表示
    avg_time = total_time / min(10, len(test_images))
    fps = 1000 / avg_time if avg_time > 0 else 0
    
    print("\n📊 推論統計:")
    print(f"  平均推論時間: {avg_time:.1f}ms")
    print(f"  FPS: {fps:.1f}")
    print(f"  総検出数: {detections}")
    print(f"  平均検出数: {detections / min(10, len(test_images)):.1f}")
    
    return avg_time, fps


def quantize_model(model_path, output_path):
    """
    モデル軽量化 - INT8量子化
    """
    print("\n" + "="*50)
    print("⚡ モデル軽量化（量子化）")
    print("="*50)
    
    model = YOLO(model_path)
    
    # INT8量子化でエクスポート
    print("🔄 INT8量子化ONNX変換中...")
    model.export(
        format='onnx',
        int8=True,
        imgsz=640,
        simplify=True
    )
    
    # ファイルサイズ比較
    original_size = os.path.getsize(model_path) / (1024 * 1024)  # MB
    quantized_path = str(Path(model_path).with_suffix('.onnx'))
    quantized_size = os.path.getsize(quantized_path) / (1024 * 1024)  # MB
    
    print(f"\n📦 ファイルサイズ比較:")
    print(f"  元のモデル: {original_size:.2f} MB")
    print(f"  量子化後: {quantized_size:.2f} MB")
    print(f"  削減率: {(1 - quantized_size/original_size)*100:.1f}%")
    
    return quantized_path


def export_optimized_models(model_path):
    """
    複数形式でエクスポート
    """
    print("\n" + "="*50)
    print("📦 最適化モデルエクスポート")
    print("="*50)
    
    model = YOLO(model_path)
    
    exports = {
        'ONNX (FP32)': {'format': 'onnx', 'int8': False},
        'ONNX (INT8)': {'format': 'onnx', 'int8': True},
        'TensorFlow Lite': {'format': 'tflite'},
        'TensorFlow Lite (INT8)': {'format': 'tflite', 'int8': True},
    }
    
    results = {}
    
    for name, kwargs in exports.items():
        try:
            print(f"\n🔄 {name} 変換中...")
            export_path = model.export(**kwargs)
            size = os.path.getsize(export_path) / (1024 * 1024)  # MB
            results[name] = {'path': export_path, 'size': size}
            print(f"✅ {name}: {size:.2f} MB")
        except Exception as e:
            print(f"❌ {name} 変換失敗: {e}")
    
    # 結果サマリー
    print("\n" + "="*50)
    print("📊 エクスポート結果サマリー")
    print("="*50)
    for name, info in results.items():
        print(f"{name:25s}: {info['size']:6.2f} MB - {info['path']}")
    
    return results


def visualize_predictions(model_path, test_images_dir, output_dir='predictions'):
    """
    予測結果を可視化
    """
    print("\n" + "="*50)
    print("🎨 予測結果可視化")
    print("="*50)
    
    model = YOLO(model_path)
    os.makedirs(output_dir, exist_ok=True)
    
    test_images = list(Path(test_images_dir).glob("*.jpg"))[:5]  # 最初の5枚
    
    for img_path in test_images:
        # 推論
        results = model(str(img_path))
        
        # 結果を画像に描画
        annotated = results[0].plot()
        
        # 保存
        output_path = Path(output_dir) / f"pred_{img_path.name}"
        cv2.imwrite(str(output_path), annotated)
        print(f"✅ 保存: {output_path}")
    
    print(f"\n📁 予測結果: {output_dir}/ に保存されました")


def benchmark_android_performance(onnx_path):
    """
    Android性能予測
    """
    print("\n" + "="*50)
    print("📱 Android性能予測")
    print("="*50)
    
    # ファイルサイズ
    size_mb = os.path.getsize(onnx_path) / (1024 * 1024)
    
    # 推定性能（経験則）
    # Snapdragon 8 Gen 2クラス
    estimated_fps_high = 30 if size_mb < 10 else 20
    # Snapdragon 7 Gen 1クラス
    estimated_fps_mid = 20 if size_mb < 10 else 15
    # Snapdragon 6 Gen 1クラス
    estimated_fps_low = 15 if size_mb < 10 else 10
    
    print(f"📦 モデルサイズ: {size_mb:.2f} MB")
    print(f"\n📊 推定FPS（ONNX Runtime使用）:")
    print(f"  ハイエンド端末: {estimated_fps_high} FPS")
    print(f"  ミドルレンジ: {estimated_fps_mid} FPS")
    print(f"  エントリー: {estimated_fps_low} FPS")
    
    print(f"\n💡 推奨:")
    if size_mb < 5:
        print("  ✅ 軽量で高速。ほとんどの端末で快適に動作します。")
    elif size_mb < 10:
        print("  ⚠️  中程度のサイズ。ミドルレンジ以上推奨。")
    else:
        print("  ❌ 大きめのモデル。ハイエンド端末推奨。量子化を検討してください。")


def main():
    """
    メイン処理
    """
    print("🏌️ YOLOv8モデル テスト・最適化ツール")
    print("="*50)
    
    # パス設定
    model_path = "runs/detect/train/weights/best.pt"
    test_images_dir = "dataset/images"
    
    # 1. 推論テスト
    if os.path.exists(model_path):
        avg_time, fps = test_inference(model_path, test_images_dir)
        
        # 2. 予測結果可視化
        visualize_predictions(model_path, test_images_dir)
        
        # 3. 最適化モデルエクスポート
        export_results = export_optimized_models(model_path)
        
        # 4. Android性能予測
        if 'ONNX (INT8)' in export_results:
            onnx_path = export_results['ONNX (INT8)']['path']
            benchmark_android_performance(onnx_path)
        
        # 5. 最終推奨
        print("\n" + "="*50)
        print("🎯 最終推奨")
        print("="*50)
        
        if fps >= 25:
            print("✅ 性能良好！そのままアプリに統合できます。")
            recommended = export_results.get('ONNX (INT8)', export_results.get('ONNX (FP32)'))
        elif fps >= 15:
            print("⚠️  性能中程度。INT8量子化を推奨します。")
            recommended = export_results.get('ONNX (INT8)')
        else:
            print("❌ 性能低め。より軽量なモデル（yolov8n）を検討してください。")
            recommended = export_results.get('ONNX (INT8)')
        
        if recommended:
            print(f"\n📱 アプリ統合用モデル:")
            print(f"  ファイル: {recommended['path']}")
            print(f"  サイズ: {recommended['size']:.2f} MB")
            print(f"\n📋 次のステップ:")
            print(f"  1. cp {recommended['path']} app/src/main/assets/clubhead_yolov8.onnx")
            print(f"  2. ./gradlew installDebug")
            print(f"  3. アプリでテスト")
    else:
        print(f"❌ モデルが見つかりません: {model_path}")
        print("   先にトレーニングを実行してください: setup_and_train.bat")


if __name__ == "__main__":
    main()
