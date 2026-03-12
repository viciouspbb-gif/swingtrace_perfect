# SwingTrace Backup Script
$datetime = Get-Date -Format "yyyyMMdd_HHmm"
$BACKUP_DIR = "C:\Users\katsunori\Desktop\SwingTraceBackup_$datetime"

Write-Host "============================================================"
Write-Host "Backup Starting..."
Write-Host "============================================================"
Write-Host ""
Write-Host "Backup destination: $BACKUP_DIR"
Write-Host ""

# Create backup directory
New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null

# Backup dataset
Write-Host "[1/6] Backing up dataset..."
if (Test-Path "dataset") {
    Copy-Item "dataset" "$BACKUP_DIR\dataset" -Recurse -Force
    Write-Host "  OK: dataset/"
}

# Backup trained model
Write-Host "[2/6] Backing up trained model..."
if (Test-Path "runs\detect\clubhead_high_confidence\weights") {
    Copy-Item "runs\detect\clubhead_high_confidence\weights" "$BACKUP_DIR\weights" -Recurse -Force
    Write-Host "  OK: Model weights"
}

# Backup ONNX models
Write-Host "[3/6] Backing up ONNX models..."
if (Test-Path "best.onnx") {
    Copy-Item "best.onnx" "$BACKUP_DIR\" -Force
}
if (Test-Path "app\src\main\assets\clubhead_yolov8.onnx") {
    Copy-Item "app\src\main\assets\clubhead_yolov8.onnx" "$BACKUP_DIR\" -Force
}
Write-Host "  OK: ONNX models"

# Backup app code
Write-Host "[4/6] Backing up app code..."
if (Test-Path "app") {
    Copy-Item "app" "$BACKUP_DIR\app" -Recurse -Force
    Write-Host "  OK: app/"
}

# Backup Python scripts
Write-Host "[5/6] Backing up Python scripts..."
Get-ChildItem "*.py" | Copy-Item -Destination "$BACKUP_DIR\" -Force
Get-ChildItem "*.bat" | Copy-Item -Destination "$BACKUP_DIR\" -Force
Write-Host "  OK: Scripts"

# Backup documentation
Write-Host "[6/6] Backing up documentation..."
Get-ChildItem "*.md" | Copy-Item -Destination "$BACKUP_DIR\" -Force
Write-Host "  OK: Documentation"

Write-Host ""
Write-Host "============================================================"
Write-Host "Backup Completed!"
Write-Host "============================================================"
Write-Host ""
Write-Host "Location: $BACKUP_DIR"
Write-Host ""

# Calculate size
$size = (Get-ChildItem -Path $BACKUP_DIR -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeGB = [math]::Round($size / 1GB, 2)
Write-Host "Size: $sizeGB GB"
Write-Host ""
Write-Host "============================================================"
Write-Host "Next Steps:"
Write-Host "============================================================"
Write-Host "1. Copy to external HDD"
Write-Host "2. Upload to cloud (recommended)"
Write-Host ""

# Open backup folder
explorer $BACKUP_DIR
