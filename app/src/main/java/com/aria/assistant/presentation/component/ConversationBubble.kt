package com.aria.assistant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.aria.assistant.translation.TranslatedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aria.assistant.data.model.ConversationMessage
import com.aria.assistant.domain.model.SearchResultItem
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import org.json.JSONObject
import com.aria.assistant.web.WebVerificationMetadata

private val userBubbleShape = RoundedCornerShape(
    topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp
)
private val ariaBubbleShape = RoundedCornerShape(
    topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp
)

@Composable
fun ConversationBubble(
    message: ConversationMessage,
    onReportResponse: (ConversationMessage) -> Unit = {},
    onEdit: (ConversationMessage) -> Unit = {},
    onRetry: (ConversationMessage) -> Unit = {},
) {
    val isUser = message.role == "user"
    val hasCards = message.metadata != null
    val clipboard = LocalClipboardManager.current
    var actionMenuExpanded by remember(message.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = { actionMenuExpanded = true },
                    onLongClick = { actionMenuExpanded = true },
                ),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            DropdownMenu(
                expanded = actionMenuExpanded,
                onDismissRequest = { actionMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                        actionMenuExpanded = false
                    },
                )
                if (isUser) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit(message)
                            actionMenuExpanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Retry") },
                    onClick = {
                        onRetry(message)
                        actionMenuExpanded = false
                    },
                )
            }
            if (isUser) {
                Text(
                    text = message.content,
                    translate = false,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(userBubbleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AuroraViolet.copy(alpha = 0.28f),
                                    Color(0xFFC04AE0).copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(0.7.dp, AuroraViolet.copy(alpha = 0.32f), userBubbleShape)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                )
            } else {
                // Aria bubble: glass surface + violet→teal left accent via drawBehind
                val barBrush = Brush.verticalGradient(listOf(AuroraViolet, AuroraTeal))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(ariaBubbleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.7.dp, Color.White.copy(alpha = 0.11f), ariaBubbleShape)
                        .drawBehind {
                            val barW = 2.5.dp.toPx()
                            val inset = size.height * 0.10f
                            drawRect(
                                brush = barBrush,
                                topLeft = Offset(0f, inset),
                                size = Size(barW, size.height - inset * 2)
                            )
                        }
                        .padding(start = 15.dp, end = 14.dp, top = 11.dp, bottom = 11.dp)
                )
                IconButton(onClick = { onReportResponse(message) }) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbDown,
                        contentDescription = "Report a poor response",
                        tint = Color.White.copy(alpha = 0.58f),
                    )
                }
            }

            if (hasCards && !isUser) {
                val verificationTrace = remember(message.metadata) {
                    WebVerificationMetadata.decode(message.metadata)
                }
                if (verificationTrace != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    VerificationTraceCard(trace = verificationTrace)
                }
                val cards = remember(message.metadata) { parseSearchCards(message.metadata) }
                if (cards.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    cards.forEach { item ->
                        SearchResultCard(
                            item = item,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseSearchCards(metadata: String?): List<SearchResultItem> {
    if (metadata == null) return emptyList()
    return try {
        val json = JSONObject(metadata)
        val type = json.optString("type", "")
        if (type == "search_results") {
            val arr = json.optJSONArray("results") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONObject(i) ?: return@mapNotNull null
                SearchResultItem(
                    title = item.optString("title", ""),
                    snippet = item.optString("snippet", "").take(150),
                    url = item.optString("url", "")
                )
            }
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
