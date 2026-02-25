package com.golftrajectory.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing 管理クラス
 */
class BillingManager(private val context: Context) {
    
    private var billingClient: BillingClient? = null
    
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium
    
    private val _isProPlan = MutableStateFlow(false)
    val isProPlan: StateFlow<Boolean> = _isProPlan
    
    companion object {
        private const val TAG = "BillingManager"
        
        // サブスクリプションID（Google Play Console で設定）
        const val PREMIUM_MONTHLY_SKU = "premium_monthly"  // 有料版
        const val PRO_MONTHLY_SKU = "pro_monthly"          // プレミアム版
    }
    
    /**
     * Billing Client 初期化
     */
    fun initialize(onReady: () -> Unit = {}) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()
        
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    queryPurchases()
                    onReady()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                // 再接続ロジックを実装可能
            }
        })
    }
    
    /**
     * 購入状態を確認
     */
    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }
    
    /**
     * 購入処理
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        var hasPremium = false
        var hasProPlan = false
        
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // 購入確認
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                
                // プラン判定
                if (purchase.products.contains(PREMIUM_MONTHLY_SKU)) {
                    hasPremium = true
                }
                if (purchase.products.contains(PRO_MONTHLY_SKU)) {
                    hasProPlan = true
                }
            }
        }
        
        _isPremium.value = hasPremium
        _isProPlan.value = hasProPlan
        
        Log.d(TAG, "Premium: $hasPremium, Pro: $hasProPlan")
    }
    
    /**
     * 購入確認
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }
    
    /**
     * サブスクリプション購入フローを開始
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken
                
                if (offerToken != null) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                    
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()
                    
                    val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
                    
                    if (result?.responseCode == BillingClient.BillingResponseCode.OK) {
                        onSuccess()
                    } else {
                        onFailure(result?.debugMessage ?: "Unknown error")
                    }
                } else {
                    onFailure("No offer token available")
                }
            } else {
                onFailure(billingResult.debugMessage)
            }
        }
    }
    
    /**
     * クリーンアップ
     */
    fun endConnection() {
        billingClient?.endConnection()
    }
}
