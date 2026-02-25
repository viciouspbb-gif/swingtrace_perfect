@echo off
REM 動画から画像を抽出するスクリプト

echo 🎬 動画から画像を抽出
echo ====================================
echo.

REM videos フォルダの確認
if not exist "videos" (
    echo ❌ videos フォルダが見つかりません
    echo    videos\ フォルダに動画を配置してください
    pause
    exit /b 1
)

REM 動画ファイルの確認
set VIDEO_COUNT=0
for %%f in (videos\*.mp4) do set /a VIDEO_COUNT+=1

if %VIDEO_COUNT%==0 (
    echo ❌ videos\ フォルダに動画がありません
    echo.
    echo 以下の手順で動画を準備してください：
    echo 1. スマホでスイング動画を撮影
    echo 2. PCに転送
    echo 3. videos\ フォルダに配置
    echo.
    pause
    exit /b 1
)

echo ✅ 動画ファイル: %VIDEO_COUNT% 個
echo.

REM dataset/images フォルダ作成
if not exist "dataset\images" mkdir dataset\images

REM 各動画から画像を抽出
echo 📸 画像抽出中...
echo.

for %%f in (videos\*.mp4) do (
    echo 処理中: %%~nf.mp4
    ffmpeg -i "%%f" -vf fps=5 "dataset\images\%%~nf_%%04d.jpg" -loglevel error
    echo ✅ 完了
    echo.
)

REM 抽出された画像数をカウント
set IMAGE_COUNT=0
for %%f in (dataset\images\*.jpg) do set /a IMAGE_COUNT+=1

echo ====================================
echo 🎉 画像抽出完了！
echo ====================================
echo.
echo 📊 結果:
echo    動画数: %VIDEO_COUNT%
echo    画像数: %IMAGE_COUNT%
echo.
echo 📁 画像保存先: dataset\images\
echo.
echo 📋 次のステップ:
echo    1. pip install labelImg
echo    2. labelImg dataset\images dataset\labels
echo    3. 各画像でクラブヘッドを囲む
echo.
pause
