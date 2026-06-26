package com.aria.assistant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val userBubbleShape = RoundedCornerShape(
    topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp
)
private val ariaBubbleShape = RoundedCornerShape(
    topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp
)

@Composable
fun ConversationBubble(message: ConversationMessage) {
    val isUser = message.role == "user"
    val hasCards = message.metadata != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(userBubbleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AuroraViolet.copy(alpha = 0.22f),
                                    Color(0xFFC04AE0).copy(alpha = 0.14f)
                                )
                            )
                        )
                        .border(0.5.dp, AuroraViolet.copy(alpha = 0.25f), userBubbleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
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
                        .background(Color.White.copy(alpha = 0.045f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.09f), ariaBubbleShape)
                        .drawBehind {
                            val barW = 2.dp.toPx()
                            val inset = size.height * 0.12f
                            drawRect(
                                brush = barBrush,
                                topLeft = Offset(0f, inset),
                                size = Size(barW, size.height - inset * 2)
                            )
                        }
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                )
            }

            if (hasCards && !isUser) {
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
