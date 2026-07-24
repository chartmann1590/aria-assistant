package com.aria.assistant.presentation.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import com.aria.assistant.BuildConfig
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.aria.assistant.translation.TranslatedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.presentation.component.AriaOrb
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.viewmodel.OnboardingDownloadState
import com.aria.assistant.presentation.viewmodel.OnboardingViewModel
import com.aria.assistant.service.AriaForegroundService
import com.aria.assistant.translation.TranslationStatus
import com.aria.assistant.translation.UiTranslationManager

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.0f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.0f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(1) }
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val voiceConfig by viewModel.voiceConfig.collectAsStateWithLifecycle()
    val translationStatus by viewModel.translationStatus.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) step = 4 else step = 3 }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { step = 5 }

    LaunchedEffect(step) { if (step == 2) viewModel.startModelDownload() }
    LaunchedEffect(downloadState.isReady) { if (downloadState.isReady && step == 2) step = 3 }

    NebulaBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                (1..6).forEach { i ->
                    Box(
                        modifier = Modifier
                            .then(
                                if (i == step) Modifier
                                    .size(width = 20.dp, height = 6.dp)
                                    .background(
                                        Brush.linearGradient(listOf(AuroraViolet, AuroraTeal)),
                                        RoundedCornerShape(3.dp)
                                    )
                                else Modifier
                                    .size(6.dp)
                                    .background(
                                        Color.White.copy(alpha = if (i < step) 0.4f else 0.15f),
                                        CircleShape
                                    )
                            )
                    )
                }
            }

            when (step) {
                1 -> {
                    AriaOrb(state = AriaState.IDLE)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Meet Aria",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AI that lives on your phone.\nPrivate. Fast. Always ready.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Choose your language",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box {
                                val selectedLanguage = viewModel.supportedUiLanguages.firstOrNull {
                                    it.tag == voiceConfig.uiLanguage
                                } ?: viewModel.supportedUiLanguages.first()
                                OutlinedButton(onClick = { languageMenuExpanded = true }) {
                                    Text(selectedLanguage.nativeName, translate = false)
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
                            when (val status = translationStatus) {
                                is TranslationStatus.Downloading -> Text(
                                    "Downloading the language model…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraViolet
                                )
                                is TranslationStatus.Error -> Text(
                                    "Language download failed: ${status.message}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFEF4444),
                                    textAlign = TextAlign.Center,
                                    translate = false
                                )
                                TranslationStatus.Ready -> if (voiceConfig.uiLanguage != UiTranslationManager.ENGLISH) {
                                    Text(
                                        "Ready for offline translation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AuroraTeal
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Translations are generated on-device and may be inaccurate. You can change the app language anytime in Settings.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    NebulaButton(
                        "Get Started",
                        enabled = translationStatus !is TranslationStatus.Downloading,
                        onClick = { step = 2 }
                    )
                }

                2 -> {
                    AriaOrb(
                        state = when {
                            downloadState.error != null -> AriaState.ERROR
                            downloadState.isReady -> AriaState.IDLE
                            else -> AriaState.PROCESSING
                        }
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Downloading Aria",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            downloadState.error != null -> "Download failed: ${downloadState.error}"
                            downloadState.isReady -> "AI model ready!"
                            downloadState.isInitializing -> "Initializing engine on GPU..."
                            downloadState.isDownloading -> "Downloading Gemma 4 model..."
                            else -> "Preparing to download..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (downloadState.isDownloading || downloadState.isInitializing) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Gemma 4 Model", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(
                                        "${(downloadState.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AuroraViolet
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { downloadState.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = AuroraViolet,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                if (downloadState.totalBytes > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${formatBytes(downloadState.bytesDownloaded)} / ${formatBytes(downloadState.totalBytes)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        if (downloadState.isInitializing) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AuroraViolet,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading model into GPU memory...", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    if (downloadState.isReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "GPU accelerated • Private • On-device",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuroraViolet
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        NebulaButton("Continue", onClick = { step = 3 })
                    }
                    if (downloadState.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NebulaButton("Retry Download", onClick = { viewModel.startModelDownload() })
                    }
                }

                3 -> {
                    Spacer(modifier = Modifier.size(120.dp))
                    Text("Microphone Access", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Aria needs microphone access to listen for your wake word and transcribe your voice.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PrivacyBullet("Audio never leaves your device", AuroraTeal)
                            PrivacyBullet("Transcription happens on-device only", AuroraTeal)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    NebulaButton("Allow Microphone Access", onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (hasMic) step = 4 else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    })
                }

                4 -> {
                    Spacer(modifier = Modifier.size(120.dp))
                    Text("Battery Optimization", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Aria stays awake in the background to listen for your wake word. This uses about 2–5% extra battery per hour.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    NebulaButton("Allow Background Access", onClick = {
                        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            batteryOptLauncher.launch(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        } else { step = 5 }
                    })
                    GhostButton("Skip", onClick = { step = 5 })
                }

                5 -> {
                    Spacer(modifier = Modifier.size(120.dp))
                    Text("Permissions", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (BuildConfig.ENABLE_RESTRICTED_MESSAGING) {
                            "Aria needs access to a few phone features for calling, messaging, and settings control."
                        } else {
                            "Aria needs access to a few phone features for calling and device controls. You choose each permission."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    NebulaButton("Review Permissions", onClick = { onNavigateToPermissions(); step = 6 })
                    GhostButton("Skip", onClick = { step = 6 })
                }

                6 -> {
                    AriaOrb(state = AriaState.LISTENING)
                    Spacer(modifier = Modifier.height(28.dp))
                    Text("Wake Word Test", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Say “Hey Aria” to try it out",
                        style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(AuroraTeal, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listening for wake word…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    NebulaButton("Done — Start Using Aria", onClick = {
                        context.startForegroundService(Intent(context, AriaForegroundService::class.java))
                        viewModel.completeOnboarding()
                        onComplete()
                    })
                }
            }
        }
    }
}

@Composable
private fun NebulaButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.38f)
                .background(
                    Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta)),
                    RoundedCornerShape(999.dp)
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun PrivacyBullet(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
