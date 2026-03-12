# 🎯 Roboflowを使ったラベル付けガイド

## 📋 目次
1. [Roboflowとは](#roboflowとは)
2. [アカウント作成](#アカウント作成)
3. [プロジェクト作成](#プロジェクト作成)
4. [画像アップロード](#画像アップロード)
5. [ラベル付け](#ラベル付け)
6. [データセットエクスポート](#データセットエクスポート)
7. [トラブルシューティング](#トラブルシューティング)

---

## 🎯 Roboflowとは

**Roboflow**は、コンピュータビジョンのデータセット管理・ラベル付け・拡張を行うWebサービスです。

### 特徴
- ✅ **無料プラン**あり（1,000枚まで）
- ✅ **ブラウザ上**でラベル付け可能
- ✅ **YOLOv8形式**で直接エクスポート
- ✅ **データ拡張**機能内蔵
- ✅ **チーム共有**可能

### 公式サイト
https://roboflow.com/

---

## 📝 アカウント作成

### ステップ1: サインアップ
1. https://roboflow.com/ にアクセス
2. 「Sign Up」をクリック
3. Googleアカウントまたはメールアドレスで登録

### ステップ2: プラン選択
- **Public Plan（無料）**: 1,000枚まで、プロジェクト公開
- **Private Plan（有料）**: プロジェクト非公開

💡 **推奨**: まずはPublic Planで試す

---

## 🆕 プロジェクト作成

### ステップ1: 新規プロジェクト
1. ダッシュボードで「Create New Project」をクリック
2. プロジェクト名: `Golf Club Head Detection`
3. プロジェクトタイプ: **Object Detection**
4. 「Create Project」をクリック

### ステップ2: クラス設定
- クラス名: `clubhead`
- 説明: Golf club head detection for swing analysis

---

## 📤 画像アップロード

### ステップ1: 画像の準備
```
downloaded_images/
├── pixabay_golf_club_head_001.jpg
├── pixabay_golf_club_head_002.jpg
├── unsplash_golf_driver_001.jpg
└── ...
```

### ステップ2: アップロード
1. プロジェクト画面で「Upload」をクリック
2. 画像をドラッグ&ドロップ
3. 「Finish Uploading」をクリック

💡 **ヒント**: 一度に50-100枚ずつアップロードすると安定

### ステップ3: 画像の確認
- アップロードされた画像が一覧表示される
- 重複画像は自動で除外される

---

## 🏷️ ラベル付け

### ステップ1: アノテーション開始
1. 「Annotate」タブをクリック
2. 画像を選択
3. ラベル付けモードに入る

### ステップ2: バウンディングボックスの描画

#### 方法1: マウスでドラッグ
1. クラブヘッドの左上をクリック
2. 右下までドラッグ
3. クラス「clubhead」を選択

#### 方法2: キーボードショートカット
- `B`: ボックスモード
- `1`: クラス1（clubhead）を選択
- `Enter`: 次の画像へ
- `Backspace`: 前の画像へ

### ステップ3: ラベル付けのコツ

#### ✅ 良いラベル
```
┌─────────────────────────────┐
│                             │
│    ┌──────┐                │
│    │ 🏌️   │ ← クラブヘッド全体を囲む
│    └──────┘                │
│                             │
└─────────────────────────────┘
```

#### ❌ 悪いラベル
```
┌─────────────────────────────┐
│                             │
│    ┌─┐                     │ ← 小さすぎる
│    └─┘                     │
│                             │
│    ┌──────────────┐         │ ← 大きすぎる
│    │              │         │
│    └──────────────┘         │
└─────────────────────────────┘
```

### ステップ4: 効率的なラベル付け

#### スマートポリゴン機能
1. クラブヘッドの輪郭をクリック
2. Roboflowが自動で境界を検出
3. 微調整して完了

#### 一括ラベル付け
- 似た画像を選択
- 「Copy Annotations」で前の画像のラベルをコピー
- 位置を微調整

### ステップ5: 進捗確認
- 左側のサイドバーで進捗を確認
- 「Annotated」: ラベル付け済み
- 「Unannotated」: 未ラベル

💡 **目標**: 1枚あたり10-30秒

---

## 📦 データセットエクスポート

### ステップ1: データセット生成
1. 「Generate」タブをクリック
2. 「Generate New Version」をクリック
3. データ拡張設定（オプション）:
   - Flip: Horizontal
   - Rotation: ±15°
   - Brightness: ±15%
   - Blur: Up to 1px
4. 「Generate」をクリック

### ステップ2: エクスポート
1. 「Export」タブをクリック
2. フォーマット: **YOLOv8**
3. 「Show Download Code」をクリック
4. 「Download ZIP to Computer」を選択
5. ZIPファイルをダウンロード

### ステップ3: ZIPファイルの展開
```
roboflow-export.zip
├── train/
│   ├── images/
│   │   ├── image1.jpg
│   │   └── ...
│   └── labels/
│       ├── image1.txt
│       └── ...
├── valid/
│   ├── images/
│   └── labels/
├── test/
│   ├── images/
│   └── labels/
└── data.yaml
```

### ステップ4: データセットの配置

#### 方法1: 手動コピー
```batch
# train/images/ の内容を dataset/images/ にコピー
# train/labels/ の内容を dataset/labels/ にコピー
```

#### 方法2: スクリプト使用
```python
# organize_dataset.py
import shutil
from pathlib import Path

def organize_roboflow_export(zip_path, output_dir):
    # ZIPを展開
    import zipfile
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall('temp_extract')
    
    # 画像とラベルをコピー
    output_dir = Path(output_dir)
    (output_dir / 'images').mkdir(parents=True, exist_ok=True)
    (output_dir / 'labels').mkdir(parents=True, exist_ok=True)
    
    # trainとvalidを統合
    for split in ['train', 'valid']:
        src_images = Path('temp_extract') / split / 'images'
        src_labels = Path('temp_extract') / split / 'labels'
        
        if src_images.exists():
            for img in src_images.glob('*.jpg'):
                shutil.copy(img, output_dir / 'images' / img.name)
        
        if src_labels.exists():
            for lbl in src_labels.glob('*.txt'):
                shutil.copy(lbl, output_dir / 'labels' / lbl.name)
    
    print(f"✅ データセットを {output_dir} に配置しました")

# 使用例
organize_roboflow_export('roboflow-export.zip', 'dataset')
```

---

## 🔧 トラブルシューティング

### 問題1: アップロードが遅い
**解決策**:
- 画像サイズを縮小（最大1920x1080推奨）
- 一度に50枚ずつアップロード
- ブラウザのキャッシュをクリア

### 問題2: ラベルがずれる
**解決策**:
- ズームイン/アウトして正確に配置
- キーボードの矢印キーで微調整
- 「Reset」で最初からやり直し

### 問題3: エクスポートが失敗する
**解決策**:
- ブラウザを再読み込み
- 別のブラウザで試す（Chrome推奨）
- サポートに問い合わせ

### 問題4: YOLOv8形式が正しくない
**解決策**:
- エクスポート時に「YOLOv8」を選択（YOLOv5ではない）
- data.yamlのパスを確認
- ラベルファイルの形式を確認（クラスID x y w h）

---

## 📊 ラベル付けの効率化

### キーボードショートカット一覧
| キー | 機能 |
|------|------|
| `B` | ボックスモード |
| `1-9` | クラス選択 |
| `Enter` | 次の画像 |
| `Backspace` | 前の画像 |
| `Delete` | ボックス削除 |
| `Ctrl+Z` | 元に戻す |
| `Ctrl+Y` | やり直し |
| `+/-` | ズームイン/アウト |

### ラベル付けの目安時間
- **簡単な画像**（クラブヘッドが明瞭）: 10秒/枚
- **普通の画像**（背景が複雑）: 20秒/枚
- **難しい画像**（ブレや隠れ）: 30秒/枚

**100枚のラベル付け**: 約30-50分

---

## 💡 ベストプラクティス

### 1. 画像の選定
- ✅ クラブヘッドが明瞭に映っている
- ✅ 様々な角度・照明条件
- ✅ 異なるクラブ種別（ドライバー、アイアン、ウェッジ）
- ❌ ブレが激しい
- ❌ クラブヘッドが隠れている

### 2. ラベルの一貫性
- クラブヘッド全体を囲む（シャフトは含めない）
- 余白は最小限に
- 全ての画像で同じ基準を適用

### 3. データの多様性
- 屋外・室内
- 晴天・曇天
- 異なる背景
- 異なるクラブ

### 4. 品質チェック
- 10枚ごとに前の画像を確認
- ラベルのずれや漏れをチェック
- 不明瞭な画像は削除

---

## 🎯 次のステップ

### ラベル付け完了後
1. ✅ YOLOv8形式でエクスポート
2. ✅ `dataset/images/` と `dataset/labels/` に配置
3. ✅ `data.yaml` のパスを確認
4. ✅ `run_auto_training.bat` を実行

### トレーニング完了後
1. ✅ `test_model.bat` でテスト
2. ✅ Logcatで信頼度を確認
3. ✅ アプリで動作確認
4. ✅ 必要に応じて追加ラベル付け

---

## 📚 参考リンク

- **Roboflow公式ドキュメント**: https://docs.roboflow.com/
- **YOLOv8ドキュメント**: https://docs.ultralytics.com/
- **Roboflowチュートリアル動画**: https://www.youtube.com/c/Roboflow

---

## 🎉 まとめ

Roboflowを使えば、**ブラウザ上で簡単にラベル付け**ができます！

**手順**:
1. 画像をアップロード
2. クラブヘッドを囲む
3. YOLOv8形式でエクスポート
4. トレーニング開始

**所要時間**: 100枚で約30-50分

**次のステップ**: `run_auto_training.bat` を実行！ 🚀
