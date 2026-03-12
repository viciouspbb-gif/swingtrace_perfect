# Critical Items Backup Script
$datetime = Get-Date -Format "yyyyMMdd_HHmm"
$BACKUP_DIR = "C:\Users\katsunori\Desktop\CriticalBackup_$datetime"

Write-Host "============================================================"
Write-Host "Critical Items Backup"
Write-Host "============================================================"
Write-Host ""
Write-Host "WARNING: This backup contains sensitive information"
Write-Host "         Store in a secure location"
Write-Host ""
Write-Host "Backup destination: $BACKUP_DIR"
Write-Host ""

# Create backup directory
New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
New-Item -ItemType Directory -Path "$BACKUP_DIR\Settings" -Force | Out-Null

# 1. SSH keys
Write-Host "[1/7] Backing up SSH keys..."
if (Test-Path "$env:USERPROFILE\.ssh") {
    Copy-Item "$env:USERPROFILE\.ssh" "$BACKUP_DIR\ssh" -Recurse -Force
    Write-Host "  OK: SSH keys (SENSITIVE)"
} else {
    Write-Host "  SKIP: .ssh folder not found"
}

# 2. Environment variables
Write-Host "[2/7] Backing up environment variables..."
reg export "HKEY_CURRENT_USER\Environment" "$BACKUP_DIR\Settings\User_Environment.reg" /y | Out-Null
$env:PIXABAY_API_KEY | Out-File "$BACKUP_DIR\Settings\env_vars.txt" -Encoding UTF8
"PIXABAY_API_KEY=$env:PIXABAY_API_KEY" | Out-File "$BACKUP_DIR\Settings\env_vars.txt" -Encoding UTF8
"UNSPLASH_ACCESS_KEY=$env:UNSPLASH_ACCESS_KEY" | Out-File "$BACKUP_DIR\Settings\env_vars.txt" -Append -Encoding UTF8
"PEXELS_API_KEY=$env:PEXELS_API_KEY" | Out-File "$BACKUP_DIR\Settings\env_vars.txt" -Append -Encoding UTF8
Write-Host "  OK: Environment variables"

# 3. Python requirements
Write-Host "[3/7] Backing up Python requirements..."
Set-Location "C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching"
pip freeze | Out-File "$BACKUP_DIR\requirements.txt" -Encoding UTF8
Write-Host "  OK: requirements.txt"

# 4. Git config
Write-Host "[4/7] Backing up Git config..."
if (Test-Path "$env:USERPROFILE\.gitconfig") {
    Copy-Item "$env:USERPROFILE\.gitconfig" "$BACKUP_DIR\Git\" -Force
    Write-Host "  OK: .gitconfig"
} else {
    Write-Host "  SKIP: .gitconfig not found"
}

# 5. VSCode extensions
Write-Host "[5/7] Backing up VSCode extensions..."
code --list-extensions | Out-File "$BACKUP_DIR\vscode_extensions.txt" -Encoding UTF8 2>$null
if ($?) {
    Write-Host "  OK: VSCode extensions list"
} else {
    Write-Host "  SKIP: VSCode not found"
}

# 6. Fonts list
Write-Host "[6/7] Backing up fonts list..."
Get-ChildItem "C:\Windows\Fonts" | Out-File "$BACKUP_DIR\installed_fonts.txt" -Encoding UTF8
Write-Host "  OK: Fonts list"

# 7. Installed apps
Write-Host "[7/7] Backing up installed apps list..."
Get-ItemProperty HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\* | 
    Select-Object DisplayName, DisplayVersion, Publisher | 
    Export-Csv "$BACKUP_DIR\InstalledApps.csv" -NoTypeInformation -Encoding UTF8
Write-Host "  OK: Installed apps list"

Write-Host ""
Write-Host "============================================================"
Write-Host "Backup Completed!"
Write-Host "============================================================"
Write-Host ""
Write-Host "Location: $BACKUP_DIR"
Write-Host ""

# Calculate size
$size = (Get-ChildItem -Path $BACKUP_DIR -Recurse | Measure-Object -Property Length -Sum).Sum
$sizeMB = [math]::Round($size / 1MB, 2)
Write-Host "Size: $sizeMB MB"
Write-Host ""
Write-Host "============================================================"
Write-Host "SECURITY WARNING"
Write-Host "============================================================"
Write-Host ""
Write-Host "This backup contains:"
Write-Host "  - SSH private keys"
Write-Host "  - API keys"
Write-Host "  - Environment variables"
Write-Host ""
Write-Host "Recommended actions:"
Write-Host "  1. Encrypt with 7-Zip (password protection)"
Write-Host "  2. Store on external HDD (secure location)"
Write-Host "  3. Do NOT upload to cloud without encryption"
Write-Host ""

# Open backup folder
explorer $BACKUP_DIR
