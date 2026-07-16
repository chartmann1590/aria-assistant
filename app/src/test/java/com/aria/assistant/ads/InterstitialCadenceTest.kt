package com.aria.assistant.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialCadenceTest {

    @Test
    fun `does not fire before the interaction threshold`() {
        var now = 0L
        val cadence = InterstitialCadence(interactionsPerInterstitial = 4, cooldownMillis = 0L, clock = { now })

        assertFalse(cadence.recordInteraction()) // 1
        assertFalse(cadence.recordInteraction()) // 2
        assertFalse(cadence.recordInteraction()) // 3
    }

    @Test
    fun `fires on the Nth interaction`() {
        var now = 0L
        val cadence = InterstitialCadence(interactionsPerInterstitial = 4, cooldownMillis = 0L, clock = { now })

        repeat(3) { cadence.recordInteraction() }
        assertTrue(cadence.recordInteraction()) // 4th
    }

    @Test
    fun `fires again after another full cycle`() {
        var now = 0L
        val cadence = InterstitialCadence(interactionsPerInterstitial = 4, cooldownMillis = 0L, clock = { now })

        repeat(4) { cadence.recordInteraction() }
        repeat(3) { assertFalse(cadence.recordInteraction()) } // 5, 6, 7
        assertTrue(cadence.recordInteraction()) // 8th
    }

    @Test
    fun `does not fire again within the cooldown window even if the count is due`() {
        var now = 0L
        val cadence = InterstitialCadence(interactionsPerInterstitial = 4, cooldownMillis = 60_000L, clock = { now })

        repeat(4) { cadence.recordInteraction() }
        cadence.markShown()

        now = 30_000L // 30s later, still within the 60s cooldown
        repeat(4) { cadence.recordInteraction() }
        assertFalse(cadence.recordInteraction()) // would be due by count, but cooldown blocks it
    }

    @Test
    fun `fires again once the cooldown has elapsed`() {
        var now = 0L
        val cadence = InterstitialCadence(interactionsPerInterstitial = 4, cooldownMillis = 60_000L, clock = { now })

        repeat(4) { cadence.recordInteraction() }
        cadence.markShown()

        now = 61_000L // past the 60s cooldown
        repeat(3) { cadence.recordInteraction() }
        assertTrue(cadence.recordInteraction())
    }
}
