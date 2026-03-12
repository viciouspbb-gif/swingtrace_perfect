@echo off
chcp 65001 >nul
echo ============================================================
echo 🔧 トレーニング環境セットアップ
echo ============================================================
echo.

REM Python確認
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Pythonがインストールされていません
    echo Python 3.8以上をインストールしてください
    echo https://www.python.org/downloads/
    pause
    exit /b 1
)

echo ✅ Python検出完了
python --version
echo.

REM 仮想環境作成
if exist "yolo_env" (
    echo ⚠️ 既存の仮想環境を検出しました
    choice /C YN /M "削除して再作成しますか？(Y/N)"
    if errorlevel 2 goto skip_venv
    rmdir /s /q yolo_env
)

echo 📦 仮想環境を作成中...
python -m venv yolo_env
if errorlevel 1 (
    echo ❌ 仮想環境の作成に失敗しました
    pause
    exit /b 1
)

:skip_venv
echo ✅ 仮想環境作成完了
echo.

REM 仮想環境をアクティベート
call yolo_env\Scripts\activate.bat

REM pipアップグレード
echo 📦 pipをアップグレード中...
python -m pip install --upgrade pip

REM 必要なライブラリをインストール
echo.
echo ============================================================
echo 📦 必要なライブラリをインストール中...
echo ============================================================
echo.

echo [1/6] ultralytics（YOLOv8）
pip install ultralytics

echo.
echo [2/6] opencv-python
pip install opencv-python

echo.
echo [3/6] albumentations（データ拡張）
pip install albumentations

echo.
echo [4/6] tqdm（プログレスバー）
pip install tqdm

echo.
echo [5/6] numpy
pip install numpy

echo.
echo [6/6] pillow
pip install pillow

echo.
echo ============================================================
echo ✅ インストール完了！
echo ============================================================
echo.

REM インストール確認
echo 📋 インストール済みパッケージ:
pip list | findstr /i "ultralytics opencv albumentations tqdm numpy pillow"

echo.
echo ============================================================
echo 🎉 セットアップ完了！
echo ============================================================
echo.
echo 次のステップ:
echo   1. run_auto_training.bat を実行
echo   2. データ拡張とトレーニングが自動で開始されます
echo.
pause
