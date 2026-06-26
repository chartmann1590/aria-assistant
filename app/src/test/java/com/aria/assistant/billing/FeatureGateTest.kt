package com.aria.assistant.billing

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FeatureGateTest {

    private lateinit var billingManager: BillingManager
    private lateinit var featureGate: FeatureGate

    @Before
    fun setUp() {
        billingManager = mock()
        whenever(billingManager.isPremium).doReturn(MutableStateFlow(false))
        featureGate = FeatureGate(billingManager)
    }

    @Test
    fun `unknown tool is allowed`() {
        assertTrue(featureGate.isAllowed("non_existent_tool"))
    }

    @Test
    fun `free tool is allowed without premium`() {
        assertTrue(featureGate.isAllowed("set_timer"))
        assertTrue(featureGate.isAllowed("web_search"))
        assertTrue(featureGate.isAllowed("get_location"))
        assertTrue(featureGate.isAllowed("launch_app"))
    }

    @Test
    fun `premium tool is denied without premium`() {
        assertFalse(featureGate.isAllowed("make_call"))
        assertFalse(featureGate.isAllowed("send_sms"))
        assertFalse(featureGate.isAllowed("adjust_setting"))
        assertFalse(featureGate.isAllowed("create_calendar_event"))
        assertFalse(featureGate.isAllowed("read_notifications"))
        assertFalse(featureGate.isAllowed("reply_notification"))
        assertFalse(featureGate.isAllowed("take_photo"))
        assertFalse(featureGate.isAllowed("get_latest_photo"))
        assertFalse(featureGate.isAllowed("read_screen"))
        assertFalse(featureGate.isAllowed("click_on"))
        assertFalse(featureGate.isAllowed("scroll"))
    }

    @Test
    fun `premium tool is allowed with premium`() {
        whenever(billingManager.isPremium).doReturn(MutableStateFlow(true))
        featureGate = FeatureGate(billingManager)

        assertTrue(featureGate.isAllowed("make_call"))
        assertTrue(featureGate.isAllowed("send_sms"))
        assertTrue(featureGate.isAllowed("read_screen"))
    }

    @Test
    fun `feature enum toggles correctly`() {
        assertFalse(featureGate.isAllowed(Feature.PHONE_CALLS))
        assertFalse(featureGate.isAllowed(Feature.SETTINGS_CONTROL))
        assertFalse(featureGate.isAllowed(Feature.PREMIUM_VOICES))
        assertFalse(featureGate.isAllowed(Feature.GEMMA_E4B))
        assertFalse(featureGate.isAllowed(Feature.SCREEN_CONTROL))

        assertTrue(featureGate.isAllowed(Feature.TIMER_ALARM))
        assertTrue(featureGate.isAllowed(Feature.WEB_SEARCH))
        assertTrue(featureGate.isAllowed(Feature.GENERAL_QA))
    }

    @Test
    fun `feature with premium grants all`() {
        whenever(billingManager.isPremium).doReturn(MutableStateFlow(true))
        featureGate = FeatureGate(billingManager)

        assertTrue(featureGate.isAllowed(Feature.PHONE_CALLS))
        assertTrue(featureGate.isAllowed(Feature.SETTINGS_CONTROL))
        assertTrue(featureGate.isAllowed(Feature.PREMIUM_VOICES))
        assertTrue(featureGate.isAllowed(Feature.GEMMA_E4B))
    }

    @Test
    fun `featureForTool maps correctly`() {
        assertEquals(Feature.TIMER_ALARM, Feature.featureForTool("set_timer"))
        assertEquals(Feature.TIMER_ALARM, Feature.featureForTool("set_alarm"))
        assertEquals(Feature.WEB_SEARCH, Feature.featureForTool("web_search"))
        assertEquals(Feature.PHONE_CALLS, Feature.featureForTool("make_call"))
        assertEquals(Feature.SMS_SEND, Feature.featureForTool("send_sms"))
        assertEquals(Feature.SETTINGS_CONTROL, Feature.featureForTool("adjust_setting"))
        assertEquals(Feature.CAMERA, Feature.featureForTool("take_photo"))
        assertEquals(Feature.SCREEN_CONTROL, Feature.featureForTool("read_screen"))
        assertEquals(Feature.SCREEN_CONTROL, Feature.featureForTool("click_on"))
        assertEquals(Feature.NOTIFICATIONS_READ_REPLY, Feature.featureForTool("read_notifications"))
        assertEquals(Feature.NOTIFICATIONS_READ_REPLY, Feature.featureForTool("reply_notification"))
        assertEquals(Feature.CALENDAR_WRITE, Feature.featureForTool("create_calendar_event"))
        assertEquals(Feature.CALENDAR_WRITE, Feature.featureForTool("set_reminder"))
        assertEquals(Feature.CALENDAR_READ, Feature.featureForTool("list_calendar_events"))
        assertEquals(Feature.LOCATION, Feature.featureForTool("get_location"))
        assertEquals(Feature.MEDIA_CONTROL, Feature.featureForTool("media_control"))
        assertEquals(Feature.APP_LAUNCH, Feature.featureForTool("launch_app"))
        assertEquals(Feature.DEVICE_INFO, Feature.featureForTool("get_battery"))
        assertEquals(Feature.SMS_READ, Feature.featureForTool("read_sms"))
        assertEquals(Feature.NOTIFICATION_DISMISS, Feature.featureForTool("dismiss_notification"))
        assertEquals(Feature.CONTACT_RESOLVE, Feature.featureForTool("resolve_contact"))
    }

    @Test
    fun `premiumFeatures lists only premium features`() {
        val all = featureGate.premiumFeatures
        assertTrue(all.contains(Feature.PHONE_CALLS))
        assertTrue(all.contains(Feature.SMS_SEND))
        assertTrue(all.contains(Feature.SETTINGS_CONTROL))
        assertTrue(all.contains(Feature.CALENDAR_WRITE))
        assertTrue(all.contains(Feature.NOTIFICATIONS_READ_REPLY))
        assertTrue(all.contains(Feature.CAMERA))
        assertTrue(all.contains(Feature.SCREEN_CONTROL))
        assertTrue(all.contains(Feature.PREMIUM_VOICES))
        assertTrue(all.contains(Feature.GEMMA_E4B))
        assertFalse(all.contains(Feature.TIMER_ALARM))
        assertFalse(all.contains(Feature.WEB_SEARCH))
    }

    @Test
    fun `isPremium reflects billingManager`() {
        assertFalse(featureGate.isPremium.value)

        whenever(billingManager.isPremium).doReturn(MutableStateFlow(true))
        featureGate = FeatureGate(billingManager)
        assertTrue(featureGate.isPremium.value)
    }
}
