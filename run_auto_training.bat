@echo off
chcp 65001 >nul
echo ============================================================
echo 🏌️ クラブヘッド検出 - 自動トレーニング実行
echo ============================================================
echo.

REM Python仮想環境の確認
if exist "yolo_env\Scripts\activate.bat" (
    echo ✅ 仮想環境を検出しました
    call yolo_env\Scripts\activate.bat
) else (
    echo ⚠️ 仮想環境が見つかりません
    echo セットアップを実行してください: setup_training_env.bat
    pause
    exit /b 1
)

echo.
echo 📦 必要なライブラリを確認中...
python -c "import ultralytics, albumentations, cv2" 2>nul
if errorlevel 1 (
    echo ❌ 必要なライブラリがインストールされていません
    echo セットアップを実行してください: setup_training_env.bat
    pause
    exit /b 1
)

echo ✅ すべてのライブラリが利用可能です
echo.
echo ============================================================
echo 🚀 自動トレーニング開始
echo ============================================================
echo.
echo 処理内容:
echo   1. データ拡張（200枚追加）
echo   2. YOLOv8トレーニング（100エポック）
echo   3. モデル評価
echo   4. ONNX変換
echo.
echo ⏱️ 推定時間: 30分〜2時間（GPU使用時）
echo.
pause

REM トレーニング実行
python auto_augment_and_train.py

echo.
echo ============================================================
echo 🎉 処理完了！
echo ============================================================
echo.
echo 📁 生成されたファイル:
echo   - runs/detect/clubhead_high_confidence/weights/best.pt
echo   - runs/detect/clubhead_high_confidence/weights/best.onnx
echo   - app/src/main/assets/clubhead_yolov8.onnx
echo.
pause
