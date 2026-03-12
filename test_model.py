#!/usr/bin/env python3
"""
トレーニング済みモデルのテストスクリプト
"""

import sys
from pathlib import Path
from ultralytics import YOLO
import cv2

def test_single_image(model_path, image_path, conf=0.5):
    """単一画像でテスト"""
    print(f"🖼️ 画像テスト: {image_path}")
    print(f"📊 信頼度閾値: {conf}")
    
    # モデルロード
    model = YOLO(model_path)
    
    # 推論
    results = model.predict(
        source=image_path,
        conf=conf,
        save=True,
        show_labels=True,
        show_conf=True
    )
    
    # 結果表示
    for r in results:
        boxes = r.boxes
        print(f"\n検出数: {len(boxes)}")
        
        for i, box in enumerate(boxes):
            conf_score = box.conf[0].item()
            cls = box.cls[0].item()
            print(f"  [{i+1}] 信頼度: {conf_score:.3f} (クラス: {int(cls)})")
            
            if conf_score >= 0.5:
                print(f"       ✅ 目標達成！（50%以上）")
            else:
                print(f"       ⚠️ 目標未達成（50%未満）")
    
    print(f"\n💾 結果保存先: runs/detect/predict/")


def test_video(model_path, video_path, conf=0.5):
    """動画でテスト"""
    print(f"🎥 動画テスト: {video_path}")
    print(f"📊 信頼度閾値: {conf}")
    
    # モデルロード
    model = YOLO(model_path)
    
    # 推論
    results = model.predict(
        source=video_path,
        conf=conf,
        save=True,
        stream=True
    )
    
    # フレームごとの検出数をカウント
    total_frames = 0
    detected_frames = 0
    high_conf_frames = 0
    
    for r in results:
        total_frames += 1
        boxes = r.boxes
        
        if len(boxes) > 0:
            detected_frames += 1
            
            # 信頼度50%以上のボックスがあるか
            for box in boxes:
                if box.conf[0].item() >= 0.5:
                    high_conf_frames += 1
                    break
    
    # 統計表示
    print(f"\n📊 統計:")
    print(f"  総フレーム数: {total_frames}")
    print(f"  検出フレーム数: {detected_frames} ({detected_frames/total_frames*100:.1f}%)")
    print(f"  高信頼度フレーム数: {high_conf_frames} ({high_conf_frames/total_frames*100:.1f}%)")
    
    if high_conf_frames / total_frames >= 0.5:
        print(f"\n🎉 良好な検出率！")
    else:
        print(f"\n⚠️ 検出率が低い可能性があります")
    
    print(f"\n💾 結果保存先: runs/detect/predict/")


def test_webcam(model_path, conf=0.5):
    """Webカメラでリアルタイムテスト"""
    print(f"📹 Webカメラテスト")
    print(f"📊 信頼度閾値: {conf}")
    print(f"⌨️ 'q'キーで終了")
    
    # モデルロード
    model = YOLO(model_path)
    
    # Webカメラオープン
    cap = cv2.VideoCapture(0)
    
    if not cap.isOpened():
        print("❌ Webカメラを開けませんでした")
        return
    
    frame_count = 0
    detected_count = 0
    high_conf_count = 0
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        frame_count += 1
        
        # 推論
        results = model.predict(frame, conf=conf, verbose=False)
        
        # 結果を描画
        annotated_frame = results[0].plot()
        
        # 検出情報を表示
        boxes = results[0].boxes
        if len(boxes) > 0:
            detected_count += 1
            
            for box in boxes:
                conf_score = box.conf[0].item()
                if conf_score >= 0.5:
                    high_conf_count += 1
                    break
        
        # 統計情報をフレームに描画
        cv2.putText(annotated_frame, f"Frames: {frame_count}", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.putText(annotated_frame, f"Detected: {detected_count} ({detected_count/frame_count*100:.1f}%)", (10, 60),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.putText(annotated_frame, f"High Conf: {high_conf_count} ({high_conf_count/frame_count*100:.1f}%)", (10, 90),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        
        # 表示
        cv2.imshow('Clubhead Detection Test', annotated_frame)
        
        # 'q'キーで終了
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    
    cap.release()
    cv2.destroyAllWindows()
    
    # 最終統計
    print(f"\n📊 最終統計:")
    print(f"  総フレーム数: {frame_count}")
    print(f"  検出フレーム数: {detected_count} ({detected_count/frame_count*100:.1f}%)")
    print(f"  高信頼度フレーム数: {high_conf_count} ({high_conf_count/frame_count*100:.1f}%)")


def main():
    """メイン処理"""
    print("="*60)
    print("🏌️ クラブヘッド検出モデル - テストツール")
    print("="*60)
    
    # モデルパス
    model_path = "runs/detect/clubhead_high_confidence/weights/best.pt"
    
    if not Path(model_path).exists():
        print(f"\n❌ モデルが見つかりません: {model_path}")
        print("まずトレーニングを実行してください: run_auto_training.bat")
        return
    
    print(f"\n✅ モデル検出: {model_path}")
    
    # テストモード選択
    print("\n📋 テストモードを選択してください:")
    print("  1. 単一画像テスト")
    print("  2. 動画テスト")
    print("  3. Webカメラテスト（リアルタイム）")
    print("  4. すべてのテスト画像で一括テスト")
    
    try:
        choice = input("\n選択 (1-4): ").strip()
    except KeyboardInterrupt:
        print("\n\n中断されました")
        return
    
    if choice == "1":
        # 単一画像テスト
        image_path = input("画像パスを入力: ").strip()
        if Path(image_path).exists():
            test_single_image(model_path, image_path)
        else:
            print(f"❌ 画像が見つかりません: {image_path}")
    
    elif choice == "2":
        # 動画テスト
        video_path = input("動画パスを入力: ").strip()
        if Path(video_path).exists():
            test_video(model_path, video_path)
        else:
            print(f"❌ 動画が見つかりません: {video_path}")
    
    elif choice == "3":
        # Webカメラテスト
        test_webcam(model_path)
    
    elif choice == "4":
        # 一括テスト
        test_dir = Path("dataset/images")
        image_files = list(test_dir.glob("*.jpg"))[:10]  # 最初の10枚
        
        if not image_files:
            print(f"❌ テスト画像が見つかりません: {test_dir}")
            return
        
        print(f"\n🖼️ {len(image_files)}枚の画像でテスト中...")
        
        total_detections = 0
        high_conf_detections = 0
        
        for img_path in image_files:
            model = YOLO(model_path)
            results = model.predict(source=str(img_path), conf=0.5, verbose=False)
            
            for r in results:
                boxes = r.boxes
                total_detections += len(boxes)
                
                for box in boxes:
                    if box.conf[0].item() >= 0.5:
                        high_conf_detections += 1
        
        print(f"\n📊 結果:")
        print(f"  総検出数: {total_detections}")
        print(f"  高信頼度検出数: {high_conf_detections}")
        
        if total_detections > 0:
            print(f"  高信頼度率: {high_conf_detections/total_detections*100:.1f}%")
            
            if high_conf_detections / total_detections >= 0.5:
                print(f"\n🎉 目標達成！50%以上の検出が高信頼度です")
            else:
                print(f"\n⚠️ 目標未達成。さらなるトレーニングが必要かもしれません")
    
    else:
        print("❌ 無効な選択です")
    
    print("\n" + "="*60)
    print("✅ テスト完了")
    print("="*60)


if __name__ == "__main__":
    main()
