package com.aria.assistant.presentation.screen

import com.aria.assistant.BuildConfig
import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.aria.assistant.translation.TranslatedText as Text
import com.aria.assistant.translation.translatedUiText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.assistant.permission.PhoneCapability
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextSecondary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.presentation.viewmodel.PermissionUiState
import com.aria.assistant.presentation.viewmodel.PermissionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refresh() }

    val specialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refresh() }

    var showServiceDialog by remember { mutableStateOf<PhoneCapability?>(null) }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showServiceDialog != null) {
        val cap = showServiceDialog!!
        AlertDialog(
            onDismissRequest = { showServiceDialog = null },
            title = { Text(cap.title, color = TextPrimary) },
            text = { Text(serviceDisclosure(cap), color = TextSecondary) },
            containerColor = Color(0xFF12122A),
            titleContentColor = TextPrimary,
            confirmButton = {
                Button(
                    onClick = {
                        showServiceDialog = null
                        val intent = viewModel.requestIntent(cap)
                        specialLauncher.launch(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
                ) { Text("I Agree & Open Settings", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showServiceDialog = null }) {
                    Text("Not Now", color = TextSecondary)
                }
            }
        )
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = translatedUiText("Back"), tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Permissions", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Standard Permissions".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                    )
                }

                val runtimeCaps = PhoneCapability.entries.filter {
                    it.kind == PhoneCapability.PermKind.RUNTIME &&
                        (BuildConfig.ENABLE_RESTRICTED_MESSAGING ||
                            (it != PhoneCapability.SMS && it != PhoneCapability.READ_SMS))
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            runtimeCaps.forEach { cap ->
                                PermissionRow(
                                    cap = cap,
                                    state = permissions[cap] ?: PermissionUiState.DENIED,
                                    onGrant = {
                                        val perms = viewModel.runtimePerms(cap)
                                        if (perms.isNotEmpty()) runtimeLauncher.launch(perms.toTypedArray())
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Special Permissions".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                    )
                }

                val specialCaps = PhoneCapability.entries.filter {
                    it.kind == PhoneCapability.PermKind.SPECIAL &&
                        (BuildConfig.ENABLE_ACCESSIBILITY_AUTOMATION || it != PhoneCapability.ACCESSIBILITY)
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            specialCaps.forEach { cap ->
                                PermissionRow(
                                    cap = cap,
                                    state = permissions[cap] ?: PermissionUiState.DENIED,
                                    onGrant = {
                                        when (cap) {
                                            PhoneCapability.NOTIFICATION_LISTENER,
                                            PhoneCapability.ACCESSIBILITY,
                                            PhoneCapability.USAGE_ACCESS -> showServiceDialog = cap
                                            else -> specialLauncher.launch(viewModel.requestIntent(cap))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

private fun serviceDisclosure(capability: PhoneCapability): String = when (capability) {
    PhoneCapability.ACCESSIBILITY ->
        "Aria uses Android Accessibility Service to read text visible on your screen and " +
            "perform only the taps or scrolls you explicitly request. Screen content is " +
            "processed on your device and is not shared with advertisers, Cloudflare, or " +
            "Aria's developer. This optional access is never used to act without your request."
    PhoneCapability.NOTIFICATION_LISTENER ->
        "Aria uses notification access to read, dismiss, and reply to notifications only " +
            "when you ask. Notification content is processed on your device and is not " +
            "shared with advertisers, Cloudflare, or Aria's developer."
    PhoneCapability.USAGE_ACCESS ->
        "Aria uses app-usage access to identify the foreground app when you request " +
            "context-aware help. This information stays on your device and is not shared."
    else -> capability.rationale
}

@Composable
private fun PermissionRow(
    cap: PhoneCapability,
    state: PermissionUiState,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(cap.title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(cap.rationale, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (state == PermissionUiState.GRANTED) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AuroraTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Granted", style = MaterialTheme.typography.labelSmall, color = AuroraTeal)
            }
        } else {
            Button(
                onClick = onGrant,
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuroraViolet.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, AuroraViolet.copy(alpha = 0.4f))
            ) {
                Text("Grant", style = MaterialTheme.typography.labelSmall, color = AuroraViolet)
            }
        }
    }
}
