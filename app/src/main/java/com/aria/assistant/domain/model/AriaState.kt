package com.aria.assistant.domain.model

enum class AriaState {
    DOWNLOADING,
    WAKING_UP,
    INITIALIZING,
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    MUTED,
    ERROR
}
