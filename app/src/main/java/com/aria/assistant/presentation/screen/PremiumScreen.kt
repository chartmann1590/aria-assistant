package com.aria.assistant.presentation.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.PremiumViewModel

private data class FeatureRow(val label: String, val free: Boolean, val premium: Boolean)

private val features = listOf(
    FeatureRow("General Q&A", true, true),
    FeatureRow("Timers & Alarms", true, true),
    FeatureRow("\"Hey Aria\" wake word", true, true),
    FeatureRow("1 TTS voice (Amy)", true, true),
    FeatureRow("Web search & Wikipedia", true, true),
    FeatureRow("Current location & navigation", true, true),
    FeatureRow("Media playback control", true, true),
    FeatureRow("Launch and list apps", true, true),
    FeatureRow("Battery & time queries", true, true),
    FeatureRow("Read SMS inbox", true, true),
    FeatureRow("Dismiss notifications", true, true),
    FeatureRow("Additional 5 premium voices", false, true),
    FeatureRow("Gemma E4B model", false, true),
    FeatureRow("Phone calls", false, true),
    FeatureRow("Send SMS", false, true),
    FeatureRow("Device settings control", false, true),
    FeatureRow("Create calendar events & reminders", false, true),
    FeatureRow("Read & reply to notifications", false, true),
    FeatureRow("Camera — take & view photos", false, true),
    FeatureRow("Screen control (read/click/scroll)", false, true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? android.app.Activity

    if (isPremium) {
        PremiumActiveScreen(onBack = onBack)
        return
    }

    NebulaBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Nav bar
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
                Text("Premium", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Aurora hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AuroraMagenta.copy(alpha = 0.35f),
                                    AuroraViolet.copy(alpha = 0.25f),
                                    AuroraTeal.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset.Unspecified,
                                radius = 600f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Aria Premium",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Unlock the full Aria experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Feature table
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        // Table header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .border(
                                    width = 0.5.dp,
                                    color = Color.White.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Feature",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                            Text(
                                "Free",
                                modifier = Modifier.width(48.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Pro",
                                modifier = Modifier.width(48.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = AuroraViolet,
                                textAlign = TextAlign.Center
                            )
                        }
                        features.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    feature.label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (feature.free) TextPrimary else AuroraViolet
                                )
                                Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (feature.free) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (feature.free) AuroraTeal else TextTertiary
                                    )
                                }
                                Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = AuroraViolet
                                    )
                                }
                            }
                        }
                    }
                }

                // Pricing cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Monthly
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Monthly", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$2.99", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("/month", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { activity?.let { viewModel.subscribe("aria_premium_monthly", it) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
                            ) {
                                Text("Subscribe", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                            }
                        }
                    }

                    // Yearly (featured)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Brush.linearGradient(listOf(AuroraViolet, AuroraTeal)),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0520), RoundedCornerShape(19.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Save 44%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta)),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Yearly", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$19.99", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("/year", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { activity?.let { viewModel.subscribe("aria_premium_yearly", it) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(listOf(AuroraViolet, AuroraMagenta)),
                                            RoundedCornerShape(999.dp)
                                        )
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Subscribe", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { viewModel.restorePurchases() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Restore Purchases", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumActiveScreen(onBack: () -> Unit) {
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Premium", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(AuroraTeal.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, AuroraTeal.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("You're Premium!", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Thank you for supporting Aria.\nAll premium features are unlocked.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
