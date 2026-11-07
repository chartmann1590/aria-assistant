package com.aria.assistant.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.aria.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.hartmannstudios.direct.DirectBilling

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    private val _formattedPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val formattedPrices: StateFlow<Map<String, String>> = _formattedPrices.asStateFlow()

    init {
        scope.launch {
            _isPremium.value = settingsRepository.isPremiumEnabled().first()
        }
    }

    private lateinit var billingClient: BillingClient

    // Direct (GitHub / charleshartmann.com) build: Stripe Checkout through DirectBilling instead of Google Play.
    private fun startDirect() {
        DirectBilling.init(context, "aria")
        _formattedPrices.value = mapOf(MONTHLY_PRODUCT_ID to "$2.99", YEARLY_PRODUCT_ID to "$19.99")
        DirectBilling.addListener { updateDirect() }
        updateDirect()
    }
    private fun updateDirect() {
        val premium = DirectBilling.isEntitled(*PREMIUM_PRODUCT_IDS.toTypedArray())
        _isPremium.value = premium
        scope.launch { settingsRepository.setPremiumEnabled(premium) }
    }
    /** Direct builds: look up Stripe purchases by email (new phone, reinstall). */
    fun restore(activity: Activity) { if (DirectBilling.isDirect) DirectBilling.restore(activity) }
    fun manageSubscription(activity: Activity) { if (DirectBilling.isDirect) DirectBilling.manageSubscription(activity) }

    init {
        if (DirectBilling.isDirect) startDirect() else startPlay()
    }

    private fun startPlay() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                    refreshProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    fun checkExistingPurchases() {
        if (DirectBilling.isDirect) { updateDirect(); return }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any(::isEntitlingPurchase)
                _isPremium.value = hasPremium
                scope.launch { settingsRepository.setPremiumEnabled(hasPremium) }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (isEntitlingPurchase(purchase)) {
            _isPremium.value = true
            scope.launch { settingsRepository.setPremiumEnabled(true) }
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { }
            }
        }
    }

    private fun isEntitlingPurchase(purchase: Purchase): Boolean =
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.any { it in PREMIUM_PRODUCT_IDS }

    private fun refreshProductDetails() {
        PREMIUM_PRODUCT_IDS.forEach { productId ->
            queryProductDetailsAsync(productId) { details ->
                val price = details?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
                if (price != null) _formattedPrices.value += productId to price
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        if (DirectBilling.isDirect) { DirectBilling.purchase(activity, productId); return }
        queryProductDetailsAsync(productId) { productDetails ->
            if (productDetails == null) return@queryProductDetailsAsync
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return@queryProductDetailsAsync

            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                ).build()

            billingClient.launchBillingFlow(activity, params)
        }
    }

    private fun queryProductDetailsAsync(productId: String, callback: (ProductDetails?) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(params) { _, queryResult ->
            callback(queryResult.productDetailsList.firstOrNull())
        }
    }

    fun retryConnection() {
        if (DirectBilling.isDirect) { updateDirect(); return }
        if (!::billingClient.isInitialized) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                    refreshProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    companion object {
        const val MONTHLY_PRODUCT_ID = "aria_premium_monthly"
        const val YEARLY_PRODUCT_ID = "aria_premium_yearly"
        val PREMIUM_PRODUCT_IDS = setOf(MONTHLY_PRODUCT_ID, YEARLY_PRODUCT_ID)
    }
}
