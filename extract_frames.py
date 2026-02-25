#!/usr/bin/env python3
"""
動画から画像を抽出するスクリプト
"""

import os
import subprocess
from pathlib import Path

def extract_frames(video_path, output_dir, fps=5):
    """
    動画から画像を抽出
    """
    video_name = Path(video_path).stem
    output_pattern = os.path.join(output_dir, f"{video_name}_%04d.jpg")
    
    cmd = [
        'ffmpeg',
        '-i', video_path,
        '-vf', f'fps={fps}',
        output_pattern,
        '-loglevel', 'error'
    ]
    
    print(f"処理中: {Path(video_path).name}")
    subprocess.run(cmd)
    print(f"✅ 完了")

def main():
    print("🎬 動画から画像を抽出")
    print("=" * 50)
    print()
    
    # ディレクトリ確認
    videos_dir = "videos"
    output_dir = "dataset/images"
    
    if not os.path.exists(videos_dir):
        print(f"❌ {videos_dir} フォルダが見つかりません")
        return
    
    os.makedirs(output_dir, exist_ok=True)
    
    # 動画ファイル取得
    video_files = list(Path(videos_dir).glob("*.mp4"))
    
    if not video_files:
        print(f"❌ {videos_dir} フォルダに動画がありません")
        print()
        print("以下の手順で動画を準備してください：")
        print("1. スマホでスイング動画を撮影")
        print("2. PCに転送")
        print(f"3. {videos_dir}\\ フォルダに配置")
        return
    
    print(f"✅ 動画ファイル: {len(video_files)} 個")
    print()
    
    # 各動画から画像を抽出
    print("📸 画像抽出中...")
    print()
    
    for video_file in video_files:
        extract_frames(str(video_file), output_dir, fps=5)
        print()
    
    # 抽出された画像数をカウント
    image_files = list(Path(output_dir).glob("*.jpg"))
    
    print("=" * 50)
    print("🎉 画像抽出完了！")
    print("=" * 50)
    print()
    print(f"📊 結果:")
    print(f"   動画数: {len(video_files)}")
    print(f"   画像数: {len(image_files)}")
    print()
    print(f"📁 画像保存先: {output_dir}\\")
    print()
    print("📋 次のステップ:")
    print("   1. pip install labelImg")
    print("   2. labelImg dataset\\images dataset\\labels")
    print("   3. 各画像でクラブヘッドを囲む")
    print()

if __name__ == "__main__":
    main()
