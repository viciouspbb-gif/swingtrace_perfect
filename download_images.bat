@echo off
chcp 65001 > nul
echo ============================================================
echo 🎯 著作権フリー画像収集スクリプト
echo ============================================================
echo.

REM Pythonの確認
python --version > nul 2>&1
if errorlevel 1 (
    echo ❌ Pythonがインストールされていません
    echo    https://www.python.org/ からダウンロードしてください
    pause
    exit /b 1
)

echo ✅ Python検出

REM 必要なライブラリの確認とインストール
echo.
echo 📦 必要なライブラリを確認中...
python -c "import requests" 2>nul
if errorlevel 1 (
    echo ⚠️ requestsがインストールされていません
    echo 📦 インストール中...
    pip install requests
)

echo.
echo ============================================================
echo 📝 APIキーの確認
echo ============================================================
echo.

REM APIキーの確認
set API_KEYS_OK=1

if "%PIXABAY_API_KEY%"=="" (
    echo ⚠️ PIXABAY_API_KEYが設定されていません
    echo    https://pixabay.com/api/docs/ でAPIキーを取得してください
    echo    設定方法: setx PIXABAY_API_KEY "your_key_here"
    set API_KEYS_OK=0
) else (
    echo ✅ PIXABAY_API_KEY: 設定済み
)

if "%UNSPLASH_ACCESS_KEY%"=="" (
    echo ⚠️ UNSPLASH_ACCESS_KEYが設定されていません
    echo    https://unsplash.com/developers でAPIキーを取得してください
    echo    設定方法: setx UNSPLASH_ACCESS_KEY "your_key_here"
    set API_KEYS_OK=0
) else (
    echo ✅ UNSPLASH_ACCESS_KEY: 設定済み
)

if "%PEXELS_API_KEY%"=="" (
    echo ⚠️ PEXELS_API_KEYが設定されていません
    echo    https://www.pexels.com/api/ でAPIキーを取得してください
    echo    設定方法: setx PEXELS_API_KEY "your_key_here"
    set API_KEYS_OK=0
) else (
    echo ✅ PEXELS_API_KEY: 設定済み
)

echo.
if %API_KEYS_OK%==0 (
    echo ============================================================
    echo ⚠️ APIキーが設定されていません
    echo ============================================================
    echo.
    echo 💡 APIキーを設定してから再実行してください
    echo    設定後、コマンドプロンプトを再起動してください
    echo.
    pause
    exit /b 1
)

echo ============================================================
echo 🚀 画像ダウンロード開始
echo ============================================================
echo.

REM スクリプト実行
python download_images.py

if errorlevel 1 (
    echo.
    echo ❌ エラーが発生しました
    pause
    exit /b 1
)

echo.
echo ============================================================
echo ✅ ダウンロード完了
echo ============================================================
echo.
echo 💡 次のステップ:
echo    1. Roboflowにアップロードしてラベル付け
echo    2. YOLOv8形式でエクスポート
echo    3. dataset/images/ と dataset/labels/ に配置
echo    4. run_auto_training.bat を実行
echo.
echo 📚 詳細は ROBOFLOW_GUIDE.md を参照してください
echo.
pause
