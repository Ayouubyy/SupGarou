package com.supgarou.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supgarou.app.data.PlayerStats
import com.supgarou.app.data.computeStats
import com.supgarou.app.data.normName
import com.supgarou.app.model.PlayerProfile
import com.supgarou.app.model.Team
import com.supgarou.app.model.teamName
import com.supgarou.app.model.tr
import java.text.DateFormat
import java.util.Date

@Composable
fun PlayersScreen(vm: AppViewModel) {
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf<PlayerProfile?>(null) }
    val stats = remember(vm.games) { computeStats(vm.games) }
    val list = vm.profiles
        .filter { query.isBlank() || normName(it.name).contains(normName(query)) }
        .sortedWith(compareByDescending<PlayerProfile> { stats[it.id]?.games ?: 0 }.thenBy { it.name.lowercase() })
    ScreenScaffold(tr("Players", "Joueurs"), onBack = { vm.back() }) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text(tr("Search", "Rechercher")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        if (vm.profiles.isEmpty()) {
            Text(
                tr("Players appear here after their first game.", "Les joueurs apparaissent ici après leur première partie."),
                Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { p ->
                val st = stats[p.id] ?: PlayerStats()
                Card(
                    onClick = { open = p },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.Bold)
                            Text(
                                tr("${st.games} games · ${st.wins} wins (${st.winRate}%)", "${st.games} parties · ${st.wins} victoires (${st.winRate} %)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        st.topRole?.let { Text(vm.role(it)?.icon ?: "") }
                    }
                }
            }
        }
    }
    open?.let { p -> PlayerDialog(vm, p, stats[p.id] ?: PlayerStats()) { open = null } }
}

@Composable
private fun PlayerDialog(vm: AppViewModel, p: PlayerProfile, st: PlayerStats, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(p.name) }
    var merge by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Games played: ${st.games}", "Parties jouées : ${st.games}"))
                Text(tr("Wins: ${st.wins} (${st.winRate}%)", "Victoires : ${st.wins} (${st.winRate} %)"))
                Team.entries.forEach { t ->
                    val n = st.winsByTeam[t] ?: 0
                    if (n > 0) Text("  • ${teamName(t)}: $n")
                }
                st.topRole?.let { Text(tr("Most played role: ", "Rôle le plus joué : ") + "${vm.role(it)?.icon ?: ""} ${vm.role(it)?.name ?: it}") }
                if (st.roles.isNotEmpty()) {
                    Text(
                        st.roles.entries.sortedByDescending { it.value }.joinToString(", ") { "${vm.role(it.key)?.name ?: it.key} ×${it.value}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (st.last > 0) Text(tr("Last played: ", "Dernière partie : ") + DateFormat.getDateInstance().format(Date(st.last)))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Name", "Nom")) }, singleLine = true)
                Row {
                    TextButton(onClick = { vm.renameProfile(p.id, name); onDismiss() }, enabled = name.isNotBlank() && name != p.name) { Text(tr("Rename", "Renommer")) }
                    TextButton(onClick = { merge = true }) { Text(tr("Merge into…", "Fusionner avec…")) }
                    TextButton(onClick = { delete = true }) { Text(tr("Delete", "Supprimer")) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
    if (merge) {
        PlayerPickerDialog(
            tr("Merge ${p.name} into…", "Fusionner ${p.name} avec…"),
            vm.profiles.filter { it.id != p.id }.mapIndexed { i, o -> i to o.name },
            emptyList(),
            1,
            onDone = { picked ->
                val others = vm.profiles.filter { it.id != p.id }
                picked.firstOrNull()?.let { others.getOrNull(it) }?.let { vm.mergeProfiles(p.id, it.id) }
                merge = false
                onDismiss()
            },
            onDismiss = { merge = false },
        )
    }
    if (delete) {
        ConfirmDialog(
            tr("Delete ${p.name}?", "Supprimer ${p.name} ?"),
            tr("Past games keep their names; they no longer count for this player.", "Les anciennes parties gardent le nom ; elles ne comptent plus pour ce joueur."),
            tr("Delete", "Supprimer"),
            onConfirm = { vm.deleteProfile(p.id); onDismiss() },
            onDismiss = { delete = false },
        )
    }
}
