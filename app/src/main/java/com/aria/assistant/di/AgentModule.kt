package com.aria.assistant.di

import com.aria.assistant.agent.AdjustSettingTool
import com.aria.assistant.agent.ClickOnTool
import com.aria.assistant.agent.CreateCalendarEventTool
import com.aria.assistant.agent.DismissNotificationTool
import com.aria.assistant.agent.GetBatteryTool
import com.aria.assistant.agent.GetLatestPhotoTool
import com.aria.assistant.agent.GetLocationTool
import com.aria.assistant.agent.GetTimeTool
import com.aria.assistant.agent.LaunchAppTool
import com.aria.assistant.agent.ListAppsTool
import com.aria.assistant.agent.ListCalendarEventsTool
import com.aria.assistant.agent.MakeCallTool
import com.aria.assistant.agent.MediaControlTool
import com.aria.assistant.agent.NavigateToTool
import com.aria.assistant.agent.ReadNotificationsTool
import com.aria.assistant.agent.ReadScreenTool
import com.aria.assistant.agent.ReadSmsTool
import com.aria.assistant.agent.ReplyNotificationTool
import com.aria.assistant.agent.ResolveContactTool
import com.aria.assistant.agent.ScrollTool
import com.aria.assistant.agent.SendSmsTool
import com.aria.assistant.agent.SetAlarmTool
import com.aria.assistant.agent.SetReminderTool
import com.aria.assistant.agent.SetTimerTool
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
        webSearchTool: WebSearchTool,
        listCalendarEventsTool: ListCalendarEventsTool,
        getLocationTool: GetLocationTool,
        navigateToTool: NavigateToTool,
        mediaControlTool: MediaControlTool,
        launchAppTool: LaunchAppTool,
        listAppsTool: ListAppsTool,
        getBatteryTool: GetBatteryTool,
        getTimeTool: GetTimeTool,
        readSmsTool: ReadSmsTool,
        dismissNotificationTool: DismissNotificationTool,
        resolveContactTool: ResolveContactTool,
        makeCallTool: MakeCallTool,
        sendSmsTool: SendSmsTool,
        adjustSettingTool: AdjustSettingTool,
        createCalendarEventTool: CreateCalendarEventTool,
        setReminderTool: SetReminderTool,
        readNotificationsTool: ReadNotificationsTool,
        replyNotificationTool: ReplyNotificationTool,
        takePhotoTool: TakePhotoTool,
        getLatestPhotoTool: GetLatestPhotoTool,
        readScreenTool: ReadScreenTool,
        clickOnTool: ClickOnTool,
        scrollTool: ScrollTool
    ): Set<Tool> {
        return setOf(
            setTimerTool, setAlarmTool, webSearchTool,
            listCalendarEventsTool, getLocationTool, navigateToTool,
            mediaControlTool, launchAppTool, listAppsTool,
            getBatteryTool, getTimeTool, readSmsTool,
            dismissNotificationTool, resolveContactTool,
            makeCallTool, sendSmsTool, adjustSettingTool,
            createCalendarEventTool, setReminderTool,
            readNotificationsTool, replyNotificationTool,
            takePhotoTool, getLatestPhotoTool,
            readScreenTool, clickOnTool, scrollTool
        )
    }
}
