# セットアップガイド

## Android Studioでプロジェクトを開く手順

### 1. Android Studioを起動

### 2. プロジェクトを開く
- **File** → **Open**
- または起動画面で **Open** をクリック

### 3. プロジェクトフォルダを選択
```
C:\Users\katsunori\CascadeProjects\GolfTrajectoryApp
```
を選択して **OK** をクリック

### 4. Gradle Syncを待つ
- 自動的にGradle Syncが開始されます
- 初回は依存関係のダウンロードに **3〜5分** かかります
- 画面下部のステータスバーで進行状況を確認できます

### 5. SDK設定（必要な場合）
もし「SDK not found」エラーが出た場合：
- **File** → **Project Structure** → **SDK Location**
- Android SDK Locationを設定（通常は自動検出されます）

### 6. エミュレータの準備

#### 新規エミュレータ作成
1. **Tools** → **Device Manager**
2. **Create Device** をクリック
3. **Phone** → **Pixel 6** を選択 → **Next**
4. **System Image**: **API 34 (Android 14)** を選択
   - ダウンロードが必要な場合は **Download** をクリック
5. **Finish** をクリック

#### 既存エミュレータを使用
- Device Managerで既存のデバイスを選択
- API Level 24以上であればOK

### 7. アプリを実行
1. 画面上部のツールバーでエミュレータを選択
2. 緑色の **▶ (Run)** ボタンをクリック
3. または **Shift + F10** を押す

### 8. ビルド完了を待つ
- 初回ビルドは **5〜10分** かかります
- 次回以降は **30秒〜2分** で起動します

## トラブルシューティング

### Gradle Sync失敗
**エラー**: `Could not resolve dependencies`

**解決策**:
1. インターネット接続を確認
2. **File** → **Invalidate Caches** → **Invalidate and Restart**
3. プロキシ設定を確認（企業ネットワークの場合）

### JDK バージョンエラー
**エラー**: `Unsupported Java version`

**解決策**:
1. **File** → **Project Structure** → **SDK Location**
2. **Gradle JDK**: JDK 17以上を選択
3. Android Studioに同梱のJDKを使用: **jbr-17** を選択

### ビルドエラー
**エラー**: `Compilation failed`

**解決策**:
```bash
# ターミナルで実行
./gradlew clean
./gradlew build
```

または Android Studio内で:
- **Build** → **Clean Project**
- **Build** → **Rebuild Project**

### エミュレータが起動しない
**解決策**:
1. **Tools** → **Device Manager**
2. エミュレータの **▼** → **Wipe Data**
3. 再度起動

### アプリがクラッシュする
**解決策**:
1. **Logcat** タブでエラーログを確認
2. エミュレータのAPI Level 24以上を確認
3. エミュレータのメモリ設定を増やす（2GB以上推奨）

## 実機でテストする場合

### Android端末の設定
1. **設定** → **デバイス情報** → **ビルド番号** を7回タップ
2. **開発者向けオプション** が表示される
3. **USBデバッグ** を有効化

### Android Studioでの設定
1. USB接続
2. 端末に「USBデバッグを許可しますか？」と表示 → **許可**
3. Android Studioのデバイス選択で実機が表示される
4. **▶ (Run)** をクリック

## 次のステップ

### アプリをカスタマイズ
- `MainActivity.kt`: UI・レイアウト変更
- `TrajectoryEngine.kt`: 物理パラメータ調整
- `TrajectoryView.kt`: 描画スタイル変更
- `res/values/strings.xml`: 文字列変更
- `res/values/colors.xml`: 色変更

### APKを生成
```bash
./gradlew assembleDebug
```
生成場所: `app/build/outputs/apk/debug/app-debug.apk`

### リリースビルド
1. **Build** → **Generate Signed Bundle / APK**
2. **APK** を選択
3. キーストアを作成（初回のみ）
4. リリースビルドを生成

## 参考リンク
- [Android Studio公式ドキュメント](https://developer.android.com/studio)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin公式サイト](https://kotlinlang.org/)
