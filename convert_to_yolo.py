#!/usr/bin/env python3
"""
labelmeのJSON形式をYOLO形式に変換
"""

import json
import os
from pathlib import Path

def convert_labelme_to_yolo(json_file, output_dir):
    """
    labelmeのJSONをYOLO形式に変換
    """
    with open(json_file, 'r') as f:
        data = json.load(f)
    
    img_width = data['imageWidth']
    img_height = data['imageHeight']
    
    # 出力ファイル名
    txt_file = Path(output_dir) / (Path(json_file).stem + '.txt')
    
    with open(txt_file, 'w') as f:
        for shape in data['shapes']:
            if shape['shape_type'] == 'rectangle':
                points = shape['points']
                x1, y1 = points[0]
                x2, y2 = points[1]
                
                # YOLO形式に変換（中心座標、幅、高さ）
                x_center = ((x1 + x2) / 2) / img_width
                y_center = ((y1 + y2) / 2) / img_height
                width = abs(x2 - x1) / img_width
                height = abs(y2 - y1) / img_height
                
                # クラスID 0（clubhead）
                f.write(f"0 {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}\n")

def main():
    print("🔄 labelme JSON → YOLO形式 変換")
    print("=" * 50)
    
    images_dir = "dataset/images"
    labels_dir = "dataset/labels"
    
    os.makedirs(labels_dir, exist_ok=True)
    
    # JSONファイルを検索
    json_files = list(Path(images_dir).glob("*.json"))
    
    if not json_files:
        print("❌ JSONファイルが見つかりません")
        print(f"   場所: {images_dir}")
        return
    
    print(f"✅ JSONファイル: {len(json_files)}個")
    print()
    
    # 変換
    for json_file in json_files:
        convert_labelme_to_yolo(json_file, labels_dir)
        print(f"✅ {json_file.name}")
    
    print()
    print("=" * 50)
    print("🎉 変換完了！")
    print("=" * 50)
    print(f"\n📁 ラベルファイル: {labels_dir}/")
    print(f"   ファイル数: {len(json_files)}")
    print()
    print("📋 次のステップ:")
    print("   python train_yolo.py")

if __name__ == "__main__":
    main()
