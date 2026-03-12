# 🔐 見落としがちな重要バックアップ項目

## 📋 目次
1. [認証情報・セキュリティ](#認証情報セキュリティ)
2. [開発環境](#開発環境)
3. [ライセンス・契約](#ライセンス契約)
4. [デザイン素材](#デザイン素材)
5. [データベース・ログ](#データベースログ)
6. [自動化スクリプト](#自動化スクリプト)
7. [ドライバ・ツール](#ドライバツール)
8. [新PC移行チェックリスト](#新pc移行チェックリスト)

---

## 🔐 認証情報・セキュリティ

### ⚠️ 最重要：SSH鍵

#### バックアップ対象
```
C:\Users\katsunori\.ssh\
├── id_rsa              # 秘密鍵（RSA）
├── id_rsa.pub          # 公開鍵（RSA）
├── id_ed25519          # 秘密鍵（Ed25519）
├── id_ed25519.pub      # 公開鍵（Ed25519）
├── config              # SSH接続設定
└── known_hosts         # 既知のホスト
```

#### バックアップ方法
```batch
# 暗号化してバックアップ（推奨）
xcopy "C:\Users\katsunori\.ssh" "C:\Backup\ssh\" /E /I /Y

# または、7-Zipで暗号化
7z a -p -mhe=on "C:\Backup\ssh_backup.7z" "C:\Users\katsunori\.ssh\*"
```

#### ⚠️ セキュリティ注意
```
❌ クラウドに平文でアップロードしない
✅ 暗号化してバックアップ
✅ 外付けHDDに保存（物理的に安全な場所）
✅ パスワード管理ツールに保存
```

#### 新PCでの復元
```batch
# .sshフォルダを復元
xcopy "E:\Backup\ssh" "C:\Users\katsunori\.ssh\" /E /I /Y

# パーミッション設定（PowerShell）
icacls "C:\Users\katsunori\.ssh\id_rsa" /inheritance:r
icacls "C:\Users\katsunori\.ssh\id_rsa" /grant:r "%USERNAME%:F"
```

---

### 🔑 2段階認証のバックアップコード

#### バックアップ対象
- Google Authenticatorのリカバリーコード
- Authyのバックアップフレーズ
- GitHubの2FAリカバリーコード
- Googleアカウントのバックアップコード
- その他重要サービスのリカバリーコード

#### バックアップ方法

**方法1: パスワード管理ツール（推奨）**
```
✅ 1Password
✅ Bitwarden
✅ LastPass
```

**方法2: 暗号化テキストファイル**
```
1. テキストファイルに記録
2. 7-Zipで暗号化
3. 外付けHDDに保存
```

**方法3: 紙に印刷**
```
✅ 物理的に安全な場所に保管
✅ 金庫やセーフティボックス
```

#### リカバリーコード取得先
```
Google: https://myaccount.google.com/security
GitHub: https://github.com/settings/security
Microsoft: https://account.microsoft.com/security
```

---

### 🔐 パスワード・認証情報

#### バックアップ対象
```
- ブラウザ保存パスワード（同期推奨）
- パスワード管理ツールのマスターパスワード
- APIキー（Pixabay, Unsplash, Pexels等）
- データベース接続情報
- サーバー接続情報
```

#### バックアップ方法
```
✅ パスワード管理ツールで一元管理
✅ ブラウザ同期機能を使用
✅ 暗号化ファイルで保存
```

---

## 💻 開発環境

### 🐍 Python仮想環境・依存関係

#### バックアップ対象
```
SwingTraceWithAICoaching/
├── requirements.txt         # 依存関係リスト
├── environment.yml          # Conda環境（使用時）
├── venv/                    # 仮想環境（オプション）
└── .python-version          # Pythonバージョン指定
```

#### バックアップ方法

**方法1: requirements.txt生成（推奨）**
```batch
# 現在の環境をエクスポート
pip freeze > requirements.txt

# バックアップ
copy requirements.txt C:\Backup\
```

**方法2: 仮想環境ごとバックアップ**
```batch
# venvフォルダをバックアップ（大容量注意）
xcopy venv C:\Backup\venv /E /I /Y
```

#### 新PCでの復元
```batch
# 仮想環境作成
python -m venv venv

# 有効化
venv\Scripts\activate

# 依存関係インストール
pip install -r requirements.txt
```

---

### 📦 Node.js・npm（使用時）

#### バックアップ対象
```
package.json
package-lock.json
node_modules/（再インストール推奨）
```

#### バックアップ方法
```batch
copy package.json C:\Backup\
copy package-lock.json C:\Backup\
```

---

### 🔧 環境変数

#### バックアップ対象
```
PIXABAY_API_KEY
UNSPLASH_ACCESS_KEY
PEXELS_API_KEY
JAVA_HOME
ANDROID_HOME
CUDA_PATH
その他カスタム環境変数
```

#### バックアップ方法
```batch
# レジストリエクスポート
reg export "HKEY_CURRENT_USER\Environment" "C:\Backup\User_Environment.reg"
reg export "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" "C:\Backup\System_Environment.reg"

# テキストファイルにも記録
echo PIXABAY_API_KEY=%PIXABAY_API_KEY% > C:\Backup\env_vars.txt
echo UNSPLASH_ACCESS_KEY=%UNSPLASH_ACCESS_KEY% >> C:\Backup\env_vars.txt
echo PEXELS_API_KEY=%PEXELS_API_KEY% >> C:\Backup\env_vars.txt
```

---

## 📄 ライセンス・契約

### 💳 ライセンスキー

#### バックアップ対象
```
- Android Studioライセンス
- JetBrains製品（IntelliJ, PyCharm等）
- Adobe製品
- Microsoft Office
- その他有料ソフトウェア
```

#### バックアップ方法
```
1. ライセンスキーをテキストファイルに記録
2. 購入確認メールを保存
3. パスワード管理ツールに保存
4. 暗号化してバックアップ
```

#### ライセンスキー確認方法
```
JetBrains: https://account.jetbrains.com/licenses
Microsoft: https://account.microsoft.com/services
```

---

### 📝 契約情報・ドキュメント

#### バックアップ対象
```
- SwingTrace利用規約ドラフト
- プライバシーポリシー
- 契約書・見積書
- 請求書・領収書
- 開発仕様書
```

#### バックアップ方法
```batch
# ドキュメントフォルダをバックアップ
xcopy "C:\Users\katsunori\Documents\SwingTrace" "C:\Backup\Documents\SwingTrace\" /E /I /Y
```

---

## 🎨 デザイン素材

### 🖼️ フォント

#### バックアップ対象
```
C:\Windows\Fonts\
または
C:\Users\katsunori\AppData\Local\Microsoft\Windows\Fonts\
```

#### 特に重要なフォント
```
- 商用利用可能なフォント
- SwingTraceのUIで使用しているフォント
- ロゴやアイコンで使用しているフォント
```

#### バックアップ方法
```batch
# インストール済みフォントをリスト化
dir C:\Windows\Fonts > C:\Backup\installed_fonts.txt

# カスタムフォントをバックアップ
xcopy "C:\Users\katsunori\AppData\Local\Microsoft\Windows\Fonts" "C:\Backup\Fonts\" /E /I /Y
```

---

### 🎨 デザイン素材

#### バックアップ対象
```
- アプリアイコン（.png, .svg）
- ロゴファイル（.ai, .psd, .svg）
- UIモックアップ（.fig, .sketch）
- スクリーンショット
- プロモーション素材
```

#### バックアップ方法
```batch
# プロジェクト内のデザイン素材
xcopy "C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching\app\src\main\res" "C:\Backup\Design\res\" /E /I /Y

# その他のデザインファイル
xcopy "C:\Users\katsunori\Documents\SwingTrace\Design" "C:\Backup\Design\" /E /I /Y
```

---

## 💾 データベース・ログ

### 🗄️ ローカルデータベース

#### バックアップ対象
```
- SQLiteファイル（*.db, *.sqlite）
- Realmファイル（*.realm）
- ローカルキャッシュ
```

#### バックアップ方法
```batch
# Androidアプリのデータベース（エミュレータ）
adb pull /data/data/com.golftrajectory.app/databases C:\Backup\Databases\

# プロジェクト内のデータベース
copy *.db C:\Backup\Databases\
copy *.sqlite C:\Backup\Databases\
```

---

### 📊 ログファイル

#### バックアップ対象
```
- トレーニングログ
- アプリのクラッシュログ
- デバッグログ
- パフォーマンスログ
```

#### バックアップ方法
```batch
# トレーニングログ
xcopy "runs\detect\clubhead_high_confidence\*.csv" "C:\Backup\Logs\" /Y
xcopy "runs\detect\clubhead_high_confidence\*.png" "C:\Backup\Logs\" /Y

# Androidログ
adb logcat -d > C:\Backup\Logs\logcat.txt
```

---

## 🤖 自動化スクリプト

### 📜 バッチファイル・スクリプト

#### バックアップ対象
```
SwingTraceWithAICoaching/
├── *.bat                    # バッチファイル
├── *.ps1                    # PowerShellスクリプト
├── *.sh                     # シェルスクリプト
├── *.py                     # Pythonスクリプト
└── scripts/                 # スクリプトフォルダ
```

#### 特に重要なスクリプト
```
✅ backup.bat
✅ complete_backup.bat
✅ setup_training_env.bat
✅ run_auto_training.bat
✅ test_model.bat
✅ download_images.bat
✅ auto_augment_and_train.py
✅ test_model.py
```

#### バックアップ方法
```batch
# すべてのスクリプトをバックアップ
copy *.bat C:\Backup\Scripts\
copy *.ps1 C:\Backup\Scripts\
copy *.py C:\Backup\Scripts\
```

---

### 🔧 ビルド・デプロイスクリプト

#### バックアップ対象
```
- Gradleビルドスクリプト
- CI/CD設定（GitHub Actions等）
- デプロイスクリプト
- テスト自動化スクリプト
```

#### バックアップ方法
```batch
# Gradleスクリプト
copy build.gradle.kts C:\Backup\Build\
copy settings.gradle.kts C:\Backup\Build\
copy gradle.properties C:\Backup\Build\

# CI/CD設定
xcopy ".github\workflows" "C:\Backup\Build\workflows\" /E /I /Y
```

---

## 🔌 ドライバ・ツール

### 📱 Androidエミュレータ設定

#### バックアップ対象
```
C:\Users\katsunori\.android\avd\
```

#### バックアップ方法
```batch
# AVD設定をバックアップ
xcopy "C:\Users\katsunori\.android\avd" "C:\Backup\AVD\" /E /I /Y
```

---

### 🎮 GPU設定・ドライバ

#### バックアップ対象
```
- NVIDIAコントロールパネル設定
- GPU監視ツール設定
- オーバークロック設定（使用時）
```

#### バックアップ方法
```
1. NVIDIAコントロールパネルのスクリーンショット
2. 設定値をテキストで記録
3. ドライバーバージョンを記録
```

---

### 🔧 特殊なツール

#### バックアップ対象
```
- FFmpeg（動画処理）
- ImageMagick（画像処理）
- ADB（Android Debug Bridge）
- カスタムビルドツール
```

#### バックアップ方法
```
1. インストーラーをバックアップ
2. バージョン情報を記録
3. 設定ファイルをバックアップ
```

---

## ✅ 新PC移行チェックリスト

### 🔧 システム設定

#### Windows設定
```
✅ 開発者モードをONにする
   設定 → 更新とセキュリティ → 開発者向け → 開発者モード

✅ ファイル拡張子を表示
   エクスプローラー → 表示 → ファイル名拡張子

✅ 隠しファイルを表示
   エクスプローラー → 表示 → 隠しファイル

✅ 長いパスを有効化
   レジストリ: HKLM\SYSTEM\CurrentControlSet\Control\FileSystem
   LongPathsEnabled = 1
```

---

### 🐍 Python環境

#### インストール確認
```batch
# Pythonバージョン確認（3.10系推奨）
python --version

# pipバージョン確認
pip --version

# 仮想環境作成
python -m venv venv

# 依存関係インストール
pip install -r requirements.txt
```

---

### 🔧 Git設定

#### 設定確認
```batch
# ユーザー名設定
git config --global user.name "Your Name"

# メールアドレス設定
git config --global user.email "your.email@example.com"

# デフォルトブランチ名
git config --global init.defaultBranch main

# 改行コード設定（Windows）
git config --global core.autocrlf true

# 確認
git config --list
```

---

### 🎨 VSCode拡張機能

#### 推奨拡張機能
```
✅ Kotlin Language
✅ Python
✅ Pylance
✅ GitLens
✅ Android iOS Emulator
✅ Markdown All in One
✅ Material Icon Theme
```

#### 一括インストール
```batch
code --install-extension fwcd.kotlin
code --install-extension ms-python.python
code --install-extension ms-python.vscode-pylance
code --install-extension eamodio.gitlens
code --install-extension diemauersbr.vscode-android-ios-emulator
code --install-extension yzhang.markdown-all-in-one
code --install-extension PKief.material-icon-theme
```

---

### 📱 Android Studio設定

#### 確認項目
```
✅ Android SDK インストール
✅ Android SDK Platform-Tools
✅ Android Emulator
✅ Gradle設定
✅ JDK設定（JDK 17推奨）
```

#### SDK確認
```
Tools → SDK Manager → SDK Platforms
✅ Android 14.0 (API 34)
✅ Android 13.0 (API 33)
```

---

### 🚀 SwingTrace動作確認

#### チェック項目
```batch
# 1. プロジェクトビルド
cd app
gradlew assembleDebug

# 2. モデル読み込み確認
python -c "from ultralytics import YOLO; model = YOLO('best.pt'); print('✅ モデル読み込み成功')"

# 3. GPU確認
python gpu_check.py

# 4. トレーニングテスト（10エポック）
python -c "from auto_augment_and_train import *; trainer = YOLOv8Trainer('dataset'); trainer.train_model(epochs=10, batch=16, device='0')"

# 5. アプリ起動確認
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.golftrajectory.app/.MainActivity
```

---

### 📊 ログ出力確認

#### 確認コマンド
```batch
# Androidログ
adb logcat | findstr "SwingTrace"

# Pythonログ
python test_model.py
```

---

## 📋 バックアップ実行スクリプト

### critical_backup.bat

```batch
@echo off
echo ============================================================
echo 🔐 重要項目バックアップ
echo ============================================================
echo.

set BACKUP_ROOT=C:\Backup\Critical_%date:~0,4%%date:~5,2%%date:~8,2%
mkdir "%BACKUP_ROOT%"

REM SSH鍵
echo [1/7] SSH鍵...
xcopy "C:\Users\katsunori\.ssh" "%BACKUP_ROOT%\ssh\" /E /I /Y /Q > nul
echo   ✅ SSH鍵をバックアップしました

REM 環境変数
echo [2/7] 環境変数...
reg export "HKEY_CURRENT_USER\Environment" "%BACKUP_ROOT%\User_Environment.reg" /y > nul
echo   ✅ 環境変数をバックアップしました

REM Python依存関係
echo [3/7] Python依存関係...
cd C:\Users\katsunori\CascadeProjects\SwingTraceWithAICoaching
pip freeze > "%BACKUP_ROOT%\requirements.txt"
echo   ✅ requirements.txtを作成しました

REM スクリプト
echo [4/7] スクリプト...
copy *.bat "%BACKUP_ROOT%\Scripts\" /Y > nul
copy *.py "%BACKUP_ROOT%\Scripts\" /Y > nul
echo   ✅ スクリプトをバックアップしました

REM Git設定
echo [5/7] Git設定...
copy "C:\Users\katsunori\.gitconfig" "%BACKUP_ROOT%\Git\" /Y > nul
echo   ✅ Git設定をバックアップしました

REM VSCode拡張機能リスト
echo [6/7] VSCode拡張機能...
code --list-extensions > "%BACKUP_ROOT%\vscode_extensions.txt"
echo   ✅ 拡張機能リストを作成しました

REM フォント
echo [7/7] フォント...
dir C:\Windows\Fonts > "%BACKUP_ROOT%\installed_fonts.txt"
echo   ✅ フォントリストを作成しました

echo.
echo ✅ 重要項目のバックアップ完了
echo 📁 保存先: %BACKUP_ROOT%
pause
```

---

## 🎯 まとめ

### 最重要バックアップ項目
1. ✅ **SSH鍵** - 暗号化して保存
2. ✅ **2段階認証コード** - パスワード管理ツール
3. ✅ **環境変数** - レジストリエクスポート
4. ✅ **requirements.txt** - 依存関係
5. ✅ **ライセンスキー** - 暗号化して保存

### バックアップ頻度
- **SSH鍵・認証情報**: 変更時
- **開発環境**: 週1回
- **ライセンス**: 取得時
- **スクリプト**: 変更時

**見落としがちな項目も完璧にバックアップ！** 🔐✨
