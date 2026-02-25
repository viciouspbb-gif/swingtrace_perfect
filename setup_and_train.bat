@echo off
REM YOLOv8 クラブヘッド検出モデル トレーニングスクリプト (Windows版)

echo 🏌️ YOLOv8 クラブヘッド検出モデル トレーニング開始
echo ================================================
echo.

REM ステップ1: 環境チェック
echo 📦 ステップ1: 環境チェック
python --version
nvidia-smi --query-gpu=name --format=csv,noheader 2>nul
if %errorlevel% equ 0 (
    echo ✅ GPU検出
) else (
    echo ⚠️  GPU未検出 ^(CPUでトレーニング^)
)
echo.

REM ステップ2: YOLOv8インストール
echo 📦 ステップ2: YOLOv8インストール
pip install ultralytics opencv-python pillow
echo.

REM ステップ3: データセット構造確認
echo 📂 ステップ3: データセット構造確認
if exist "dataset" (
    echo ✅ dataset\ ディレクトリ存在
    dir /b /s dataset\images\*.jpg 2>nul | find /c /v "" > temp_count.txt
    set /p IMG_COUNT=<temp_count.txt
    echo    画像数: %IMG_COUNT%
    dir /b /s dataset\labels\*.txt 2>nul | find /c /v "" > temp_count.txt
    set /p LBL_COUNT=<temp_count.txt
    echo    ラベル数: %LBL_COUNT%
    del temp_count.txt
) else (
    echo ❌ dataset\ ディレクトリが見つかりません
    echo.
    echo 以下の構造でデータセットを準備してください：
    echo dataset\
    echo ├── images\
    echo │   ├── img001.jpg
    echo │   └── ...
    echo └── labels\
    echo     ├── img001.txt
    echo     └── ...
    pause
    exit /b 1
)
echo.

REM ステップ4: clubhead.yaml作成
echo 📝 ステップ4: clubhead.yaml作成
(
echo path: %CD%\dataset
echo train: images
echo val: images
echo.
echo nc: 1
echo names: ['clubhead']
) > clubhead.yaml
echo ✅ clubhead.yaml 作成完了
type clubhead.yaml
echo.

REM ステップ5: トレーニング
echo 🚀 ステップ5: トレーニング開始
echo    モデル: yolov8n.pt ^(軽量^)
echo    エポック: 50
echo    画像サイズ: 640x640
echo.

yolo task=detect mode=train model=yolov8n.pt data=clubhead.yaml epochs=50 imgsz=640

REM ステップ6: 評価
echo.
echo 📊 ステップ6: モデル評価
yolo task=detect mode=val model=runs\detect\train\weights\best.pt data=clubhead.yaml

REM ステップ7: ONNX変換
echo.
echo 🔄 ステップ7: ONNX変換
yolo export model=runs\detect\train\weights\best.pt format=onnx

REM ステップ8: アプリに配置
echo.
echo 📱 ステップ8: Androidアプリに配置
set ASSETS_DIR=app\src\main\assets
if not exist "%ASSETS_DIR%" mkdir "%ASSETS_DIR%"
copy runs\detect\train\weights\best.onnx "%ASSETS_DIR%\clubhead_yolov8.onnx"
echo ✅ %ASSETS_DIR%\clubhead_yolov8.onnx に配置完了

REM 完了
echo.
echo 🎉 トレーニング完了！
echo ================================================
echo.
echo 📊 結果:
echo    モデルファイル: runs\detect\train\weights\best.pt
echo    ONNXファイル: %ASSETS_DIR%\clubhead_yolov8.onnx
echo.
echo 📱 次のステップ:
echo    1. Android Studioでプロジェクトを開く
echo    2. ビルド: gradlew installDebug
echo    3. アプリで「クラブヘッド軌道追跡」をテスト
echo.
pause
