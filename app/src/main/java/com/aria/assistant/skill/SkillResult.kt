package com.aria.assistant.skill

import com.aria.assistant.permission.PhoneCapability

sealed interface SkillResult<out T> {
    data class Success<T>(val data: T) : SkillResult<T>
    data class Failure(val reason: String) : SkillResult<Nothing>
    data class NeedsPermission(val capability: PhoneCapability) : SkillResult<Nothing>
    data class NeedsClarification(val question: String) : SkillResult<Nothing>
    data class RequiresPremium(val action: String) : SkillResult<Nothing>
}
