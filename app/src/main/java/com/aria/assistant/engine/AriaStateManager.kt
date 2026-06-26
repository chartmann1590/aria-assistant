package com.aria.assistant.engine

import com.aria.assistant.domain.model.AriaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AriaStateManager @Inject constructor() {
    private val _state = MutableStateFlow(AriaState.IDLE)
    val state: StateFlow<AriaState> = _state.asStateFlow()

    fun setState(state: AriaState) {
        if (_state.value != state) {
            _state.value = state
        }
    }
}
