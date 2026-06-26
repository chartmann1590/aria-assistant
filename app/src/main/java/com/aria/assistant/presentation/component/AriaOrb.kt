package com.aria.assistant.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aria.assistant.domain.model.AriaState
import com.aria.assistant.presentation.ui.theme.AuroraAmber
import com.aria.assistant.presentation.ui.theme.AuroraTeal
import com.aria.assistant.presentation.ui.theme.AuroraViolet
import com.aria.assistant.presentation.ui.theme.color
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AriaOrb(
    state: AriaState,
    modifier: Modifier = Modifier
) {
    // Slow breath when idle
    val breathAnim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            breathAnim.animateTo(1.025f, tween(3000, easing = FastOutSlowInEasing))
            breathAnim.animateTo(1f, tween(3000, easing = FastOutSlowInEasing))
        }
    }

    // Faster pulse when listening
    val listenAnim = remember { Animatable(1f) }
    LaunchedEffect(state == AriaState.LISTENING) {
        if (state == AriaState.LISTENING) {
            while (isActive) {
                listenAnim.animateTo(1.09f, tween(550, easing = FastOutSlowInEasing))
                listenAnim.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            }
        } else {
            listenAnim.snapTo(1f)
        }
    }

    // Expanding rings — 2 with phase offset
    val ring1 = remember { Animatable(0f) }
    val ring2 = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            while (isActive) {
                ring1.snapTo(0f)
                ring1.animateTo(1f, tween(2600, easing = LinearOutSlowInEasing))
            }
        }
        kotlinx.coroutines.delay(1300)
        launch {
            while (isActive) {
                ring2.snapTo(0f)
                ring2.animateTo(1f, tween(2600, easing = LinearOutSlowInEasing))
            }
        }
    }

    val rawScale = when (state) {
        AriaState.IDLE        -> breathAnim.value
        AriaState.LISTENING   -> listenAnim.value
        AriaState.SPEAKING    -> 1.07f
        AriaState.MUTED       -> 0.68f
        else                  -> 1.03f
    }
    val stateScale by animateFloatAsState(
        targetValue = rawScale,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "orbScale"
    )

    val orbColor by animateColorAsState(state.color(), tween(500), label = "orbColor")

    val glowAlpha by animateFloatAsState(
        targetValue = if (state == AriaState.IDLE || state == AriaState.MUTED) 0.22f else 0.38f,
        animationSpec = tween(400),
        label = "glowAlpha"
    )

    // Ring visibility — only show when not muted
    val ringVisible = state != AriaState.MUTED
    val ringAlphaScale by animateFloatAsState(
        targetValue = if (ringVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "ringVisible"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(220.dp)) {

        // Outer bloom — large soft radial gradient behind orb
        Canvas(modifier = Modifier.size(280.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orbColor.copy(alpha = glowAlpha * 0.65f), Color.Transparent)
                ),
                radius = size.minDimension / 2
            )
        }

        // Expanding ring 1
        val r1Scale = 0.68f + ring1.value * 0.62f
        val r1Alpha = 0.45f * (1f - ring1.value) * ringAlphaScale
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .scale(r1Scale)
                .alpha(r1Alpha)
        ) {
            drawCircle(
                color = orbColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Expanding ring 2 (phase offset)
        val r2Scale = 0.68f + ring2.value * 0.62f
        val r2Alpha = 0.45f * (1f - ring2.value) * ringAlphaScale
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .scale(r2Scale)
                .alpha(r2Alpha)
        ) {
            drawCircle(
                color = orbColor,
                radius = size.minDimension / 2,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Main orb body
        Canvas(modifier = Modifier.size(180.dp).scale(stateScale)) {
            val orbRadius = size.minDimension / 2

            // Multi-stop radial gradient: white shimmer → accent color → deep color → near-black
            val deepColor = when (state) {
                AriaState.LISTENING   -> Color(0xFF042820)
                AriaState.SPEAKING    -> Color(0xFF2A1000)
                AriaState.MUTED       -> Color(0xFF140808)
                AriaState.ERROR       -> Color(0xFF1A0404)
                else                  -> Color(0xFF08031A)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.22f),
                        0.28f to orbColor.copy(alpha = 0.95f),
                        0.65f to deepColor.copy(alpha = 0.88f),
                        1.00f to Color(0xFF020204).copy(alpha = 0.97f)
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.32f),
                    radius = orbRadius
                ),
                radius = orbRadius
            )

            // Small shimmer highlight — upper-left
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = orbRadius * 0.18f,
                center = Offset(size.width * 0.30f, size.height * 0.26f)
            )
        }
    }
}
