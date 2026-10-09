package com.supgarou.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supgarou.app.engine.Engine
import com.supgarou.app.engine.GameState
import com.supgarou.app.engine.Phase
import com.supgarou.app.engine.StepResult
import com.supgarou.app.engine.causeText
import com.supgarou.app.model.Action
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.RuleSettings
import com.supgarou.app.model.Status
import com.supgarou.app.model.Team
import com.supgarou.app.model.VoyanteSees
import com.supgarou.app.model.teamName
import com.supgarou.app.model.tr

@Composable
fun ResultDialog(s: GameState, stepId: String, onDismiss: () -> Unit) {
    val r = s.results[stepId] ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
        title = { Text(Engine.stepTitle(s, stepId)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (r) {
                    is StepResult.Seen -> {
                        Text(tr("Show the Voyante:", "Montre à la Voyante :"), style = MaterialTheme.typography.bodyLarge)
                        if (s.rules.voyanteSees == VoyanteSees.TEAM) {
                            Text(if (r.shownTeam == Team.WOLVES) "🐺" else "🏡", fontSize = 72.sp)
                            Text(teamName(r.shownTeam), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        } else {
                            Text(s.roleIcon(r.shownRole), fontSize = 72.sp)
                            Text(s.roleName(r.shownRole), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("(${s.name(r.pid)})", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (r.hidden) Text(
                            tr("Loup blanc: she sees him as a Villageois.", "Loup blanc : elle le voit comme un Villageois."),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is StepResult.Sniff -> {
                        Text(if (r.found) "👍" else "👎", fontSize = 72.sp)
                        Text(
                            if (r.found) tr("YES, there is a wolf", "OUI, il y a un loup") else tr("NO wolf", "PAS de loup"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(s.names(r.pids), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (r.lostPower) Text(tr("The Renard loses his power.", "Le Renard perd son pouvoir."), fontWeight = FontWeight.SemiBold)
                    }
                    is StepResult.Sheep -> {
                        Text("🐑", fontSize = 56.sp)
                        r.outcomes.forEach { (pid, died) ->
                            Text(
                                s.name(pid) + ": " + if (died) "💀 " + tr("the sheep died (wolf)", "le mouton est mort (loup)") else "✅ " + tr("the sheep came back", "le mouton est revenu"),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        if (r.outcomes.isEmpty()) Text(tr("No sheep sent.", "Aucun mouton envoyé."))
                        Text(tr("Sheep left: ${r.left}", "Moutons restants : ${r.left}"), fontWeight = FontWeight.SemiBold)
                    }
                    StepResult.Blocked -> {
                        Text("🔒", fontSize = 56.sp)
                        Text(tr("Blocked or powerless: nothing happens tonight.", "Bloqué ou sans pouvoir : rien ne se passe cette nuit."), textAlign = TextAlign.Center)
                    }
                }
            }
        },
    )
}

/** What the Voyante saw, as the narrator tells it at dawn. */
fun seenText(s: GameState, seen: StepResult.Seen): String {
    val what = if (s.rules.voyanteSees == VoyanteSees.TEAM) teamName(seen.shownTeam)
    else "${s.roleIcon(seen.shownRole)} ${s.roleName(seen.shownRole)}"
    return tr("The Voyante looked at ${s.name(seen.pid)}: $what", "La Voyante a regardé ${s.name(seen.pid)} : $what")
}

fun blockedText(s: GameState, pid: Int): String {
    val role = s.roleName(s.p(pid)?.role)
    return tr(
        "${s.name(pid)} ($role) was blocked by the Loup rouge: no power last night",
        "${s.name(pid)} ($role) a été bloqué par le Loup rouge : pas de pouvoir cette nuit",
    )
}

@Composable
fun DawnDialog(s: GameState, onDismiss: () -> Unit) {
    val rep = s.dawn ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("☀️ " + tr("Start the day", "Commencer la journée")) } },
        title = { Text("🌅 " + tr("Dawn — day ${rep.night}", "Aube — jour ${rep.night}")) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rep.deaths.isEmpty()) {
                    Text("🎉 " + tr("Nobody died tonight.", "Personne n'est mort cette nuit."), style = MaterialTheme.typography.titleMedium)
                }
                rep.deaths.forEach { d ->
                    val p = s.p(d.pid) ?: return@forEach
                    Column {
                        Text("💀 ${p.name} — ${Engine.revealText(s, p)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(causeText(d.cause), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                rep.oursGrowl?.let {
                    Text("🐻 Ours: " + if (it) tr("GRRR! (growl)", "GRRR ! (grognement)") else tr("silence", "silence"), fontWeight = FontWeight.SemiBold)
                }
                rep.grandOursGrowl?.let {
                    Text(
                        "🧸 Grand ours (${s.name(rep.grandOursTarget)}): " + if (it) tr("GRRR! (growl)", "GRRR ! (grognement)") else tr("silence", "silence"),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                rep.seen?.let { Text("🔮 " + seenText(s, it), fontWeight = FontWeight.SemiBold) }
                rep.blocked.forEach { Text("🔒 " + blockedText(s, it), fontWeight = FontWeight.SemiBold) }
                if (rep.muted.isNotEmpty()) Text("🔇 " + s.names(rep.muted) + tr(" is muted today", " est muet aujourd'hui"))
                if (rep.corbeau.isNotEmpty()) Text("🐦 " + s.names(rep.corbeau) + tr(" starts the vote with ${s.rules.corbeauVotes} votes against", " commence le vote avec ${s.rules.corbeauVotes} voix contre lui"))
                if (rep.starter.isNotEmpty()) Text("🗣️ " + s.names(rep.starter) + tr(" starts talking and voting", " commence à parler et à voter"))
                if (rep.juge.isNotEmpty() || rep.sheep.isNotEmpty() || rep.notes.isNotEmpty()) {
                    HorizontalDivider()
                    Text("🤫 " + tr("For the narrator only", "Pour le narrateur seulement"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (rep.juge.isNotEmpty()) Text("⚖️ " + tr("Protected from the vote: ", "Protégé du vote : ") + s.names(rep.juge))
                if (rep.sheep.isNotEmpty()) Text(
                    "🐑 " + tr("Tell the Berger: ", "Dis au Berger : ") +
                        rep.sheep.joinToString(", ") { (pid, died) -> s.name(pid) + if (died) " 💀" else " ✅" },
                )
                rep.notes.forEach { Text("• $it") }
                if (s.pendingShots.isNotEmpty()) Text("🎯 " + tr("The Chasseur shoots next.", "Le Chasseur tire ensuite."), fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun EndGameDialog(s: GameState, preselect: List<Int>, onEnd: (List<Int>, String) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(preselect.toSet()) }
    var label by remember { mutableStateOf(if (preselect.size == 1) s.roleName(s.p(preselect.first())?.role) else "") }
    fun pick(set: Collection<Int>, l: String) { chosen = set.toSet(); label = l }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🏁 " + tr("End the game", "Terminer la partie")) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(tr("Who won?", "Qui a gagné ?"), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { pick(s.players.filter { s.teamOf(it) == Team.VILLAGE }.map { it.pid }, teamName(Team.VILLAGE)) }, label = { Text("🏡 " + teamName(Team.VILLAGE)) })
                    AssistChip(onClick = { pick(s.players.filter { s.teamOf(it) == Team.WOLVES }.map { it.pid }, teamName(Team.WOLVES)) }, label = { Text("🐺 " + teamName(Team.WOLVES)) })
                    val lovers = s.players.filter { it.lover != null }
                    if (lovers.isNotEmpty()) AssistChip(onClick = { pick(lovers.map { it.pid }, tr("Lovers", "Amoureux")) }, label = { Text("💞 " + tr("Lovers", "Amoureux")) })
                    s.players.filter { s.teamOf(it) == Team.SOLO }.forEach { p ->
                        AssistChip(onClick = { pick(listOf(p.pid), s.roleName(p.role)) }, label = { Text("${s.roleIcon(p.role)} ${p.name}") })
                    }
                    AssistChip(onClick = { pick(emptyList(), tr("Nobody", "Personne")) }, label = { Text(tr("Nobody", "Personne")) })
                }
                Spacer(Modifier.height(8.dp))
                s.players.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = if (p.pid in chosen) chosen - p.pid else chosen + p.pid; label = "" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = p.pid in chosen, onCheckedChange = null)
                        Text("${s.roleIcon(p.role)} ${p.name}" + if (!p.alive) " 💀" else "", Modifier.padding(start = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEnd(chosen.toList(), label.ifBlank { tr("Custom", "Personnalisé") }) }) {
                Text(tr("End the game", "Terminer"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Annuler")) } },
    )
}

private fun targetsOf(a: Action): List<Int>? = when (a) {
    is Action.Step -> a.targets
    is Action.Vote -> listOfNotNull(a.pid)
    is Action.Shot -> listOfNotNull(a.target)
    is Action.AlienGuess -> listOf(a.target)
    is Action.Assign -> a.pids
    is Action.Kill -> listOf(a.pid)
    else -> null
}

private fun maxTargetsOf(a: Action, n: Int): Int = when (a) {
    is Action.Step, is Action.Assign -> n
    else -> 1
}

private fun withTargets(a: Action, t: List<Int>): Action? = when (a) {
    is Action.Step -> a.copy(targets = t, skipped = false)
    is Action.Vote -> a.copy(pid = t.firstOrNull())
    is Action.Shot -> a.copy(target = t.firstOrNull())
    is Action.AlienGuess -> t.firstOrNull()?.let { a.copy(target = it) }
    is Action.Assign -> a.copy(pids = t)
    is Action.Kill -> t.firstOrNull()?.let { a.copy(pid = it) }
    else -> null
}

@Composable
fun LogDialog(vm: AppViewModel, s: GameState, rec: GameRecord, onDismiss: () -> Unit) {
    var menuFor by remember { mutableStateOf<Int?>(null) }
    var editFor by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column {
                TopAppBar(
                    title = { Text(tr("Game log", "Journal de la partie")) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = tr("Close", "Fermer")) } },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
                Text(
                    tr("Tap an action to change it, delete it, or undo back to it. The game replays from the start.", "Touche une action pour la modifier, la supprimer, ou revenir jusqu'à elle. La partie est rejouée depuis le début."),
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (s.log.isEmpty()) Text(tr("Nothing yet.", "Rien pour l'instant."), Modifier.padding(16.dp))
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    items(s.log.asReversed(), key = { it.index }) { line ->
                        Box {
                            Row(
                                Modifier.fillMaxWidth().clickable { menuFor = line.index }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(line.label, Modifier.width(72.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Column(Modifier.weight(1f)) {
                                    Text(line.text, style = MaterialTheme.typography.bodyMedium)
                                    line.flag?.let { Text("⚠️ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                }
                            }
                            DropdownMenu(expanded = menuFor == line.index, onDismissRequest = { menuFor = null }) {
                                val action = rec.actions.getOrNull(line.index)
                                if (action != null && targetsOf(action) != null) {
                                    DropdownMenuItem(text = { Text("✏️ " + tr("Change players…", "Changer les joueurs…")) }, onClick = { editFor = line.index; menuFor = null })
                                }
                                DropdownMenuItem(text = { Text("↩️ " + tr("Undo back to here", "Annuler jusqu'ici")) }, onClick = { vm.undoFrom(line.index); menuFor = null })
                                DropdownMenuItem(text = { Text("🗑️ " + tr("Delete this action", "Supprimer cette action")) }, onClick = { vm.deleteAction(line.index); menuFor = null })
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
    editFor?.let { i ->
        val action = rec.actions.getOrNull(i) ?: return@let
        val current = targetsOf(action) ?: return@let
        PlayerPickerDialog(
            tr("Choose the players", "Choisis les joueurs"),
            s.players.map { it.pid to it.name },
            current,
            maxTargetsOf(action, s.players.size),
            onDone = { t -> withTargets(action, t)?.let { vm.replaceAction(i, it) }; editFor = null },
            onDismiss = { editFor = null },
        )
    }
}

@Composable
fun PlayerSheet(vm: AppViewModel, s: GameState, pid: Int, onDismiss: () -> Unit) {
    val p = s.p(pid) ?: return
    var rename by remember(pid) { mutableStateOf(p.name) }
    var pickRole by remember { mutableStateOf(false) }
    var pickLover by remember { mutableStateOf(false) }
    var confirmKill by remember { mutableStateOf(false) }
    val r = s.role(p.role)
    val cycle = if (s.phase == Phase.NIGHT) s.night else s.day
    val nextNight = if (s.phase == Phase.NIGHT) s.night else s.day + 1
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r?.icon ?: "❓", fontSize = 40.sp, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        (r?.name ?: tr("Unknown role", "Rôle inconnu")) + " · " + teamName(s.teamOf(p)) +
                            (if (p.startRole != null && p.startRole != p.role) " · " + tr("was ", "était ") + s.roleName(p.startRole) else ""),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!p.alive) Text("💀 " + causeText(p.cause) + (p.diedAt?.let { " ($it)" } ?: ""), Modifier.padding(top = 6.dp))
            r?.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionTitle(tr("Player", "Joueur"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = rename, onValueChange = { rename = it }, label = { Text(tr("Name", "Nom")) }, singleLine = true, modifier = Modifier.weight(1f))
                TextButton(onClick = { if (rename.isNotBlank() && rename != p.name) vm.dispatch(Action.Rename(pid, rename.trim())) }) { Text(tr("Rename", "Renommer")) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickRole = true }) { Text("🃏 " + tr("Change role", "Changer de rôle")) }
                if (p.alive) {
                    OutlinedButton(onClick = { if (vm.settings.confirmDestructive) confirmKill = true else vm.dispatch(Action.Kill(pid)) }) { Text("💀 " + tr("Kill", "Tuer")) }
                } else {
                    OutlinedButton(onClick = { vm.dispatch(Action.Revive(pid)) }) { Text("❤️‍🩹 " + tr("Revive", "Ressusciter")) }
                }
            }
            SectionTitle(tr("Statuses", "États"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val items = listOf(
                    Triple(Status.PROTECTED, "🛡️", p.protectedNight == nextNight),
                    Triple(Status.BLOCKED, "🔒", p.blockedNight == nextNight),
                    Triple(Status.MUTED, "🔇", p.mutedDay == cycle),
                    Triple(Status.CHARMED, "🎶", p.charmed),
                    Triple(Status.INFECTED, "🦠", p.infected),
                    Triple(Status.JUGE, "⚖️", p.jugeDay != null),
                    Triple(Status.CORBEAU, "🐦", p.corbeauDay == cycle),
                    Triple(Status.POWERLESS, "🚫", p.powerless),
                )
                items.forEach { (st, icon, on) ->
                    FilterChip(
                        selected = on,
                        onClick = { vm.dispatch(Action.SetStatus(pid, st, !on)) },
                        label = { Text("$icon ${Engine.statusName(st)}") },
                    )
                }
            }
            SectionTitle(tr("Lover", "Amoureux"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("❤️ " + (p.lover?.let { s.name(it) } ?: tr("none", "aucun")), Modifier.weight(1f))
                TextButton(onClick = { pickLover = true }) { Text(tr("Link…", "Lier…")) }
                if (p.lover != null) TextButton(onClick = { vm.dispatch(Action.SetLovers(pid, null)) }) { Text(tr("Remove", "Retirer")) }
            }
            SectionTitle(tr("Seat", "Place"))
            val idx = s.seating.indexOf(pid)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.dispatch(Action.MoveSeat(pid, idx - 1)) }, enabled = idx > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = tr("Earlier seat", "Place précédente"))
                }
                Text(tr("Seat ${idx + 1} of ${s.seating.size}", "Place ${idx + 1} sur ${s.seating.size}"))
                IconButton(onClick = { vm.dispatch(Action.MoveSeat(pid, idx + 1)) }, enabled = idx < s.seating.lastIndex) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("Later seat", "Place suivante"))
                }
                Text(
                    tr("Neighbors: ", "Voisins : ") + s.names(s.neighbors(pid)),
                    Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (pickRole) {
        RolePickerDialog(
            tr("Role of ${p.name}", "Rôle de ${p.name}"),
            s.setup.roles,
            onPick = { vm.dispatch(Action.SetRole(pid, it)); pickRole = false },
            onDismiss = { pickRole = false },
        )
    }
    if (pickLover) {
        PlayerPickerDialog(
            tr("Lover of ${p.name}", "Amoureux de ${p.name}"),
            s.players.filter { it.pid != pid }.map { it.pid to it.name },
            listOfNotNull(p.lover),
            1,
            onDone = { t -> t.firstOrNull()?.let { vm.dispatch(Action.SetLovers(pid, it)) }; pickLover = false },
            onDismiss = { pickLover = false },
        )
    }
    if (confirmKill) {
        ConfirmDialog(
            tr("Kill ${p.name}?", "Tuer ${p.name} ?"),
            tr("Lovers and the Chasseur react as usual. You can undo it.", "Les amoureux et le Chasseur réagissent normalement. Tu peux annuler."),
            tr("Kill", "Tuer"),
            onConfirm = { vm.dispatch(Action.Kill(pid)) },
            onDismiss = { confirmKill = false },
        )
    }
}

@Composable
fun RulesDialog(rules: RuleSettings, onSave: (RuleSettings) -> Unit, onDismiss: () -> Unit) {
    var r by remember { mutableStateOf(rules) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column {
                TopAppBar(
                    title = { Text(tr("Rules for this game", "Règles de cette partie")) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = tr("Close", "Fermer")) } },
                    actions = { TextButton(onClick = { onSave(r) }) { Text(tr("Save", "Enregistrer")) } },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    RuleEditor(r) { r = it }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
