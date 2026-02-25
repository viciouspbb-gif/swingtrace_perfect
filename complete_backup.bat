@echo off
chcp 65001 > nul
echo ============================================================
echo 💾 完全バックアップスクリプト
echo ============================================================
echo.

REM 日付取得
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set mytime=%mytime: =0%
set datetime=%mydate%_%mytime%

REM バックアップ先を選択
echo 📁 バックアップ先を選択してください:
echo.
echo 1. デスクトップ（小規模・テスト用）
echo 2. Dドライブ（推奨）
echo 3. 外付けドライブ（カスタム）
echo.
set /p choice="選択 (1-3): "

if "%choice%"=="1" (
    set BACKUP_ROOT=C:\Users\katsunori\Desktop\CompleteBackup_%datetime%
) else if "%choice%"=="2" (
    set BACKUP_ROOT=D:\CompleteBackup_%datetime%
) else if "%choice%"=="3" (
    set /p BACKUP_ROOT="バックアップ先のパスを入力: "
    set BACKUP_ROOT=%BACKUP_ROOT%\CompleteBackup_%datetime%
) else (
    echo ❌ 無効な選択です
    pause
    exit /b 1
)

mkdir "%BACKUP_ROOT%"

echo.
echo 📁 バックアップ先: %BACKUP_ROOT%
echo.
echo ============================================================
echo 📦 バックアップ開始
echo ============================================================
echo.

REM 1. プロジェクトデータ
echo [1/9] プロジェクトデータ...
if exist "C:\Users\katsunori\CascadeProjects" (
    xcopy "C:\Users\katsunori\CascadeProjects" "%BACKUP_ROOT%\Projects\" /E /I /Y /Q > nul
    echo   ✅ プロジェクトをバックアップしました
) else (
    echo   ⚠️ プロジェクトフォルダが見つかりません
)

REM 2. Outlookデータ
echo [2/9] Outlookデータ...
set outlook_found=0
if exist "C:\Users\katsunori\Documents\Outlookファイル" (
    xcopy "C:\Users\katsunori\Documents\Outlookファイル" "%BACKUP_ROOT%\Outlook\" /E /I /Y /Q > nul
    set outlook_found=1
)
if exist "C:\Users\katsunori\AppData\Local\Microsoft\Outlook" (
    xcopy "C:\Users\katsunori\AppData\Local\Microsoft\Outlook\*.pst" "%BACKUP_ROOT%\Outlook\" /Y /Q > nul 2>&1
    set outlook_found=1
)
if %outlook_found%==1 (
    echo   ✅ Outlookをバックアップしました
) else (
    echo   ⚠️ Outlookデータが見つかりません
)

REM 3. Thunderbirdデータ
echo [3/9] Thunderbirdデータ...
if exist "C:\Users\katsunori\AppData\Roaming\Thunderbird" (
    xcopy "C:\Users\katsunori\AppData\Roaming\Thunderbird" "%BACKUP_ROOT%\Thunderbird\" /E /I /Y /Q > nul
    echo   ✅ Thunderbirdをバックアップしました
) else (
    echo   ⚠️ Thunderbirdが見つかりません（スキップ）
)

REM 4. ブラウザデータ
echo [4/9] ブラウザデータ...
set browser_found=0

REM Chrome
if exist "C:\Users\katsunori\AppData\Local\Google\Chrome\User Data\Default\Bookmarks" (
    mkdir "%BACKUP_ROOT%\Browser\Chrome" > nul 2>&1
    copy "C:\Users\katsunori\AppData\Local\Google\Chrome\User Data\Default\Bookmarks" "%BACKUP_ROOT%\Browser\Chrome\" /Y > nul
    set browser_found=1
)

REM Edge
if exist "C:\Users\katsunori\AppData\Local\Microsoft\Edge\User Data\Default\Bookmarks" (
    mkdir "%BACKUP_ROOT%\Browser\Edge" > nul 2>&1
    copy "C:\Users\katsunori\AppData\Local\Microsoft\Edge\User Data\Default\Bookmarks" "%BACKUP_ROOT%\Browser\Edge\" /Y > nul
    set browser_found=1
)

REM Firefox
if exist "C:\Users\katsunori\AppData\Roaming\Mozilla\Firefox\Profiles" (
    xcopy "C:\Users\katsunori\AppData\Roaming\Mozilla\Firefox\Profiles" "%BACKUP_ROOT%\Browser\Firefox\" /E /I /Y /Q > nul
    set browser_found=1
)

if %browser_found%==1 (
    echo   ✅ ブラウザデータをバックアップしました
) else (
    echo   ⚠️ ブラウザデータが見つかりません
)

REM 5. Android Studio設定
echo [5/9] Android Studio設定...
set as_found=0
for /d %%D in ("C:\Users\katsunori\.AndroidStudio*") do (
    xcopy "%%D" "%BACKUP_ROOT%\AndroidStudio\" /E /I /Y /Q > nul
    set as_found=1
)
if exist "C:\Users\katsunori\.android" (
    xcopy "C:\Users\katsunori\.android" "%BACKUP_ROOT%\android\" /E /I /Y /Q > nul
    set as_found=1
)
if %as_found%==1 (
    echo   ✅ Android Studioをバックアップしました
) else (
    echo   ⚠️ Android Studio設定が見つかりません
)

REM 6. VSCode設定
echo [6/9] VSCode設定...
if exist "C:\Users\katsunori\AppData\Roaming\Code\User" (
    xcopy "C:\Users\katsunori\AppData\Roaming\Code\User" "%BACKUP_ROOT%\VSCode\" /E /I /Y /Q > nul
    echo   ✅ VSCodeをバックアップしました
) else (
    echo   ⚠️ VSCode設定が見つかりません
)

REM 7. Git設定
echo [7/9] Git設定...
set git_found=0
if exist "C:\Users\katsunori\.gitconfig" (
    mkdir "%BACKUP_ROOT%\Git" > nul 2>&1
    copy "C:\Users\katsunori\.gitconfig" "%BACKUP_ROOT%\Git\" /Y > nul
    set git_found=1
)
if exist "C:\Users\katsunori\.ssh" (
    xcopy "C:\Users\katsunori\.ssh" "%BACKUP_ROOT%\Git\ssh\" /E /I /Y /Q > nul
    set git_found=1
)
if %git_found%==1 (
    echo   ✅ Git設定をバックアップしました
) else (
    echo   ⚠️ Git設定が見つかりません
)

REM 8. 個人データ
echo [8/9] 個人データ...
echo   デスクトップ...
xcopy "C:\Users\katsunori\Desktop" "%BACKUP_ROOT%\Desktop\" /E /I /Y /Q > nul 2>&1
echo   ドキュメント...
xcopy "C:\Users\katsunori\Documents" "%BACKUP_ROOT%\Documents\" /E /I /Y /Q > nul 2>&1
echo   ピクチャ...
xcopy "C:\Users\katsunori\Pictures" "%BACKUP_ROOT%\Pictures\" /E /I /Y /Q > nul 2>&1
echo   ダウンロード...
xcopy "C:\Users\katsunori\Downloads" "%BACKUP_ROOT%\Downloads\" /E /I /Y /Q > nul 2>&1
echo   ✅ 個人データをバックアップしました

REM 9. システム設定
echo [9/9] システム設定...
mkdir "%BACKUP_ROOT%\Settings" > nul 2>&1
reg export "HKEY_CURRENT_USER\Environment" "%BACKUP_ROOT%\Settings\User_Environment.reg" /y > nul 2>&1
echo   ✅ システム設定をバックアップしました

echo.
echo ============================================================
echo ✅ バックアップ完了
echo ============================================================
echo 📁 保存先: %BACKUP_ROOT%
echo.

REM フォルダサイズを計算
echo 📊 バックアップサイズを計算中...
powershell -command "$size = (Get-ChildItem -Path '%BACKUP_ROOT%' -Recurse | Measure-Object -Property Length -Sum).Sum; Write-Host ('サイズ: {0:N2} GB' -f ($size / 1GB))"

echo.
echo ============================================================
echo 💡 次のステップ
echo ============================================================
echo 1. バックアップ内容を確認
echo 2. 外付けHDDにコピー（推奨）
echo 3. クラウドにアップロード（推奨）
echo.
echo 📚 詳細は COMPLETE_BACKUP_GUIDE.md を参照
echo.

REM バックアップフォルダを開く
echo バックアップフォルダを開きますか？ (Y/N)
set /p open="選択: "
if /i "%open%"=="Y" (
    explorer "%BACKUP_ROOT%"
)

echo.
pause
