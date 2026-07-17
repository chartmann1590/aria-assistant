package com.aria.assistant.presentation.screen

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.aria.assistant.translation.TranslatedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.aria.assistant.data.feedback.BugReport
import com.aria.assistant.data.feedback.GithubComment
import com.aria.assistant.data.feedback.GithubIssue
import com.aria.assistant.engine.VoiceDownloadState
import com.aria.assistant.engine.VoiceInfo
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraLavender
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.GlassFill
import com.aria.assistant.presentation.ui.theme.GlassStroke
import com.aria.assistant.presentation.ui.theme.NebulaBase
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.FeedbackViewModel
import com.aria.assistant.presentation.viewmodel.FeedbackFormState
import com.aria.assistant.presentation.viewmodel.IssueDetailState
import com.aria.assistant.presentation.viewmodel.SettingsViewModel
import com.aria.assistant.translation.TranslationStatus
import com.aria.assistant.translation.UiTranslationManager
import com.aria.assistant.translation.translatedUiText
import com.aria.assistant.web.WebVerificationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    feedbackViewModel: FeedbackViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
) {
    val voiceConfig by viewModel.voiceConfig.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val translationStatus by viewModel.translationStatus.collectAsState()
    val privacyOptionsRequired by viewModel.privacyOptionsRequired.collectAsState()
    val activity = LocalContext.current as? Activity
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var webModeMenuExpanded by remember { mutableStateOf(false) }

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
                        contentDescription = translatedUiText("Back"),
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
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val selectedLanguage = viewModel.supportedUiLanguages.firstOrNull {
                        it.tag == voiceConfig.uiLanguage
                    } ?: viewModel.supportedUiLanguages.first()
                    SettingRow(
                        label = "App Language",
                        sub = "Translate menus and interface text on this device"
                    ) {
                        Box {
                            OutlinedButton(
                                onClick = { languageMenuExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassStroke)
                            ) {
                                Text(
                                    selectedLanguage.nativeName,
                                    style = MaterialTheme.typography.labelSmall,
                                    translate = false
                                )
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                viewModel.supportedUiLanguages.forEach { language ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (language.nativeName.equals(language.englishName, ignoreCase = true)) {
                                                    language.nativeName
                                                } else {
                                                    "${language.nativeName} (${language.englishName})"
                                                },
                                                translate = false
                                            )
                                        },
                                        onClick = {
                                            languageMenuExpanded = false
                                            viewModel.updateUiLanguage(language.tag)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    when (val status = translationStatus) {
                        is TranslationStatus.Downloading -> Text(
                            "Downloading the language model…",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuroraViolet,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        is TranslationStatus.Error -> Text(
                            "Translation model download failed: ${status.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            translate = false
                        )
                        TranslationStatus.Ready -> if (selectedLanguage.tag != UiTranslationManager.ENGLISH) {
                            Text(
                                "Language model downloaded — translations work offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuroraTeal,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Text(
                        "Powered by Google ML Kit",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    SettingRow(
                        label = "Web Verification",
                        sub = "Free public web search; queries are sent to search providers"
                    ) {
                        Box {
                            OutlinedButton(
                                onClick = { webModeMenuExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassStroke)
                            ) {
                                Text(
                                    when (voiceConfig.webVerificationMode) {
                                        WebVerificationMode.ALWAYS_FACTUAL -> "All facts"
                                        WebVerificationMode.CURRENT_ONLY -> "Current facts"
                                        WebVerificationMode.EXPLICIT_ONLY -> "When asked"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            DropdownMenu(
                                expanded = webModeMenuExpanded,
                                onDismissRequest = { webModeMenuExpanded = false }
                            ) {
                                listOf(
                                    WebVerificationMode.ALWAYS_FACTUAL to "All factual questions",
                                    WebVerificationMode.CURRENT_ONLY to "Current information only",
                                    WebVerificationMode.EXPLICIT_ONLY to "Only when I ask"
                                ).forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            webModeMenuExpanded = false
                                            viewModel.updateWebVerificationMode(mode)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    SettingRow(label = "Privacy Mode", sub = "Process speech entirely on-device (Whisper STT)") {
                        Switch(
                            checked = voiceConfig.privacyMode,
                            onCheckedChange = { viewModel.updatePrivacyMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AuroraTeal,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                    SettingRow(label = "Model", sub = if (voiceConfig.selectedModel == "E4B") "Gemma E4B (Premium)" else "Gemma E2B (Default)") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = voiceConfig.selectedModel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = voiceConfig.selectedModel == "E4B",
                                onCheckedChange = { checked ->
                                    if (checked && !isPremium) {
                                        onUpgrade()
                                    } else {
                                        viewModel.updateSelectedModel(if (checked) "E4B" else "E2B")
                                    }
                                },
                                enabled = !(voiceConfig.selectedModel == "E2B" && isPremium),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = if (isPremium) AuroraViolet else TextTertiary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
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

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Privacy")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        label = "Ad privacy choices",
                        sub = if (privacyOptionsRequired) {
                            "Review or change your advertising consent"
                        } else {
                            "Advertising privacy settings"
                        }
                    ) {
                        Button(
                            onClick = { activity?.let(viewModel::showAdPrivacyOptions) },
                            enabled = activity != null,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                        ) {
                            Text("Open", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionLabel("Support & Feedback")
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        label = "Support & Feedback",
                        sub = "Report bugs, track issues, reply to threads"
                    ) {
                        Button(
                            onClick = onNavigateToFeedback,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (feedbackViewModel.isConfigured) AuroraViolet else TextTertiary
                            ),
                            enabled = feedbackViewModel.isConfigured
                        ) {
                            Text(
                                "Open",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
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
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = TextSecondary,
        modifier = Modifier.padding(start = 6.dp, bottom = 10.dp, top = 6.dp)
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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            if (sub != null) {
                Spacer(modifier = Modifier.height(2.dp))
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
        // Avatar initials — unique gradient per voice initial
        val avatarGradient = when (voice.displayName.take(1).uppercase()) {
            "A", "E", "I" -> Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta))
            "N", "O", "U" -> Brush.linearGradient(listOf(AuroraTeal, AuroraViolet))
            else -> Brush.linearGradient(listOf(AuroraMagenta, AuroraTeal))
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .background(avatarGradient, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = voice.displayName.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
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

// ── Feedback / Bug Report Composables ──────────────────────────────────────

@Composable
private fun BugReportRow(
    report: BugReport,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = "#${report.number} · ${report.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        val statusColor = if (report.status == "open") AuroraTeal else Color(0xFFEF4444)
        Text(
            text = report.status.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ReportDialog(
    feedbackViewModel: FeedbackViewModel,
    formState: FeedbackFormState,
    onImagePick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .background(NebulaBase, RoundedCornerShape(16.dp))
                .border(0.5.dp, GlassStroke, RoundedCornerShape(16.dp))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Report a Problem",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Warning
            Text(
                text = "Your report will be submitted to this app's GitHub issue tracker. " +
                        "Do not include passwords, private keys, medical information, financial " +
                        "information, or anything you do not want visible to the repository " +
                        "maintainers. If this repository is public, your report may be publicly visible.",
                style = MaterialTheme.typography.bodySmall,
                color = AuroraAmber,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuroraAmber.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, AuroraAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val scrollState = rememberScrollState()
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                OutlinedTextField(
                    value = formState.title,
                    onValueChange = { feedbackViewModel.updateTitle(it) },
                    label = { Text("Title / Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.description,
                    onValueChange = { feedbackViewModel.updateDescription(it) },
                    label = { Text("Description *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formState.includeDiagnostics,
                        onCheckedChange = { feedbackViewModel.updateIncludeDiagnostics(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AuroraViolet,
                            uncheckedColor = TextTertiary
                        )
                    )
                    Text(
                        "Include phone/app diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.userName,
                    onValueChange = { feedbackViewModel.updateUserName(it) },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formState.userEmail,
                    onValueChange = { feedbackViewModel.updateUserEmail(it) },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Image picker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onImagePick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp, GlassStroke
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Attach Screenshot", style = MaterialTheme.typography.labelSmall)
                    }
                    if (formState.imageUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { feedbackViewModel.updateImageUri(null) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = translatedUiText("Remove"),
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Remove", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                formState.imageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ImagePreview(uri = uri, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message
            formState.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !formState.isSubmitting) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { feedbackViewModel.submitReport() },
                    enabled = formState.title.isNotBlank()
                            && formState.description.isNotBlank()
                            && !formState.isSubmitting
                            && feedbackViewModel.isConfigured,
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (formState.isSubmitting) "Submitting..." else "Submit",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueDetailDialog(
    feedbackViewModel: FeedbackViewModel,
    issueDetail: IssueDetailState,
    issueNumber: Int,
    onReplyImagePick: () -> Unit,
    onDismiss: () -> Unit
) {
    // Load issue detail if not already loading/loaded
    if (!issueDetail.isLoading && issueDetail.issue == null && issueDetail.error == null) {
        feedbackViewModel.loadIssueDetail(issueNumber)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .background(NebulaBase, RoundedCornerShape(16.dp))
                .border(0.5.dp, GlassStroke, RoundedCornerShape(16.dp))
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(20.dp)
        ) {
            when {
                issueDetail.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AuroraViolet)
                    }
                }

                issueDetail.error != null -> {
                    Text(
                        text = issueDetail.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { feedbackViewModel.loadIssueDetail(issueNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }

                else -> {
                    val issue = issueDetail.issue
                    if (issue != null) {
                        // Issue header
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusColor = if (issue.state == "open") AuroraTeal else Color(0xFFEF4444)
                            Text(
                                text = "#$issueNumber",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = issue.state.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Created ${issue.createdAt.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    // Comments
                    Text(
                        text = "Comments (${issueDetail.comments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    val commentsScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(commentsScrollState)
                    ) {
                        if (issueDetail.comments.isEmpty()) {
                            Text(
                                text = "No comments yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            issueDetail.comments.forEach { comment ->
                                CommentItem(comment = comment)
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.04f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    // Reply input
                    OutlinedTextField(
                        value = issueDetail.replyText,
                        onValueChange = { feedbackViewModel.updateReplyText(it) },
                        label = { Text("Write a reply...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onReplyImagePick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassStroke)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Image", style = MaterialTheme.typography.labelSmall)
                        }
                        if (issueDetail.replyImageUri != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { feedbackViewModel.updateReplyImageUri(null) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = translatedUiText("Remove"),
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        issueDetail.error?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        Button(
                            onClick = { feedbackViewModel.postReply(issueNumber) },
                            enabled = issueDetail.replyText.isNotBlank()
                                    && !issueDetail.isPostingReply,
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal)
                        ) {
                            if (issueDetail.isPostingReply) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                if (issueDetail.isPostingReply) "..." else "Reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: GithubComment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.user.login,
                style = MaterialTheme.typography.labelMedium,
                color = AuroraViolet
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = comment.createdAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.body,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ImagePreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = translatedUiText("Image preview"),
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, GlassStroke, RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = AuroraViolet,
    unfocusedBorderColor = GlassStroke,
    cursorColor = AuroraViolet,
    focusedLabelColor = AuroraLavender,
    unfocusedLabelColor = TextTertiary
)
