#!/usr/bin/env python3
"""
クラブヘッド検出モデル - データ拡張と自動トレーニング
目標: 信頼度50%以上の検出精度を達成
"""

import os
import shutil
from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO
import albumentations as A
from tqdm import tqdm

class ClubheadDataAugmenter:
    """データ拡張クラス"""
    
    def __init__(self, dataset_path):
        self.dataset_path = Path(dataset_path)
        self.images_dir = self.dataset_path / "images"
        self.labels_dir = self.dataset_path / "labels"
        self.augmented_dir = self.dataset_path / "augmented"
        
        # 拡張後の保存先
        self.aug_images_dir = self.augmented_dir / "images"
        self.aug_labels_dir = self.augmented_dir / "labels"
        
    def setup_directories(self):
        """ディレクトリ構造を作成"""
        self.aug_images_dir.mkdir(parents=True, exist_ok=True)
        self.aug_labels_dir.mkdir(parents=True, exist_ok=True)
        print(f"✅ ディレクトリ作成完了: {self.augmented_dir}")
        
    def get_augmentation_pipeline(self):
        """データ拡張パイプライン（Roboflow風）"""
        return A.Compose([
            A.OneOf([
                A.HorizontalFlip(p=1.0),  # 左右反転
                A.VerticalFlip(p=0.3),    # 上下反転（稀に）
                A.Rotate(limit=15, p=1.0),  # ±15度回転
            ], p=0.7),
            
            A.OneOf([
                A.MotionBlur(blur_limit=7, p=1.0),  # モーションブレ
                A.GaussianBlur(blur_limit=5, p=1.0),  # ガウシアンブレ
            ], p=0.5),
            
            A.OneOf([
                A.RandomBrightnessContrast(brightness_limit=0.3, contrast_limit=0.3, p=1.0),
                A.CLAHE(clip_limit=4.0, p=1.0),  # 明暗差強調
                A.RandomGamma(gamma_limit=(80, 120), p=1.0),
            ], p=0.6),
            
            A.OneOf([
                A.HueSaturationValue(hue_shift_limit=10, sat_shift_limit=20, val_shift_limit=20, p=1.0),
                A.RGBShift(r_shift_limit=15, g_shift_limit=15, b_shift_limit=15, p=1.0),
            ], p=0.4),
            
        ], bbox_params=A.BboxParams(format='yolo', label_fields=['class_labels']))
    
    def read_yolo_label(self, label_path):
        """YOLOラベルファイルを読み込み"""
        bboxes = []
        class_labels = []
        
        if not label_path.exists():
            return bboxes, class_labels
            
        with open(label_path, 'r') as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) == 5:
                    class_id = int(parts[0])
                    x_center, y_center, width, height = map(float, parts[1:])
                    bboxes.append([x_center, y_center, width, height])
                    class_labels.append(class_id)
        
        return bboxes, class_labels
    
    def write_yolo_label(self, label_path, bboxes, class_labels):
        """YOLOラベルファイルを書き込み"""
        with open(label_path, 'w') as f:
            for bbox, class_id in zip(bboxes, class_labels):
                x_center, y_center, width, height = bbox
                f.write(f"{class_id} {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}\n")
    
    def augment_dataset(self, target_count=200):
        """データセットを拡張"""
        self.setup_directories()
        
        # 既存の画像とラベルを取得
        image_files = sorted(self.images_dir.glob("*.jpg"))
        label_files = sorted(self.labels_dir.glob("*.txt"))
        
        # ラベルがある画像のみを対象
        labeled_images = []
        for img_path in image_files:
            label_path = self.labels_dir / f"{img_path.stem}.txt"
            if label_path.exists():
                labeled_images.append(img_path)
        
        original_count = len(labeled_images)
        print(f"📊 元データ: {original_count}枚（ラベル付き）")
        print(f"🎯 目標: {target_count}枚追加")
        
        # まず元データをコピー
        print("\n📁 元データをコピー中...")
        for img_path in tqdm(labeled_images, desc="コピー"):
            label_path = self.labels_dir / f"{img_path.stem}.txt"
            shutil.copy(img_path, self.aug_images_dir / img_path.name)
            shutil.copy(label_path, self.aug_labels_dir / label_path.name)
        
        # 拡張パイプライン
        transform = self.get_augmentation_pipeline()
        
        # 追加で必要な枚数を計算
        augmentations_per_image = max(1, target_count // original_count)
        
        print(f"\n🔄 データ拡張中（1枚あたり{augmentations_per_image}バリエーション）...")
        
        aug_count = 0
        for img_path in tqdm(labeled_images, desc="拡張"):
            # 画像読み込み
            image = cv2.imread(str(img_path))
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            
            # ラベル読み込み
            label_path = self.labels_dir / f"{img_path.stem}.txt"
            bboxes, class_labels = self.read_yolo_label(label_path)
            
            if not bboxes:
                continue
            
            # 複数バリエーション生成
            for i in range(augmentations_per_image):
                try:
                    # 拡張適用
                    transformed = transform(image=image, bboxes=bboxes, class_labels=class_labels)
                    aug_image = transformed['image']
                    aug_bboxes = transformed['bboxes']
                    aug_class_labels = transformed['class_labels']
                    
                    # 保存
                    aug_filename = f"{img_path.stem}_aug{i+1}"
                    aug_img_path = self.aug_images_dir / f"{aug_filename}.jpg"
                    aug_label_path = self.aug_labels_dir / f"{aug_filename}.txt"
                    
                    # 画像保存
                    aug_image_bgr = cv2.cvtColor(aug_image, cv2.COLOR_RGB2BGR)
                    cv2.imwrite(str(aug_img_path), aug_image_bgr)
                    
                    # ラベル保存
                    self.write_yolo_label(aug_label_path, aug_bboxes, aug_class_labels)
                    
                    aug_count += 1
                    
                    if aug_count >= target_count:
                        break
                        
                except Exception as e:
                    print(f"⚠️ エラー: {img_path.name} - {e}")
                    continue
            
            if aug_count >= target_count:
                break
        
        total_count = original_count + aug_count
        print(f"\n✅ データ拡張完了！")
        print(f"   元データ: {original_count}枚")
        print(f"   拡張データ: {aug_count}枚")
        print(f"   合計: {total_count}枚")
        
        return total_count


class ClubheadModelTrainer:
    """モデルトレーニングクラス"""
    
    def __init__(self, dataset_path, augmented_path):
        self.dataset_path = Path(dataset_path)
        self.augmented_path = Path(augmented_path)
        
    def create_training_yaml(self):
        """トレーニング用YAMLファイルを作成"""
        yaml_content = f"""# クラブヘッド検出 - 拡張データセット
path: {self.augmented_path.absolute()}
train: images
val: images

nc: 1
names: ['clubhead']

# データ拡張設定（トレーニング時）
augment: true
hsv_h: 0.015
hsv_s: 0.7
hsv_v: 0.4
degrees: 10.0
translate: 0.1
scale: 0.5
fliplr: 0.5
mosaic: 1.0
"""
        
        yaml_path = self.augmented_path / "data.yaml"
        with open(yaml_path, 'w', encoding='utf-8') as f:
            f.write(yaml_content)
        
        print(f"✅ トレーニング設定ファイル作成: {yaml_path}")
        return yaml_path
    
    def train_model(self, epochs=100, batch=16, imgsz=640):
        """モデルをトレーニング"""
        print("\n" + "="*60)
        print("🏌️ YOLOv8 クラブヘッド検出モデル トレーニング開始")
        print("="*60)
        
        # YAML作成
        yaml_path = self.create_training_yaml()
        
        # モデルロード
        model = YOLO('yolov8n.pt')
        
        # トレーニング設定
        print(f"\n⚙️ トレーニング設定:")
        print(f"   エポック数: {epochs}")
        print(f"   バッチサイズ: {batch}")
        print(f"   画像サイズ: {imgsz}")
        print(f"   目標信頼度: 50%以上")
        
        # トレーニング実行
        results = model.train(
            data=str(yaml_path),
            epochs=epochs,
            imgsz=imgsz,
            batch=batch,
            patience=20,  # 早期終了
            save=True,
            plots=True,
            name='clubhead_high_confidence',
            conf=0.5,  # 信頼度閾値
            iou=0.5,
            verbose=True
        )
        
        print("\n✅ トレーニング完了！")
        print(f"📁 モデル保存先: runs/detect/clubhead_high_confidence/weights/best.pt")
        
        return model
    
    def evaluate_model(self, model):
        """モデルを評価"""
        print("\n" + "="*60)
        print("📊 モデル評価")
        print("="*60)
        
        yaml_path = self.augmented_path / "data.yaml"
        metrics = model.val(data=str(yaml_path))
        
        print(f"\n📈 評価結果:")
        print(f"   mAP50: {metrics.box.map50:.3f}")
        print(f"   mAP50-95: {metrics.box.map:.3f}")
        print(f"   Precision: {metrics.box.mp:.3f}")
        print(f"   Recall: {metrics.box.mr:.3f}")
        
        # 信頼度チェック
        if metrics.box.map50 >= 0.5:
            print("\n🎉 目標達成！信頼度50%以上")
        else:
            print(f"\n⚠️ 目標未達成（現在: {metrics.box.map50*100:.1f}%）")
            print("   推奨: データをさらに追加するか、エポック数を増やす")
        
        return metrics
    
    def export_to_onnx(self, model):
        """ONNXフォーマットにエクスポート"""
        print("\n" + "="*60)
        print("🔄 ONNX変換")
        print("="*60)
        
        model_path = Path('runs/detect/clubhead_high_confidence/weights/best.pt')
        
        if not model_path.exists():
            print("❌ モデルファイルが見つかりません")
            return None
        
        # ONNX変換
        export_path = YOLO(str(model_path)).export(format='onnx', int8=False)
        
        print(f"✅ ONNX変換完了: {export_path}")
        
        # Androidアプリ用にコピー
        app_assets = Path('app/src/main/assets')
        if app_assets.exists():
            dest_path = app_assets / 'clubhead_yolov8.onnx'
            shutil.copy(export_path, dest_path)
            print(f"✅ アプリに配置: {dest_path}")
        
        return export_path


def main():
    """メイン処理"""
    print("="*60)
    print("🏌️ クラブヘッド検出 - 自動データ拡張＆トレーニング")
    print("="*60)
    print("\n目標:")
    print("  ✅ データ拡張: 200枚追加")
    print("  ✅ トレーニング: 100エポック")
    print("  ✅ 信頼度: 50%以上")
    print("\n" + "="*60 + "\n")
    
    # パス設定
    dataset_path = Path("dataset")
    augmented_path = dataset_path / "augmented"
    
    # ステップ1: データ拡張
    print("【ステップ1】データ拡張")
    augmenter = ClubheadDataAugmenter(dataset_path)
    total_images = augmenter.augment_dataset(target_count=200)
    
    # ステップ2: モデルトレーニング
    print("\n【ステップ2】モデルトレーニング")
    trainer = ClubheadModelTrainer(dataset_path, augmented_path)
    model = trainer.train_model(epochs=100, batch=16, imgsz=640)
    
    # ステップ3: モデル評価
    print("\n【ステップ3】モデル評価")
    metrics = trainer.evaluate_model(model)
    
    # ステップ4: ONNX変換
    print("\n【ステップ4】ONNX変換")
    onnx_path = trainer.export_to_onnx(model)
    
    # 完了メッセージ
    print("\n" + "="*60)
    print("🎉 すべての処理が完了しました！")
    print("="*60)
    print("\n📋 次のステップ:")
    print("  1. モデルの精度を確認")
    print("  2. テスト画像で推論テスト")
    print("  3. Androidアプリでリアルタイムテスト")
    print("\n💡 推論テストコマンド:")
    print("  yolo predict model=runs/detect/clubhead_high_confidence/weights/best.pt source=test.jpg conf=0.5")
    print("\n" + "="*60)


if __name__ == "__main__":
    # 必要なライブラリのインストール確認
    try:
        import albumentations
    except ImportError:
        print("⚠️ albumentationsがインストールされていません")
        print("インストールコマンド: pip install albumentations")
        exit(1)
    
    main()
