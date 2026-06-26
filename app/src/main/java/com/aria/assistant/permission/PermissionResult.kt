package com.aria.assistant.permission

sealed interface PermissionResult {
    data object Granted : PermissionResult
    data class Denied(val permanent: Boolean) : PermissionResult
}
