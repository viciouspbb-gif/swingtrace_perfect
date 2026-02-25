# YOLOv8汎用モデルをダウンロード

Write-Host "🏌️ YOLOv8汎用モデルをダウンロード" -ForegroundColor Green
Write-Host "=" * 50

$url = "https://github.com/ultralytics/assets/releases/download/v8.0.0/yolov8n.onnx"
$output = "app\src\main\assets\clubhead_yolov8.onnx"

Write-Host "`n📥 ダウンロード中..."
Write-Host "   URL: $url"
Write-Host "   保存先: $output"

try {
    Invoke-WebRequest -Uri $url -OutFile $output
    
    $size = (Get-Item $output).Length / 1MB
    
    Write-Host "`n✅ ダウンロード完了！" -ForegroundColor Green
    Write-Host "   ファイルサイズ: $([math]::Round($size, 2)) MB"
    
    Write-Host "`n" + "=" * 50
    Write-Host "🎉 完了！" -ForegroundColor Green
    Write-Host "=" * 50
    
    Write-Host "`n⚠️  注意:" -ForegroundColor Yellow
    Write-Host "   このモデルはクラブヘッド専用ではありません。"
    Write-Host "   汎用物体検出モデルなので、精度は低いです。"
    Write-Host "   動作確認用として使用してください。"
    
    Write-Host "`n📋 次のステップ:"
    Write-Host "   1. .\gradlew installDebug"
    Write-Host "   2. アプリで「クラブヘッド軌道追跡」をテスト"
    Write-Host "   3. 動作確認後、専用モデルに置き換え"
    
} catch {
    Write-Host "`n❌ エラー: $_" -ForegroundColor Red
    Write-Host "`n手動ダウンロード:"
    Write-Host "   1. ブラウザで以下のURLを開く:"
    Write-Host "      $url"
    Write-Host "   2. ダウンロードしたファイルを以下に配置:"
    Write-Host "      $output"
}

Write-Host "`n"
