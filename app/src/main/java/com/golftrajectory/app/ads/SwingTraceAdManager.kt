package com.golftrajectory.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 広告管理マネージャー
 */
class SwingTraceAdManager(private val context: Context) {
    
    companion object {
        // 広告ユニットID（本番環境では実際のIDに置き換え）
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // テスト用
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // テスト用
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // テスト用
    }
    
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady
    
    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady
    
    init {
        MobileAds.initialize(context)
        loadInterstitialAd()
        loadRewardedAd()
    }
    
    /**
     * インタースティシャル広告をロード
     */
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isInterstitialReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                }
            }
        )
    }
    
    /**
     * リワード広告をロード
     */
    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _isRewardedReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                }
            }
        )
    }
    
    /**
     * インタースティシャル広告を表示
     * 
     * 表示タイミング:
     * - 分析完了時（3回に1回）
     * - アプリ起動時（1日1回）
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdClosed: () -> Unit = {},
        onAdFailed: () -> Unit = {}
    ) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd()
                    onAdClosed()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    onAdFailed()
                }
            }
            
            ad.show(activity)
        } ?: run {
            onAdFailed()
            loadInterstitialAd()
        }
    }
    
    /**
     * リワード広告を表示
     * 
     * 表示タイミング:
     * - 無料枠超過時
     * - ユーザーが「広告を見て+1回」を選択
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdClosed: () -> Unit = {},
        onAdFailed: () -> Unit = {}
    ) {
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    loadRewardedAd()
                    onAdClosed()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    onAdFailed()
                }
            }
            
            ad.show(activity) { rewardItem ->
                // リワード獲得
                onRewarded()
            }
        } ?: run {
            onAdFailed()
            loadRewardedAd()
        }
    }
    
    /**
     * バナー広告のAdView を取得
     */
    fun createBannerAdView(): AdView {
        return AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)
            loadAd(AdRequest.Builder().build())
        }
    }
}

/**
 * 広告表示戦略
 */
class AdDisplayStrategy(
    private val adManager: SwingTraceAdManager
) {
    private var analysisCount = 0
    private var lastInterstitialTime = 0L
    
    companion object {
        private const val INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L // 5分
        private const val ANALYSIS_COUNT_FOR_AD = 3 // 3回に1回
    }
    
    /**
     * 分析完了時に広告を表示すべきか判定
     */
    fun shouldShowAdAfterAnalysis(): Boolean {
        analysisCount++
        
        val now = System.currentTimeMillis()
        val timeSinceLastAd = now - lastInterstitialTime
        
        return analysisCount % ANALYSIS_COUNT_FOR_AD == 0 &&
               timeSinceLastAd >= INTERSTITIAL_INTERVAL_MS
    }
    
    /**
     * 広告表示を記録
     */
    fun recordAdShown() {
        lastInterstitialTime = System.currentTimeMillis()
    }
}
