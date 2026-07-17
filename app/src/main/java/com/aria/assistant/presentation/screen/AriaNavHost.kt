package com.aria.assistant.presentation.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aria.assistant.presentation.component.NebulaBackground

@Composable
fun AriaNavHost(navController: NavHostController, startDestination: String = "onboarding") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = { navController.navigate("main") { popUpTo("onboarding") { inclusive = true } } },
                onNavigateToPermissions = { navController.navigate("permissions") }
            )
        }
        composable("main") {
            MainScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToPremium = { navController.navigate("premium") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onUpgrade = { navController.navigate("premium") },
                onNavigateToPermissions = { navController.navigate("permissions") },
                onNavigateToAbout = { navController.navigate("about") },
                onNavigateToFeedback = { navController.navigate("feedback") }
            )
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable("permissions") {
            PermissionsScreen(onBack = { navController.popBackStack() })
        }
        composable("premium") {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable("feedback") {
            FeedbackScreen(onBack = { navController.popBackStack() })
        }
    }
}
