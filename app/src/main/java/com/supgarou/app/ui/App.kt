package com.supgarou.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun App(vm: AppViewModel) {
    val ctx = LocalContext.current
    BackHandler(enabled = vm.nav.size > 1) { vm.back() }
    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            vm.toast = null
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (val screen = vm.screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.Setup -> SetupScreen(vm)
                Screen.Reveal -> RevealScreen(vm)
                Screen.Game -> GameScreen(vm)
                Screen.History -> HistoryScreen(vm)
                is Screen.GameDetail -> GameDetailScreen(vm, screen.id)
                Screen.Players -> PlayersScreen(vm)
                Screen.Roles -> RolesScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
            }
        }
    }
}
