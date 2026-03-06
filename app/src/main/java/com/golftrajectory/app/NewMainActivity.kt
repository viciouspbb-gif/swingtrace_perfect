package com.swingtrace.aicoaching

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.golftrajectory.app.UserPreferences
import com.golftrajectory.app.billing.BillingManager
import com.golftrajectory.app.plan.UserPlanManager
import com.golftrajectory.app.plan.Plan
import com.swingtrace.aicoaching.api.*
import com.swingtrace.aicoaching.repository.SwingAnalysisRepository
import com.swingtrace.aicoaching.screens.AnalysisResultScreen
import com.swingtrace.aicoaching.screens.AuthScreen
import com.swingtrace.aicoaching.screens.AutoTrajectoryScreen
import com.swingtrace.aicoaching.screens.EnhancedCameraScreen
import com.swingtrace.aicoaching.screens.HistoryScreen
import com.golftrajectory.app.screens.ModernTopScreen
import com.golftrajectory.app.screens.SettingsScreen
import com.swingtrace.aicoaching.screens.SimpleMainScreen
import com.swingtrace.aicoaching.screens.SimpleMainScreenWithLogout
import com.swingtrace.aicoaching.screens.SwingPoseAnalysisScreen
import com.swingtrace.aicoaching.screens.VideoPreviewScreen
import com.swingtrace.aicoaching.screens.PremiumScreen
import com.swingtrace.aicoaching.screens.AICoachingScreen
import com.swingtrace.aicoaching.screens.SwingComparisonScreen
import com.swingtrace.aicoaching.screens.ProComparisonScreen
import com.golftrajectory.app.screens.SmartAIAnalysisScreen
import com.swingtrace.aicoaching.ui.theme.SwingTraceWithAICoachingTheme
import com.swingtrace.aicoaching.RealTimeTrajectoryScreen
import com.swingtrace.aicoaching.ads.AdManager
import com.golftrajectory.app.ai.GeminiAIManager
import com.golftrajectory.app.ai.CoachingStyle
import com.golftrajectory.app.ClubHeadDetector
import com.golftrajectory.app.ClubHeadTrackingScreen
import com.golftrajectory.app.ClubHeadTrackingUseCase
import com.golftrajectory.app.SimpleSwingPhaseDetector
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * 新しいMainActivity
 * シンプルなUI + 完全自動分析
 */
class NewMainActivity : ComponentActivity() {
    
    private lateinit var repository: SwingAnalysisRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var database: com.swingtrace.aicoaching.database.AppDatabase
    private lateinit var adManager: AdManager
    private lateinit var billingManager: BillingManager
    private lateinit var aiManager: GeminiAIManager
    private lateinit var userPlanManager: UserPlanManager
    
    // 権限リクエスト
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "カメラとマイクの権限が必要です", Toast.LENGTH_LONG).show()
        }
    }
    
    // 動画選択ランチャー
    private lateinit var onVideoSelected: (Uri) -> Unit
    private val videoPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // ファイルサイズチェック
            val fileSize = getFileSize(it)
            val fileSizeMB = fileSize / (1024.0 * 1024.0)
            
            if (fileSizeMB > 100) {
                Toast.makeText(
                    this,
                    "ファイルサイズが大きすぎます（${String.format("%.1f", fileSizeMB)}MB > 100MB）",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onVideoSelected(it)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 分析中は画面をオンに保つ
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // RetrofitClientを初期化（認証インターセプター用）
        RetrofitClient.initialize(this)
        
        // UserPreferencesを初期化
        userPreferences = UserPreferences(this)
        userPlanManager = UserPlanManager.getInstance(this)
        
        // データベースを初期化
        database = com.swingtrace.aicoaching.database.AppDatabase.getDatabase(this)
        
        // 権限をリクエスト（初回のみ）
        if (savedInstanceState == null) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        }
        
        repository = SwingAnalysisRepository(this)
        
        // AdManager を初期化
        adManager = AdManager(this)
        adManager.initialize()
        adManager.loadInterstitialAd()
        adManager.loadRewardedAd()
        
        // BillingManager を初期化
        billingManager = BillingManager(this)
        billingManager.initialize()
        
        // GeminiAIManager を初期化
        aiManager = GeminiAIManager(this)
        aiManager.initialize()
        
        setContent {
            SwingTraceWithAICoachingTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                
                var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
                var aiCoaching by remember { mutableStateOf<AICoachingResponse?>(null) }
                var currentVideoUri by remember { mutableStateOf<Uri?>(null) }
                var isAnalyzing by remember { mutableStateOf(false) }
                var isLoggedIn by remember { mutableStateOf(userPreferences.isLoggedIn()) }
                var isGuest by remember { mutableStateOf(userPreferences.isGuest()) }
                
                // スイング分析データ（AIコーチに渡す用）
                var currentSwingData by remember { mutableStateOf<com.swingtrace.aicoaching.domain.usecase.SwingData?>(null) }
                var currentProSimilarity by remember { mutableStateOf<com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.SimilarityResult?>(null) }
                var previousScore by remember { mutableStateOf<Int?>(null) }
                
                val planManager = remember { userPlanManager }
                val planTier by planManager.planFlow.collectAsState(initial = com.golftrajectory.app.plan.Plan.PRACTICE)
                val isPremium = planTier != com.golftrajectory.app.plan.Plan.PRACTICE
                val isProPlan = planTier == com.golftrajectory.app.plan.Plan.PRO
                
                // コーチングスタイル
                var coachingStyle by remember { 
                    mutableStateOf(
                        CoachingStyle.valueOf(
                            userPreferences.getCoachingStyle() ?: CoachingStyle.FRIENDLY.name
                        )
                    )
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn || isGuest) "home" else "auth"
                    ) {
                        // 認証画面
                        composable("auth") {
                            AuthScreen(
                                onLoginSuccess = {
                                    isLoggedIn = true
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onLogin = { email, password ->
                                    loginUser(email, password)
                                },
                                onRegister = { email, password, name ->
                                    registerUser(email, password, name)
                                },
                                onGuestMode = {
                                    userPreferences.setGuestMode()
                                    isGuest = true
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // ホーム画面（モダンデザイン）
                        composable("home") {
                            ModernTopScreen(
                                userName = userPreferences.getUserName() ?: "Guest",
                                latestScore = null,
                                planTier = planTier,
                                userPlanManager = userPlanManager,
                                onCameraClick = {
                                    navController.navigate("camera")
                                },
                                onSmartAIAnalysisClick = {
                                    navController.navigate("smart_ai")
                                },
                                onRearSwingClick = {
                                    // 後方スイング分析
                                    onVideoSelected = { uri ->
                                        currentVideoUri = uri
                                        navController.navigate("rearSwing")
                                    }
                                    videoPickerLauncher.launch("video/*")
                                },
                                onFrontSwingClick = {
                                    // 正面スイング分析
                                    if (isPremium) {
                                        onVideoSelected = { uri ->
                                            currentVideoUri = uri
                                            navController.navigate("frontSwing")
                                        }
                                        videoPickerLauncher.launch("video/*")
                                    } else {
                                        navController.navigate("premium")
                                    }
                                },
                                onClubHeadTrackingClick = {
                                    navController.navigate("club_head")
                                },
                                onAICoachClick = { 
                                    if (isPremium) {
                                        // TODO: Show AI Coach Overlay within Analysis Screen
                                        // navController.navigate("aiCoach") - DEPRECATED
                                    } else {
                                        navController.navigate("premium")
                                    }
                                },
                                onComparisonClick = { navController.navigate("comparison") },
                                onHistoryClick = {
                                    if (isPremium) {
                                        navController.navigate("history")
                                    } else {
                                        // Practiceユーザーには履歴機能を制限
                                        scope.launch {
                                            androidx.appcompat.app.AlertDialog.Builder(this@NewMainActivity)
                                                .setTitle("機能制限")
                                                .setMessage("履歴機能はアスリート版限定です")
                                                .setPositiveButton("OK", null)
                                                .setIcon(android.R.drawable.ic_lock_lock)
                                                .show()
                                        }
                                    }
                                },
                                onPremiumClick = { navController.navigate("premium") },
                                onSettingsClick = { navController.navigate("settings") },
                                onLogout = {
                                    scope.launch {
                                        logoutUser()
                                        isLoggedIn = false
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onRequestPlanSwitch = {
                                    scope.launch {
                                        val nextTier = when (planTier) {
                                            com.golftrajectory.app.plan.Plan.PRACTICE -> com.golftrajectory.app.plan.Plan.ATHLETE
                                            com.golftrajectory.app.plan.Plan.ATHLETE -> com.golftrajectory.app.plan.Plan.PRO
                                            com.golftrajectory.app.plan.Plan.PRO -> com.golftrajectory.app.plan.Plan.PRACTICE
                                        }
                                        planManager.setPlan(nextTier)
                                    }
                                },
                                onPlanBadgeLongPress = {
                                    scope.launch {
                                        val nextTier = when (planTier) {
                                            com.golftrajectory.app.plan.Plan.PRACTICE -> com.golftrajectory.app.plan.Plan.ATHLETE
                                            else -> com.golftrajectory.app.plan.Plan.PRACTICE
                                        }
                                        planManager.setPlan(nextTier)
                                        Toast.makeText(
                                            this@NewMainActivity,
                                            "Debug: Plan switched to ${nextTier.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                            )
                        }

                        // スマートAI解析画面
                        composable("smart_ai") {
                            SmartAIAnalysisScreen(
                                onStartCamera = {
                                    navController.navigate("camera")
                                },
                                onPickVideo = {
                                    onVideoSelected = { uri ->
                                        currentVideoUri = uri
                                        navController.navigate("rearSwing")
                                    }
                                    videoPickerLauncher.launch("video/*")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        // 履歴画面
                        composable("history") {
                            val historyList by database.analysisHistoryDao()
                                .getHistoryByUser(userPreferences.getUserId() ?: "guest")
                                .collectAsState(initial = emptyList())
                            
                            HistoryScreen(
                                historyList = historyList,
                                isPremium = isPremium,
                                onHistoryClick = { history ->
                                    // 履歴データから分析結果を復元してAIコーチに遷移
                                    try {
                                        // スイングデータを復元
                                        currentSwingData = com.swingtrace.aicoaching.domain.usecase.SwingData(
                                            totalScore = history.totalScore,
                                            backswingAngle = history.backswingAngle,
                                            downswingSpeed = history.downswingSpeed,
                                            hipRotation = history.hipRotation,
                                            shoulderRotation = history.shoulderRotation,
                                            headStability = history.headStability,
                                            weightShift = history.weightTransfer,
                                            swingPlane = history.swingPlane
                                        )
                                        
                                        // プロ類似度を復元（保存されている場合）
                                        if (history.topProName != null && history.topProSimilarity != null) {
                                            val pro = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.getProByName(history.topProName)
                                            if (pro != null) {
                                                currentProSimilarity = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.SimilarityResult(
                                                    pro = pro,
                                                    similarity = history.topProSimilarity,
                                                    breakdown = mapOf(
                                                        "バックスイング" to 0.0,
                                                        "ダウンスイング" to 0.0,
                                                        "腰の回転" to 0.0,
                                                        "肩の回転" to 0.0,
                                                        "頭の安定性" to 0.0,
                                                        "体重移動" to 0.0
                                                    )
                                                )
                                            }
                                        } else {
                                            // プロ類似度を再計算
                                            val similarities = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.calculateSimilarities(
                                                backswingAngle = history.backswingAngle,
                                                downswingSpeed = history.downswingSpeed,
                                                hipRotation = history.hipRotation,
                                                shoulderRotation = history.shoulderRotation,
                                                headStability = history.headStability,
                                                weightTransfer = history.weightTransfer
                                            )
                                            currentProSimilarity = similarities.firstOrNull()
                                        }
                                        
                                        previousScore = null // 履歴の前回スコアは不明
                                        
                                        // TODO: Show AI Coach Overlay within Analysis Screen
                                        // navController.navigate("aiCoach") - DEPRECATED
                                        
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            this@NewMainActivity,
                                            "データの読み込みに失敗しました: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onDeleteHistory = { history ->
                                    // 履歴を削除
                                    scope.launch {
                                        try {
                                            database.analysisHistoryDao().delete(history)
                                            Toast.makeText(
                                                this@NewMainActivity,
                                                "履歴を削除しました",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                this@NewMainActivity,
                                                "削除に失敗しました: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // プレミアム画面
                        composable("premium") {
                            PremiumScreen(
                                onBack = { navController.popBackStack() },
                                onPurchasePremium = {
                                    billingManager.launchPurchaseFlow(
                                        activity = this@NewMainActivity,
                                        productId = BillingManager.PREMIUM_MONTHLY_SKU,
                                        onSuccess = {
                                            Toast.makeText(this@NewMainActivity, "購入処理を開始しました", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(this@NewMainActivity, "購入に失敗しました: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onPurchasePro = {
                                    billingManager.launchPurchaseFlow(
                                        activity = this@NewMainActivity,
                                        productId = BillingManager.PRO_MONTHLY_SKU,
                                        onSuccess = {
                                            Toast.makeText(this@NewMainActivity, "購入処理を開始しました", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(this@NewMainActivity, "購入に失敗しました: $error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                isPremium = isPremium,
                                isProPlan = isProPlan
                            )
                        }
                        
                        // AIコーチング画面（プレミアム版のみ）
                        // DEPRECATED: To be integrated into Analysis Screen
                        composable("aiCoach") {
                            AICoachingScreen(
                                aiManager = aiManager,
                                swingData = currentSwingData,
                                proSimilarity = currentProSimilarity,
                                previousScore = previousScore,
                                planTier = planTier,
                                isPremium = isPremium,
                                userPlanManager = userPlanManager,
                                onBack = { navController.popBackStack() },
                                onUpgradeClick = { navController.navigate("premium") },
                                onCameraClick = { navController.navigate("camera") },
                                onVideoPickClick = { /* TODO: Video picker */ }
                            )
                        }
                        
                        // スイング比較画面（有料版以上）
                        composable("comparison") {
                            if (isPremium || isProPlan) {
                                val historyList by database.analysisHistoryDao()
                                    .getHistoryByUser(userPreferences.getUserId() ?: "guest")
                                    .collectAsState(initial = emptyList())
                                
                                if (historyList.isNotEmpty()) {
                                    val currentAnalysis = historyList.first()
                                    val pastAnalyses = historyList.drop(1)
                                    
                                    SwingComparisonScreen(
                                        currentAnalysis = currentAnalysis,
                                        pastAnalyses = pastAnalyses,
                                        aiManager = aiManager,
                                        onBack = { navController.popBackStack() },
                                        onSelectPastAnalysis = { /* 選択時の処理 */ },
                                        onAICoachClick = { current, past ->
                                            // 比較データを持ってAIコーチに遷移
                                            currentSwingData = com.swingtrace.aicoaching.domain.usecase.SwingData(
                                                totalScore = current.totalScore,
                                                backswingAngle = current.backswingAngle,
                                                downswingSpeed = current.downswingSpeed,
                                                hipRotation = current.hipRotation,
                                                shoulderRotation = current.shoulderRotation,
                                                headStability = current.headStability,
                                                weightShift = current.weightTransfer,
                                                swingPlane = current.swingPlane
                                            )
                                            
                                            // プロ類似度を計算
                                            val similarities = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.calculateSimilarities(
                                                backswingAngle = current.backswingAngle,
                                                downswingSpeed = current.downswingSpeed,
                                                hipRotation = current.hipRotation,
                                                shoulderRotation = current.shoulderRotation,
                                                headStability = current.headStability,
                                                weightTransfer = current.weightTransfer
                                            )
                                            currentProSimilarity = similarities.firstOrNull()
                                            previousScore = past.totalScore
                                            
                                            // TODO: Show AI Coach Overlay within Analysis Screen
                                            // navController.navigate("aiCoach") - DEPRECATED
                                        }
                                    )
                                } else {
                                    // データがない場合
                                    LaunchedEffect(Unit) {
                                        Toast.makeText(this@NewMainActivity, "比較するデータがありません", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            } else {
                                LaunchedEffect(Unit) {
                                    Toast.makeText(this@NewMainActivity, "有料プランが必要です", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                        
                        // 設定画面
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        // プロ比較画面（有料版以上）
                        composable("proComparison") {
                            if (isPremium || isProPlan) {
                                val historyList by database.analysisHistoryDao()
                                    .getHistoryByUser(userPreferences.getUserId() ?: "guest")
                                    .collectAsState(initial = emptyList())
                                
                                if (historyList.isNotEmpty()) {
                                    val currentAnalysis = historyList.first()
                                    
                                    ProComparisonScreen(
                                        currentAnalysis = currentAnalysis,
                                        onBack = { navController.popBackStack() },
                                        onSelectPro = { pro ->
                                            // プロを目標に設定
                                            Toast.makeText(
                                                this@NewMainActivity,
                                                "${pro.name}を目標に設定しました",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // TODO: 目標プロをUserPreferencesに保存
                                        }
                                    )
                                } else {
                                    // データがない場合
                                    LaunchedEffect(Unit) {
                                        Toast.makeText(this@NewMainActivity, "分析データがありません", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                }
                            } else {
                                LaunchedEffect(Unit) {
                                    Toast.makeText(this@NewMainActivity, "有料プランが必要です", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                        
                        // カメラ画面
                        composable("camera") {
                            EnhancedCameraScreen(
                                onVideoRecorded = { uri ->
                                    currentVideoUri = uri
                                    // 直接バイオメカニクス解析画面へ遷移（オンデバイス処理）
                                    navController.navigate("poseAnalysis?videoUri=${uri}")
                                },
                                onAutoAnalysisStart = { uri ->
                                    currentVideoUri = uri
                                    // 自動で解析画面へ遷移
                                    navController.navigate("swingAnalysis")
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // プレビュー画面（既存動画）
                        composable("preview") {
                            if (currentVideoUri != null) {
                                VideoPreviewScreen(
                                    videoUri = currentVideoUri!!,
                                    isAnalyzing = isAnalyzing,
                                    onAnalyze = {
                                        isAnalyzing = true
                                        scope.launch {
                                            val (result, coaching) = analyzeVideoInternal(currentVideoUri!!)
                                            analysisResult = result
                                            aiCoaching = coaching
                                            isAnalyzing = false
                                            if (result != null) {
                                                navController.navigate("result")
                                            }
                                        }
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                        
                        // 自動弾道表示画面（オフライン版）
                        composable("autoTrajectory") {
                            if (currentVideoUri != null) {
                                AutoTrajectoryScreen(
                                    videoUri = currentVideoUri!!,
                                    onBack = {
                                        navController.popBackStack("home", inclusive = false)
                                    }
                                )
                            }
                        }
                        
                        // バイオメカニクス解析画面（オンデバイス処理）
                        composable("poseAnalysis?videoUri={videoUri}") { backStackEntry ->
                            val videoUriString = backStackEntry.arguments?.getString("videoUri")
                            if (videoUriString != null) {
                                val videoUri = Uri.parse(videoUriString)
                                SwingPoseAnalysisScreen(
                                    videoUri = videoUri,
                                    analysisMode = "rear",
                                    planTier = planTier,
                                    aiManager = aiManager,
                                    userPreferences = userPreferences,
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    onAICoachClick = { result ->
                                        // TODO: Show AI Coach Overlay within Analysis Screen
                                        // navController.navigate("aiCoach") - DEPRECATED
                                    }
                                )
                            }
                        }
                        
                        // 後方スイング分析画面
                        composable("rearSwing") {
                            if (currentVideoUri != null) {
                                SwingPoseAnalysisScreen(
                                    videoUri = currentVideoUri!!,
                                    analysisMode = "rear",
                                    planTier = planTier,
                                    aiManager = aiManager,
                                    userPreferences = userPreferences,
                                    onBack = {
                                        navController.popBackStack("home", inclusive = false)
                                    },
                                    onAICoachClick = { result ->
                                        // 実際の分析結果を使用
                                        currentSwingData = com.swingtrace.aicoaching.domain.usecase.SwingData(
                                            totalScore = result.score,
                                            backswingAngle = result.backswingAngle.toDouble(),
                                            downswingSpeed = result.downswingSpeed.toDouble(),
                                            hipRotation = result.hipRotation.toDouble(),
                                            shoulderRotation = result.shoulderRotation.toDouble(),
                                            headStability = result.headStability.toDouble(),
                                            weightShift = result.weightTransfer.toDouble(),
                                            swingPlane = "正常"
                                        )
                                        
                                        // プロ類似度を計算
                                        val similarities = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.calculateSimilarities(
                                            backswingAngle = result.backswingAngle.toDouble(),
                                            downswingSpeed = result.downswingSpeed.toDouble(),
                                            hipRotation = result.hipRotation.toDouble(),
                                            shoulderRotation = result.shoulderRotation.toDouble(),
                                            headStability = result.headStability.toDouble(),
                                            weightTransfer = result.weightTransfer.toDouble()
                                        )
                                        currentProSimilarity = similarities.firstOrNull()
                                        
                                        // 前回スコアを履歴から取得
                                        scope.launch {
                                            val historyList = database.analysisHistoryDao()
                                                .getHistoryByUser(userPreferences.getUserId() ?: "guest")
                                                .first()
                                            previousScore = historyList.getOrNull(1)?.totalScore
                                        }
                                        
                                        // TODO: Show AI Coach Overlay within Analysis Screen
                                        // navController.navigate("aiCoach") - DEPRECATED
                                    }
                                )
                            }
                        }
                        
                        // 正面スイング分析画面
                        composable("frontSwing") {
                            if (currentVideoUri != null) {
                                SwingPoseAnalysisScreen(
                                    videoUri = currentVideoUri!!,
                                    analysisMode = "front",
                                    planTier = planTier,
                                    aiManager = aiManager,
                                    userPreferences = userPreferences,
                                    onBack = {
                                        navController.popBackStack("home", inclusive = false)
                                    },
                                    onAICoachClick = { result ->
                                        // 実際の分析結果を使用
                                        currentSwingData = com.swingtrace.aicoaching.domain.usecase.SwingData(
                                            totalScore = result.score,
                                            backswingAngle = result.backswingAngle.toDouble(),
                                            downswingSpeed = result.downswingSpeed.toDouble(),
                                            hipRotation = result.hipRotation.toDouble(),
                                            shoulderRotation = result.shoulderRotation.toDouble(),
                                            headStability = result.headStability.toDouble(),
                                            weightShift = result.weightTransfer.toDouble(),
                                            swingPlane = "正常"
                                        )
                                        
                                        // プロ類似度を計算
                                        val similarities = com.swingtrace.aicoaching.analysis.ProSimilarityCalculator.calculateSimilarities(
                                            backswingAngle = result.backswingAngle.toDouble(),
                                            downswingSpeed = result.downswingSpeed.toDouble(),
                                            hipRotation = result.hipRotation.toDouble(),
                                            shoulderRotation = result.shoulderRotation.toDouble(),
                                            headStability = result.headStability.toDouble(),
                                            weightTransfer = result.weightTransfer.toDouble()
                                        )
                                        currentProSimilarity = similarities.firstOrNull()
                                        
                                        // 前回スコアを履歴から取得
                                        scope.launch {
                                            val historyList = database.analysisHistoryDao()
                                                .getHistoryByUser(userPreferences.getUserId() ?: "guest")
                                                .first()
                                            previousScore = historyList.getOrNull(1)?.totalScore
                                        }
                                        
                                        // TODO: Show AI Coach Overlay within Analysis Screen
                                        // navController.navigate("aiCoach") - DEPRECATED
                                    }
                                )
                            }
                        }
                        
                        // 結果画面
                        composable("result") {
                            if (analysisResult != null && currentVideoUri != null) {
                                AnalysisResultScreen(
                                    analysisResult = analysisResult!!,
                                    aiCoaching = aiCoaching,
                                    videoUri = currentVideoUri!!,
                                    onBack = {
                                        navController.popBackStack("home", inclusive = false)
                                    },
                                    onShareResults = {
                                        shareResults(analysisResult!!, aiCoaching)
                                    }
                                )
                            }
                        }
                        
                        // クラブヘッド軌道追跡画面
                        composable("club_head_tracking") {
                            val context = LocalContext.current
                            val detector = remember {
                                runCatching { ClubHeadDetector(this@NewMainActivity) }
                                    .onFailure {
                                        Log.e(
                                            "NewMainActivity",
                                            "Failed to initialize ClubHeadDetector. clubhead_yolov8.onnx is missing?",
                                            it
                                        )
                                    }
                                    .getOrNull()
                            }
                            
                            if (detector == null) {
                                LaunchedEffect(Unit) {
                                    Toast.makeText(
                                        context,
                                        "クラブヘッド軌道解析は現在利用できません（モデルファイル未検出）。",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.popBackStack()
                                }
                            } else {
                                val phaseDetector = remember { SimpleSwingPhaseDetector() }
                                val useCase = remember {
                                    ClubHeadTrackingUseCase(detector, phaseDetector)
                                }
                                
                                ClubHeadTrackingScreen(
                                    useCase = useCase,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                                
                                DisposableEffect(Unit) {
                                    onDispose {
                                        detector.close()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun shareResults(result: AnalysisResult, coaching: AICoachingResponse?) {
        val text = buildString {
            appendLine("🏌️ SwingTrace AI分析結果")
            appendLine()
            appendLine("📊 弾道データ")
            appendLine("飛距離: ${String.format("%.1f", result.carry_distance)}m")
            appendLine("最高到達点: ${String.format("%.1f", result.max_height)}m")
            appendLine("滞空時間: ${String.format("%.2f", result.flight_time)}秒")
            
            if (coaching != null) {
                appendLine()
                appendLine("🤖 AIコーチング")
                appendLine("スコア: ${coaching.score}点")
                appendLine()
                appendLine(coaching.advice)
            }
            
            appendLine()
            appendLine("#SwingTrace #ゴルフ #AI分析")
        }
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "結果を共有"))
    }
    
    // ファイルサイズを取得
    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    // 履歴に保存
    private suspend fun saveToHistory(
        videoUri: Uri,
        analysisResult: AnalysisResult,
        aiCoaching: AICoachingResponse?
    ) {
        try {
            val userId = userPreferences.getUserId() ?: "guest"
            val swingData = analysisResult.swing_data
            
            val historyEntity = com.swingtrace.aicoaching.database.AnalysisHistoryEntity(
                userId = userId,
                videoUri = videoUri.toString(),
                timestamp = System.currentTimeMillis(),
                ballDetected = analysisResult.ball_detected,
                carryDistance = analysisResult.carry_distance,
                maxHeight = analysisResult.max_height,
                flightTime = analysisResult.flight_time,
                confidence = analysisResult.confidence,
                aiAdvice = aiCoaching?.advice,
                aiScore = aiCoaching?.score,
                swingSpeed = swingData?.swing_speed,
                backswingTime = swingData?.backswing_time,
                downswingTime = swingData?.downswing_time,
                impactSpeed = swingData?.impact_speed,
                tempo = swingData?.tempo
            )
            
            database.analysisHistoryDao().insert(historyEntity)
            println("[INFO] 履歴を保存しました: ${historyEntity.id}")
        } catch (e: Exception) {
            println("[ERROR] 履歴保存エラー: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // 動画分析の共通処理
    private suspend fun analyzeVideoInternal(uri: Uri): Pair<AnalysisResult?, AICoachingResponse?> {
        try {
            Toast.makeText(
                this@NewMainActivity,
                "AI分析中...",
                Toast.LENGTH_SHORT
            ).show()
            
            // 動画分析
            val result = repository.analyzeSwingVideo(uri)
            
            if (result.isSuccess) {
                val analysisResult = result.getOrNull()
                var aiCoaching: AICoachingResponse? = null
                
                // AIコーチング取得
                val swingData = analysisResult?.swing_data
                if (swingData != null) {
                    val userId = userPreferences.getUserId() ?: "guest_user"
                    val coachingResult = repository.getAICoaching(
                        userId = userId,
                        swingSpeed = swingData.swing_speed,
                        backswingTime = swingData.backswing_time,
                        downswingTime = swingData.downswing_time,
                        impactSpeed = swingData.impact_speed,
                        carryDistance = analysisResult?.carry_distance ?: 0.0
                    )
                    
                    if (coachingResult.isSuccess) {
                        aiCoaching = coachingResult.getOrNull()
                    }
                }
                
                // 履歴をデータベースに保存
                if (analysisResult != null) {
                    saveToHistory(uri, analysisResult, aiCoaching)
                }
                
                return Pair(analysisResult, aiCoaching)
            } else {
                Toast.makeText(
                    this@NewMainActivity,
                    "分析エラー: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return Pair(null, null)
            }
            
        } catch (e: Exception) {
            Toast.makeText(
                this@NewMainActivity,
                "エラー: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return Pair(null, null)
        }
    }
    
    // ログイン処理
    private suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val response = RetrofitClient.apiService.login(
                LoginRequest(email = email, password = password)
            )
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // トークンとユーザー情報を保存
                userPreferences.saveAuthToken(authResponse.access_token)
                userPreferences.saveUserInfo(
                    userId = authResponse.user_id,
                    email = authResponse.email,
                    name = authResponse.name
                )
                Result.success("ログイン成功")
            } else {
                Result.failure(Exception("ログインに失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 登録処理
    private suspend fun registerUser(email: String, password: String, name: String): Result<String> {
        return try {
            val response = RetrofitClient.apiService.register(
                RegisterRequest(email = email, password = password, name = name)
            )
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // トークンとユーザー情報を保存
                userPreferences.saveAuthToken(authResponse.access_token)
                userPreferences.saveUserInfo(
                    userId = authResponse.user_id,
                    email = authResponse.email,
                    name = authResponse.name
                )
                Result.success("登録成功")
            } else {
                Result.failure(Exception("登録に失敗しました"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ログアウト処理
    private suspend fun logoutUser() {
        try {
            // サーバーにログアウトリクエスト
            RetrofitClient.apiService.logout()
        } catch (e: Exception) {
            // エラーでもローカルのトークンは削除
        } finally {
            // ローカルの認証情報を削除
            userPreferences.logout()
            Toast.makeText(this, "ログアウトしました", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // BillingClient をクリーンアップ
        billingManager.endConnection()
    }
}