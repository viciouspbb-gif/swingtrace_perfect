"""
信頼度スコア確認スクリプト
"""
from ultralytics import YOLO
import glob

print("="*60)
print("📊 信頼度スコア詳細確認")
print("="*60)
print()

# モデル読み込み
model = YOLO('runs/detect/clubhead_high_confidence/weights/best.pt')
print("✅ モデル読み込み完了")
print()

# テスト画像
images = glob.glob('dataset/images/*.jpg')[:20]
print(f"📸 テスト画像数: {len(images)}枚")
print()

# 推論実行
print("🔍 推論実行中...")
results = []
confidences = []

for i, img in enumerate(images):
    result = model(img, verbose=False)[0]
    if len(result.boxes) > 0:
        conf = float(result.boxes.conf[0])
        confidences.append(conf)
        results.append((i+1, img.split('\\')[-1], conf))
    else:
        results.append((i+1, img.split('\\')[-1], 0))

print()
print("="*60)
print("📊 信頼度スコア一覧")
print("="*60)
print()

# UI閾値
ui_threshold_025 = 0.25
ui_threshold_050 = 0.50

for idx, filename, conf in results:
    if conf > 0:
        status_025 = "✅ 採用" if conf >= ui_threshold_025 else "❌ 除外"
        status_050 = "✅ 採用" if conf >= ui_threshold_050 else "❌ 除外"
        print(f"{idx:2d}. {filename[:30]:30s} 信頼度: {conf:.1%}  [{status_025} @0.25] [{status_050} @0.50]")
    else:
        print(f"{idx:2d}. {filename[:30]:30s} 検出なし")

print()
print("="*60)
print("📈 統計情報")
print("="*60)
print()

if confidences:
    avg_conf = sum(confidences) / len(confidences)
    max_conf = max(confidences)
    min_conf = min(confidences)
    
    # UI閾値での採用率
    adopted_025 = sum(1 for c in confidences if c >= ui_threshold_025)
    adopted_050 = sum(1 for c in confidences if c >= ui_threshold_050)
    
    print(f"検出成功: {len(confidences)}/{len(images)}枚 ({len(confidences)/len(images)*100:.1f}%)")
    print()
    print(f"平均信頼度: {avg_conf:.1%}")
    print(f"最大信頼度: {max_conf:.1%}")
    print(f"最小信頼度: {min_conf:.1%}")
    print()
    print(f"UI閾値 0.25 での採用率: {adopted_025}/{len(confidences)}枚 ({adopted_025/len(confidences)*100:.1f}%)")
    print(f"UI閾値 0.50 での採用率: {adopted_050}/{len(confidences)}枚 ({adopted_050/len(confidences)*100:.1f}%)")
    print()
    
    # 評価
    print("="*60)
    print("🎯 評価")
    print("="*60)
    print()
    
    if avg_conf >= 0.50:
        print("⭐⭐⭐⭐⭐ 優秀！平均信頼度50%以上")
    elif avg_conf >= 0.40:
        print("⭐⭐⭐⭐ 良好！平均信頼度40%以上")
    elif avg_conf >= 0.30:
        print("⭐⭐⭐ 合格！平均信頼度30%以上")
    else:
        print("⭐⭐ 要改善：平均信頼度30%未満")
    
    if adopted_025 / len(confidences) >= 0.70:
        print("✅ UI表示（閾値0.25）: 採用率70%以上 - 安定した表示")
    elif adopted_025 / len(confidences) >= 0.50:
        print("✅ UI表示（閾値0.25）: 採用率50%以上 - 実用的")
    else:
        print("⚠️ UI表示（閾値0.25）: 採用率50%未満 - 改善推奨")
    
    print()

print("="*60)
print("✅ 確認完了")
print("="*60)
