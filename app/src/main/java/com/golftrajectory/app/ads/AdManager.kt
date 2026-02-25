package com.swingtrace.aicoaching.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob 広告管理クラス
 */
class AdManager(private val context: Context) {
    
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    companion object {
        private const val TAG = "AdManager"
        
        // テスト広告ID（本番では変更必要）
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
    
    /**
     * AdMob 初期化
     */
    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }
    
    /**
     * インタースティシャル広告をロード
     */
    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}, onAdFailedToLoad: (String) -> Unit = {}) {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    onAdFailedToLoad(error.message)
                }
            }
        )
    }
    
    /**
     * インタースティシャル広告を表示
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit = {},
        onAdFailed: () -> Unit = {}
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed")
                    interstitialAd = null
                    onAdDismissed()
                    // 次の広告をプリロード
                    loadInterstitialAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    onAdFailed()
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.w(TAG, "Interstitial ad not ready")
            onAdFailed()
            // 広告がない場合はロードを試みる
            loadInterstitialAd()
        }
    }
    
    /**
     * リワード広告をロード
     */
    fun loadRewardedAd(onAdLoaded: () -> Unit = {}, onAdFailedToLoad: (String) -> Unit = {}) {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    onAdFailedToLoad(error.message)
                }
            }
        )
    }
    
    /**
     * リワード広告を表示
     */
    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: (Int) -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdFailed: () -> Unit = {}
    ) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    rewardedAd = null
                    onAdDismissed()
                    // 次の広告をプリロード
                    loadRewardedAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    onAdFailed()
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed")
                }
            }
            
            rewardedAd?.show(activity) { rewardItem ->
                val rewardAmount = rewardItem.amount
                Log.d(TAG, "User earned reward: $rewardAmount")
                onUserEarnedReward(rewardAmount)
            }
        } else {
            Log.w(TAG, "Rewarded ad not ready")
            onAdFailed()
            // 広告がない場合はロードを試みる
            loadRewardedAd()
        }
    }
    
    /**
     * バナー広告のAdRequest を取得
     */
    fun getBannerAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }
}
