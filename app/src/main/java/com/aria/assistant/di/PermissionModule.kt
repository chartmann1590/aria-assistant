package com.aria.assistant.di

import com.aria.assistant.permission.PermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: android.content.Context
    ): PermissionManager {
        return PermissionManager(context)
    }
}
