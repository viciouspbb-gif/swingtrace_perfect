# 弾道アニメーション機能

## 概要
ゴルフボールが弾道に沿って動くアニメーション機能を実装しました。

## 実装内容

### 1. TrajectoryView.kt の改善
- **アニメーション進行度の管理**
  - `animateFloatAsState` を使用して0.0〜1.0の進行度を管理
  - 滞空時間に応じてアニメーション速度を自動調整（1〜8秒）
  - 再生/一時停止機能のサポート
  - 速度調整機能（0.5x〜3x）

- **段階的な弾道表示**
  - アニメーション進行度に応じて、弾道を徐々に描画
  - 既に通過した軌跡は線と点で表示

- **ボールの高度な表示**
  - 現在位置に白いゴルフボールを表示
  - **回転エフェクト**: ディンプル模様が回転（2回転/アニメーション）
  - **残像効果**: 過去3フレーム分の軌跡を薄く表示
  - **スピード線**: 前フレームからの移動を白い線で表現
  - **動的な影**: 高さに応じてサイズが変化する地面の影
  - **光のエフェクト**: 黄色の光輪を2重に表示

- **完了後のマーカー**
  - アニメーション完了後に最高到達点と着地点のマーカーを表示

### 2. MainActivity.kt の拡張
- **弾道シミュレーター画面の追加**
  - ナビゲーションに "simulator" ルートを追加
  - ホーム画面に「弾道シミュレーター」ボタンを追加

- **TrajectorySimulatorScreen の実装**
  - パラメータ入力フィールド（ボール初速、打ち出し角、スピン量）
  - クラブ別プリセットボタン（ドライバー、7番アイアン、ウェッジ）
  - **再生コントロール**:
    - リセットボタン（弾道をクリア）
    - 再生/一時停止ボタン
    - 速度調整ボタン（0.5x〜3x）
  - 計算実行ボタン
  - 結果表示カード（飛距離、最高到達点、滞空時間）
  - アニメーション付き弾道表示

## アニメーションの仕組み

### フロー
1. ユーザーが「計算実行」ボタンをタップ
2. `TrajectoryEngine` が弾道を計算
3. `trajectoryResult` が更新される
4. `LaunchedEffect` がトリガーされ、アニメーション進行度がリセット
5. `animateFloatAsState` が0から1まで徐々に値を増加
6. Canvas の再描画時に、進行度に応じた部分のみ描画
7. ボールが現在位置に表示される
8. アニメーション完了後、マーカーが表示される

### コード例
```kotlin
// アニメーション進行度の管理
val animatedProgress by animateFloatAsState(
    targetValue = if (trajectoryResult != null) 1f else 0f,
    animationSpec = tween(
        durationMillis = (trajectoryResult?.flightTime?.times(1000)?.toInt() ?: 2000).coerceIn(1500, 5000),
        easing = LinearEasing
    ),
    label = "trajectory_animation"
)

// 表示するポイント数の計算
val visiblePointCount = (points.size * animationProgress).toInt().coerceAtLeast(1)
val visiblePoints = points.take(visiblePointCount)

// ボールの現在位置
if (visiblePointCount < points.size) {
    val currentPoint = points[visiblePointCount - 1]
    val ballX = offsetX + currentPoint.x.toFloat() * scaleX
    val ballY = groundY - currentPoint.y.toFloat() * scaleY
    
    // ボールを描画
    drawCircle(
        color = Color.White,
        radius = 12f,
        center = Offset(ballX, ballY)
    )
}
```

## ビジュアル効果

### ボールの表現
- **本体**: 白い円（半径12dp）
- **ディンプル**: 3つの灰色の点が回転（半径2dp）
- **輪郭**: 青い線（2dp）
- **光のエフェクト**: 
  - 内側: 黄色の円（半径20dp、透明度40%）
  - 外側: 黄色のリング（半径16dp、透明度60%）
- **残像**: 過去3フレーム分の白い円（透明度減衰）
- **スピード線**: 前フレームからの白い線（透明度50%）
- **影**: 黒い円（地面、透明度20%、高さに応じてサイズ変化）

### 弾道の色
- 開始: 緑
- 中間: 黄色 → オレンジ
- 終了: 赤

### マーカー
- **最高到達点**: 赤い円 + 地面への破線
- **着地点**: 緑の円

## 使用技術
- **Jetpack Compose**: UI フレームワーク
- **Canvas API**: カスタム描画
- **Animation API**: `animateFloatAsState`, `tween`, `LinearEasing`
- **LaunchedEffect**: 状態変化の検知

## パフォーマンス最適化
- ポイント数を1000個に制限（`TrajectoryEngine.kt`）
- 軌跡ポイントは10個に1個のみ表示
- アニメーション時間を1.5〜5秒に制限

## 実装済み機能
- [x] アニメーション速度の調整機能（0.5x〜3x）
- [x] 一時停止・再生ボタン
- [x] スロー再生機能
- [x] ボールの回転表現（ディンプル模様の回転）
- [x] 残像効果とスピード線
- [x] 動的な影のサイズ変化

## 今後の改善案
- [ ] 複数の弾道を同時に表示（比較機能）
- [ ] 風の影響を考慮したシミュレーション
- [ ] ボールの3D回転（より高度な表現）
- [ ] 弾道の軌跡を保存・共有機能
- [ ] カメラアングルの変更（正面・側面・上空）
- [ ] 着地時のバウンド表現

## テスト方法
1. Android Studio でプロジェクトを開く
2. エミュレータまたは実機で実行
3. ホーム画面で「弾道シミュレーター」をタップ
4. プリセットボタン（ドライバー）をタップ
5. 「計算実行」ボタンをタップ
6. ボールがアニメーションで動くことを確認

## 関連ファイル
- `TrajectoryView.kt`: アニメーション表示ロジック
- `MainActivity.kt`: シミュレーター画面UI
- `TrajectoryEngine.kt`: 弾道計算エンジン
- `README.md`: プロジェクト概要
