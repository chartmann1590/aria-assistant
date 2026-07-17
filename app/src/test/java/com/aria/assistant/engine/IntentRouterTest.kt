package com.aria.assistant.engine

import com.aria.assistant.agent.AgentRunner
import com.aria.assistant.domain.model.AriaIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class IntentRouterTest {

    private lateinit var router: IntentRouter

    @Before
    fun setUp() {
        router = IntentRouter(
            agentRunner = mock()
        )
    }

    @Test
    fun `set timer for 10 minutes returns SetTimer with 600 seconds`() {
        val response = """{"action":"set_timer","params":{"duration_seconds":600}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetTimer)
        assertEquals(600, (result as AriaIntent.SetTimer).durationSeconds)
    }

    @Test
    fun `timer for 30 seconds returns SetTimer with 30 seconds`() {
        val response = """{"action":"set_timer","params":{"duration_seconds":30}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetTimer)
        assertEquals(30, (result as AriaIntent.SetTimer).durationSeconds)
    }

    @Test
    fun `set a 2 hour timer returns SetTimer with 7200 seconds`() {
        val response = """{"action":"set_timer","params":{"duration_seconds":7200}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetTimer)
        assertEquals(7200, (result as AriaIntent.SetTimer).durationSeconds)
    }

    @Test
    fun `wake me up at 7 30 returns SetAlarm with hour 7 minute 30`() {
        val response = """{"action":"set_alarm","params":{"hour":7,"minute":30}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetAlarm)
        assertEquals(7, (result as AriaIntent.SetAlarm).hour)
        assertEquals(30, (result as AriaIntent.SetAlarm).minute)
    }

    @Test
    fun `set alarm for 6am returns SetAlarm with hour 6 minute 0`() {
        val response = """{"action":"set_alarm","params":{"hour":6,"minute":0,"label":"Morning alarm"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetAlarm)
        assertEquals(6, (result as AriaIntent.SetAlarm).hour)
        assertEquals(0, (result as AriaIntent.SetAlarm).minute)
    }

    @Test
    fun `call Mom returns MakeCall with contact Mom`() {
        val response = """{"action":"make_call","params":{"contact":"Mom"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MakeCall)
        assertEquals("Mom", (result as AriaIntent.MakeCall).contact)
    }

    @Test
    fun `call John Smith returns MakeCall with contact John Smith`() {
        val response = """{"action":"make_call","params":{"contact":"John Smith"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MakeCall)
        assertEquals("John Smith", (result as AriaIntent.MakeCall).contact)
    }

    @Test
    fun `text Sarah returns SendSms with contact Sarah and message`() {
        val response = """{"action":"send_sms","params":{"contact":"Sarah","message":"I'll be late"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SendSms)
        assertEquals("Sarah", (result as AriaIntent.SendSms).contact)
        assertEquals("I'll be late", (result as AriaIntent.SendSms).message)
    }

    @Test
    fun `turn down volume returns AdjustSetting with volume down`() {
        val response = """{"action":"adjust_setting","params":{"setting":"volume","value":"down"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.AdjustSetting)
        assertEquals("volume", (result as AriaIntent.AdjustSetting).setting)
        assertEquals("down", (result as AriaIntent.AdjustSetting).value)
    }

    @Test
    fun `brighten screen returns AdjustSetting with brightness up`() {
        val response = """{"action":"adjust_setting","params":{"setting":"brightness","value":"up"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.AdjustSetting)
        assertEquals("brightness", (result as AriaIntent.AdjustSetting).setting)
        assertEquals("up", (result as AriaIntent.AdjustSetting).value)
    }

    @Test
    fun `web search query returns WebSearch`() {
        val response = """{"action":"web_search","params":{"query":"weather in London"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.WebSearch)
        assertEquals("weather in London", (result as AriaIntent.WebSearch).query)
    }

    @Test
    fun `general response returns GeneralResponse`() {
        val response = "The capital of France is Paris."
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GeneralResponse)
        assertEquals(response, (result as AriaIntent.GeneralResponse).text)
    }

    @Test
    fun `malformed JSON falls back to GeneralResponse`() {
        val response = """{"action":"set_timer","params":{"duration_seconds":}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GeneralResponse)
    }

    @Test
    fun `timer response with trailing text extracts confirmation`() {
        val response = """{"action":"set_timer","params":{"duration_seconds":600}} Sure! Timer set for 10 minutes."""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetTimer)
        assertEquals(600, (result as AriaIntent.SetTimer).durationSeconds)
    }

    @Test
    fun `unknown action returns Unrecognized`() {
        val response = """{"action":"do_something_weird","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.Unrecognized)
    }

    @Test
    fun `empty response returns GeneralResponse`() {
        val result = router.resolve("")
        assertTrue(result is AriaIntent.GeneralResponse)
    }

    @Test
    fun `JSON with extra whitespace before is parsed correctly`() {
        val response = """  {"action":"set_timer","params":{"duration_seconds":300}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetTimer)
        assertEquals(300, (result as AriaIntent.SetTimer).durationSeconds)
    }

    @Test
    fun `create calendar event returns CreateCalendarEvent with title and timestamps`() {
        val response = """{"action":"create_calendar_event","params":{"title":"Dentist","start_ms":1700000000000,"end_ms":1700003600000,"location":"Clinic"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.CreateCalendarEvent)
        assertEquals("Dentist", (result as AriaIntent.CreateCalendarEvent).title)
        assertEquals(1700000000000L, (result as AriaIntent.CreateCalendarEvent).startMs)
    }

    @Test
    fun `list calendar events returns ListCalendarEvents`() {
        val response = """{"action":"list_calendar_events","params":{"from_ms":1700000000000}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ListCalendarEvents)
    }

    @Test
    fun `set reminder returns SetReminder`() {
        val response = """{"action":"set_reminder","params":{"label":"Buy groceries","minutes_before":15}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.SetReminder)
        assertEquals("Buy groceries", (result as AriaIntent.SetReminder).label)
        assertEquals(15, (result as AriaIntent.SetReminder).minutesBefore)
    }

    @Test
    fun `read notifications returns ReadNotifications`() {
        val response = """{"action":"read_notifications","params":{"filter":"messages"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReadNotifications)
        assertEquals("messages", (result as AriaIntent.ReadNotifications).filter)
    }

    @Test
    fun `reply notification returns ReplyNotification`() {
        val response = """{"action":"reply_notification","params":{"notification_key":"key123","reply_text":"On my way"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReplyNotification)
        assertEquals("key123", (result as AriaIntent.ReplyNotification).notificationKey)
        assertEquals("On my way", (result as AriaIntent.ReplyNotification).replyText)
    }

    @Test
    fun `get location returns GetLocation`() {
        val response = """{"action":"get_location","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetLocation)
    }

    @Test
    fun `navigate to returns NavigateTo`() {
        val response = """{"action":"navigate_to","params":{"place":"nearest coffee shop"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.NavigateTo)
        assertEquals("nearest coffee shop", (result as AriaIntent.NavigateTo).place)
    }

    @Test
    fun `take photo returns TakePhoto`() {
        val response = """{"action":"take_photo","params":{"label":"document"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.TakePhoto)
        assertEquals("document", (result as AriaIntent.TakePhoto).label)
    }

    @Test
    fun `get latest photo returns GetLatestPhoto`() {
        val response = """{"action":"get_latest_photo","params":{"count":3}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetLatestPhoto)
        assertEquals(3, (result as AriaIntent.GetLatestPhoto).count)
    }

    @Test
    fun `media control play returns MediaControl with play`() {
        val response = """{"action":"media_control","params":{"action":"play"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("play", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control next returns MediaControl with next`() {
        val response = """{"action":"media_control","params":{"action":"next"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("next", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `launch app returns LaunchApp`() {
        val response = """{"action":"launch_app","params":{"app":"Spotify"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.LaunchApp)
        assertEquals("Spotify", (result as AriaIntent.LaunchApp).appName)
    }

    @Test
    fun `list apps returns ListApps`() {
        val response = """{"action":"list_apps","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ListApps)
    }

    @Test
    fun `read screen returns ReadScreen`() {
        val response = """{"action":"read_screen","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReadScreen)
    }

    @Test
    fun `click on returns ClickOn`() {
        val response = """{"action":"click_on","params":{"label":"Send"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ClickOn)
        assertEquals("Send", (result as AriaIntent.ClickOn).label)
    }

    @Test
    fun `scroll returns Scroll`() {
        val response = """{"action":"scroll","params":{"direction":"down"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.Scroll)
        assertEquals("down", (result as AriaIntent.Scroll).direction)
    }

    @Test
    fun `get battery returns GetBattery`() {
        val response = """{"action":"get_battery","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetBattery)
    }

    @Test
    fun `get time returns GetTime`() {
        val response = """{"action":"get_time","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetTime)
    }

    @Test
    fun `read notifications with no filter returns filter null`() {
        val response = """{"action":"read_notifications","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReadNotifications)
    }

    @Test
    fun `read notifications with filter important`() {
        val response = """{"action":"read_notifications","params":{"filter":"important"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReadNotifications)
        assertEquals("important", (result as AriaIntent.ReadNotifications).filter)
    }

    @Test
    fun `reply notification missing reply_text falls back`() {
        val response = """{"action":"reply_notification","params":{"notification_key":"key123"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ReplyNotification)
        assertEquals("key123", (result as AriaIntent.ReplyNotification).notificationKey)
    }

    @Test
    fun `scroll up`() {
        val response = """{"action":"scroll","params":{"direction":"up"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.Scroll)
        assertEquals("up", (result as AriaIntent.Scroll).direction)
    }

    @Test
    fun `scroll left`() {
        val response = """{"action":"scroll","params":{"direction":"left"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.Scroll)
        assertEquals("left", (result as AriaIntent.Scroll).direction)
    }

    @Test
    fun `scroll right`() {
        val response = """{"action":"scroll","params":{"direction":"right"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.Scroll)
        assertEquals("right", (result as AriaIntent.Scroll).direction)
    }

    @Test
    fun `media control pause`() {
        val response = """{"action":"media_control","params":{"action":"pause"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("pause", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control play_pause`() {
        val response = """{"action":"media_control","params":{"action":"play_pause"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("play_pause", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control prev`() {
        val response = """{"action":"media_control","params":{"action":"prev"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("prev", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control previous`() {
        val response = """{"action":"media_control","params":{"action":"previous"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("previous", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control now_playing`() {
        val response = """{"action":"media_control","params":{"action":"now_playing"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("now_playing", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `media control now playing with space`() {
        val response = """{"action":"media_control","params":{"action":"now playing"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.MediaControl)
        assertEquals("now playing", (result as AriaIntent.MediaControl).action)
    }

    @Test
    fun `create calendar event without end ms`() {
        val response = """{"action":"create_calendar_event","params":{"title":"Meeting","start_ms":1700000000000}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.CreateCalendarEvent)
        assertEquals("Meeting", (result as AriaIntent.CreateCalendarEvent).title)
        assertEquals(1700000000000L, (result as AriaIntent.CreateCalendarEvent).startMs)
    }

    @Test
    fun `create calendar event without location`() {
        val response = """{"action":"create_calendar_event","params":{"title":"Party","start_ms":1700000000000,"end_ms":170003600000}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.CreateCalendarEvent)
        assertEquals("Party", (result as AriaIntent.CreateCalendarEvent).title)
    }

    @Test
    fun `list apps with filter`() {
        val response = """{"action":"list_apps","params":{"filter":"music"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.ListApps)
        assertEquals("music", (result as AriaIntent.ListApps).filter)
    }

    @Test
    fun `get latest photo without count`() {
        val response = """{"action":"get_latest_photo","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetLatestPhoto)
    }

    @Test
    fun `take photo without label`() {
        val response = """{"action":"take_photo","params":{}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.TakePhoto)
    }

    @Test
    fun `get location with query`() {
        val response = """{"action":"get_location","params":{"query":"coffee"}}"""
        val result = router.resolve(response)
        assertTrue(result is AriaIntent.GetLocation)
        assertEquals("coffee", (result as AriaIntent.GetLocation).query)
    }

    @Test
    fun `all registered tool vocabulary resolves to a typed intent`() {
        val actions = listOf(
            "read_sms" to "{\"limit\":5}",
            "dismiss_notification" to "{\"notification_key\":\"key123\"}",
            "resolve_contact" to "{\"name\":\"Mom\"}",
            "cancel_timer" to "{}",
            "cancel_alarm" to "{\"alarm_id\":\"alarm-1\"}",
            "read_last_calls" to "{\"limit\":5}",
            "answer_call" to "{}",
            "reject_call" to "{}",
            "nearby_search" to "{\"query\":\"coffee\"}",
            "reverse_geocode" to "{\"lat\":37.7,\"lng\":-122.4}",
            "get_storage" to "{}",
            "share_to_whatsapp" to "{\"contact\":\"Mom\",\"message\":\"Hi\"}",
            "share_to_telegram" to "{\"contact\":\"Mom\",\"message\":\"Hi\"}",
            "flashlight" to "{\"on\":true}",
            "clipboard_read" to "{}",
            "clipboard_write" to "{\"text\":\"hello\"}",
            "email_compose" to "{\"to\":\"a@example.com\",\"subject\":\"Hi\",\"body\":\"Hello\"}",
            "convert" to "{\"value\":100,\"from\":\"km\",\"to\":\"mi\"}"
        )

        actions.forEach { (action, params) ->
            val result = router.resolve("{\"action\":\"$action\",\"params\":$params}")
            assertTrue("$action should resolve to a typed intent", result !is AriaIntent.Unrecognized)
        }
    }
}
