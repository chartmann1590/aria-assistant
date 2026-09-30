package com.aria.assistant.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.data.model.Memory
import com.aria.assistant.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val memories: StateFlow<List<Memory>> = memoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch { memoryRepository.add(content) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { memoryRepository.delete(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { memoryRepository.deleteAll() }
    }
}
