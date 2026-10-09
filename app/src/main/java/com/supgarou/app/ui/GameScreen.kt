package com.supgarou.app.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supgarou.app.engine.Engine
import com.supgarou.app.engine.GameState
import com.supgarou.app.engine.NightStep
import com.supgarou.app.engine.P
import com.supgarou.app.engine.Phase
import com.supgarou.app.engine.StepKind
import com.supgarou.app.model.Action
import com.supgarou.app.model.RoleIds
import com.supgarou.app.model.StepIds
import com.supgarou.app.model.Team
import com.supgarou.app.model.tr
import kotlinx.coroutines.delay

sealed class Mode {
    data class Identify(val step: NightStep, val role: String, val count: Int) : Mode()
    data class Extras(val step: NightStep, val count: Int) : Mode()
    data class Act(val step: NightStep, val editIndex: Int?) : Mode()
    data object NightOver : Mode()
    data class Shot(val hunter: Int) : Mode()
    data object Vote : Mode()
    data object AfterVote : Mode()
    data class Alien(val alien: Int) : Mode()
}

private fun computeMode(s: GameState, steps: List<NightStep>, editStep: String?, alienMode: Boolean): Mode {
    if (s.pendingShots.isNotEmpty()) return Mode.Shot(s.pendingShots.first())
    if (s.phase == Phase.DAY) {
        if (alienMode) s.holders(RoleIds.ALIEN).firstOrNull { it.alive }?.let { return Mode.Alien(it.pid) }
        return if (s.votedToday) Mode.AfterVote else Mode.Vote
    }
    if (editStep != null) {
        steps.firstOrNull { it.id == editStep }?.let { return Mode.Act(it, s.done[editStep]) }
    }
    val step = steps.firstOrNull { it.id !in s.done } ?: return Mode.NightOver
    step.identify.firstOrNull { s.needsIdentify(it) }?.let { r ->
        return Mode.Identify(step, r, (s.activeCount(r) - s.assignedCount(r)).coerceAtLeast(1))
    }
    if (step.kind == StepKind.VOLEUR && !s.extrasKnown && !step.fake) return Mode.Extras(step, s.rules.voleurExtraCards)
    return Mode.Act(step, null)
}

fun badgesFor(s: GameState, p: P): String = buildString {
    val c = if (s.phase == Phase.NIGHT) s.night else s.day
    val night = s.phase == Phase.NIGHT
    if (p.lover != null) append("❤️")
    if (p.charmed) append("🎶")
    if (p.infected) append("🦠")
    if (p.powerless) append("🚫")
    if (p.mutedDay == c) append("🔇")
    if (p.blockedNight == c) append("🔒")
    if (p.seenNight == c) append("🔮")
    if (night && p.protectedNight == s.night) append("🛡️")
    val jd = p.jugeDay
    if (jd != null && (s.rules.jugeUntilUsed || jd == c)) append("⚖️")
    if (p.corbeauDay == c) append("🐦")
    if (p.starterDay == c) append("🗣️")
    if (night && p.sheepNight == s.night) append("🐑")
    p.badges.forEach { (rid, n) -> if (n == c) append(s.role(rid)?.customBadge ?: "⭐") }
    if (night && s.nightVictim == p.pid) append("🐺")
    if (night && s.nightPoison == p.pid) append("☠️")
}

fun vibrate(ctx: Context) {
    try {
        val v = ctx.getSystemService(Vibrator::class.java) ?: return
        v.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
    }
}

@Composable
fun GameScreen(vm: AppViewModel) {
    val s = vm.state
    val rec = vm.record
    if (s == null || rec == null) {
        LaunchedEffect(Unit) { vm.home() }
        return
    }
    val settings = vm.settings
    val rules = s.rules
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val haptic = LocalHapticFeedback.current

    var hideRoles by rememberSaveable { mutableStateOf(!settings.showRoleIcons) }
    var editStep by remember { mutableStateOf<String?>(null) }
    var sheetPid by remember { mutableStateOf<Int?>(null) }
    var showLog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var endPreselect by remember { mutableStateOf(listOf<Int>()) }
    var showRules by remember { mutableStateOf(false) }
    var showDawn by remember { mutableStateOf(false) }
    var confirmAbandon by remember { mutableStateOf(false) }
    var alienMode by remember { mutableStateOf(false) }
    var alienTarget by remember { mutableStateOf<Int?>(null) }
    var warning by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var holdDone by remember { mutableStateOf<String?>(null) }
    var trollSeen by remember { mutableStateOf<Int?>(null) }

    val steps = remember(s) { Engine.nightSteps(s) }
    val activeEdit = editStep?.takeIf { s.phase == Phase.NIGHT && it in s.done }
    val mode = computeMode(s, steps, activeEdit, alienMode)
    val modeKey = "${rec.actions.size}|$mode"
    val editAction = (mode as? Mode.Act)?.editIndex?.let { rec.actions.getOrNull(it) as? Action.Step }

    var selected by remember(modeKey) { mutableStateOf(editAction?.targets ?: emptyList()) }
    var option by remember(modeKey) { mutableStateOf(editAction?.option) }
    var witch by remember(modeKey) {
        mutableStateOf(
            if (editAction != null && editAction.stepId == StepIds.SORCIERE) {
                WitchChoice(save = editAction.option == "save", poison = editAction.targets.firstOrNull())
            } else WitchChoice(),
        )
    }
    val isWitchStep = mode is Mode.Act && mode.step.kind == StepKind.SORCIERE
    val witchVictim = s.nightVictim
    val witchLifeLeft = !s.lifeUsed || editAction?.option == "save"
    val witchDeathLeft = !s.deathUsed || editAction?.targets?.isNotEmpty() == true
    var extrasPick by remember(modeKey) { mutableStateOf(listOf<String>()) }
    var signText by remember(modeKey) { mutableStateOf(editAction?.option ?: s.alienSign ?: "") }

    val holding = holdDone != null && s.phase == Phase.NIGHT && holdDone in s.done && activeEdit == null

    val maxSel = when (mode) {
        is Mode.Identify -> mode.count
        is Mode.Act -> when (mode.step.kind) {
            StepKind.SORCIERE -> if (witchLifeLeft || witchDeathLeft) 1 else 0
            StepKind.WOLVES -> if (option == "caught") 1 else mode.step.maxTargets
            StepKind.TARGETS, StepKind.BERGER, StepKind.CUSTOM -> maxOf(mode.step.maxTargets, editAction?.targets?.size ?: 0)
            else -> 0
        }
        is Mode.Shot, Mode.Vote, is Mode.Alien -> 1
        else -> 0
    }.let { if (holding) 0 else it }

    fun warnFor(pid: Int): String? = when (mode) {
        is Mode.Identify -> Engine.targetWarning(s, null, pid, "identify")
        is Mode.Act -> when {
            maxSel == 0 -> null
            isWitchStep && pid == witchVictim && !witch.poisonMode ->
                if (s.p(pid)?.role == RoleIds.SORCIERE && !rules.sorciereSelfSave) tr("The Sorcière can't save herself.", "La Sorcière ne peut pas se sauver.") else null
            isWitchStep -> Engine.targetWarning(s, StepIds.SORCIERE, pid, "poison")
            else -> Engine.targetWarning(s, mode.step.id, pid)
        }
        is Mode.Shot, Mode.Vote, is Mode.Alien -> when {
            s.p(pid)?.alive == false -> tr("${s.name(pid)} is dead.", "${s.name(pid)} est mort.")
            mode is Mode.Shot && pid == mode.hunter -> tr("That's the Chasseur himself.", "C'est le Chasseur lui-même.")
            mode is Mode.Alien && pid == mode.alien -> tr("That's the Alien himself.", "C'est l'Alien lui-même.")
            else -> null
        }
        else -> null
    }

    fun tap(pid: Int) {
        if (maxSel == 0) { sheetPid = pid; return }
        if (isWitchStep) {
            // Tap the victim to revive them, anyone else to poison them.
            val next = witch.tap(pid, witchVictim, witchLifeLeft, witchDeathLeft, rules.sorciereBothSameNight)
            if (next == null) { sheetPid = pid; return }
            val apply: () -> Unit = {
                witch = next
                if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            val adding = (next.save && !witch.save) || (next.poison == pid && witch.poison != pid)
            val w = if (adding) warnFor(pid) else null
            if (w != null && settings.warnings) warning = w to apply else apply()
            return
        }
        val apply: () -> Unit = {
            selected = when {
                pid in selected -> selected - pid
                maxSel == 1 -> listOf(pid)
                selected.size < maxSel -> selected + pid
                else -> selected.drop(1) + pid
            }
            if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        val w = if (pid in selected) null else warnFor(pid)
        if (w != null && settings.warnings) warning = w to apply else apply()
    }

    val actorPids: Set<Int> = when (mode) {
        is Mode.Act -> when (mode.step.kind) {
            StepKind.INFO_LOVERS -> s.players.filter { it.lover != null && it.alive }.map { it.pid }.toSet()
            StepKind.INFO_CHARMED -> s.players.filter { it.charmed && it.alive }.map { it.pid }.toSet()
            StepKind.WOLVES -> s.players.filter { it.alive && (it.role in RoleIds.PACK || it.infected) }.map { it.pid }.toSet()
            else -> mode.step.roleId?.let { r -> s.players.filter { it.role == r && it.alive }.map { it.pid }.toSet() } ?: emptySet()
        }
        is Mode.Shot -> setOf(mode.hunter)
        is Mode.Alien -> setOf(mode.alien)
        else -> emptySet()
    }

    val dimOdd = settings.warnings && maxSel > 0
    val shownSelection: Set<Int> =
        if (isWitchStep) setOfNotNull(witch.poison, if (witch.save) witchVictim else null) else selected.toSet()
    val tiles = s.seating.mapIndexed { i, pid ->
        val p = s.players[pid]
        val r = s.role(p.role)
        val witchMark = if (!isWitchStep) "" else
            (if (witch.save && pid == witchVictim) "💚" else "") + (if (witch.poison == pid) "☠️" else "")
        TileInfo(
            pid = pid,
            seat = i + 1,
            name = p.name,
            roleIcon = r?.icon ?: "❓",
            roleName = r?.name ?: tr("Unknown", "Inconnu"),
            team = s.teamOf(p),
            alive = p.alive,
            selected = pid in shownSelection,
            dimmed = dimOdd && pid !in shownSelection && warnFor(pid) != null,
            actor = pid in actorPids,
            badges = witchMark + badgesFor(s, p),
        )
    }

    val voleurBothWolves = s.extras.size >= 2 && s.extras.all { s.role(it)?.team == Team.WOLVES } && rules.voleurMustTakeWolf

    fun afterStep(id: String, edited: Boolean) {
        if (!settings.autoAdvance && !edited) holdDone = id
    }

    fun confirm() {
        if (settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (holding) { holdDone = null; return }
        when (mode) {
            is Mode.Identify -> vm.dispatch(Action.Assign(mode.role, selected))
            is Mode.Extras -> vm.dispatch(Action.Extras(extrasPick))
            is Mode.Act -> {
                val st = mode.step
                val action = when (st.kind) {
                    StepKind.SORCIERE -> Action.Step(st.id, listOfNotNull(witch.poison), if (witch.save) "save" else null)
                    StepKind.WOLVES -> Action.Step(st.id, selected, option)
                    StepKind.VOLEUR -> Action.Step(st.id, option = option ?: "keep")
                    StepKind.ALIEN_SIGN -> Action.Step(st.id, option = signText.trim())
                    else -> Action.Step(st.id, selected)
                }
                val edit = mode.editIndex
                if (edit != null) { vm.replaceAction(edit, action); editStep = null } else vm.dispatch(action)
                afterStep(st.id, edit != null)
            }
            Mode.NightOver -> vm.dispatch(Action.Dawn)
            is Mode.Shot -> vm.dispatch(Action.Shot(mode.hunter, selected.firstOrNull()))
            Mode.Vote -> vm.dispatch(Action.Vote(selected.firstOrNull()))
            Mode.AfterVote -> vm.dispatch(Action.Nightfall)
            is Mode.Alien -> alienTarget = selected.firstOrNull()
        }
    }

    fun skip() {
        when (mode) {
            is Mode.Identify -> vm.dispatch(Action.Absent(mode.role))
            is Mode.Act -> {
                val a = Action.Step(mode.step.id, skipped = true)
                val e = mode.editIndex
                if (e != null) { vm.replaceAction(e, a); editStep = null } else vm.dispatch(a)
                afterStep(mode.step.id, e != null)
            }
            is Mode.Shot -> vm.dispatch(Action.Shot(mode.hunter, null))
            Mode.Vote -> vm.dispatch(Action.Vote(null))
            is Mode.Alien -> alienMode = false
            else -> Unit
        }
    }

    val canConfirm = holding || when (mode) {
        is Mode.Identify -> selected.isNotEmpty()
        is Mode.Extras -> extrasPick.size == mode.count
        is Mode.Act -> when (mode.step.kind) {
            StepKind.TARGETS -> mode.step.fake || mode.step.maxTargets == 0 || selected.size >= maxOf(1, minOf(mode.step.minTargets, maxSel))
            StepKind.WOLVES -> mode.step.fake || mode.step.maxTargets == 0 || selected.size == 1
            StepKind.SORCIERE -> !witch.poisonMode || witch.poison != null
            StepKind.VOLEUR -> !(voleurBothWolves && (option ?: "keep") == "keep")
            else -> true
        }
        Mode.NightOver, Mode.AfterVote -> true
        is Mode.Shot, Mode.Vote, is Mode.Alien -> selected.size == 1
    }

    val confirmLabel = when {
        holding -> tr("Next ▶", "Suivant ▶")
        else -> when (mode) {
            is Mode.Identify -> tr("Confirm", "Valider") + " (${selected.size}/${mode.count})"
            is Mode.Extras -> tr("Confirm the cards", "Valider les cartes")
            is Mode.Act -> when {
                mode.editIndex != null -> tr("Save change", "Enregistrer")
                mode.step.kind == StepKind.INFO_LOVERS || mode.step.kind == StepKind.INFO_CHARMED || mode.step.kind == StepKind.IDENTIFY_REST -> tr("Next ▶", "Suivant ▶")
                else -> tr("Confirm", "Valider")
            }
            Mode.NightOver -> "🌅 " + tr("Dawn", "Aube")
            is Mode.Shot -> "🎯 " + tr("Shoot", "Tirer") + (selected.firstOrNull()?.let { " ${s.name(it)}" } ?: "")
            Mode.Vote -> tr("Eliminate", "Éliminer") + (selected.firstOrNull()?.let { " ${s.name(it)}" } ?: "")
            Mode.AfterVote -> "🌙 " + tr("Night falls", "La nuit tombe")
            is Mode.Alien -> tr("Next: the role", "Suivant : le rôle")
        }
    }
    val skipLabel: String? = if (holding) null else when (mode) {
        is Mode.Identify -> tr("Not in play", "Pas en jeu")
        is Mode.Act -> if (mode.step.kind == StepKind.INFO_LOVERS || mode.step.kind == StepKind.INFO_CHARMED || mode.step.kind == StepKind.IDENTIFY_REST) null else tr("Skip", "Passer")
        is Mode.Shot -> tr("No shot", "Pas de tir")
        Mode.Vote -> tr("Nobody", "Personne")
        is Mode.Alien -> tr("Cancel", "Annuler")
        else -> null
    }

    // Voice
    val sayKey = when (mode) {
        is Mode.Act -> "${s.night}-${mode.step.id}"
        is Mode.Identify -> "${s.night}-${mode.step.id}"
        else -> null
    }
    val sayText = when (mode) {
        is Mode.Act -> if (mode.editIndex == null) Engine.stepTexts(s, mode.step).say else null
        is Mode.Identify -> Engine.stepTexts(s, mode.step).say
        else -> null
    }
    LaunchedEffect(sayKey) { if (sayText != null) vm.speak(sayText) }

    // Dawn report pops up once per day
    LaunchedEffect(s.phase, s.day) {
        if (s.phase == Phase.DAY && s.dawn?.night == s.day && vm.dawnSeenDay < s.day) showDawn = true
    }

    val banner: @Composable () -> Unit = {
        val cs = MaterialTheme.colorScheme
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(cs.secondaryContainer.copy(alpha = 0.55f))
                .border(1.5.dp, cs.secondary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (holding) {
                val id = holdDone ?: ""
                Header("✅", Engine.stepTitle(s, id), tr("Done. Tap Next when you're ready.", "Fait. Touche Suivant quand tu es prêt."))
                return@Column
            }
            when (mode) {
                is Mode.Identify -> {
                    val tx = Engine.stepTexts(s, mode.step)
                    Header(s.roleIcon(mode.role), s.roleName(mode.role), Engine.identifyQuestion(s, mode.role))
                    if (settings.showScript) Say(tx.say)
                    Hint(tr("Physical cards: tap the player(s) who woke up. Choose “Not in play” if this card isn't in the game.", "Cartes physiques : touche le(s) joueur(s) réveillé(s). Choisis « Pas en jeu » si cette carte n'est pas dans la partie."))
                }
                is Mode.Extras -> {
                    Header("🃏", tr("Cards in the middle", "Cartes du milieu"), tr("Which ${mode.count} cards are left in the middle?", "Quelles ${mode.count} cartes sont restées au milieu ?"))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.setup.roleCounts.keys.forEach { id ->
                            val picked = extrasPick.count { it == id }
                            val max = s.setup.roleCounts[id] ?: 0
                            FilterChip(
                                selected = picked > 0,
                                onClick = {
                                    extrasPick = when {
                                        extrasPick.size < mode.count && picked < max -> extrasPick + id
                                        picked > 0 -> extrasPick.toMutableList().also { it.remove(id) }
                                        else -> extrasPick
                                    }
                                },
                                label = { Text("${s.roleIcon(id)} ${s.roleName(id)}" + if (picked > 1) " ×$picked" else "") },
                            )
                        }
                    }
                }
                is Mode.Act -> {
                    val st = mode.step
                    val tx = Engine.stepTexts(s, st)
                    if (mode.editIndex != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✏️ " + tr("Editing this step", "Modification de l'étape"), color = cs.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { editStep = null }) { Text(tr("Cancel", "Annuler")) }
                        }
                    }
                    Header(tx.icon, tx.title, tx.question)
                    if (settings.showScript) Say(tx.say)
                    if (st.fake) Hint("👻 " + tr("Fake turn: nobody can act. Call them anyway so nobody notices.", "Faux tour : personne ne peut agir. Appelle-les quand même pour ne rien révéler."))
                    if (st.blocked) Hint("🔒 " + tr("Blocked by the Loup rouge: call them, nothing will happen.", "Bloqué par le Loup rouge : appelle-les, rien ne se passera."))
                    StepTimer(modeKey, settings.nightTimerSeconds, settings.vibrateOnTimer)
                    when (st.kind) {
                        StepKind.SORCIERE -> {
                            val victim = witchVictim
                            Text(
                                "🐺 " + tr("Wolves' victim: ", "Victime des loups : ") + (victim?.let { s.name(it) } ?: tr("none", "aucune")),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "💚 " + tr("Life", "Vie") + ": " + (if (witchLifeLeft) tr("available", "disponible") else tr("used", "utilisée")) +
                                    "   ☠️ " + tr("Death", "Mort") + ": " + (if (witchDeathLeft) tr("available", "disponible") else tr("used", "utilisée")),
                                fontSize = 13.sp,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = witch.save,
                                    enabled = victim != null && witchLifeLeft,
                                    onClick = {
                                        val next = witch.toggleSave(rules.sorciereBothSameNight)
                                        val selfSave = next.save && victim != null && s.p(victim)?.role == RoleIds.SORCIERE && !rules.sorciereSelfSave
                                        if (selfSave && settings.warnings) {
                                            warning = tr("The Sorcière can't save herself.", "La Sorcière ne peut pas se sauver.") to { witch = next }
                                        } else witch = next
                                    },
                                    label = { Text("💚 " + tr("Revive", "Sauver") + (victim?.let { " ${s.name(it)}" } ?: "")) },
                                )
                                FilterChip(
                                    selected = witch.poisonMode || witch.poison != null,
                                    enabled = witchDeathLeft,
                                    onClick = { witch = witch.togglePoisonMode(rules.sorciereBothSameNight) },
                                    label = { Text("☠️ " + tr("Poison", "Empoisonner") + (witch.poison?.let { " ${s.name(it)}" } ?: "")) },
                                )
                            }
                            Hint(
                                if (witch.poisonMode && witch.poison == null) tr("Tap the player to poison.", "Touche le joueur à empoisonner.")
                                else if (rules.sorciereBothSameNight) tr("Tap the victim to revive them, tap anyone else to poison them. She can do both tonight.", "Touche la victime pour la sauver, touche quelqu'un d'autre pour l'empoisonner. Elle peut faire les deux cette nuit.")
                                else tr("Tap the victim to revive them, or anyone else to poison them (one potion per night).", "Touche la victime pour la sauver, ou quelqu'un d'autre pour l'empoisonner (une potion par nuit)."),
                            )
                        }
                        StepKind.WOLVES -> {
                            if (st.maxTargets == 0) Hint(tr("No kill on night 1 (rule): the wolves just meet.", "Pas de victime la nuit 1 (règle) : les loups se reconnaissent."))
                            val infect = Engine.canInfect(s) || option == "infect"
                            val catchable = Engine.canCatchPetiteFille(s) || option == "caught"
                            if (infect || catchable) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (infect) FilterChip(
                                        selected = option == "infect",
                                        onClick = { option = if (option == "infect") null else "infect" },
                                        label = { Text("🦠 " + tr("Infect instead of kill", "Infecter au lieu de tuer")) },
                                    )
                                    if (catchable) FilterChip(
                                        selected = option == "caught",
                                        onClick = {
                                            if (option == "caught") option = null
                                            else {
                                                option = "caught"
                                                selected = s.holders(RoleIds.PETITE_FILLE).filter { it.alive }.map { it.pid }.take(1)
                                            }
                                        },
                                        label = { Text("👧 " + tr("Petite fille caught", "Petite fille surprise")) },
                                    )
                                }
                            }
                            if (option == "caught") Hint(tr("Tap the Petite fille: she dies tonight instead.", "Touche la Petite fille : elle meurt cette nuit à la place."))
                            else if (Engine.canCatchPetiteFille(s)) Hint("👧 " + tr("The Petite fille may peek.", "La Petite fille peut espionner."))
                        }
                        StepKind.VOLEUR -> {
                            Text(tr("Middle: ", "Milieu : ") + s.extras.joinToString(", ") { "${s.roleIcon(it)} ${s.roleName(it)}" })
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = (option ?: "keep") == "keep",
                                    enabled = !voleurBothWolves,
                                    onClick = { option = "keep" },
                                    label = { Text("🎭 " + tr("Keep his card", "Garder sa carte")) },
                                )
                                s.extras.distinct().filter { it != RoleIds.VOLEUR }.forEach { id ->
                                    FilterChip(
                                        selected = option == "take:$id",
                                        onClick = { option = "take:$id" },
                                        label = { Text("${s.roleIcon(id)} " + tr("Take", "Prendre") + " ${s.roleName(id)}") },
                                    )
                                }
                            }
                            if (voleurBothWolves) Hint(tr("Both cards are wolves: he must take one.", "Les deux cartes sont des loups : il doit en prendre une."))
                        }
                        StepKind.ALIEN_SIGN -> OutlinedTextField(
                            value = signText,
                            onValueChange = { signText = it },
                            label = { Text(tr("His secret sign (note)", "Son signe secret (note)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        StepKind.BERGER -> Hint("🐑 × ${s.sheepLeft} — " + tr("tap one or more players, or Skip.", "touche un ou plusieurs joueurs, ou Passer."))
                        StepKind.INFO_LOVERS -> {
                            val lovers = s.players.filter { it.lover != null && it.pid < (it.lover ?: 0) }
                            Text("💞 " + lovers.joinToString("; ") { "${it.name} + ${s.name(it.lover)}" }.ifEmpty { tr("No lovers", "Pas d'amoureux") })
                        }
                        StepKind.INFO_CHARMED -> Text("🎶 " + s.names(s.players.filter { it.charmed && it.alive }.map { it.pid }))
                        StepKind.IDENTIFY_REST -> Hint(tr("All roles are identified. Everyone else is a Villageois.", "Tous les rôles sont identifiés. Les autres sont Villageois."))
                        StepKind.CUSTOM -> s.role(st.roleId)?.description?.takeIf { it.isNotBlank() }?.let { Hint(it) }
                        StepKind.TARGETS -> when (st.id) {
                            StepIds.SALVA -> s.salvaHistory[s.night - 1]?.let { Hint(tr("Protected last night: ${s.name(it)}", "Protégé la nuit dernière : ${s.name(it)}")) }
                            StepIds.RENARD -> Hint(tr("The app checks this player and their 2 living neighbors.", "L'app vérifie ce joueur et ses 2 voisins vivants."))
                            StepIds.GRAND_OURS -> Hint(tr("The growl is announced at dawn.", "Le grognement est annoncé à l'aube."))
                            StepIds.FLUTE -> {
                                val others = s.players.count { it.alive && it.role != RoleIds.FLUTE }
                                val charmed = s.players.count { it.alive && it.charmed && it.role != RoleIds.FLUTE }
                                Hint("🎶 " + tr("Charmed: $charmed of $others", "Charmés : $charmed sur $others"))
                            }
                            else -> Unit
                        }
                    }
                }
                Mode.NightOver -> Header(
                    "🌅",
                    tr("Night ${s.night} is over", "La nuit ${s.night} est finie"),
                    tr("Tap Dawn to see what happened.", "Touche Aube pour voir ce qui s'est passé."),
                )
                is Mode.Shot -> {
                    Header("🎯", "${s.name(mode.hunter)} (Chasseur)", tr("The Chasseur shoots: tap his target.", "Le Chasseur tire : touche sa cible."))
                }
                Mode.Vote, Mode.AfterVote -> {
                    Header(
                        "☀️",
                        tr("Day ${s.day}", "Jour ${s.day}"),
                        if (mode == Mode.Vote) tr("Vote: tap the player the village eliminates.", "Vote : touche le joueur éliminé par le village.")
                        else tr("The vote is done. Night falls when you're ready.", "Le vote est fait. La nuit tombe quand tu es prêt."),
                    )
                    s.dawn?.takeIf { it.night == s.day }?.let { rep ->
                        rep.seen?.let { Info("🔮", seenText(s, it)) }
                        rep.blocked.forEach { Info("🔒", blockedText(s, it)) }
                    }
                    val muted = s.players.filter { it.alive && it.mutedDay == s.day }.map { it.pid }
                    if (muted.isNotEmpty()) Info(
                        "🔇",
                        s.names(muted) + if (rules.mutedCanVote) tr(" can't talk today", " ne peut pas parler aujourd'hui") else tr(" can't talk or vote today", " ne peut ni parler ni voter aujourd'hui"),
                    )
                    val corbeau = s.players.filter { it.alive && it.corbeauDay == s.day }.map { it.pid }
                    if (corbeau.isNotEmpty()) Info("🐦", s.names(corbeau) + tr(" starts the vote with ${rules.corbeauVotes} votes against", " commence le vote avec ${rules.corbeauVotes} voix contre lui"))
                    val starter = s.players.filter { it.alive && it.starterDay == s.day }.map { it.pid }
                    if (starter.isNotEmpty()) Info("🗣️", s.names(starter) + tr(" starts talking and voting", " commence à parler et à voter"))
                    val juge = s.players.filter { p -> p.alive && p.jugeDay.let { it != null && (rules.jugeUntilUsed || it == s.day) } }.map { it.pid }
                    if (juge.isNotEmpty()) Info("⚖️", tr("(secret) Protected from the vote: ", "(secret) Protégé du vote : ") + s.names(juge))
                    val alienAlive = s.holders(RoleIds.ALIEN).any { it.alive }
                    if (alienAlive && !s.alienSign.isNullOrBlank()) Info("👽", tr("Alien's sign: ", "Signe de l'Alien : ") + s.alienSign)
                    s.dayEvents.forEach { Info("•", it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DayTimer(s.day, settings.dayTimerMinutes, settings.vibrateOnTimer)
                        if (alienAlive) AssistChip(onClick = { alienMode = true }, label = { Text("👽 " + tr("Alien sign", "Signe de l'Alien")) })
                    }
                }
                is Mode.Alien -> Header(
                    "👽",
                    "Alien — ${s.name(mode.alien)}",
                    tr("Everyone sleeps. Tap the player the Alien points at, then pick the role he names.", "Tout le monde dort. Touche le joueur désigné par l'Alien, puis choisis le rôle annoncé."),
                )
            }
        }
    }

    val topBar: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    (if (s.phase == Phase.NIGHT) "🌙 " else "☀️ ") + s.label(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (!hideRoles) {
                    Text(
                        "🐺 ${s.aliveCount(Team.WOLVES)}  🏡 ${s.aliveCount(Team.VILLAGE)}  🎭 ${s.aliveCount(Team.SOLO)}  ·  " +
                            tr("${s.players.count { it.alive }} alive", "${s.players.count { it.alive }} vivants"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { hideRoles = !hideRoles }) {
                Icon(if (hideRoles) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = tr("Hide roles", "Cacher les rôles"))
            }
            IconButton(onClick = { holdDone = null; editStep = null; vm.undo() }, enabled = rec.actions.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Undo", "Annuler"))
            }
            IconButton(onClick = { holdDone = null; vm.redo() }, enabled = rec.redo.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Redo", "Rétablir"))
            }
            IconButton(onClick = { showLog = true }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = tr("Game log", "Journal"))
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Menu", "Menu")) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (s.dawn != null) DropdownMenuItem(text = { Text("🌅 " + tr("Last dawn report", "Dernier rapport de l'aube")) }, onClick = { showDawn = true; showMenu = false })
                    DropdownMenuItem(text = { Text("📜 " + tr("Game rules", "Règles de la partie")) }, onClick = { showRules = true; showMenu = false })
                    DropdownMenuItem(text = { Text("🏁 " + tr("End the game", "Terminer la partie")) }, onClick = { endPreselect = emptyList(); showEnd = true; showMenu = false })
                    DropdownMenuItem(text = { Text("🏠 " + tr("Home (game is saved)", "Accueil (partie sauvegardée)")) }, onClick = { showMenu = false; vm.home() })
                    DropdownMenuItem(text = { Text("🗑️ " + tr("Abandon and delete", "Abandonner et supprimer")) }, onClick = { confirmAbandon = true; showMenu = false })
                }
            }
        }
    }

    val dots: @Composable () -> Unit = {
        if (s.phase == Phase.NIGHT && steps.isNotEmpty()) {
            val current = (mode as? Mode.Act)?.step?.id ?: (mode as? Mode.Identify)?.step?.id ?: (mode as? Mode.Extras)?.step?.id
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val cs = MaterialTheme.colorScheme
                steps.forEach { st ->
                    val done = st.id in s.done
                    val isCur = st.id == current
                    Box(
                        Modifier.size(if (isCur) 34.dp else 28.dp).clip(CircleShape)
                            .background(if (done) cs.primary.copy(alpha = 0.22f) else if (isCur) cs.primaryContainer else cs.surfaceContainerHigh)
                            .border(if (isCur) 2.dp else 1.dp, if (isCur) cs.primary else cs.outlineVariant, CircleShape)
                            .clickable(enabled = done) { holdDone = null; editStep = st.id },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(Engine.stepTexts(s, st).icon, fontSize = if (isCur) 16.sp else 13.sp)
                    }
                }
            }
        }
    }

    val buttons: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (skipLabel != null) {
                OutlinedButton(onClick = { skip() }, modifier = Modifier.height(56.dp)) { Text(skipLabel, maxLines = 1) }
            }
            Button(onClick = { confirm() }, enabled = canConfirm, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(confirmLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight && maxWidth >= 560.dp
        val panelWidth = minOf(380.dp, maxWidth * 0.4f)
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(8.dp)) {
                Column(Modifier.width(panelWidth).fillMaxHeight()) {
                    topBar()
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { banner() }
                    dots()
                    buttons()
                }
                Spacer(Modifier.width(10.dp))
                PlayerGrid(tiles, hideRoles, split = true, onTap = { tap(it) }, onLong = { sheetPid = it }, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            val bannerMax = maxHeight * 0.42f
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                topBar()
                Column(Modifier.heightIn(max = bannerMax).verticalScroll(rememberScrollState())) { banner() }
                Spacer(Modifier.height(6.dp))
                PlayerGrid(tiles, hideRoles, split = false, onTap = { tap(it) }, onLong = { sheetPid = it }, modifier = Modifier.weight(1f).fillMaxWidth())
                dots()
                buttons()
            }
        }
    }

    // ---------------------------------------------------------------- dialogs

    warning?.let { (text, action) ->
        ConfirmDialog(
            "⚠️ " + tr("Are you sure?", "Tu es sûr ?"),
            text,
            tr("Pick anyway", "Choisir quand même"),
            onConfirm = action,
            onDismiss = { warning = null },
        )
    }
    vm.resultFor?.let { id ->
        if (s.results.containsKey(id)) ResultDialog(s, id) { vm.resultFor = null }
    }
    if (showDawn) {
        DawnDialog(s) { showDawn = false; vm.dawnSeenDay = s.day }
    }
    val troll = s.trollWin
    if (troll != null && trollSeen != troll && !showEnd) {
        ConfirmDialog(
            "👺 " + tr("The Troll wins!", "Le Troll gagne !"),
            tr("${s.name(troll)} was voted out: the game is over.", "${s.name(troll)} a été éliminé par le vote : la partie est finie."),
            tr("End the game", "Terminer la partie"),
            onConfirm = { endPreselect = listOf(troll); showEnd = true },
            onDismiss = { trollSeen = troll },
        )
    }
    if (showEnd) {
        EndGameDialog(s, endPreselect, onEnd = { winners, label -> showEnd = false; vm.endGame(winners, label) }, onDismiss = { showEnd = false; s.trollWin?.let { trollSeen = it } })
    }
    if (showLog) LogDialog(vm, s, rec) { showLog = false }
    sheetPid?.let { pid -> PlayerSheet(vm, s, pid) { sheetPid = null } }
    if (showRules) {
        RulesDialog(rules, onSave = { vm.dispatch(Action.Rules(it)); showRules = false }, onDismiss = { showRules = false })
    }
    if (confirmAbandon) {
        ConfirmDialog(
            tr("Abandon this game?", "Abandonner cette partie ?"),
            tr("The game is deleted and won't appear in the history.", "La partie est supprimée et n'apparaîtra pas dans l'historique."),
            tr("Delete", "Supprimer"),
            onConfirm = { vm.abandonGame() },
            onDismiss = { confirmAbandon = false },
        )
    }
    val alien = (mode as? Mode.Alien)?.alien
    val target = alienTarget
    if (alien != null && target != null) {
        RolePickerDialog(
            tr("Which role does the Alien name for ${s.name(target)}?", "Quel rôle l'Alien annonce-t-il pour ${s.name(target)} ?"),
            s.setup.roles.filter { (s.setup.roleCounts[it.id] ?: 0) > 0 || it.id == RoleIds.LOUP },
            onPick = { role ->
                vm.dispatch(Action.AlienGuess(alien, target, role))
                alienTarget = null
                alienMode = false
            },
            onDismiss = { alienTarget = null },
        )
    }
}

@Composable
private fun Header(icon: String, title: String, question: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 34.sp, modifier = Modifier.padding(end = 10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(question, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Say(text: String) {
    Text(
        "🗣️ “$text”",
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Info(icon: String, text: String) {
    Row {
        Text(icon, modifier = Modifier.width(26.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepTimer(key: Any, seconds: Int, vibrateAtEnd: Boolean) {
    if (seconds <= 0) return
    val ctx = LocalContext.current
    var left by remember(key) { mutableIntStateOf(seconds) }
    LaunchedEffect(key) {
        while (left > 0) {
            delay(1000)
            left--
        }
        if (vibrateAtEnd) vibrate(ctx)
    }
    Text(
        "⏱️ ${left}s",
        fontWeight = FontWeight.Bold,
        color = if (left <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DayTimer(day: Int, minutes: Int, vibrateAtEnd: Boolean) {
    if (minutes <= 0) return
    val ctx = LocalContext.current
    var left by remember(day, minutes) { mutableIntStateOf(minutes * 60) }
    var running by remember(day, minutes) { mutableStateOf(false) }
    LaunchedEffect(running, day) {
        while (running && left > 0) {
            delay(1000)
            left--
        }
        if (running && left == 0) {
            running = false
            if (vibrateAtEnd) vibrate(ctx)
        }
    }
    AssistChip(
        onClick = { if (left == 0) left = minutes * 60 else running = !running },
        label = { Text((if (running) "⏸️ " else "▶️ ") + "%d:%02d".format(left / 60, left % 60)) },
        trailingIcon = {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = tr("Reset", "Réinitialiser"),
                modifier = Modifier.size(18.dp).clickable { running = false; left = minutes * 60 },
            )
        },
    )
}
