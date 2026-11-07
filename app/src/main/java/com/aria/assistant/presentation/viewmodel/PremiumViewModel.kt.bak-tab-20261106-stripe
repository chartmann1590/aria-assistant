package com.aria.assistant.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.aria.assistant.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = billingManager.isPremium
    val formattedPrices: StateFlow<Map<String, String>> = billingManager.formattedPrices

    fun subscribe(productId: String, activity: Activity) {
        billingManager.launchBillingFlow(activity, productId)
    }

    fun restorePurchases() {
        billingManager.retryConnection()
    }
}
