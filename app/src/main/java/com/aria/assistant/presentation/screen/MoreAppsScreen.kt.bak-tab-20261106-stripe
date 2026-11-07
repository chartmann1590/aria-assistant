package com.aria.assistant.presentation.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aria.assistant.presentation.component.GlassCard
import com.aria.assistant.presentation.component.NebulaBackground
import com.aria.assistant.presentation.ui.theme.TextPrimary
import com.aria.assistant.presentation.ui.theme.TextTertiary
import com.aria.assistant.translation.TranslatedText as Text

data class CrossPromoApp(
    val name: String,
    val packageName: String,
    val tagline: String,
)

val crossPromoApps: List<CrossPromoApp> = listOf(
    CrossPromoApp("NutriSnap: AI Calorie Tracker", "com.charles.nutrisnap", "Snap a meal, get instant calories & macros — 100% on-device AI, private."),
    CrossPromoApp("ScamRadar: AI Scam Detector", "com.charles.scamradar.app", "On-device AI catches scams in texts, voicemails & notifications. Free."),
    CrossPromoApp("MeshTalk: Bluetooth Mesh Chat", "com.charles.meshtalk.app", "Chat, talk & AI over Bluetooth mesh. No internet, no accounts, fully offline."),
    CrossPromoApp("DriveVault Dashcam", "com.drivevault.dashcam", "Privacy-first dashcam: GPS overlays, dual-camera, background recording."),
    CrossPromoApp("PixelDream: Offline AI Images", "com.hartmann.pixeldream", "Private, offline AI image generator. Your prompts and pictures never leave."),
    CrossPromoApp("Pocket-Assistant", "com.charles.pocketassistant", "Local AI organizer: save bills & notes, chat, tasks, and reminders on-device."),
    CrossPromoApp("TextPilot AI Messaging", "com.charles.messenger.v2", "Clean, fast SMS app with AI smart replies and web browser access to your texts."),
    CrossPromoApp("Pixel Fish Tank", "com.charles.virtualpet.fishtank", "Cozy virtual pet game — feed, clean & customize your pixel fish. Play & relax!"),
    CrossPromoApp("TrailSage AI: Road Trip Guide", "com.charles.trailsage", "Private, offline GPS audio tour guide with on-device AI storytelling."),
    CrossPromoApp("Knightfall: Chess with AI Coach", "com.chartmann.knightfall", "Play chess against Stockfish AI with Gemma 4 coaching, online, or on the web!"),
    CrossPromoApp("CaptionBurn: Video Captions", "com.charlesh.captionburn", "On-device auto-captions, burned into your video, with built-in translation."),
    CrossPromoApp("Jury Simulator: Trial Verdict", "com.charles.jurysim", "Step into jury duty with AI trials, eleven jurors, and the verdict in your hands."),
    CrossPromoApp("Photobooth Event Camera", "com.charles.photobooth", "Turn any Android device into a fun event photo booth with sharing and prints."),
    CrossPromoApp("Path - Daily Bible Study", "com.biblereadingpath.app", "Build daily Bible study habits with gentle streaks."),
    CrossPromoApp("Dreamloom: AI Dream Journal", "com.charles.app.dreamloom", "Private dream journal with on-device AI insights, symbols, and weekly patterns."),
    CrossPromoApp("SkyPulse: Live Flight Tracker", "com.charles.skypulse.app", "Track live flights overhead in real time — aircraft, airports & smart alerts."),
    CrossPromoApp("Grocy Fridge Scanner", "com.charleshartmann.grocyfridge", "Snap your fridge. On-device AI updates your Grocy stock in seconds. No cloud."),
    CrossPromoApp("CrowdTransit: Bus & Train", "com.charles.crowdtransit.app", "Find your ride — free live transit stops, schedules & reviews, nationwide."),
)

private fun openPlayStoreListing(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
            }
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    NebulaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("More from this developer") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(crossPromoApps) { app ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openPlayStoreListing(context, app.packageName) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.name, translate = false, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    app.tagline,
                                    translate = false,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
