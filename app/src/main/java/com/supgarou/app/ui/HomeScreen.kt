package com.supgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supgarou.app.model.tr

@Composable
fun HomeScreen(vm: AppViewModel) {
    val st = vm.state
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Text("🌙🐺", fontSize = 52.sp)
        Text("Sup'Garou", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(tr("Narrator companion", "Compagnon du narrateur"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (st != null) {
                ElevatedCard(onClick = { vm.go(Screen.Game) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(tr("Resume game", "Reprendre la partie"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val alive = st.players.count { it.alive }
                            Text(
                                "${st.label()} · " + tr("$alive of ${st.players.size} players alive", "$alive joueurs vivants sur ${st.players.size}"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            Button(onClick = { vm.startNewSetup() }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  " + tr("New game", "Nouvelle partie"), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTile(Icons.Filled.History, tr("Game history", "Historique"), Modifier.weight(1f)) { vm.go(Screen.History) }
                HomeTile(Icons.Filled.Groups, tr("Players", "Joueurs"), Modifier.weight(1f)) { vm.go(Screen.Players) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeTile(Icons.Filled.Style, tr("Roles", "Rôles"), Modifier.weight(1f)) { vm.go(Screen.Roles) }
                HomeTile(Icons.Filled.Settings, tr("Settings", "Réglages"), Modifier.weight(1f)) { vm.go(Screen.Settings) }
            }
        }
        Spacer(Modifier.height(32.dp))
        Credits()
    }
}

@Composable
private fun Credits() {
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(tr("Credits", "Crédits"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(tr("Created by ", "Créé par ") + "Ayoub Said", fontWeight = FontWeight.SemiBold)
        Text("(totally not vibe coded by claude)", style = MaterialTheme.typography.bodySmall, color = quiet, fontStyle = FontStyle.Italic)
        Text(tr("Idea by ", "Idée de ") + "Aya Shkiri", fontWeight = FontWeight.SemiBold)
        Text(tr("Honorable mentions", "Mentions honorables"), style = MaterialTheme.typography.bodySmall, color = quiet)
        listOf("Moslem Brahem", "Mohamed Aziz Jouini", "Adem Ben Salah").forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HomeTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
