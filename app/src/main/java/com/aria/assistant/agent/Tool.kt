package com.aria.assistant.agent

import org.json.JSONObject

interface Tool {
    val name: String
    val description: String
    val paramSchema: String
    val requiresPremium: Boolean

    suspend fun execute(params: JSONObject): ToolResult
}
