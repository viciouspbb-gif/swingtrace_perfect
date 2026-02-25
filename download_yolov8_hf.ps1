$url = "https://huggingface.co/Ultralytics/YOLOv8/resolve/main/yolov8n.tflite"
$output = "app\src\main\assets\yolov8n.tflite"

Write-Host "YOLOv8モデルをダウンロード中（Hugging Face）..." -ForegroundColor Green
Write-Host "URL: $url"
Write-Host "保存先: $output"
Write-Host ""

try {
    Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    
    if (Test-Path $output) {
        $size = (Get-Item $output).Length / 1MB
        Write-Host ""
        Write-Host "✅ ダウンロード完了！" -ForegroundColor Green
        Write-Host "ファイルサイズ: $([math]::Round($size, 2)) MB"
        Write-Host "場所: $output"
    } else {
        Write-Host ""
        Write-Host "❌ ダウンロード失敗" -ForegroundColor Red
    }
} catch {
    Write-Host ""
    Write-Host "❌ エラー: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "手動でダウンロードしてください:" -ForegroundColor Yellow
    Write-Host $url
}
