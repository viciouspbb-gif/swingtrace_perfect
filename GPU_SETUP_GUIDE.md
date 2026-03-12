# 🚀 GPU環境セットアップガイド（RTX 4070版）

## 📋 目次
1. [システム要件](#システム要件)
2. [セットアップ手順](#セットアップ手順)
3. [GPU確認](#gpu確認)
4. [トレーニング最適化](#トレーニング最適化)
5. [トラブルシューティング](#トラブルシューティング)

---

## 💻 システム要件

### ハードウェア
- ✅ GPU: NVIDIA GeForce RTX 4070 (12GB VRAM)
- ✅ CPU: Intel Core i7-14700F (20コア/28スレッド)
- ✅ RAM: 64GB DDR5-4800
- ✅ ストレージ: 2TB NVMe SSD

### ソフトウェア
- Windows 11 Home 64ビット
- Python 3.10以上
- CUDA 12.1
- cuDNN 8.9

---

## 🔧 セットアップ手順

### ステップ1: NVIDIA ドライバーインストール（10分）

#### 1-1. GeForce Experienceをダウンロード
```
URL: https://www.nvidia.com/ja-jp/geforce/geforce-experience/
```

1. 「今すぐダウンロード」をクリック
2. `GeForce_Experience_vX.X.X.X.exe` をダウンロード
3. インストーラーを実行
4. 「エクスプレスインストール」を選択
5. 再起動

#### 1-2. 最新ドライバーをインストール
1. GeForce Experienceを起動
2. 「ドライバー」タブをクリック
3. 「ダウンロード」をクリック
4. インストール完了後、再起動

#### 1-3. ドライバー確認
```batch
# コマンドプロンプトで実行
nvidia-smi
```

**期待される出力**:
```
+-----------------------------------------------------------------------------+
| NVIDIA-SMI 545.84       Driver Version: 545.84       CUDA Version: 12.3     |
|-------------------------------+----------------------+----------------------+
| GPU  Name            TCC/WDDM | Bus-Id        Disp.A | Volatile Uncorr. ECC |
|   0  NVIDIA GeForce RTX 4070  | 00000000:01:00.0  On |                  N/A |
| 30%   45C    P8    15W / 200W |    500MiB / 12288MiB |      0%      Default |
+-------------------------------+----------------------+----------------------+
```

---

### ステップ2: CUDA Toolkit インストール（15分）

#### 2-1. CUDA 12.1をダウンロード
```
URL: https://developer.nvidia.com/cuda-12-1-0-download-archive
```

**選択項目**:
- Operating System: Windows
- Architecture: x86_64
- Version: 11
- Installer Type: exe (local)

**ファイルサイズ**: 約3.0GB

#### 2-2. インストール
1. `cuda_12.1.0_531.14_windows.exe` を実行
2. 「カスタム（詳細）」を選択
3. すべてのコンポーネントを選択
4. インストール先: デフォルト（C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1）
5. インストール完了（約10分）

#### 2-3. 環境変数の確認
```batch
# コマンドプロンプトで確認
echo %CUDA_PATH%
echo %CUDA_PATH_V12_1%
```

**期待される出力**:
```
C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1
C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1
```

---

### ステップ3: cuDNN インストール（10分）

#### 3-1. cuDNN 8.9をダウンロード
```
URL: https://developer.nvidia.com/cudnn
```

1. NVIDIAアカウントでログイン（無料）
2. 「Download cuDNN」をクリック
3. 「cuDNN v8.9.7 for CUDA 12.x」を選択
4. 「Local Installer for Windows (Zip)」をダウンロード

#### 3-2. インストール
1. ZIPファイルを展開
2. 以下のファイルをコピー:

```batch
# cudnn-windows-x86_64-8.9.7.29_cuda12-archive\bin\*.dll
→ C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1\bin\

# cudnn-windows-x86_64-8.9.7.29_cuda12-archive\include\*.h
→ C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1\include\

# cudnn-windows-x86_64-8.9.7.29_cuda12-archive\lib\x64\*.lib
→ C:\Program Files\NVIDIA GPU Computing Toolkit\CUDA\v12.1\lib\x64\
```

---

### ステップ4: PyTorch GPU版インストール（10分）

#### 4-1. 現在のPyTorchをアンインストール
```batch
pip uninstall torch torchvision torchaudio -y
```

#### 4-2. GPU版をインストール
```batch
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

**ダウンロードサイズ**: 約2.5GB
**所要時間**: 5-10分

#### 4-3. GPU認識確認
```batch
python -c "import torch; print(f'CUDA利用可能: {torch.cuda.is_available()}'); print(f'GPU名: {torch.cuda.get_device_name(0)}'); print(f'VRAM: {torch.cuda.get_device_properties(0).total_memory / 1e9:.2f} GB')"
```

**期待される出力**:
```
CUDA利用可能: True
GPU名: NVIDIA GeForce RTX 4070
VRAM: 12.00 GB
```

---

## 🎯 GPU確認

### GPU情報の詳細確認
```python
# gpu_check.py
import torch

print("="*60)
print("🔍 GPU環境確認")
print("="*60)

# CUDA確認
print(f"\n✅ CUDA利用可能: {torch.cuda.is_available()}")
print(f"✅ CUDAバージョン: {torch.version.cuda}")
print(f"✅ PyTorchバージョン: {torch.__version__}")

if torch.cuda.is_available():
    # GPU情報
    print(f"\n📊 GPU情報:")
    print(f"  GPU名: {torch.cuda.get_device_name(0)}")
    print(f"  VRAM: {torch.cuda.get_device_properties(0).total_memory / 1e9:.2f} GB")
    print(f"  計算能力: {torch.cuda.get_device_capability(0)}")
    
    # メモリ情報
    print(f"\n💾 メモリ情報:")
    print(f"  割り当て済み: {torch.cuda.memory_allocated(0) / 1e9:.2f} GB")
    print(f"  予約済み: {torch.cuda.memory_reserved(0) / 1e9:.2f} GB")
    
    # テスト計算
    print(f"\n🧪 テスト計算:")
    x = torch.rand(1000, 1000).cuda()
    y = torch.rand(1000, 1000).cuda()
    z = torch.matmul(x, y)
    print(f"  ✅ GPU計算成功")
else:
    print("\n❌ GPUが認識されていません")

print("\n" + "="*60)
```

**実行**:
```batch
python gpu_check.py
```

---

## ⚡ トレーニング最適化

### auto_augment_and_train.py の最適化

#### 現在の設定（CPU版）
```python
trainer.train_model(epochs=100, batch=16)
```

#### GPU最適化版
```python
trainer.train_model(
    epochs=200,           # 100 → 200（高速なので倍増）
    batch=32,             # 16 → 32（VRAM 12GBで余裕）
    imgsz=640,            # 画像サイズ
    device='0',           # GPU使用（'0' = 最初のGPU）
    workers=8,            # データローダーのワーカー数
    cache=True,           # メモリキャッシュ（高速化）
    amp=True,             # 混合精度（さらに高速化）
    patience=20,          # 早期終了
    save_period=10,       # 10エポックごとに保存
    plots=True            # トレーニングプロット生成
)
```

### バッチサイズの調整

| VRAM | 推奨バッチサイズ | 画像サイズ |
|------|-----------------|-----------|
| 8GB  | 16-24 | 640 |
| 12GB | 32-48 | 640 |
| 16GB | 48-64 | 640 |
| 24GB | 64-96 | 640 |

**RTX 4070（12GB）の推奨設定**:
```python
batch=32  # 安定
batch=48  # 最大（ギリギリ）
```

---

## 📊 性能比較

### トレーニング時間（100エポック、266枚）

| 環境 | 時間 | 速度比 |
|------|------|--------|
| CPU（現在のPC） | 2-4時間 | 1x |
| RTX 4070 | 20-30分 | **8x** ⚡ |
| RTX 4090 | 15-20分 | 12x |
| A100（クラウド） | 10-15分 | 16x |

### 推論速度（動画処理、90フレーム）

| 環境 | FPS | 処理時間 |
|------|-----|----------|
| CPU | 10-20 FPS | 4.5-9秒 |
| RTX 4070 | 60-120 FPS | **0.75-1.5秒** ⚡ |

---

## 🔧 トラブルシューティング

### 問題1: `nvidia-smi` が認識されない

**症状**:
```
'nvidia-smi' は、内部コマンドまたは外部コマンド、
操作可能なプログラムまたはバッチ ファイルとして認識されていません。
```

**解決策**:
1. NVIDIAドライバーを再インストール
2. 環境変数PATHに追加:
   ```
   C:\Program Files\NVIDIA Corporation\NVSMI
   ```

---

### 問題2: `torch.cuda.is_available()` が False

**症状**:
```python
torch.cuda.is_available()  # False
```

**解決策**:

#### 確認1: CUDAバージョンの一致
```batch
# PyTorchのCUDAバージョン
python -c "import torch; print(torch.version.cuda)"

# システムのCUDAバージョン
nvcc --version
```

#### 確認2: GPU版PyTorchのインストール
```batch
# 現在のPyTorchを確認
pip show torch

# GPU版を再インストール
pip uninstall torch torchvision torchaudio -y
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

---

### 問題3: CUDA out of memory

**症状**:
```
RuntimeError: CUDA out of memory. Tried to allocate X.XX GiB
```

**解決策**:

#### 方法1: バッチサイズを下げる
```python
trainer.train_model(batch=16)  # 32 → 16
```

#### 方法2: 画像サイズを下げる
```python
trainer.train_model(imgsz=416)  # 640 → 416
```

#### 方法3: キャッシュを無効化
```python
trainer.train_model(cache=False)
```

#### 方法4: メモリをクリア
```python
import torch
torch.cuda.empty_cache()
```

---

### 問題4: トレーニングが遅い

**症状**:
- GPU使用率が低い（<50%）
- CPUボトルネック

**解決策**:

#### ワーカー数を増やす
```python
trainer.train_model(workers=8)  # 4 → 8
```

#### キャッシュを有効化
```python
trainer.train_model(cache=True)
```

#### 混合精度を有効化
```python
trainer.train_model(amp=True)
```

---

## 📊 GPU監視

### リアルタイム監視
```batch
# 1秒ごとに更新
nvidia-smi -l 1
```

### トレーニング中の表示例
```
+-----------------------------------------------------------------------------+
| NVIDIA-SMI 545.84       Driver Version: 545.84       CUDA Version: 12.3     |
|-------------------------------+----------------------+----------------------+
|   0  NVIDIA GeForce RTX 4070  | 00000000:01:00.0  On |                  N/A |
| 75%   68C    P2   180W / 200W |  10500MiB / 12288MiB |     95%      Default |
+-------------------------------+----------------------+----------------------+
```

**読み方**:
- **Fan**: 75% - ファン速度
- **Temp**: 68C - GPU温度
- **Perf**: P2 - パフォーマンスステート（P0が最高）
- **Pwr**: 180W / 200W - 消費電力
- **Memory**: 10500MiB / 12288MiB - VRAM使用量
- **GPU-Util**: 95% - GPU使用率

---

## 🎯 最適なトレーニング設定（RTX 4070）

```python
# auto_augment_and_train.py

# データ拡張
augmenter.augment_dataset(target_count=500)  # 200 → 500

# トレーニング
trainer.train_model(
    epochs=200,           # 高速なので倍増
    batch=32,             # VRAM 12GBで最適
    imgsz=640,
    device='0',           # GPU使用
    workers=8,            # CPUコア活用
    cache=True,           # メモリキャッシュ
    amp=True,             # 混合精度
    patience=20,
    save_period=10,
    plots=True
)
```

**期待される結果**:
```
トレーニング時間: 20-30分
mAP50: 0.7-0.9
平均信頼度: 40-60%
採用率: 60-80%
```

---

## 🎉 まとめ

### セットアップ完了後
- ✅ GPU認識確認
- ✅ トレーニング速度: **8倍高速化**
- ✅ バッチサイズ: **2倍増加**
- ✅ 推論速度: **6-12倍高速化**

### 次のステップ
1. GPU版でトレーニング実行
2. 性能比較
3. より大規模なデータセットで実験
4. リアルタイム推論のテスト

**GPU環境で快適な機械学習ライフを！** 🚀
