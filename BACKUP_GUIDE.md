# 💾 バックアップ・移行ガイド

## 📋 目次
1. [バックアップ対象](#バックアップ対象)
2. [バックアップ方法](#バックアップ方法)
3. [新PCへの移行](#新pcへの移行)
4. [自動バックアップ](#自動バックアップ)
5. [復元方法](#復元方法)

---

## 📦 バックアップ対象

### 必須ファイル（絶対にバックアップ）

#### 1. データセット
```
dataset/
├── images/          # 元画像（66枚）
├── labels/          # ラベルファイル（66枚）
└── data.yaml        # データセット設定
```
**サイズ**: 約10-50MB
**重要度**: ⭐⭐⭐⭐⭐

#### 2. トレーニング済みモデル
```
runs/detect/clubhead_high_confidence/
└── weights/
    ├── best.pt      # 最良モデル（PyTorch）
    └── last.pt      # 最終モデル
```
**サイズ**: 約6-12MB
**重要度**: ⭐⭐⭐⭐⭐

#### 3. ONNXモデル
```
best.onnx                                    # プロジェクトルート
app/src/main/assets/clubhead_yolov8.onnx    # アプリ用
```
**サイズ**: 約6MB
**重要度**: ⭐⭐⭐⭐⭐

#### 4. プロジェクトファイル
```
app/                                         # Androidアプリ
├── src/
│   └── main/
│       ├── java/                           # Kotlinコード
│       ├── res/                            # リソース
│       └── AndroidManifest.xml
└── build.gradle.kts

auto_augment_and_train.py                   # トレーニングスクリプト
test_model.py                               # テストスクリプト
download_images.py                          # 画像収集スクリプト
```
**サイズ**: 約50-100MB
**重要度**: ⭐⭐⭐⭐

### オプションファイル（余裕があればバックアップ）

#### 5. 拡張データセット
```
dataset/augmented/
├── images/          # 拡張画像（200枚）
└── labels/          # 拡張ラベル
```
**サイズ**: 約50-200MB
**重要度**: ⭐⭐⭐（再生成可能）

#### 6. トレーニングログ
```
runs/detect/clubhead_high_confidence/
├── results.csv      # トレーニング結果
├── results.png      # グラフ
└── confusion_matrix.png
```
**サイズ**: 約5-10MB
**重要度**: ⭐⭐（記録用）

#### 7. ダウンロード済み画像
```
downloaded_images/   # Pixabayからダウンロード
```
**サイズ**: 約50-200MB
**重要度**: ⭐⭐（再ダウンロード可能）

---

## 💾 バックアップ方法

### 方法1: 手動バックアップ（推奨）

#### ステップ1: バックアップフォルダ作成
```batch
# デスクトップにバックアップフォルダを作成
mkdir C:\Users\katsunori\Desktop\SwingTraceBackup
cd C:\Users\katsunori\Desktop\SwingTraceBackup
```

#### ステップ2: 必須ファイルをコピー
```batch
# プロジェクトルートから実行
cd C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching

# データセット
xcopy dataset C:\Users\katsunori\Desktop\SwingTraceBackup\dataset /E /I /Y

# トレーニング済みモデル
xcopy runs\detect\clubhead_high_confidence\weights C:\Users\katsunori\Desktop\SwingTraceBackup\weights /E /I /Y

# ONNXモデル
copy best.onnx C:\Users\katsunori\Desktop\SwingTraceBackup\
copy app\src\main\assets\clubhead_yolov8.onnx C:\Users\katsunori\Desktop\SwingTraceBackup\

# プロジェクトファイル
xcopy app C:\Users\katsunori\Desktop\SwingTraceBackup\app /E /I /Y
copy *.py C:\Users\katsunori\Desktop\SwingTraceBackup\
copy *.bat C:\Users\katsunori\Desktop\SwingTraceBackup\
copy *.md C:\Users\katsunori\Desktop\SwingTraceBackup\
```

#### ステップ3: ZIP圧縮
```batch
# PowerShellで圧縮
Compress-Archive -Path C:\Users\katsunori\Desktop\SwingTraceBackup -DestinationPath C:\Users\katsunori\Desktop\SwingTraceBackup_20251031.zip
```

---

### 方法2: バックアップスクリプト（自動）

#### backup.bat を作成
```batch
@echo off
chcp 65001 > nul
echo ============================================================
echo 💾 SwingTrace バックアップスクリプト
echo ============================================================
echo.

REM 日付取得
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set datetime=%mydate%_%mytime%

REM バックアップ先
set BACKUP_DIR=C:\Users\katsunori\Desktop\SwingTraceBackup_%datetime%

echo 📁 バックアップ先: %BACKUP_DIR%
echo.

REM フォルダ作成
mkdir "%BACKUP_DIR%"

echo 📦 データセットをバックアップ中...
xcopy dataset "%BACKUP_DIR%\dataset" /E /I /Y /Q

echo 📦 モデルをバックアップ中...
if exist runs\detect\clubhead_high_confidence\weights (
    xcopy runs\detect\clubhead_high_confidence\weights "%BACKUP_DIR%\weights" /E /I /Y /Q
)

echo 📦 ONNXモデルをバックアップ中...
if exist best.onnx (
    copy best.onnx "%BACKUP_DIR%\" /Y > nul
)
if exist app\src\main\assets\clubhead_yolov8.onnx (
    copy app\src\main\assets\clubhead_yolov8.onnx "%BACKUP_DIR%\" /Y > nul
)

echo 📦 プロジェクトファイルをバックアップ中...
xcopy app "%BACKUP_DIR%\app" /E /I /Y /Q
copy *.py "%BACKUP_DIR%\" /Y > nul 2>&1
copy *.bat "%BACKUP_DIR%\" /Y > nul 2>&1
copy *.md "%BACKUP_DIR%\" /Y > nul 2>&1
copy *.gradle.kts "%BACKUP_DIR%\" /Y > nul 2>&1
copy settings.gradle.kts "%BACKUP_DIR%\" /Y > nul 2>&1
copy local.properties "%BACKUP_DIR%\" /Y > nul 2>&1

echo.
echo ============================================================
echo ✅ バックアップ完了
echo ============================================================
echo 📁 保存先: %BACKUP_DIR%
echo.

REM ZIP圧縮（オプション）
echo 🗜️ ZIP圧縮しますか？ (Y/N)
set /p compress="選択: "
if /i "%compress%"=="Y" (
    echo 圧縮中...
    powershell -command "Compress-Archive -Path '%BACKUP_DIR%' -DestinationPath '%BACKUP_DIR%.zip' -Force"
    echo ✅ 圧縮完了: %BACKUP_DIR%.zip
)

echo.
pause
```

#### 実行
```batch
backup.bat
```

---

### 方法3: クラウドバックアップ

#### Google Drive
```
1. Google Driveデスクトップアプリをインストール
2. バックアップフォルダをGoogle Driveに移動
3. 自動同期
```

#### OneDrive
```
1. OneDriveフォルダにバックアップをコピー
2. 自動同期
```

#### GitHub（推奨：コード管理）
```batch
# Gitリポジトリ初期化
cd C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching
git init
git add app/ *.py *.bat *.md
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/SwingTrace.git
git push -u origin main
```

**注意**: 大きなファイル（モデル、データセット）は`.gitignore`に追加

---

## 🔄 新PCへの移行

### ステップ1: 新PCの準備

#### 1-1. 必要なソフトウェアをインストール
```
✅ Python 3.10
✅ Android Studio
✅ Git
✅ NVIDIA ドライバー
✅ CUDA Toolkit 12.1
✅ cuDNN 8.9
```

#### 1-2. プロジェクトフォルダを作成
```batch
mkdir C:\Users\katsunori\CascadeProjects
```

---

### ステップ2: バックアップを復元

#### 2-1. ZIPファイルを展開
```batch
# デスクトップのZIPファイルを展開
Expand-Archive -Path C:\Users\katsunori\Desktop\SwingTraceBackup_20251031.zip -DestinationPath C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching
```

#### 2-2. または、手動でコピー
```batch
# USBメモリやネットワーク経由でコピー
xcopy D:\SwingTraceBackup C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching /E /I /Y
```

---

### ステップ3: 環境セットアップ

#### 3-1. Python環境
```batch
cd C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching

# 必要なライブラリをインストール
pip install opencv-python albumentations ultralytics pyyaml tqdm requests

# GPU版PyTorch
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

#### 3-2. GPU確認
```batch
python -c "import torch; print(f'CUDA利用可能: {torch.cuda.is_available()}')"
```

#### 3-3. Android Studio
```
1. Android Studioを起動
2. 「Open」→ プロジェクトフォルダを選択
3. Gradle同期
4. ビルド確認
```

---

### ステップ4: 動作確認

#### 4-1. モデルテスト
```batch
python test_model.py
```

#### 4-2. トレーニングテスト
```batch
# 小規模テスト（10エポック）
python -c "from auto_augment_and_train import *; trainer = YOLOv8Trainer('dataset'); trainer.train_model(epochs=10, batch=16, device='0')"
```

#### 4-3. アプリビルド
```batch
cd app
gradlew assembleDebug
```

---

## 🔁 自動バックアップ

### Windows タスクスケジューラで自動化

#### ステップ1: タスク作成
```
1. 「タスクスケジューラ」を起動
2. 「基本タスクの作成」をクリック
3. 名前: SwingTrace自動バックアップ
4. トリガー: 毎日 or 毎週
5. 操作: プログラムの開始
6. プログラム: C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching\backup.bat
```

#### ステップ2: 設定
```
- 最上位の特権で実行: チェック
- ユーザーがログオンしているかどうかにかかわらず実行: チェック
```

---

## 🔧 復元方法

### 問題: モデルファイルが見つからない

**症状**:
```
FileNotFoundError: best.pt が見つかりません
```

**解決策**:
```batch
# バックアップから復元
copy C:\Users\katsunori\Desktop\SwingTraceBackup\weights\best.pt runs\detect\clubhead_high_confidence\weights\
```

---

### 問題: データセットが見つからない

**症状**:
```
FileNotFoundError: dataset/images が見つかりません
```

**解決策**:
```batch
# バックアップから復元
xcopy C:\Users\katsunori\Desktop\SwingTraceBackup\dataset dataset /E /I /Y
```

---

### 問題: ONNXモデルが見つからない

**症状**:
```
アプリでモデルが読み込めない
```

**解決策**:
```batch
# バックアップから復元
copy C:\Users\katsunori\Desktop\SwingTraceBackup\clubhead_yolov8.onnx app\src\main\assets\
```

---

## 📊 バックアップサイズ目安

| ファイル | サイズ | 圧縮後 |
|---------|--------|--------|
| データセット（66枚） | 10-50MB | 5-25MB |
| 拡張データセット（266枚） | 50-200MB | 25-100MB |
| トレーニング済みモデル | 6-12MB | 3-6MB |
| ONNXモデル | 6MB | 3MB |
| プロジェクトファイル | 50-100MB | 25-50MB |
| **合計** | **120-370MB** | **60-185MB** |

---

## 💡 ベストプラクティス

### 1. 定期的なバックアップ
```
✅ トレーニング前: 必ずバックアップ
✅ 重要な変更後: バックアップ
✅ 週1回: 定期バックアップ
```

### 2. 複数の保存先
```
✅ ローカル: デスクトップ or 外付けHDD
✅ クラウド: Google Drive or OneDrive
✅ GitHub: コードのみ
```

### 3. バージョン管理
```
SwingTraceBackup_20251031_1000.zip
SwingTraceBackup_20251107_1500.zip
SwingTraceBackup_20251114_2000.zip
```

### 4. 古いバックアップの削除
```
✅ 1ヶ月以上前のバックアップは削除
✅ 重要なマイルストーンは保持
```

---

## 🎯 チェックリスト

### バックアップ前
- [ ] トレーニングが完了している
- [ ] モデルファイルが存在する
- [ ] データセットが完全である
- [ ] アプリがビルドできる

### バックアップ後
- [ ] ZIPファイルが作成された
- [ ] ファイルサイズが妥当
- [ ] 展開してファイルを確認
- [ ] クラウドにアップロード（オプション）

### 新PC移行後
- [ ] Python環境が整っている
- [ ] GPU が認識されている
- [ ] モデルテストが成功
- [ ] アプリがビルドできる
- [ ] トレーニングが実行できる

---

## 🎉 まとめ

### 必須バックアップ
1. **dataset/** - データセット
2. **runs/detect/clubhead_high_confidence/weights/** - モデル
3. **best.onnx** - ONNXモデル
4. **app/** - アプリコード
5. ***.py** - スクリプト

### 推奨バックアップ方法
1. **backup.bat** で自動バックアップ
2. **ZIP圧縮** でサイズ削減
3. **クラウド** に保存

### 新PC移行
1. バックアップを復元
2. GPU環境セットアップ
3. 動作確認
4. トレーニング実行

**定期的なバックアップで安心！** 💾
