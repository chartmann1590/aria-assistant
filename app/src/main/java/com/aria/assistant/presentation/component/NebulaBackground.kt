package com.aria.assistant.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aria.assistant.presentation.ui.theme.AuroraMagenta
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.NebulaBase

@Composable
fun NebulaBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_drift"
    )

    Box(modifier = modifier.background(NebulaBase)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Violet blob — top-left, drifts slowly
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraViolet.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(w * (0.15f + drift * 0.10f), h * (0.20f - drift * 0.08f)),
                    radius = h * 0.55f
                )
            )
            // Magenta blob — top-right
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraMagenta.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * (0.85f - drift * 0.08f), h * (0.10f + drift * 0.05f)),
                    radius = h * 0.45f
                )
            )
            // Teal blob — bottom-right
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(AuroraTeal.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * (0.75f + drift * 0.06f), h * (0.85f - drift * 0.04f)),
                    radius = h * 0.50f
                )
            )
        }
        content()
    }
}
