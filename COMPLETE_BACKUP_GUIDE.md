# 💾 完全バックアップガイド（PC全体）

## 📋 目次
1. [バックアップ対象](#バックアップ対象)
2. [プロジェクトデータ](#プロジェクトデータ)
3. [アプリケーションデータ](#アプリケーションデータ)
4. [メール・連絡先](#メール連絡先)
5. [ブラウザデータ](#ブラウザデータ)
6. [システム設定](#システム設定)
7. [バックアップ実行](#バックアップ実行)
8. [新PCへの移行](#新pcへの移行)

---

## 📦 バックアップ対象

### 🎯 プロジェクト関連
- ✅ SwingTraceプロジェクト
- ✅ データセット・モデル
- ✅ ソースコード

### 📧 メール・連絡先
- ✅ Outlookデータ
- ✅ Thunderbirdデータ
- ✅ Gmailアカウント設定
- ✅ 連絡先

### 🌐 ブラウザデータ
- ✅ ブックマーク
- ✅ パスワード
- ✅ 拡張機能
- ✅ 閲覧履歴

### 💼 アプリケーションデータ
- ✅ Android Studioの設定
- ✅ Visual Studio Codeの設定
- ✅ IDEの設定・プラグイン
- ✅ 開発環境の設定

### 📁 個人データ
- ✅ デスクトップ
- ✅ ドキュメント
- ✅ ピクチャ
- ✅ ダウンロード

---

## 🎯 プロジェクトデータ

### SwingTraceプロジェクト

#### バックアップ対象
```
C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching\
├── dataset/                 # データセット
├── runs/                    # トレーニング結果
├── app/                     # アプリコード
├── *.py                     # Pythonスクリプト
├── *.bat                    # バッチファイル
└── *.md                     # ドキュメント
```

#### バックアップ方法
```batch
# 既存のスクリプトを使用
backup.bat
```

---

## 📧 メール・連絡先

### Microsoft Outlook

#### データの場所
```
C:\Users\katsunori\Documents\Outlookファイル\
または
C:\Users\katsunori\AppData\Local\Microsoft\Outlook\
```

#### バックアップ対象
- `*.pst` - メールデータ
- `*.ost` - オフラインデータ
- 連絡先
- カレンダー
- タスク

#### バックアップ方法

**方法1: Outlookからエクスポート**
```
1. Outlook起動
2. ファイル → 開く/エクスポート → インポート/エクスポート
3. 「ファイルにエクスポート」を選択
4. 「Outlookデータファイル(.pst)」を選択
5. エクスポート先を指定
6. 保存
```

**方法2: 手動コピー**
```batch
# PSTファイルをバックアップ
xcopy "C:\Users\katsunori\Documents\Outlookファイル\*.pst" "C:\Backup\Outlook\" /Y
xcopy "C:\Users\katsunori\AppData\Local\Microsoft\Outlook\*.pst" "C:\Backup\Outlook\" /Y
```

#### 新PCでの復元
```
1. Outlook起動
2. ファイル → 開く/エクスポート → Outlookデータファイルを開く
3. バックアップしたPSTファイルを選択
4. インポート
```

---

### Gmail（Webメール）

#### バックアップ方法

**Google Takeout**
```
1. https://takeout.google.com/ にアクセス
2. 「選択を解除」をクリック
3. 「メール」のみチェック
4. 「次のステップ」
5. エクスポート形式: MBOX
6. 「エクスポートを作成」
7. ダウンロード
```

#### 含まれるデータ
- すべてのメール
- ラベル
- フィルタ設定

---

### Thunderbird

#### データの場所
```
C:\Users\katsunori\AppData\Roaming\Thunderbird\Profiles\
```

#### バックアップ方法
```batch
# プロファイルフォルダ全体をバックアップ
xcopy "C:\Users\katsunori\AppData\Roaming\Thunderbird" "C:\Backup\Thunderbird\" /E /I /Y
```

#### 新PCでの復元
```
1. Thunderbirdをインストール
2. Thunderbirdを終了
3. バックアップしたProfilesフォルダを上書き
4. Thunderbird起動
```

---

## 🌐 ブラウザデータ

### Google Chrome

#### データの場所
```
C:\Users\katsunori\AppData\Local\Google\Chrome\User Data\
```

#### バックアップ対象
- ブックマーク
- パスワード（暗号化済み）
- 拡張機能
- 閲覧履歴
- Cookie

#### バックアップ方法

**方法1: Googleアカウント同期（推奨）**
```
1. Chrome設定 → 同期とGoogleサービス
2. 「同期を有効にする」
3. 同期する項目を選択
4. 新PCでログインすれば自動復元
```

**方法2: 手動バックアップ**
```batch
# User Dataフォルダをバックアップ
xcopy "C:\Users\katsunori\AppData\Local\Google\Chrome\User Data" "C:\Backup\Chrome\" /E /I /Y
```

**方法3: ブックマークのみ**
```
1. Chrome → ブックマーク → ブックマークマネージャ
2. 右上の「︙」→ ブックマークをエクスポート
3. HTMLファイルで保存
```

---

### Microsoft Edge

#### バックアップ方法

**Microsoftアカウント同期（推奨）**
```
1. Edge設定 → プロファイル → 同期
2. 「同期を有効にする」
3. 新PCでログインすれば自動復元
```

---

### Firefox

#### データの場所
```
C:\Users\katsunori\AppData\Roaming\Mozilla\Firefox\Profiles\
```

#### バックアップ方法
```batch
# プロファイルフォルダをバックアップ
xcopy "C:\Users\katsunori\AppData\Roaming\Mozilla\Firefox\Profiles" "C:\Backup\Firefox\" /E /I /Y
```

---

## 💼 アプリケーションデータ

### Android Studio

#### 設定の場所
```
C:\Users\katsunori\.AndroidStudio<バージョン>\
C:\Users\katsunori\.android\
C:\Users\katsunori\.gradle\
```

#### バックアップ対象
- IDE設定
- キーマップ
- カラースキーム
- プラグイン
- SDKの場所

#### バックアップ方法

**方法1: 設定のエクスポート**
```
1. Android Studio → File → Manage IDE Settings → Export Settings
2. エクスポート先を指定
3. 保存
```

**方法2: 手動バックアップ**
```batch
xcopy "C:\Users\katsunori\.AndroidStudio*" "C:\Backup\AndroidStudio\" /E /I /Y
xcopy "C:\Users\katsunori\.android" "C:\Backup\android\" /E /I /Y
```

#### 新PCでの復元
```
1. Android Studioをインストール
2. File → Manage IDE Settings → Import Settings
3. バックアップファイルを選択
```

---

### Visual Studio Code

#### 設定の場所
```
C:\Users\katsunori\AppData\Roaming\Code\User\
```

#### バックアップ対象
- settings.json
- keybindings.json
- 拡張機能リスト
- スニペット

#### バックアップ方法

**方法1: Settings Sync（推奨）**
```
1. VSCode → 設定アイコン → Settings Sync is On
2. GitHubまたはMicrosoftアカウントでログイン
3. 新PCでログインすれば自動復元
```

**方法2: 手動バックアップ**
```batch
xcopy "C:\Users\katsunori\AppData\Roaming\Code\User" "C:\Backup\VSCode\" /E /I /Y
```

**方法3: 拡張機能リストのエクスポート**
```batch
# インストール済み拡張機能をリスト化
code --list-extensions > extensions.txt

# 新PCで一括インストール
for /F "tokens=*" %A in (extensions.txt) do code --install-extension %A
```

---

### Git設定

#### 設定の場所
```
C:\Users\katsunori\.gitconfig
C:\Users\katsunori\.ssh\
```

#### バックアップ方法
```batch
# Git設定
copy "C:\Users\katsunori\.gitconfig" "C:\Backup\Git\"

# SSHキー
xcopy "C:\Users\katsunori\.ssh" "C:\Backup\Git\ssh\" /E /I /Y
```

---

## 📁 個人データ

### Windows標準フォルダ

#### バックアップ対象
```
C:\Users\katsunori\Desktop\        # デスクトップ
C:\Users\katsunori\Documents\      # ドキュメント
C:\Users\katsunori\Pictures\       # ピクチャ
C:\Users\katsunori\Downloads\      # ダウンロード
C:\Users\katsunori\Videos\         # ビデオ
C:\Users\katsunori\Music\          # ミュージック
```

#### バックアップ方法
```batch
# すべてバックアップ
xcopy "C:\Users\katsunori\Desktop" "C:\Backup\Desktop\" /E /I /Y
xcopy "C:\Users\katsunori\Documents" "C:\Backup\Documents\" /E /I /Y
xcopy "C:\Users\katsunori\Pictures" "C:\Backup\Pictures\" /E /I /Y
xcopy "C:\Users\katsunori\Downloads" "C:\Backup\Downloads\" /E /I /Y
```

---

## 🔧 システム設定

### 環境変数

#### バックアップ方法
```batch
# システム環境変数をエクスポート
reg export "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "C:\Backup\System_Environment.reg"

# ユーザー環境変数をエクスポート
reg export "HKEY_CURRENT_USER\Environment" "C:\Backup\User_Environment.reg"
```

#### 新PCでの復元
```batch
# レジストリファイルをダブルクリックしてインポート
```

---

### インストール済みアプリケーションリスト

#### リスト作成
```powershell
# PowerShellで実行
Get-ItemProperty HKLM:\Software\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\* | Select-Object DisplayName, DisplayVersion, Publisher, InstallDate | Export-Csv "C:\Backup\InstalledApps.csv" -NoTypeInformation
```

---

## 🚀 バックアップ実行

### 完全バックアップスクリプト

#### complete_backup.bat を作成
```batch
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

REM バックアップ先
set BACKUP_ROOT=D:\CompleteBackup_%datetime%
mkdir "%BACKUP_ROOT%"

echo 📁 バックアップ先: %BACKUP_ROOT%
echo.
echo ============================================================
echo 📦 バックアップ開始
echo ============================================================
echo.

REM 1. プロジェクトデータ
echo [1/8] プロジェクトデータ...
if exist "C:\Users\katsunori\CascadeProjects" (
    xcopy "C:\Users\katsunori\CascadeProjects" "%BACKUP_ROOT%\Projects\" /E /I /Y /Q > nul
    echo   ✅ プロジェクトをバックアップしました
)

REM 2. Outlookデータ
echo [2/8] Outlookデータ...
if exist "C:\Users\katsunori\Documents\Outlookファイル" (
    xcopy "C:\Users\katsunori\Documents\Outlookファイル" "%BACKUP_ROOT%\Outlook\" /E /I /Y /Q > nul
    echo   ✅ Outlookをバックアップしました
)
if exist "C:\Users\katsunori\AppData\Local\Microsoft\Outlook" (
    xcopy "C:\Users\katsunori\AppData\Local\Microsoft\Outlook\*.pst" "%BACKUP_ROOT%\Outlook\" /Y /Q > nul 2>&1
)

REM 3. ブラウザデータ
echo [3/8] ブラウザデータ...
if exist "C:\Users\katsunori\AppData\Local\Google\Chrome\User Data" (
    xcopy "C:\Users\katsunori\AppData\Local\Google\Chrome\User Data" "%BACKUP_ROOT%\Chrome\" /E /I /Y /Q > nul
    echo   ✅ Chromeをバックアップしました
)

REM 4. Android Studio設定
echo [4/8] Android Studio設定...
if exist "C:\Users\katsunori\.AndroidStudio*" (
    xcopy "C:\Users\katsunori\.AndroidStudio*" "%BACKUP_ROOT%\AndroidStudio\" /E /I /Y /Q > nul
    echo   ✅ Android Studioをバックアップしました
)

REM 5. VSCode設定
echo [5/8] VSCode設定...
if exist "C:\Users\katsunori\AppData\Roaming\Code\User" (
    xcopy "C:\Users\katsunori\AppData\Roaming\Code\User" "%BACKUP_ROOT%\VSCode\" /E /I /Y /Q > nul
    echo   ✅ VSCodeをバックアップしました
)

REM 6. Git設定
echo [6/8] Git設定...
if exist "C:\Users\katsunori\.gitconfig" (
    copy "C:\Users\katsunori\.gitconfig" "%BACKUP_ROOT%\Git\" /Y > nul
)
if exist "C:\Users\katsunori\.ssh" (
    xcopy "C:\Users\katsunori\.ssh" "%BACKUP_ROOT%\Git\ssh\" /E /I /Y /Q > nul
    echo   ✅ Git設定をバックアップしました
)

REM 7. 個人データ
echo [7/8] 個人データ...
xcopy "C:\Users\katsunori\Desktop" "%BACKUP_ROOT%\Desktop\" /E /I /Y /Q > nul
xcopy "C:\Users\katsunori\Documents" "%BACKUP_ROOT%\Documents\" /E /I /Y /Q > nul
xcopy "C:\Users\katsunori\Pictures" "%BACKUP_ROOT%\Pictures\" /E /I /Y /Q > nul
echo   ✅ 個人データをバックアップしました

REM 8. システム設定
echo [8/8] システム設定...
reg export "HKEY_CURRENT_USER\Environment" "%BACKUP_ROOT%\Settings\User_Environment.reg" /y > nul 2>&1
echo   ✅ システム設定をバックアップしました

echo.
echo ============================================================
echo ✅ バックアップ完了
echo ============================================================
echo 📁 保存先: %BACKUP_ROOT%
echo.
echo 💡 次のステップ:
echo 1. 外付けHDDにコピー
echo 2. クラウドにアップロード
echo.
pause
```

---

## 📊 バックアップサイズ目安

| カテゴリ | サイズ |
|---------|--------|
| プロジェクト | 0.5-2GB |
| Outlook | 1-10GB |
| ブラウザ | 0.5-2GB |
| Android Studio | 0.5-1GB |
| VSCode | 0.1-0.5GB |
| 個人データ | 10-100GB |
| **合計** | **15-120GB** |

---

## 💡 推奨バックアップ先

### 1. 外付けHDD/SSD（必須）
```
✅ 大容量（1TB以上推奨）
✅ 物理的に別の場所に保管
✅ 定期的にバックアップ
```

### 2. クラウドストレージ（推奨）
```
✅ Google Drive（15GB無料、2TB 1,300円/月）
✅ OneDrive（5GB無料、1TB 1,284円/月）
✅ Dropbox（2GB無料、2TB 1,500円/月）
```

### 3. NAS（オプション）
```
✅ 自動バックアップ
✅ 複数PC対応
✅ 初期費用: 3-10万円
```

---

## 🎯 まとめ

### 必須バックアップ
1. ✅ プロジェクトデータ
2. ✅ メール（Outlook/Gmail）
3. ✅ ブラウザ（ブックマーク・パスワード）
4. ✅ 個人データ（デスクトップ・ドキュメント）

### 推奨バックアップ
5. ✅ Android Studio設定
6. ✅ VSCode設定
7. ✅ Git設定
8. ✅ システム設定

### バックアップ頻度
- **プロジェクト**: 毎日 or トレーニング後
- **メール**: 週1回
- **個人データ**: 週1回
- **設定**: 月1回

**定期的なバックアップで安心！** 💾
