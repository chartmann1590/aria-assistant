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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingManager: BillingManager
) {
    private val cadence = InterstitialCadence()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var interstitialAd: InterstitialAd? = null
    private var pendingInterstitialRequest = false
    private var adsInitialized = false
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val _showInterstitialEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showInterstitialEvents: SharedFlow<Unit> = _showInterstitialEvents.asSharedFlow()

    fun requestConsent(activity: Activity) {
        val parameters = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                updateConsentState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let {
                        AriaLogger.d("AdManager", "Consent form error: ${it.message}")
                    }
                    updateConsentState()
                }
            },
            { requestError ->
                AriaLogger.d("AdManager", "Consent update error: ${requestError.message}")
                updateConsentState()
            }
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let {
                AriaLogger.d("AdManager", "Privacy options error: ${it.message}")
            }
            updateConsentState()
        }
    }

    private fun updateConsentState() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val allowed = consentInformation.canRequestAds()
        _canRequestAds.value = allowed
        if (allowed) initializeAdsOnce()
    }

    private fun initializeAdsOnce() {
        if (adsInitialized) return
        adsInitialized = true
        MobileAds.initialize(context) {
            loadInterstitial()
        }
    }

    fun recordInteraction() {
        if (billingManager.isPremium.value || !_canRequestAds.value) return
        val due = cadence.recordInteraction()
        if (due) {
            if (interstitialAd != null) {
                _showInterstitialEvents.tryEmit(Unit)
            } else {
                pendingInterstitialRequest = true
            }
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
                    if (pendingInterstitialRequest) {
                        pendingInterstitialRequest = false
                        _showInterstitialEvents.tryEmit(Unit)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AriaLogger.d("AdManager", "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    scope.launch {
                        delay(RETRY_DELAY_MS)
                        loadInterstitial()
                    }
                }
            }
        )
    }

    private companion object {
        const val RETRY_DELAY_MS = 30_000L
    }
}
