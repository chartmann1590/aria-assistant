package com.aria.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single durable fact Aria remembers about the user, stored entirely
 * on-device. Created by the extract_memory tool after a user turn; visible
 * and deletable from Settings -> Memory.
 */
@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The fact, phrased as a standalone statement: "Mom's phone number is 555-0100." */
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
