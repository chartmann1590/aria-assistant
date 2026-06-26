package com.aria.assistant.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.assistant.permission.PermissionManager
import com.aria.assistant.permission.PermissionResult
import com.aria.assistant.permission.PhoneCapability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _permissions = MutableStateFlow<Map<PhoneCapability, PermissionUiState>>(emptyMap())
    val permissions: StateFlow<Map<PhoneCapability, PermissionUiState>> = _permissions.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _permissions.value = PhoneCapability.entries.associateWith { cap ->
                when (permissionManager.status(cap)) {
                    is PermissionResult.Granted -> PermissionUiState.GRANTED
                    is PermissionResult.Denied -> PermissionUiState.DENIED
                }
            }
        }
    }

    fun runtimePerms(cap: PhoneCapability): List<String> = permissionManager.runtimePerms(cap)

    fun requestIntent(cap: PhoneCapability): Intent = permissionManager.requestIntent(cap)
}

enum class PermissionUiState { GRANTED, DENIED }
