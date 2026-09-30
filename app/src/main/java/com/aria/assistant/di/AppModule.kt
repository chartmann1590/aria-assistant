package com.aria.assistant.di

import android.content.Context
import androidx.room.Room
import com.aria.assistant.data.repository.ConversationRepositoryImpl
import com.aria.assistant.data.repository.MemoryRepositoryImpl
import com.aria.assistant.data.repository.SettingsRepositoryImpl
import com.aria.assistant.data.source.AriaDatabase
import com.aria.assistant.data.source.ConversationDao
import com.aria.assistant.data.source.MemoryDao
import com.aria.assistant.domain.repository.ConversationRepository
import com.aria.assistant.domain.repository.MemoryRepository
import com.aria.assistant.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): AriaDatabase {
            return Room.databaseBuilder(
                context,
                AriaDatabase::class.java,
                "aria_database"
            )
                .addMigrations(AriaDatabase.MIGRATION_2_3)
                .build()
        }

        @Provides
        fun provideConversationDao(database: AriaDatabase): ConversationDao {
            return database.conversationDao()
        }

        @Provides
        fun provideMemoryDao(database: AriaDatabase): MemoryDao {
            return database.memoryDao()
        }
    }

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: MemoryRepositoryImpl): MemoryRepository
}
