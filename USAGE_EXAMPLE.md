# 使用例

## 1. ViewModelの初期化

```kotlin
// MainActivity.kt または DI Container

val detector = ClubHeadDetector(context)

val viewModel = SwingTraceViewModel(
    detectClubHeadUseCase = DetectClubHeadUseCase(detector),
    recordTrajectoryUseCase = RecordTrajectoryUseCase(),
    classifySwingPhaseUseCase = ClassifySwingPhaseUseCase(
        geminiApiKey = BuildConfig.GEMINI_API_KEY
    ),
    drawTrajectoryUseCase = DrawTrajectoryUseCase()
)
```

## 2. 画面表示

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSwingTrace = {
                    navController.navigate("swing_trace")
                }
            )
        }
        
        composable("swing_trace") {
            SwingTraceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

## 3. アニメーション描画

```kotlin
@Composable
fun SwingResultScreen(
    pathPoints: List<Pair<Float, Float>>,
    phaseColors: List<Color>
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    Column {
        // アニメーション付き軌道
        AnimatedSwingPathCanvas(
            pathPoints = pathPoints,
            phaseColors = phaseColors,
            showImpactMarker = true,
            showScore = true,
            score = 85.5f
        )
        
        // 再生ボタン
        Button(onClick = { isPlaying = !isPlaying }) {
            Text(if (isPlaying) "停止" else "再生")
        }
        
        // 再生制御付きCanvas
        if (isPlaying) {
            PlayableSwingPathCanvas(
                pathPoints = pathPoints,
                phaseColors = phaseColors,
                isPlaying = isPlaying,
                onPlaybackComplete = { isPlaying = false }
            )
        }
    }
}
```

## 4. Gemini分類の使用

```kotlin
// ローカル分類（高速）
viewModel.stopRecording(useGemini = false)

// Gemini分類（高精度）
viewModel.stopRecording(useGemini = true)
```

## 5. データフロー

```
カメラフレーム
    ↓
YOLOv8検出（ONNX）
    ↓
ClubHeadDetection
    ↓
RecordTrajectoryUseCase
    ↓
FrameData List
    ↓
ClassifySwingPhaseUseCase
    ├─ ローカル判定（dx/dy）
    └─ Gemini分類（AI）
    ↓
SwingPhase List
    ↓
DrawTrajectoryUseCase
    ↓
DrawablePoint List
    ↓
AnimatedSwingPathCanvas
    ↓
画面表示
```

## 6. カスタマイズ

### 色の変更

```kotlin
// DrawTrajectoryUseCase.kt
private fun phaseToColor(phase: ClassifySwingPhaseUseCase.SwingPhase): Color {
    return when (phase) {
        SwingPhase.TAKEBACK -> Color(0xFF2196F3)   // カスタムブルー
        SwingPhase.DOWNSWING -> Color(0xFFF44336)  // カスタムレッド
        SwingPhase.FOLLOW -> Color(0xFF4CAF50)     // カスタムグリーン
    }
}
```

### アニメーション速度

```kotlin
AnimatedSwingPathCanvas(
    pathPoints = pathPoints,
    phaseColors = phaseColors,
    animationDuration = 2000  // 2秒
)
```

### 信頼度閾値

```kotlin
// ClubHeadDetector.kt
companion object {
    private const val CONFIDENCE_THRESHOLD = 0.7f  // 0.6 → 0.7
}
```

## 7. トラブルシューティング

### ONNXモデルが読み込めない
```kotlin
// assets/clubhead_yolov8.onnx が存在するか確認
val modelExists = context.assets.list("")?.contains("clubhead_yolov8.onnx")
Log.d("Model", "Exists: $modelExists")
```

### Gemini APIエラー
```kotlin
// フォールバックでローカル判定を使用
classifySwingPhaseUseCase.classifyWithGemini(trajectory)
    .getOrElse {
        // エラー時はローカル判定
        classifySwingPhaseUseCase.classifyLocally(trajectory)
    }
```

### 検出精度が低い
```kotlin
// 1. 照明を改善
// 2. カメラ解像度を上げる
// 3. クラブヘッドにマーカーを貼る
// 4. モデルを再トレーニング
```
