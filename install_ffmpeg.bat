@echo off
echo FFmpeg インストールガイド
echo ====================================
echo.
echo 以下の手順でFFmpegをインストールしてください：
echo.
echo 1. ブラウザで以下のURLを開く：
echo    https://www.gyan.dev/ffmpeg/builds/
echo.
echo 2. 「ffmpeg-release-essentials.zip」をダウンロード
echo.
echo 3. C:\ffmpeg\ に解凍
echo.
echo 4. 環境変数Pathに追加：
echo    C:\ffmpeg\bin
echo.
echo 5. PowerShellを再起動して確認：
echo    ffmpeg -version
echo.
echo ====================================
echo.
echo または、以下のコマンドでwingetを使用：
echo winget install Gyan.FFmpeg
echo.
pause
