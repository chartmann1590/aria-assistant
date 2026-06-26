package com.aria.assistant.agent

import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.skill.SkillResult

sealed interface ToolResult {
    data class Success(val payload: String) : ToolResult
    data class Failure(val reason: String) : ToolResult
    data class NeedsPermission(val capability: PhoneCapability) : ToolResult
    data class NeedsClarification(val question: String) : ToolResult
    data class Say(val text: String) : ToolResult

    companion object {
        fun <T> fromSkillResult(result: SkillResult<T>): ToolResult {
            return when (result) {
                is SkillResult.Success -> Success(result.data?.toString() ?: "")
                is SkillResult.Failure -> Failure(result.reason)
                is SkillResult.NeedsPermission -> NeedsPermission(result.capability)
                is SkillResult.NeedsClarification -> NeedsClarification(result.question)
                is SkillResult.RequiresPremium -> Say("That requires a Premium subscription.")
            }
        }
    }
}
