package com.aria.assistant.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.aria.assistant.billing.BillingManager
import com.aria.assistant.domain.repository.SettingsRepository
import com.aria.assistant.engine.AriaLogger
import com.aria.assistant.presentation.screen.AriaNavHost
import com.aria.assistant.presentation.ui.theme.AriaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AriaLogger.d("MainActivity", "Activity onCreate")
        billingManager.checkExistingPurchases()
        setContent {
            val onboardingComplete by settingsRepository.isOnboardingComplete().collectAsStateWithLifecycle(initialValue = false)
            val startDest = if (onboardingComplete) "main" else "onboarding"
            AriaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AriaNavHost(navController = navController, startDestination = startDest)
                }
            }
        }
    }
}
