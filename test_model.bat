@echo off
chcp 65001 >nul
echo ============================================================
echo 🏌️ クラブヘッド検出モデル - テストツール
echo ============================================================
echo.

REM 仮想環境をアクティベート
if exist "yolo_env\Scripts\activate.bat" (
    call yolo_env\Scripts\activate.bat
) else (
    echo ⚠️ 仮想環境が見つかりません
    echo セットアップを実行してください: setup_training_env.bat
    pause
    exit /b 1
)

REM テストスクリプト実行
python test_model.py

pause
