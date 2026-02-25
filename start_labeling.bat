@echo off
echo 🎨 labelme ラベリングツール起動
echo ====================================
echo.
echo 📋 操作方法:
echo   1. 画像が表示されます
echo   2. 右クリック → Create Rectangle
echo   3. クラブヘッドを囲む
echo   4. ラベル名: clubhead
echo   5. Ctrl+S で保存
echo   6. D キーで次の画像
echo.
echo ⏱️  目標: 30枚（15-20分）
echo.
pause

labelme dataset\images --output dataset\labels --labels clubhead
