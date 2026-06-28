package com.aria.assistant.presentation.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.presentation.component.AriaOrb
import com.aria.assistant.presentation.component.ConversationBubble
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.ui.theme.color
import com.aria.assistant.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
) {
    val ariaState by viewModel.ariaState.collectAsStateWithLifecycle()
    val messages by viewModel.recentMessages.collectAsStateWithLifecycle()
    val downloadInfo by viewModel.downloadInfo.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startServiceWithMic()
    }

    NebulaBackground(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lowercase "aria" wordmark with violet dot accent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "aria",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5f).sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.size(5.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                Brush.radialGradient(listOf(AuroraViolet, AuroraMagenta)),
                                CircleShape
                            )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GlassIconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    GlassIconButton(onClick = onNavigateToPremium) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Premium",
                            tint = AuroraAmber,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    GlassIconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // ── Orb hero zone ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.4f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AriaOrb(state = ariaState)

                Spacer(modifier = Modifier.height(14.dp))

                val stateLabel = when (ariaState) {
                    AriaState.DOWNLOADING -> {
                        val pct = (downloadInfo.totalProgress * 100).toInt()
                        "Downloading model... $pct%"
                    }
                    AriaState.IDLE         -> "Tap mic or type below"
                    AriaState.LISTENING    -> "Listening..."
                    AriaState.PROCESSING   -> "Thinking..."
                    AriaState.SPEAKING     -> "Speaking..."
                    AriaState.MUTED        -> "Muted"
                    AriaState.INITIALIZING -> "Starting up..."
                    AriaState.WAKING_UP    -> "Waking up..."
                    AriaState.ERROR        -> "Grant mic access or type below"
                }
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ariaState.color().copy(alpha = 0.90f)
                )

                if (ariaState == AriaState.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { downloadInfo.totalProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.60f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AuroraViolet,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    val parts = buildList {
                        if (downloadInfo.gemmaProgress > 0) add("Gemma: ${(downloadInfo.gemmaProgress * 100).toInt()}%")
                        if (downloadInfo.whisperProgress > 0) add("STT: ${(downloadInfo.whisperProgress * 100).toInt()}%")
                        if (downloadInfo.voiceProgress > 0) add("Voice: ${(downloadInfo.voiceProgress * 100).toInt()}%")
                    }
                    if (parts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = parts.joinToString("  ·  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }

                if (streamingText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = streamingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                }
            }

            // ── Conversation list ────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.take(10)) { message ->
                    ConversationBubble(message = message)
                }
            }

            // ── Floating input bar ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 20.dp, top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AuroraViolet.copy(alpha = 0.09f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * 0.6f
                                )
                            )
                        }
                        .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(999.dp))
                        .border(0.7.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine = true,
                        cursorBrush = SolidColor(AuroraViolet),
                        enabled = ariaState == AriaState.IDLE || ariaState == AriaState.MUTED,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        decorationBox = { inner ->
                            if (textInput.isEmpty()) {
                                Text(
                                    "Type a message…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary
                                )
                            }
                            inner()
                        }
                    )

                    val canSend = textInput.isNotBlank() && (ariaState == AriaState.IDLE || ariaState == AriaState.MUTED)
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSend) AuroraAmber else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val isActive = ariaState == AriaState.LISTENING ||
                        ariaState == AriaState.PROCESSING ||
                        ariaState == AriaState.SPEAKING ||
                        ariaState == AriaState.WAKING_UP

                    val fabGradient = when {
                        ariaState == AriaState.MUTED -> Brush.linearGradient(listOf(AuroraAmber, Color(0xFFF97316)))
                        isActive -> Brush.linearGradient(listOf(AuroraTeal, AuroraViolet))
                        else -> Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta))
                    }

                    FloatingActionButton(
                        onClick = {
                            if (viewModel.needsMicPermission()) {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.triggerListening()
                            }
                        },
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(fabGradient, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    ariaState == AriaState.MUTED -> Icons.Default.MicOff
                                    isActive -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = when {
                                    ariaState == AriaState.MUTED -> "Unmute"
                                    isActive -> "Stop"
                                    else -> "Listen"
                                },
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.07f), CircleShape)
            .border(0.7.dp, Color.White.copy(alpha = 0.10f), CircleShape)
    ) {
        content()
    }
}
