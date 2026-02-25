$url = "https://github.com/ultralytics/assets/releases/download/v0.0.0/yolov8n.tflite"
$output = "app\src\main\assets\yolov8n.tflite"

Write-Host "YOLOv8モデルをダウンロード中..." -ForegroundColor Green
Write-Host "URL: $url"
Write-Host "保存先: $output"
Write-Host ""

Invoke-WebRequest -Uri $url -OutFile $output

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
