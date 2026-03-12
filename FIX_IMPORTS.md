# インポート修正ガイド

## 各ファイルに追加が必要なインポート

### SwingResultScreen.kt
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
```

### SwingTraceScreen.kt
```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
```

### VideoAnalysisScreen.kt
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Error
```

### TrajectoryDatabase.kt
```kotlin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
```

### SwingTraceViewModel.kt
```kotlin
// isPremium.value → isPremium.first() に変更
// または collectAsState() で使用
```

## 一括修正スクリプト

すべてのファイルに必要なインポートを追加してください。
