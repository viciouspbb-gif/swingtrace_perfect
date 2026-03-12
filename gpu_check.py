"""
GPU環境確認スクリプト
新PCでGPUが正しく認識されているか確認
"""

import sys

print("="*60)
print("🔍 GPU環境確認")
print("="*60)
print()

# PyTorchのインポート確認
try:
    import torch
    print("✅ PyTorch インポート成功")
    print(f"   バージョン: {torch.__version__}")
except ImportError:
    print("❌ PyTorchがインストールされていません")
    print("   インストール: pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121")
    sys.exit(1)

print()

# CUDA確認
cuda_available = torch.cuda.is_available()
print(f"{'✅' if cuda_available else '❌'} CUDA利用可能: {cuda_available}")

if cuda_available:
    print(f"   CUDAバージョン: {torch.version.cuda}")
    print(f"   cuDNNバージョン: {torch.backends.cudnn.version()}")
    print()
    
    # GPU情報
    print("📊 GPU情報:")
    gpu_count = torch.cuda.device_count()
    print(f"   GPU数: {gpu_count}")
    
    for i in range(gpu_count):
        print(f"\n   GPU {i}:")
        print(f"     名前: {torch.cuda.get_device_name(i)}")
        
        props = torch.cuda.get_device_properties(i)
        print(f"     VRAM: {props.total_memory / 1e9:.2f} GB")
        print(f"     計算能力: {props.major}.{props.minor}")
        print(f"     マルチプロセッサ数: {props.multi_processor_count}")
    
    print()
    
    # メモリ情報
    print("💾 メモリ情報:")
    print(f"   割り当て済み: {torch.cuda.memory_allocated(0) / 1e9:.3f} GB")
    print(f"   予約済み: {torch.cuda.memory_reserved(0) / 1e9:.3f} GB")
    print(f"   最大割り当て: {torch.cuda.max_memory_allocated(0) / 1e9:.3f} GB")
    
    print()
    
    # テスト計算
    print("🧪 テスト計算:")
    try:
        import time
        
        # CPU計算
        print("   CPU計算中...")
        x_cpu = torch.rand(5000, 5000)
        y_cpu = torch.rand(5000, 5000)
        start = time.time()
        z_cpu = torch.matmul(x_cpu, y_cpu)
        cpu_time = time.time() - start
        print(f"   ✅ CPU: {cpu_time:.3f}秒")
        
        # GPU計算
        print("   GPU計算中...")
        x_gpu = torch.rand(5000, 5000).cuda()
        y_gpu = torch.rand(5000, 5000).cuda()
        torch.cuda.synchronize()
        start = time.time()
        z_gpu = torch.matmul(x_gpu, y_gpu)
        torch.cuda.synchronize()
        gpu_time = time.time() - start
        print(f"   ✅ GPU: {gpu_time:.3f}秒")
        
        speedup = cpu_time / gpu_time
        print(f"   🚀 高速化: {speedup:.1f}倍")
        
        # メモリ使用量
        print(f"\n   GPU使用メモリ: {torch.cuda.memory_allocated(0) / 1e9:.3f} GB")
        
    except Exception as e:
        print(f"   ❌ テスト計算失敗: {e}")
    
    print()
    
    # 推奨設定
    vram_gb = props.total_memory / 1e9
    print("💡 推奨トレーニング設定:")
    
    if vram_gb >= 12:
        print("   batch=32-48")
        print("   imgsz=640")
        print("   workers=8")
        print("   cache=True")
        print("   amp=True")
    elif vram_gb >= 8:
        print("   batch=16-24")
        print("   imgsz=640")
        print("   workers=4")
        print("   cache=True")
        print("   amp=True")
    else:
        print("   batch=8-16")
        print("   imgsz=416")
        print("   workers=2")
        print("   cache=False")
        print("   amp=True")
    
else:
    print()
    print("❌ GPUが認識されていません")
    print()
    print("📝 確認事項:")
    print("   1. NVIDIAドライバーがインストールされているか")
    print("      確認: nvidia-smi")
    print()
    print("   2. CUDA Toolkitがインストールされているか")
    print("      確認: nvcc --version")
    print()
    print("   3. PyTorch GPU版がインストールされているか")
    print("      再インストール:")
    print("      pip uninstall torch torchvision torchaudio -y")
    print("      pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121")
    print()
    print("📚 詳細は GPU_SETUP_GUIDE.md を参照してください")

print()
print("="*60)
print("✅ 確認完了")
print("="*60)
