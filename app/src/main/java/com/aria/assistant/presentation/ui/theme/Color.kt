package com.aria.assistant.presentation.ui.theme

import androidx.compose.ui.graphics.Color
import com.aria.assistant.domain.model.AriaState

// ── Nebula base canvas ──────────────────────────────────────────────────────
val NebulaBase     = Color(0xFF06060B)
val NebulaBase1    = Color(0xFF0E0E18)
val NebulaBase2    = Color(0xFF16162A)

// ── Aurora accent stops ──────────────────────────────────────────────────────
val AuroraViolet   = Color(0xFF7C5CFF)
val AuroraMagenta  = Color(0xFFC04AE0)
val AuroraTeal     = Color(0xFF22D3C5)
val AuroraLavender = Color(0xFFA78BFA)
val AuroraAmber    = Color(0xFFF59E0B)

// ── Glass material tokens ────────────────────────────────────────────────────
val GlassFill      = Color(0x0BFFFFFF)  // white @ 4.5%
val GlassStroke    = Color(0x17FFFFFF)  // white @ 9%
val GlassStrokeBright = Color(0x22FFFFFF) // white @ 13%

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary    = Color(0xFFF0EFFE)
val TextSecondary  = Color(0xFF9896B8)
val TextTertiary   = Color(0xFF5E5C80)

// ── Legacy aliases — keep names, point to Nebula values ──────────────────────
val AriaViolet     = AuroraViolet
val AriaLavender   = AuroraLavender
val AriaAmber      = AuroraAmber
val AriaBlue       = AuroraTeal
val AriaBackground = NebulaBase
val AriaSurface    = NebulaBase1
val AriaOnSurface  = TextPrimary

// ── State → color extension (public API, used by screens and orb) ────────────
fun AriaState.color(): Color = when (this) {
    AriaState.IDLE        -> AuroraViolet
    AriaState.LISTENING   -> AuroraTeal
    AriaState.PROCESSING  -> AuroraLavender
    AriaState.SPEAKING    -> AuroraAmber
    AriaState.MUTED       -> Color(0xFFE05555)
    AriaState.WAKING_UP   -> AuroraLavender
    AriaState.DOWNLOADING -> AuroraViolet
    AriaState.INITIALIZING -> AuroraLavender
    AriaState.ERROR       -> Color(0xFFEF4444)
}
