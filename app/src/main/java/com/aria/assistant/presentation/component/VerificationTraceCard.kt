package com.aria.assistant.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.aria.assistant.translation.TranslatedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.web.VerificationStatus
import com.aria.assistant.web.VerificationTrace
import java.text.DateFormat
import java.util.Date

@Composable
fun VerificationTraceCard(trace: VerificationTrace, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(trace.retrievedAt) { mutableStateOf(false) }
    val statusColor = when (trace.status) {
        VerificationStatus.VERIFIED -> AuroraTeal
        VerificationStatus.PARTIAL, VerificationStatus.CONFLICTING -> AuroraAmber
        VerificationStatus.UNAVAILABLE -> Color(0xFFEF4444)
        VerificationStatus.NOT_REQUIRED -> TextTertiary
    }
    val statusLabel = when (trace.status) {
        VerificationStatus.VERIFIED -> "Verified on the web"
        VerificationStatus.PARTIAL -> "Partially verified"
        VerificationStatus.CONFLICTING -> "Sources conflict"
        VerificationStatus.UNAVAILABLE -> "Web verification unavailable"
        VerificationStatus.NOT_REQUIRED -> "Web not required"
    }
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.045f), shape)
            .border(0.6.dp, statusColor.copy(alpha = 0.28f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = statusColor, modifier = Modifier.size(17.dp))
            Text(
                statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${trace.sources.size} sources",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse verification" else "Expand verification",
                tint = AuroraViolet,
                modifier = Modifier.padding(start = 4.dp).size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text("Query", style = MaterialTheme.typography.labelSmall, color = AuroraViolet)
                Text(trace.query, style = MaterialTheme.typography.bodySmall, color = TextSecondary, translate = false)
                Spacer(modifier = Modifier.height(8.dp))
                trace.steps.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = statusColor, translate = false)
                        Column {
                            Text(step.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
                trace.warning?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AuroraAmber)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    "Retrieved ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(trace.retrievedAt))} • ${trace.elapsedMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    translate = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                trace.sources.forEach { source ->
                    SearchResultCard(
                        item = com.aria.assistant.domain.model.SearchResultItem(source.title, source.snippet, source.url),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
