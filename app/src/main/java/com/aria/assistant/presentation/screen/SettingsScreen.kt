package com.aria.assistant.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aria.assistant.engine.VoiceDownloadState
import com.aria.assistant.engine.VoiceInfo
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
) {
    val voiceConfig by viewModel.voiceConfig.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    NebulaBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ── Nav bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            // ── Scrollable content ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                SectionLabel("Voice Controls")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingRow(label = "Wake Word", sub = "Say \"Hey Aria\" to activate") {
                            Switch(
                                checked = voiceConfig.wakeWordEnabled,
                                onCheckedChange = { viewModel.updateWakeWordEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AuroraViolet,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }

                        SliderRow(
                            label = "Wake Sensitivity",
                            value = voiceConfig.wakeWordSensitivity,
                            valueText = String.format("%.1f", voiceConfig.wakeWordSensitivity),
                            range = 0f..1f,
                            onValueChange = { viewModel.updateWakeWordSensitivity(it) }
                        )
                        SliderRow(
                            label = "TTS Speed",
                            value = voiceConfig.ttsSpeed,
                            valueText = "${String.format("%.1f", voiceConfig.ttsSpeed)}×",
                            range = 0.5f..2f,
                            onValueChange = { viewModel.updateTtsSpeed(it) }
                        )
                        SliderRow(
                            label = "TTS Pitch",
                            value = voiceConfig.ttsPitch,
                            valueText = "${String.format("%.1f", voiceConfig.ttsPitch)}×",
                            range = 0.5f..2f,
                            onValueChange = { viewModel.updateTtsPitch(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel("Preferences")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(label = "Temperature Unit") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (voiceConfig.temperatureUnit == "fahrenheit") "°F" else "°C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = voiceConfig.temperatureUnit == "fahrenheit",
                                onCheckedChange = { checked ->
                                    viewModel.updateTemperatureUnit(if (checked) "fahrenheit" else "celsius")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AuroraTeal,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permissions button
                Button(
                    onClick = onNavigateToPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.13f))
                ) {
                    Text(
                        "Permissions & Access",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Voice Selection")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        viewModel.availableVoices.forEach { voice ->
                            VoiceItem(
                                voice = voice,
                                isPremium = isPremium,
                                isSelected = voiceConfig.selectedVoice == voice.id,
                                downloadState = downloadStates[voice.id],
                                isDownloaded = viewModel.isVoiceDownloaded(voice.id),
                                onSelect = { viewModel.updateSelectedVoice(voice.id) },
                                onPreview = { viewModel.previewVoice(voice.id) },
                                onDownload = { viewModel.downloadVoice(voice.id) },
                                onUpgrade = onUpgrade
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextTertiary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun SettingRow(
    label: String,
    sub: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        trailing()
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = AuroraViolet)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AuroraViolet,
                activeTrackColor = AuroraViolet,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun VoiceItem(
    voice: VoiceInfo,
    isPremium: Boolean,
    isSelected: Boolean,
    downloadState: VoiceDownloadState?,
    isDownloaded: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onUpgrade: () -> Unit = {}
) {
    val isPremiumLocked = voice.isPremium && !isPremium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initials
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.linearGradient(listOf(AuroraViolet, AuroraTeal)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = voice.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = voice.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                if (voice.isDefault) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VoiceTag(text = "Default", color = AuroraTeal)
                }
                if (voice.isPremium) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VoiceTag(text = "Premium", color = AuroraAmber)
                }
            }
            if (voice.accent.isNotBlank()) {
                Text(voice.accent, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        when {
            isPremiumLocked -> {
                TextButton(onClick = onUpgrade) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = AuroraAmber)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unlock", style = MaterialTheme.typography.labelSmall, color = AuroraAmber)
                }
            }
            isDownloaded -> {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.13f))
                ) {
                    Text("Preview", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AuroraTeal.copy(alpha = 0.2f) else AuroraViolet
                    ),
                    border = if (isSelected)
                        androidx.compose.foundation.BorderStroke(0.5.dp, AuroraTeal.copy(alpha = 0.4f))
                    else null
                ) {
                    Text(
                        if (isSelected) "✓ On" else "Select",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) AuroraTeal else Color.White
                    )
                }
            }
            downloadState is VoiceDownloadState.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    progress = { downloadState.progress },
                    color = AuroraViolet,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "${(downloadState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            else -> {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                ) {
                    Text("Download", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun VoiceTag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
