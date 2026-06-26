package com.aria.assistant.data.source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aria.assistant.data.model.ConversationMessage

@Database(entities = [ConversationMessage::class], version = 2, exportSchema = false)
abstract class AriaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
