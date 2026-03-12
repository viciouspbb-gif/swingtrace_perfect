@echo off
chcp 65001 > nul
echo ============================================================
echo 🔐 重要項目バックアップスクリプト
echo ============================================================
echo.

REM 日付取得
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c%%a%%b)
for /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a%%b)
set mytime=%mytime: =0%
set datetime=%mydate%_%mytime%

REM バックアップ先
set BACKUP_ROOT=C:\Users\katsunori\Desktop\CriticalBackup_%datetime%
mkdir "%BACKUP_ROOT%"

echo 📁 バックアップ先: %BACKUP_ROOT%
echo.
echo ⚠️ このバックアップには機密情報が含まれます
echo    安全な場所に保管してください
echo.
echo ============================================================
echo 📦 バックアップ開始
echo ============================================================
echo.

REM 1. SSH鍵
echo [1/9] SSH鍵...
if exist "C:\Users\katsunori\.ssh" (
    xcopy "C:\Users\katsunori\.ssh" "%BACKUP_ROOT%\ssh\" /E /I /Y /Q > nul
    echo   ✅ SSH鍵をバックアップしました
    echo   ⚠️ 機密情報：暗号化推奨
) else (
    echo   ⚠️ .sshフォルダが見つかりません
)

REM 2. 環境変数
echo [2/9] 環境変数...
mkdir "%BACKUP_ROOT%\Settings" > nul 2>&1
reg export "HKEY_CURRENT_USER\Environment" "%BACKUP_ROOT%\Settings\User_Environment.reg" /y > nul 2>&1
reg export "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "%BACKUP_ROOT%\Settings\System_Environment.reg" /y > nul 2>&1

REM 環境変数をテキストでも保存
echo PIXABAY_API_KEY=%PIXABAY_API_KEY% > "%BACKUP_ROOT%\Settings\env_vars.txt"
echo UNSPLASH_ACCESS_KEY=%UNSPLASH_ACCESS_KEY% >> "%BACKUP_ROOT%\Settings\env_vars.txt"
echo PEXELS_API_KEY=%PEXELS_API_KEY% >> "%BACKUP_ROOT%\Settings\env_vars.txt"
echo JAVA_HOME=%JAVA_HOME% >> "%BACKUP_ROOT%\Settings\env_vars.txt"
echo ANDROID_HOME=%ANDROID_HOME% >> "%BACKUP_ROOT%\Settings\env_vars.txt"
echo CUDA_PATH=%CUDA_PATH% >> "%BACKUP_ROOT%\Settings\env_vars.txt"
echo   ✅ 環境変数をバックアップしました

REM 3. Python依存関係
echo [3/9] Python依存関係...
cd /d "C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching"
pip freeze > "%BACKUP_ROOT%\requirements.txt" 2>nul
if exist requirements.txt (
    copy requirements.txt "%BACKUP_ROOT%\" /Y > nul
)
echo   ✅ requirements.txtを作成しました

REM 4. スクリプト
echo [4/9] スクリプト...
mkdir "%BACKUP_ROOT%\Scripts" > nul 2>&1
copy *.bat "%BACKUP_ROOT%\Scripts\" /Y > nul 2>&1
copy *.py "%BACKUP_ROOT%\Scripts\" /Y > nul 2>&1
copy *.ps1 "%BACKUP_ROOT%\Scripts\" /Y > nul 2>&1
echo   ✅ スクリプトをバックアップしました

REM 5. Git設定
echo [5/9] Git設定...
mkdir "%BACKUP_ROOT%\Git" > nul 2>&1
if exist "C:\Users\katsunori\.gitconfig" (
    copy "C:\Users\katsunori\.gitconfig" "%BACKUP_ROOT%\Git\" /Y > nul
    echo   ✅ Git設定をバックアップしました
) else (
    echo   ⚠️ .gitconfigが見つかりません
)

REM 6. VSCode拡張機能リスト
echo [6/9] VSCode拡張機能...
code --list-extensions > "%BACKUP_ROOT%\vscode_extensions.txt" 2>nul
if exist "%BACKUP_ROOT%\vscode_extensions.txt" (
    echo   ✅ 拡張機能リストを作成しました
) else (
    echo   ⚠️ VSCodeが見つかりません
)

REM 7. フォント
echo [7/9] フォント...
dir C:\Windows\Fonts > "%BACKUP_ROOT%\installed_fonts.txt"
if exist "C:\Users\katsunori\AppData\Local\Microsoft\Windows\Fonts" (
    xcopy "C:\Users\katsunori\AppData\Local\Microsoft\Windows\Fonts" "%BACKUP_ROOT%\Fonts\" /E /I /Y /Q > nul 2>&1
)
echo   ✅ フォント情報をバックアップしました

REM 8. Android Studio設定
echo [8/9] Android Studio設定...
set as_found=0
for /d %%D in ("C:\Users\katsunori\.AndroidStudio*") do (
    xcopy "%%D\options" "%BACKUP_ROOT%\AndroidStudio\options\" /E /I /Y /Q > nul 2>&1
    set as_found=1
)
if %as_found%==1 (
    echo   ✅ Android Studio設定をバックアップしました
) else (
    echo   ⚠️ Android Studio設定が見つかりません
)

REM 9. インストール済みアプリケーションリスト
echo [9/9] インストール済みアプリケーション...
powershell -command "Get-ItemProperty HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\* | Select-Object DisplayName, DisplayVersion, Publisher | Export-Csv '%BACKUP_ROOT%\InstalledApps.csv' -NoTypeInformation" > nul 2>&1
echo   ✅ アプリケーションリストを作成しました

echo.
echo ============================================================
echo ✅ バックアップ完了
echo ============================================================
echo 📁 保存先: %BACKUP_ROOT%
echo.

REM サイズ計算
powershell -command "$size = (Get-ChildItem -Path '%BACKUP_ROOT%' -Recurse | Measure-Object -Property Length -Sum).Sum; Write-Host ('サイズ: {0:N2} MB' -f ($size / 1MB))"

echo.
echo ============================================================
echo ⚠️ セキュリティ注意事項
echo ============================================================
echo.
echo 🔐 このバックアップには以下の機密情報が含まれます:
echo    - SSH秘密鍵
echo    - APIキー
echo    - 環境変数
echo.
echo 💡 推奨される保管方法:
echo    1. 7-Zipで暗号化（パスワード保護）
echo    2. 外付けHDDに保存（物理的に安全な場所）
echo    3. クラウドには暗号化後のみアップロード
echo.
echo 暗号化しますか？ (Y/N)
set /p encrypt="選択: "

if /i "%encrypt%"=="Y" (
    echo.
    echo 🔒 7-Zipで暗号化中...
    echo    パスワードを設定してください
    
    REM 7-Zipがインストールされているか確認
    if exist "C:\Program Files\7-Zip\7z.exe" (
        "C:\Program Files\7-Zip\7z.exe" a -p -mhe=on "%BACKUP_ROOT%.7z" "%BACKUP_ROOT%\*"
        if exist "%BACKUP_ROOT%.7z" (
            echo   ✅ 暗号化完了: %BACKUP_ROOT%.7z
            echo.
            echo   元のフォルダを削除しますか？ (Y/N)
            set /p delete="選択: "
            if /i "!delete!"=="Y" (
                rmdir /s /q "%BACKUP_ROOT%"
                echo   ✅ 元のフォルダを削除しました
            )
        )
    ) else (
        echo   ❌ 7-Zipがインストールされていません
        echo      https://www.7-zip.org/ からダウンロードしてください
    )
)

echo.
echo ============================================================
echo 💡 次のステップ
echo ============================================================
echo 1. バックアップを外付けHDDにコピー
echo 2. 安全な場所に保管
echo 3. 定期的にバックアップを更新
echo.
echo 📚 詳細は CRITICAL_BACKUP_ITEMS.md を参照
echo.

REM バックアップフォルダを開く
if exist "%BACKUP_ROOT%" (
    echo バックアップフォルダを開きますか？ (Y/N)
    set /p open="選択: "
    if /i "%open%"=="Y" (
        explorer "%BACKUP_ROOT%"
    )
) else if exist "%BACKUP_ROOT%.7z" (
    echo 暗号化ファイルの場所を開きますか？ (Y/N)
    set /p open="選択: "
    if /i "%open%"=="Y" (
        explorer /select,"%BACKUP_ROOT%.7z"
    )
)

echo.
pause
