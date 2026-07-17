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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.aria.assistant.translation.TranslatedText as Text
import com.aria.assistant.translation.translatedUiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.AboutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
) {
    NebulaBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = translatedUiText("Back"), tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("About", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App identity card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Aria",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Private AI Assistant",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "v${viewModel.appVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuroraViolet
                        )
                    }
                }

                // Privacy
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Privacy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your voice never leaves your phone. All audio processing, speech recognition, " +
                                    "LLM inference, and text-to-speech happen entirely on-device. " +
                                    "No analytics, no advertising, no crash-reporting SDKs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Diagnostics card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device Info", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                        val lines = viewModel.diagnostics.lines().drop(1)
                        lines.forEach { line ->
                            val clean = line.removePrefix("- ")
                            val colon = clean.indexOf(": ")
                            if (colon > 0) {
                                val label = clean.substring(0, colon)
                                val value = clean.substring(colon + 2)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                    Text(value, style = MaterialTheme.typography.bodySmall, color = TextSecondary, translate = false)
                                }
                            }
                        }
                    }
                }

                // Models card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Models", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ModelRow("LLM", "Gemma 4 E2B / E4B (LiteRT)")
                        ModelRow("STT", "Whisper Tiny (ONNX) / Android SpeechRecognizer")
                        ModelRow("TTS", "Piper (ONNX) / Android TTS")
                        ModelRow("Wake Word", "Energy detection")
                    }
                }

                // Open source
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Licensed under Apache 2.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = AuroraTeal, translate = false)
    }
}
