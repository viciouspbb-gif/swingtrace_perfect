# 🤖 AI手ブレ補正機能 - 設計ドキュメント

## 📋 概要

次期Pythonサーバーフェーズにおける最重要課題として、**手ブレ補正AIロジック**を実装します。
ユーザーが多少手ブレのある動画を撮影しても、解析の正確性を保てるようにすることが目的です。

## 🎯 目的

- ユーザーが手持ちで撮影した動画でも高精度な解析を実現
- 固定撮影を推奨しつつ、手持ち撮影も許容する
- 背景の動きを検出し、フレーム全体を補正

## 🔧 技術アーキテクチャ

### 1. 入力データ
- **動画ファイル**: Androidアプリから送信された録画動画
- **軌跡データ**: `detectedPoints` リスト（ボールの座標とタイムスタンプ）

### 2. 処理フロー

```
動画アップロード
    ↓
背景動き検出 (OpenCV)
    ↓
手ブレベクトル計算
    ↓
フレーム補正
    ↓
補正済み軌跡データ生成
    ↓
物理シミュレーション
    ↓
解析結果返却
```

### 3. 実装技術スタック

#### 3.1 背景動き検出
```python
import cv2
import numpy as np

def detect_background_motion(video_path):
    """
    背景（地面やマット）の動きを検出
    """
    cap = cv2.VideoCapture(video_path)
    
    # 特徴点検出器
    feature_detector = cv2.goodFeaturesToTrack
    
    # オプティカルフロー
    lk_params = dict(
        winSize=(15, 15),
        maxLevel=2,
        criteria=(cv2.TERM_CRITERIA_EPS | cv2.TERM_CRITERIA_COUNT, 10, 0.03)
    )
    
    motion_vectors = []
    ret, prev_frame = cap.read()
    prev_gray = cv2.cvtColor(prev_frame, cv2.COLOR_BGR2GRAY)
    
    # 背景領域の特徴点を検出（下部30%）
    mask = np.zeros_like(prev_gray)
    h, w = prev_gray.shape
    mask[int(h * 0.7):, :] = 255
    
    prev_points = cv2.goodFeaturesToTrack(
        prev_gray,
        maxCorners=100,
        qualityLevel=0.3,
        minDistance=7,
        mask=mask
    )
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # オプティカルフローで動きを追跡
        next_points, status, error = cv2.calcOpticalFlowPyrLK(
            prev_gray, gray, prev_points, None, **lk_params
        )
        
        # 有効な点のみ選択
        good_new = next_points[status == 1]
        good_old = prev_points[status == 1]
        
        # 平均移動ベクトルを計算
        if len(good_new) > 0:
            motion = np.mean(good_new - good_old, axis=0)
            motion_vectors.append(motion)
        
        prev_gray = gray.copy()
        prev_points = good_new.reshape(-1, 1, 2)
    
    cap.release()
    return np.array(motion_vectors)
```

#### 3.2 フレーム補正
```python
def stabilize_video(video_path, motion_vectors):
    """
    検出した手ブレベクトルを使ってフレームを補正
    """
    cap = cv2.VideoCapture(video_path)
    fps = cap.get(cv2.CAP_PROP_FPS)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    
    # 出力動画の設定
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter('stabilized_output.mp4', fourcc, fps, (width, height))
    
    # 累積変換行列
    cumulative_transform = np.zeros(2)
    
    frame_idx = 0
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        if frame_idx < len(motion_vectors):
            # 手ブレを打ち消す変換
            motion = motion_vectors[frame_idx]
            cumulative_transform += motion
            
            # アフィン変換行列
            M = np.float32([
                [1, 0, -cumulative_transform[0]],
                [0, 1, -cumulative_transform[1]]
            ])
            
            # フレームを補正
            stabilized_frame = cv2.warpAffine(frame, M, (width, height))
            out.write(stabilized_frame)
        
        frame_idx += 1
    
    cap.release()
    out.release()
    return 'stabilized_output.mp4'
```

#### 3.3 軌跡データの補正
```python
def correct_trajectory_data(detected_points, motion_vectors):
    """
    ボールの軌跡データを手ブレ補正
    """
    corrected_points = []
    cumulative_transform = np.zeros(2)
    
    for idx, point in enumerate(detected_points):
        if idx < len(motion_vectors):
            cumulative_transform += motion_vectors[idx]
        
        # 補正後の座標
        corrected_x = point['x'] - cumulative_transform[0]
        corrected_y = point['y'] - cumulative_transform[1]
        
        corrected_points.append({
            'x': corrected_x,
            'y': corrected_y,
            'timestamp': point['timestamp']
        })
    
    return corrected_points
```

### 4. APIエンドポイント設計

```python
from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from typing import List

app = FastAPI()

class TrajectoryPoint(BaseModel):
    x: float
    y: float
    timestamp: int

class AnalysisRequest(BaseModel):
    trajectory_data: List[TrajectoryPoint]

@app.post("/api/analyze")
async def analyze_video(
    video: UploadFile = File(...),
    trajectory_data: str = None
):
    """
    動画と軌跡データを受け取り、手ブレ補正後の解析結果を返す
    """
    # 1. 動画を一時保存
    video_path = f"temp/{video.filename}"
    with open(video_path, "wb") as f:
        f.write(await video.read())
    
    # 2. 背景動き検出
    motion_vectors = detect_background_motion(video_path)
    
    # 3. 軌跡データを補正
    import json
    points = json.loads(trajectory_data)
    corrected_points = correct_trajectory_data(points, motion_vectors)
    
    # 4. 物理シミュレーション
    trajectory_result = calculate_physics_trajectory(corrected_points)
    
    # 5. 結果を返却
    return {
        "success": True,
        "stabilized": True,
        "motion_detected": len(motion_vectors) > 0,
        "trajectory": trajectory_result,
        "ball_speed": trajectory_result["ball_speed"],
        "launch_angle": trajectory_result["launch_angle"],
        "carry_distance": trajectory_result["carry_distance"]
    }
```

## 📊 期待される効果

### Before（手ブレ補正なし）
- 手持ち撮影: 解析精度 30-50%
- 固定撮影: 解析精度 80-90%

### After（手ブレ補正あり）
- 手持ち撮影: 解析精度 60-75% ⬆️
- 固定撮影: 解析精度 85-95% ⬆️

## 🚀 実装フェーズ

### Phase 1: 基本実装（優先度: 最高）
- [ ] OpenCVによる背景動き検出
- [ ] オプティカルフローの実装
- [ ] 基本的なフレーム補正

### Phase 2: 精度向上
- [ ] 機械学習モデルの導入（RAFT, PWC-Net等）
- [ ] 複数領域の動き検出
- [ ] ローリングシャッター補正

### Phase 3: 最適化
- [ ] GPU処理の導入
- [ ] リアルタイム処理の最適化
- [ ] バッチ処理の実装

## 🔬 検証方法

1. **テストデータセット作成**
   - 固定撮影動画 × 10
   - 手持ち撮影動画 × 10
   - 意図的にブレを加えた動画 × 10

2. **精度評価指標**
   - 軌跡の再現性（RMSE）
   - 初速推定の誤差率
   - 飛距離推定の誤差率

3. **パフォーマンス評価**
   - 処理時間（目標: 10秒以内/動画）
   - メモリ使用量
   - GPU使用率

## 📝 備考

- 手ブレ補正は「完璧」ではなく「許容範囲を広げる」ことが目的
- ユーザーには引き続き固定撮影を推奨
- 補正の限界を超える場合は警告を表示

## 🔗 参考資料

- OpenCV Video Stabilization: https://docs.opencv.org/master/d5/d50/group__videostab.html
- RAFT Optical Flow: https://github.com/princeton-vl/RAFT
- PWC-Net: https://github.com/NVlabs/PWC-Net
