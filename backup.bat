@echo off
chcp 65001 > nul
echo ============================================================
echo 💾 SwingTrace バックアップスクリプト
echo ============================================================
echo.

REM 日付取得
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set mytime=%mytime: =0%
set datetime=%mydate%_%mytime%

REM バックアップ先
set BACKUP_DIR=C:\Users\katsunori\Desktop\SwingTraceBackup_%datetime%

echo 📁 バックアップ先: %BACKUP_DIR%
echo.

REM フォルダ作成
mkdir "%BACKUP_DIR%"

echo ============================================================
echo 📦 バックアップ中...
echo ============================================================
echo.

REM データセット
echo [1/6] データセット...
if exist dataset (
    xcopy dataset "%BACKUP_DIR%\dataset" /E /I /Y /Q > nul
    echo   ✅ dataset/ をバックアップしました
) else (
    echo   ⚠️ dataset/ が見つかりません
)

REM トレーニング済みモデル
echo [2/6] トレーニング済みモデル...
if exist runs\detect\clubhead_high_confidence\weights (
    xcopy runs\detect\clubhead_high_confidence\weights "%BACKUP_DIR%\weights" /E /I /Y /Q > nul
    echo   ✅ モデルをバックアップしました
) else (
    echo   ⚠️ モデルが見つかりません
)

REM ONNXモデル
echo [3/6] ONNXモデル...
set onnx_found=0
if exist best.onnx (
    copy best.onnx "%BACKUP_DIR%\" /Y > nul
    set onnx_found=1
)
if exist app\src\main\assets\clubhead_yolov8.onnx (
    copy app\src\main\assets\clubhead_yolov8.onnx "%BACKUP_DIR%\" /Y > nul
    set onnx_found=1
)
if %onnx_found%==1 (
    echo   ✅ ONNXモデルをバックアップしました
) else (
    echo   ⚠️ ONNXモデルが見つかりません
)

REM アプリコード
echo [4/6] アプリコード...
if exist app (
    xcopy app "%BACKUP_DIR%\app" /E /I /Y /Q > nul
    echo   ✅ app/ をバックアップしました
) else (
    echo   ⚠️ app/ が見つかりません
)

REM Pythonスクリプト
echo [5/6] Pythonスクリプト...
copy *.py "%BACKUP_DIR%\" /Y > nul 2>&1
copy *.bat "%BACKUP_DIR%\" /Y > nul 2>&1
echo   ✅ スクリプトをバックアップしました

REM ドキュメント
echo [6/6] ドキュメント...
copy *.md "%BACKUP_DIR%\" /Y > nul 2>&1
copy *.gradle.kts "%BACKUP_DIR%\" /Y > nul 2>&1
copy settings.gradle.kts "%BACKUP_DIR%\" /Y > nul 2>&1
copy local.properties "%BACKUP_DIR%\" /Y > nul 2>&1
echo   ✅ ドキュメントをバックアップしました

echo.
echo ============================================================
echo ✅ バックアップ完了
echo ============================================================
echo 📁 保存先: %BACKUP_DIR%
echo.

REM フォルダサイズを計算
for /f "tokens=3" %%a in ('dir "%BACKUP_DIR%" /s /-c ^| find "個のファイル"') do set size=%%a
echo 📊 バックアップサイズ: %size% バイト
echo.

REM ZIP圧縮（オプション）
echo ============================================================
echo 🗜️ ZIP圧縮しますか？ (Y/N)
echo ============================================================
set /p compress="選択: "
if /i "%compress%"=="Y" (
    echo.
    echo 圧縮中...
    powershell -command "Compress-Archive -Path '%BACKUP_DIR%' -DestinationPath '%BACKUP_DIR%.zip' -Force"
    if exist "%BACKUP_DIR%.zip" (
        echo ✅ 圧縮完了: %BACKUP_DIR%.zip
        
        REM 圧縮後のサイズ
        for %%A in ("%BACKUP_DIR%.zip") do set zipsize=%%~zA
        echo 📊 圧縮後サイズ: %zipsize% バイト
        
        echo.
        echo 元のフォルダを削除しますか？ (Y/N)
        set /p delete="選択: "
        if /i "!delete!"=="Y" (
            rmdir /s /q "%BACKUP_DIR%"
            echo ✅ 元のフォルダを削除しました
        )
    ) else (
        echo ❌ 圧縮に失敗しました
    )
)

echo.
echo ============================================================
echo 💡 次のステップ
echo ============================================================
echo 1. バックアップファイルを確認
echo 2. クラウドにアップロード（推奨）
echo 3. 外付けHDDにコピー（推奨）
echo.
echo 📚 詳細は BACKUP_GUIDE.md を参照してください
echo.
pause
