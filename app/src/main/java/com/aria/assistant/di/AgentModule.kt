package com.aria.assistant.di

import com.aria.assistant.agent.AdjustSettingTool
import com.aria.assistant.agent.AnswerCallTool
import com.aria.assistant.agent.CancelAlarmTool
import com.aria.assistant.agent.CancelTimerTool
import com.aria.assistant.agent.ClickOnTool
import com.aria.assistant.agent.ClipboardReadTool
import com.aria.assistant.agent.ClipboardWriteTool
import com.aria.assistant.agent.ConvertTool
import com.aria.assistant.agent.CreateCalendarEventTool
import com.aria.assistant.agent.DismissNotificationTool
import com.aria.assistant.agent.EmailComposeTool
import com.aria.assistant.agent.FlashlightTool
import com.aria.assistant.agent.GetBatteryTool
import com.aria.assistant.agent.GetLatestPhotoTool
import com.aria.assistant.agent.GetLocationTool
import com.aria.assistant.agent.GetStorageTool
import com.aria.assistant.agent.GetTimeTool
import com.aria.assistant.agent.LaunchAppTool
import com.aria.assistant.agent.ListAppsTool
import com.aria.assistant.agent.ListCalendarEventsTool
import com.aria.assistant.agent.MakeCallTool
import com.aria.assistant.agent.MediaControlTool
import com.aria.assistant.agent.NavigateToTool
import com.aria.assistant.agent.NearbySearchTool
import com.aria.assistant.agent.ReadLastCallsTool
import com.aria.assistant.agent.ReadNotificationsTool
import com.aria.assistant.agent.ReadScreenTool
import com.aria.assistant.agent.ReadSmsTool
import com.aria.assistant.agent.RejectCallTool
import com.aria.assistant.agent.ReplyNotificationTool
import com.aria.assistant.agent.ResolveContactTool
import com.aria.assistant.agent.ReverseGeocodeTool
import com.aria.assistant.agent.ScrollTool
import com.aria.assistant.agent.SendSmsTool
import com.aria.assistant.agent.SetAlarmTool
import com.aria.assistant.agent.SetReminderTool
import com.aria.assistant.agent.SetTimerTool
import com.aria.assistant.agent.ShareToTelegramTool
import com.aria.assistant.agent.ShareToWhatsAppTool
import com.aria.assistant.agent.TakePhotoTool
import com.aria.assistant.agent.Tool
import com.aria.assistant.agent.WebSearchTool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideToolCatalog(
        setTimerTool: SetTimerTool,
        setAlarmTool: SetAlarmTool,
        cancelTimerTool: CancelTimerTool,
        cancelAlarmTool: CancelAlarmTool,
        webSearchTool: WebSearchTool,
        listCalendarEventsTool: ListCalendarEventsTool,
        getLocationTool: GetLocationTool,
        navigateToTool: NavigateToTool,
        nearbySearchTool: NearbySearchTool,
        reverseGeocodeTool: ReverseGeocodeTool,
        mediaControlTool: MediaControlTool,
        launchAppTool: LaunchAppTool,
        listAppsTool: ListAppsTool,
        getBatteryTool: GetBatteryTool,
        getTimeTool: GetTimeTool,
        getStorageTool: GetStorageTool,
        readSmsTool: ReadSmsTool,
        dismissNotificationTool: DismissNotificationTool,
        resolveContactTool: ResolveContactTool,
        makeCallTool: MakeCallTool,
        readLastCallsTool: ReadLastCallsTool,
        answerCallTool: AnswerCallTool,
        rejectCallTool: RejectCallTool,
        sendSmsTool: SendSmsTool,
        shareToWhatsAppTool: ShareToWhatsAppTool,
        shareToTelegramTool: ShareToTelegramTool,
        adjustSettingTool: AdjustSettingTool,
        createCalendarEventTool: CreateCalendarEventTool,
        setReminderTool: SetReminderTool,
        readNotificationsTool: ReadNotificationsTool,
        replyNotificationTool: ReplyNotificationTool,
        takePhotoTool: TakePhotoTool,
        getLatestPhotoTool: GetLatestPhotoTool,
        readScreenTool: ReadScreenTool,
        clickOnTool: ClickOnTool,
        scrollTool: ScrollTool,
        flashlightTool: FlashlightTool,
        clipboardReadTool: ClipboardReadTool,
        clipboardWriteTool: ClipboardWriteTool,
        emailComposeTool: EmailComposeTool,
        convertTool: ConvertTool
    ): Set<Tool> {
        return setOf(
            setTimerTool, setAlarmTool, cancelTimerTool, cancelAlarmTool,
            webSearchTool,
            listCalendarEventsTool, getLocationTool, navigateToTool,
            nearbySearchTool, reverseGeocodeTool,
            mediaControlTool, launchAppTool, listAppsTool,
            getBatteryTool, getTimeTool, getStorageTool,
            readSmsTool, dismissNotificationTool, resolveContactTool,
            makeCallTool, readLastCallsTool, answerCallTool, rejectCallTool,
            sendSmsTool, shareToWhatsAppTool, shareToTelegramTool,
            adjustSettingTool, createCalendarEventTool, setReminderTool,
            readNotificationsTool, replyNotificationTool,
            takePhotoTool, getLatestPhotoTool,
            readScreenTool, clickOnTool, scrollTool,
            flashlightTool, clipboardReadTool, clipboardWriteTool,
            emailComposeTool, convertTool
        )
    }
}
