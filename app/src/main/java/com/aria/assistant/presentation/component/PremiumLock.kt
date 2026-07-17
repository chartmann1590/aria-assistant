package com.aria.assistant.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.aria.assistant.translation.TranslatedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aria.assistant.presentation.ui.theme.AriaLavender
import com.aria.assistant.presentation.ui.theme.AriaOnSurface

@Composable
fun PremiumLock(
    isPremium: Boolean,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        content()
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onUpgradeClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = AriaLavender
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Premium Feature",
                        color = AriaOnSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "Tap to upgrade",
                        color = AriaLavender,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
