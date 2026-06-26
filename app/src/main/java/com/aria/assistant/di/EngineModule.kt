package com.aria.assistant.di

import android.content.Context
import com.aria.assistant.engine.AndroidTTSFallback
import com.aria.assistant.engine.LitertLmEngine
import com.aria.assistant.engine.LlmEngine
import com.aria.assistant.engine.VoiceActivityDetector
import com.aria.assistant.engine.VoiceModelManager
import com.aria.assistant.engine.WakeWordDetector
import com.aria.assistant.engine.WhisperSTT
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindLlmEngine(impl: LitertLmEngine): LlmEngine

    companion object {
        @Provides
        @Singleton
        fun provideWakeWordDetector(
            @ApplicationContext context: Context
        ): WakeWordDetector {
            return WakeWordDetector(context)
        }

        @Provides
        @Singleton
        fun provideWhisperSTT(
            @ApplicationContext context: Context
        ): WhisperSTT {
            return WhisperSTT(context)
        }

        @Provides
        @Singleton
        fun provideVoiceActivityDetector(
            @ApplicationContext context: Context
        ): VoiceActivityDetector {
            return VoiceActivityDetector(context)
        }

        @Provides
        @Singleton
        fun provideVoiceModelManager(
            @ApplicationContext context: Context
        ): VoiceModelManager {
            return VoiceModelManager(context)
        }

        @Provides
        @Singleton
        fun provideAndroidTTSFallback(
            @ApplicationContext context: Context
        ): AndroidTTSFallback {
            return AndroidTTSFallback(context)
        }
    }
}
