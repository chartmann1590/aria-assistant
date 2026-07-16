package com.aria.assistant.ads

class InterstitialCadence(
    private val interactionsPerInterstitial: Int = 4,
    private val cooldownMillis: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private var interactionCount = 0
    private var lastShownAtMillis = -cooldownMillis

    fun recordInteraction(): Boolean {
        interactionCount++
        val dueByCount = interactionCount % interactionsPerInterstitial == 0
        val cooldownElapsed = clock() - lastShownAtMillis >= cooldownMillis
        return dueByCount && cooldownElapsed
    }

    fun markShown() {
        lastShownAtMillis = clock()
    }
}
