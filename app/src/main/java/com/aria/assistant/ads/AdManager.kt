package com.aria.assistant.ads

import android.app.Activity
import android.content.Context
import com.aria.assistant.BuildConfig
import com.aria.assistant.billing.BillingManager
import com.aria.assistant.engine.AriaLogger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingManager: BillingManager
) {
    private val cadence = InterstitialCadence()
    private var interstitialAd: InterstitialAd? = null

    private val _showInterstitialEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialEvents: SharedFlow<Unit> = _showInterstitialEvents.asSharedFlow()

    init {
        MobileAds.initialize(context)
        loadInterstitial()
    }

    fun recordInteraction() {
        if (billingManager.isPremium.value) return
        val due = cadence.recordInteraction()
        if (due && interstitialAd != null) {
            _showInterstitialEvents.tryEmit(Unit)
        }
    }

    fun showInterstitial(activity: Activity) {
        val ad = interstitialAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AriaLogger.d("AdManager", "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                loadInterstitial()
            }
        }
        cadence.markShown()
        ad.show(activity)
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AriaLogger.d("AdManager", "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }
}
