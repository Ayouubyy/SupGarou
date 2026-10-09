package com.supgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supgarou.app.engine.Engine
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.tr
import java.text.DateFormat
import java.util.Date

private fun dateText(t: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(t))

private fun durationText(g: GameRecord): String {
    val end = g.endedAt ?: return ""
    val min = ((end - g.startedAt) / 60000).toInt()
    return if (min >= 60) "${min / 60}h${"%02d".format(min % 60)}" else "$min min"
}

@Composable
fun HistoryScreen(vm: AppViewModel) {
    ScreenScaffold(tr("Game history", "Historique des parties"), onBack = { vm.back() }) {
        if (vm.games.isEmpty()) {
            Text(tr("No games yet.", "Aucune partie pour l'instant."), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.games, key = { it.id }) { g ->
                Card(
                    onClick = { vm.go(Screen.GameDetail(g.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(dateText(g.startedAt), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            if (g.endedAt == null) Text("⏳ " + tr("In progress", "En cours"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            else Text(durationText(g), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Text(
                            tr("${g.setup.players.size} players", "${g.setup.players.size} joueurs") +
                                (g.winnerLabel?.let { " · 🏆 $it" } ?: ""),
                        )
                        Text(
                            g.setup.players.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameDetailScreen(vm: AppViewModel, id: String) {
    val g = vm.games.firstOrNull { it.id == id }
    var confirmDelete by remember { mutableStateOf(false) }
    if (g == null) {
        ScreenScaffold(tr("Game", "Partie"), onBack = { vm.back() }) {
            Text(tr("This game no longer exists.", "Cette partie n'existe plus."), Modifier.padding(24.dp))
        }
        return
    }
    val s = remember(g) { Engine.replay(g) }
    ScreenScaffold(
        dateText(g.startedAt),
        onBack = { vm.back() },
        actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = tr("Delete", "Supprimer")) } },
    ) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    if (g.endedAt == null) "⏳ " + tr("In progress — ", "En cours — ") + s.label()
                    else "🏆 " + (g.winnerLabel ?: "") + "  ·  " + durationText(g),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.playAgain(g) }) { Text("🔁 " + tr("Play again", "Rejouer")) }
                    if (g.endedAt == null && vm.record?.id == g.id) {
                        OutlinedButton(onClick = { vm.go(Screen.Game) }) { Text(tr("Resume", "Reprendre")) }
                    }
                }
                SectionTitle(tr("Players", "Joueurs"))
            }
            items(s.seating) { pid ->
                val p = s.players[pid]
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.seating.indexOf(pid) + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.roleIcon(p.role), Modifier.width(32.dp), fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name + if (pid in g.winners) "  🏆" else "", fontWeight = FontWeight.SemiBold)
                        val roleLine = if (p.startRole != null && p.startRole != p.role) "${s.roleName(p.startRole)} → ${s.roleName(p.role)}" else s.roleName(p.role)
                        Text(
                            roleLine + (if (p.infected) " 🦠" else "") + (if (p.lover != null) " ❤️" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(if (p.alive) "🟢" else "💀 ${p.diedAt ?: ""}", fontSize = 13.sp)
                }
                HorizontalDivider()
            }
            item { SectionTitle(tr("What happened", "Déroulement")) }
            items(s.log) { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text(line.label, Modifier.width(72.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(line.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            tr("Delete this game?", "Supprimer cette partie ?"),
            tr("It is removed from the history and from player stats.", "Elle est retirée de l'historique et des statistiques."),
            tr("Delete", "Supprimer"),
            onConfirm = { vm.deleteGame(id); vm.back() },
            onDismiss = { confirmDelete = false },
        )
    }
}
