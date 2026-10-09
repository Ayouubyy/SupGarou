package com.supgarou.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supgarou.app.data.normName
import com.supgarou.app.model.Handout
import com.supgarou.app.model.RoleDef
import com.supgarou.app.model.Team
import com.supgarou.app.model.tr

@Composable
fun SetupScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val d = vm.draft
    val total = d.counts.values.sum()
    val needed = vm.neededCards()
    ScreenScaffold(tr("New game", "Nouvelle partie"), onBack = { vm.back() }) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("Players", "Joueurs") + " ${d.players.size}") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Roles", "Rôles") + " $total/$needed") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(tr("Start", "Lancer")) })
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> PlayersTab(vm) { tab = 1 }
                1 -> RolesTab(vm, needed) { tab = 2 }
                else -> StartTab(vm, needed)
            }
        }
    }
}

@Composable
private fun PlayersTab(vm: AppViewModel, onNext: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val d = vm.draft
    val sugg = if (vm.settings.suggestNames && text.isNotBlank()) vm.suggestions(text, d.players.map { it.profileId }.toSet()) else emptyList()

    fun submit() {
        if (text.isBlank()) return
        val exact = sugg.firstOrNull { normName(it.name) == normName(text) }
        vm.addPlayer(exact?.name ?: text, exact?.id)
        text = ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(tr("Player name", "Nom du joueur")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = { IconButton(onClick = { submit() }) { Icon(Icons.Filled.Add, contentDescription = tr("Add", "Ajouter")) } },
        )
        if (sugg.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                sugg.forEachIndexed { i, p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.addPlayer(p.name, p.id); text = "" }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(p.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        val n = vm.gamesPlayed(p.id)
                        Text(tr("$n games", "$n parties"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                    if (i < sugg.lastIndex) HorizontalDivider()
                }
            }
        }
        if (vm.games.isNotEmpty()) {
            OutlinedButton(onClick = { vm.addLastGamePlayers() }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text("+ " + tr("Add everyone from the last game", "Ajouter tous les joueurs de la dernière partie"))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Seat order around the table — drag ≡ to reorder", "Ordre autour de la table — glisse ≡ pour réordonner"),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (d.players.isEmpty()) {
            Text(
                tr("No players yet. Type a name and press Enter.", "Aucun joueur. Tape un nom puis Entrée."),
                Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ReorderColumn(d.players, keyOf = { it.profileId }, onMove = { a, b -> vm.movePlayer(a, b) }, rowHeight = 54.dp) { index, p, handle ->
            Row(
                Modifier.fillMaxSize().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { vm.removePlayer(index) }) { Icon(Icons.Filled.Close, contentDescription = tr("Remove", "Retirer")) }
                Icon(Icons.Filled.DragHandle, contentDescription = tr("Drag", "Glisser"), modifier = handle.padding(12.dp))
            }
        }
        if (d.players.size >= 3) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text(tr("Next: roles", "Suivant : rôles")) }
        }
    }
}

@Composable
private fun RolesTab(vm: AppViewModel, needed: Int, onNext: () -> Unit) {
    val d = vm.draft
    val total = d.counts.values.sum()
    var presetMenu by remember { mutableStateOf(false) }
    var savePreset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val ok = total == needed
            Text(
                (if (ok) "✅ " else "⚠️ ") + tr("Roles: $total of $needed", "Rôles : $total sur $needed"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )
        }
        if (needed > d.players.size) {
            Text(
                tr("Includes ${needed - d.players.size} extra cards for the Voleur.", "Inclut ${needed - d.players.size} cartes en plus pour le Voleur."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { vm.fillVillagers() }, label = { Text(tr("Fill villagers", "Compléter en villageois")) })
            if (vm.settings.suggestMix) AssistChip(onClick = { vm.suggestMix() }, label = { Text(tr("Suggest a mix", "Suggérer")) })
            if (vm.games.isNotEmpty()) AssistChip(onClick = { vm.sameAsLastGame() }, label = { Text(tr("Same as last game", "Comme la dernière partie")) })
            Box {
                AssistChip(onClick = { presetMenu = true }, label = { Text(tr("Presets", "Préréglages")) })
                DropdownMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                    vm.presets.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = { vm.loadPreset(p); presetMenu = false },
                            trailingIcon = { IconButton(onClick = { vm.deletePreset(p) }) { Icon(Icons.Filled.Delete, contentDescription = null) } },
                        )
                    }
                    DropdownMenuItem(text = { Text(tr("Save this mix…", "Enregistrer ce mélange…")) }, onClick = { savePreset = true; presetMenu = false })
                }
            }
            AssistChip(onClick = { vm.clearCounts() }, label = { Text(tr("Clear", "Vider")) })
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.roles, key = { it.id }) { role ->
                RoleCountCard(role, d.counts[role.id] ?: 0) { vm.setCount(role.id, it) }
            }
            item {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(72.dp)) { Text(tr("Next", "Suivant")) }
            }
        }
    }
    if (savePreset) {
        TextInputDialog(
            tr("Save role mix", "Enregistrer le mélange"), "", tr("Name", "Nom"),
            onDone = { vm.savePreset(it); savePreset = false },
            onDismiss = { savePreset = false },
        )
    }
}

@Composable
private fun RoleCountCard(role: RoleDef, count: Int, onChange: (Int) -> Unit) {
    Card(
        Modifier.fillMaxWidth().height(72.dp).alpha(if (count == 0) 0.6f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = if (count > 0) BorderStroke(1.dp, teamColor(role.team).copy(alpha = 0.7f)) else null,
    ) {
        Row(Modifier.fillMaxSize().padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamStripe(teamColor(role.team), Modifier.padding(vertical = 10.dp))
            Text(role.icon, fontSize = 22.sp, modifier = Modifier.padding(start = 6.dp))
            Text(
                role.name,
                Modifier.weight(1f).padding(horizontal = 6.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChange(count - 1) }, enabled = count > 0, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "-")
                    }
                    Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { onChange(count + 1) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "+")
                    }
                }
            }
        }
    }
}

@Composable
private fun StartTab(vm: AppViewModel, needed: Int) {
    val d = vm.draft
    var confirm by remember { mutableStateOf(false) }
    val total = d.counts.values.sum()
    val wolves = d.counts.filterKeys { vm.teamOfRole(it) == Team.WOLVES }.values.sum()
    val solos = d.counts.filterKeys { vm.teamOfRole(it) == Team.SOLO }.values.sum()
    val villagers = d.counts["villageois"] ?: 0
    val specials = total - wolves - solos - villagers
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr("How are roles handed out?", "Comment les rôles sont-ils distribués ?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HandoutOption(
            vm, Handout.PHYSICAL, "🃏 " + tr("Physical cards", "Cartes physiques"),
            tr("Deal real cards; on night 1 the app asks who each role is as it wakes.", "Distribue de vraies cartes ; la nuit 1, l'app demande qui a chaque rôle quand il se réveille."),
        )
        HandoutOption(
            vm, Handout.APP_DEALS, "🎲 " + tr("App deals", "L'app distribue"),
            tr("The app shuffles; each player taps their name to see their role.", "L'app mélange ; chaque joueur touche son nom pour voir son rôle."),
        )
        HandoutOption(
            vm, Handout.MANUAL, "✍️ " + tr("Manual", "Manuel"),
            tr("Tap a role, then the players who have it.", "Touche un rôle, puis les joueurs qui l'ont."),
        )
        if (d.handout == Handout.MANUAL) ManualAssign(vm)

        SectionTitle(tr("Summary", "Résumé"))
        Text(
            tr(
                "${d.players.size} players · $wolves wolves · $specials village powers · $solos solo · $villagers villagers",
                "${d.players.size} joueurs · $wolves loups · $specials pouvoirs du village · $solos solitaires · $villagers villageois",
            ),
        )
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            d.counts.forEach { (id, c) ->
                val r = vm.role(id)
                AssistChip(onClick = {}, label = { Text("${r?.icon ?: ""} ${r?.name ?: id} ×$c") })
            }
        }
        val problems = buildList {
            if (d.players.size < 3) add(tr("Add at least 3 players.", "Ajoute au moins 3 joueurs."))
            if (total != needed) add(tr("There are $total role cards for $needed needed.", "Il y a $total cartes rôle pour $needed nécessaires."))
        }
        problems.forEach { Text("⚠️ $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (problems.isEmpty()) vm.startGame() else confirm = true },
            enabled = d.players.size >= 3,
            modifier = Modifier.fillMaxWidth().height(60.dp),
        ) {
            Text(
                if (d.handout == Handout.APP_DEALS) tr("Deal the roles", "Distribuer les rôles") else "🌙 " + tr("Start night 1", "Commencer la nuit 1"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (confirm) {
        ConfirmDialog(
            tr("Start anyway?", "Commencer quand même ?"),
            tr("The number of role cards doesn't match the players. You can fix roles during the game.", "Le nombre de cartes ne correspond pas aux joueurs. Tu pourras corriger les rôles pendant la partie."),
            tr("Start", "Commencer"),
            onConfirm = { vm.startGame() },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun HandoutOption(vm: AppViewModel, h: Handout, title: String, desc: String) {
    val selected = vm.draft.handout == h
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
            .border(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { vm.draft = vm.draft.copy(handout = h) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = { vm.draft = vm.draft.copy(handout = h) })
        Column(Modifier.padding(start = 6.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun ManualAssign(vm: AppViewModel) {
    val d = vm.draft
    var current by remember { mutableStateOf<String?>(null) }
    SectionTitle(tr("Assign roles", "Attribuer les rôles"))
    Text(
        tr("Pick a role, then tap the players who have it. Unassigned players are identified during night 1.", "Choisis un rôle puis touche les joueurs qui l'ont. Les joueurs sans rôle seront identifiés pendant la nuit 1."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        d.counts.forEach { (id, c) ->
            val used = d.manual.values.count { it == id }
            val r = vm.role(id)
            FilterChip(
                selected = current == id,
                onClick = { current = if (current == id) null else id },
                label = { Text("${r?.icon ?: ""} ${r?.name ?: id} ${c - used}") },
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        d.players.forEach { p ->
            val assigned = d.manual[p.profileId]
            FilterChip(
                selected = assigned != null,
                onClick = {
                    val role = current
                    if (role == null || assigned == role) vm.assignManual(p.profileId, null) else vm.assignManual(p.profileId, role)
                },
                label = { Text(p.name + (assigned?.let { "  " + (vm.role(it)?.icon ?: "") } ?: "")) },
            )
        }
    }
}

@Composable
fun RevealScreen(vm: AppViewModel) {
    val s = vm.state ?: return
    var showing by remember { mutableStateOf<Int?>(null) }
    var seen by remember { mutableStateOf(setOf<Int>()) }
    ScreenScaffold(tr("Pass the phone", "Faites passer le téléphone"), onBack = { vm.back() }) {
        Text(
            tr("Each player taps their name, looks at their role, then taps Hide.", "Chaque joueur touche son nom, regarde son rôle, puis touche Cacher."),
            Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            modifier = Modifier.weight(1f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(s.players, key = { it.pid }) { p ->
                Card(onClick = { showing = p.pid }, modifier = Modifier.height(64.dp)) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.pid in seen) Text("✅")
                    }
                }
            }
        }
        Button(onClick = { vm.finishReveal() }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp)) {
            Text("🌙 " + tr("Start night 1", "Commencer la nuit 1"), fontSize = 18.sp)
        }
    }
    showing?.let { pid ->
        val p = s.p(pid) ?: return@let
        val r = s.role(p.role)
        Dialog(onDismissRequest = { showing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Text(r?.icon ?: "❓", fontSize = 96.sp)
                Text(r?.name ?: tr("No role", "Pas de rôle"), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Box(Modifier.padding(vertical = 8.dp).width(120.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(teamColor(r?.team)))
                Text(r?.description ?: "", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { seen = seen + pid; showing = null }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(tr("Hide", "Cacher"), fontSize = 18.sp)
                }
            }
        }
    }
}
