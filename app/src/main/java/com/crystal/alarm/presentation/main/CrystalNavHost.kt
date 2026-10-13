package com.crystal.alarm.presentation.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.crystal.alarm.presentation.editor.EditorScreen
import com.crystal.alarm.presentation.home.HomeScreen
import com.crystal.alarm.presentation.register.RegisterCodeScreen
import com.crystal.alarm.presentation.settings.SettingsScreen
import com.crystal.alarm.presentation.sound.SoundScreen
import com.crystal.alarm.presentation.theme.CrystalColors
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object EditorRoute
@Serializable data object SettingsRoute
@Serializable data object RegisterCodeRoute
@Serializable data object SoundRoute

private const val DURATION = 320

@Composable
fun CrystalNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier
            .fillMaxSize()
            .background(CrystalColors.Background),
        enterTransition = { slideInHorizontally(tween(DURATION)) { it / 4 } + fadeIn(tween(DURATION)) },
        exitTransition = { slideOutHorizontally(tween(DURATION)) { -it / 8 } + fadeOut(tween(DURATION)) },
        popEnterTransition = { slideInHorizontally(tween(DURATION)) { -it / 8 } + fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutHorizontally(tween(DURATION)) { it / 4 } + fadeOut(tween(DURATION)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onEditAlarm = { navController.navigate(EditorRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onRegisterCode = { navController.navigate(RegisterCodeRoute) },
            )
        }
        composable<EditorRoute> {
            EditorScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onRegisterCode = { navController.navigate(RegisterCodeRoute) },
                onOpenSound = { navController.navigate(SoundRoute) },
            )
        }
        composable<SoundRoute> {
            SoundScreen(onBack = { navController.popBackStack() })
        }
        composable<RegisterCodeRoute> {
            RegisterCodeScreen(onDone = { navController.popBackStack() })
        }
    }
}
