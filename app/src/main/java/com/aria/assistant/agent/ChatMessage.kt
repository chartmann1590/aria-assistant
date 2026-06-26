package com.aria.assistant.agent

data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role { SYSTEM, USER, MODEL, TOOL }
}
