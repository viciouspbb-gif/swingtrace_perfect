"""
YOLOv8 クラブヘッド検出モデルのトレーニングスクリプト
"""

from ultralytics import YOLO

def train_clubhead_detector():
    # YOLOv8nモデルをロード（軽量版）
    model = YOLO('yolov8n.pt')
    
    # トレーニング
    results = model.train(
        data='dataset/clubhead.yaml',
        epochs=50,
        imgsz=640,
        batch=16,
        name='clubhead_detector',
        device='cuda'  # GPU使用（CPUの場合は'cpu'）
    )
    
    # 精度確認
    metrics = model.val()
    print(f"✅ 検証完了")
    print(f"mAP50: {metrics.box.map50}")
    print(f"mAP50-95: {metrics.box.map}")
    
    # ONNXにエクスポート（Android用）
    print("\n📦 ONNXエクスポート中...")
    model.export(
        format='onnx',
        imgsz=640,
        simplify=True,
        opset=12
    )
    
    print("\n✅ トレーニング完了！")
    print(f"モデル保存先: runs/detect/clubhead_detector/weights/best.pt")
    print(f"ONNX保存先: runs/detect/clubhead_detector/weights/best.onnx")
    print(f"\n次のステップ:")
    print(f"1. best.onnxをapp/src/main/assets/にコピー")
    print(f"2. アプリをビルド")
    print(f"3. テスト！")

def test_inference():
    """推論テスト"""
    model = YOLO('runs/detect/clubhead_detector/weights/best.pt')
    
    # テスト動画で推論
    results = model.predict(
        source='test_video.mp4',
        save=True,
        conf=0.6  # 信頼度閾値
    )
    
    print("✅ 推論テスト完了")
    print(f"結果保存先: runs/detect/predict/")

if __name__ == '__main__':
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == 'test':
        test_inference()
    else:
        train_clubhead_detector()
