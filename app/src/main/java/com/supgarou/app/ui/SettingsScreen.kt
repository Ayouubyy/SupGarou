package com.supgarou.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.supgarou.app.engine.Engine
import com.supgarou.app.model.Handout
import com.supgarou.app.model.Language
import com.supgarou.app.model.RevealDead
import com.supgarou.app.model.RoleDef
import com.supgarou.app.model.RuleSettings
import com.supgarou.app.model.StepIds
import com.supgarou.app.model.ThemeMode
import com.supgarou.app.model.VoyanteSees
import com.supgarou.app.model.tr

fun stepLabel(id: String, roles: List<RoleDef>): String {
    val base = when (id) {
        StepIds.LOVERS -> "💞 " + tr("Lovers", "Amoureux")
        StepIds.WOLVES -> "🐺 " + tr("Wolves", "Loups-garous")
        StepIds.CHARMED -> "🎶 " + tr("Charmed players", "Joueurs charmés")
        else -> Engine.stepRole(id)?.let { rid -> roles.firstOrNull { it.id == rid }?.let { "${it.icon} ${it.name}" } } ?: id
    }
    val night1 = id in setOf(StepIds.VOLEUR, StepIds.CUPIDON, StepIds.LOVERS, StepIds.ALIEN_SIGN)
    return if (night1) base + tr(" (night 1)", " (nuit 1)") else base
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s = vm.settings
    val ctx = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
                vm.toast = tr("Backup saved", "Sauvegarde enregistrée")
            } catch (e: Exception) {
                vm.toast = tr("Could not save the backup", "Impossible d'enregistrer la sauvegarde")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            } catch (e: Exception) {
                null
            }
            vm.toast = if (vm.importJson(text)) tr("Backup imported", "Sauvegarde importée") else tr("This file is not a Sup'Garou backup", "Ce fichier n'est pas une sauvegarde Sup'Garou")
        }
    }

    ScreenScaffold(tr("Settings", "Réglages"), onBack = { vm.back() }) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle(tr("General", "Général"))
            ChoiceRow(
                tr("Language", "Langue"),
                listOf(Language.ENGLISH_FR_ROLES to "English + rôles FR", Language.FULL_FRENCH to "Tout en français"),
                s.language,
            ) { v -> vm.updateSettings { it.copy(language = v) } }
            ChoiceRow(
                tr("Theme", "Thème"),
                listOf(ThemeMode.DARK to tr("Dark", "Sombre"), ThemeMode.LIGHT to tr("Light", "Clair"), ThemeMode.SYSTEM to tr("Like the phone", "Comme le téléphone")),
                s.theme,
            ) { v -> vm.updateSettings { it.copy(theme = v) } }
            ChoiceRow(
                tr("Text size", "Taille du texte"),
                listOf(1f to tr("Normal", "Normal"), 1.15f to tr("Large", "Grand"), 1.3f to tr("Extra large", "Très grand")),
                s.textScale,
            ) { v -> vm.updateSettings { it.copy(textScale = v) } }
            SwitchRow(tr("Keep the screen on during a game", "Garder l'écran allumé pendant la partie"), s.keepScreenOn) { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
            SwitchRow(tr("Vibrate on taps", "Vibrer au toucher"), s.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
            SwitchRow(
                tr("Show roles on player tiles", "Afficher les rôles sur les tuiles"),
                s.showRoleIcons,
                tr("The eye button in a game hides them at any time.", "Le bouton œil les cache à tout moment."),
            ) { v -> vm.updateSettings { it.copy(showRoleIcons = v) } }
            SwitchRow(tr("Ask before killing a player", "Confirmer avant de tuer un joueur"), s.confirmDestructive) { v -> vm.updateSettings { it.copy(confirmDestructive = v) } }

            SectionTitle(tr("Narrator help", "Aide au narrateur"))
            SwitchRow(tr("Show the line to read aloud", "Afficher la phrase à lire"), s.showScript) { v -> vm.updateSettings { it.copy(showScript = v) } }
            SwitchRow(tr("Read the line aloud (phone voice)", "Lire la phrase à voix haute"), s.readAloud) { v -> vm.updateSettings { it.copy(readAloud = v) } }
            SwitchRow(tr("Go to the next step after Confirm", "Passer à l'étape suivante après Valider"), s.autoAdvance) { v -> vm.updateSettings { it.copy(autoAdvance = v) } }
            SwitchRow(
                tr("Warn on odd picks", "Avertir des choix inhabituels"),
                s.warnings,
                tr("Odd targets are faded and ask before being picked.", "Les cibles inhabituelles sont atténuées et demandent confirmation."),
            ) { v -> vm.updateSettings { it.copy(warnings = v) } }

            SectionTitle(tr("Timers", "Minuteurs"))
            StepperRow(tr("Day discussion timer", "Minuteur du débat"), s.dayTimerMinutes, 0..15, { if (it == 0) tr("off", "non") else "$it min" }) { v -> vm.updateSettings { it.copy(dayTimerMinutes = v) } }
            StepperRow(tr("Night step timer", "Minuteur des étapes de nuit"), s.nightTimerSeconds / 5, 0..12, { if (it == 0) tr("off", "non") else "${it * 5}s" }) { v ->
                vm.updateSettings { it.copy(nightTimerSeconds = v * 5) }
            }
            SwitchRow(tr("Vibrate when a timer ends", "Vibrer à la fin d'un minuteur"), s.vibrateOnTimer) { v -> vm.updateSettings { it.copy(vibrateOnTimer = v) } }

            SectionTitle(tr("New games", "Nouvelles parties"))
            ChoiceRow(
                tr("How roles are handed out", "Distribution des rôles"),
                listOf(Handout.PHYSICAL to tr("Physical cards", "Cartes physiques"), Handout.APP_DEALS to tr("App deals", "L'app distribue"), Handout.MANUAL to tr("Manual", "Manuel")),
                s.handout,
            ) { v -> vm.updateSettings { it.copy(handout = v) } }
            SwitchRow(tr("Start from the last game's players and roles", "Partir des joueurs et rôles de la dernière partie"), s.startFromLast) { v -> vm.updateSettings { it.copy(startFromLast = v) } }
            SwitchRow(tr("Suggest names from past players", "Suggérer les noms des anciens joueurs"), s.suggestNames) { v -> vm.updateSettings { it.copy(suggestNames = v) } }
            SwitchRow(tr("Offer a suggested role mix", "Proposer un mélange de rôles"), s.suggestMix) { v -> vm.updateSettings { it.copy(suggestMix = v) } }

            Text(
                tr("Rules below are the defaults for new games. A game in progress keeps its own rules (menu ⋮ → Game rules).", "Les règles ci-dessous s'appliquent aux nouvelles parties. Une partie en cours garde ses règles (menu ⋮ → Règles de la partie)."),
                Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RuleEditor(s.rules) { r -> vm.updateSettings { it.copy(rules = r) } }

            SectionTitle(tr("Night order", "Ordre de la nuit"))
            NightOrderEditor(s.rules, vm.roles) { r -> vm.updateSettings { it.copy(rules = r) } }

            SectionTitle(tr("Data", "Données"))
            OutlinedButton(onClick = { exportLauncher.launch("supgarou-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("⬆️ " + tr("Export a backup file", "Exporter une sauvegarde"))
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("⬇️ " + tr("Import a backup file", "Importer une sauvegarde"))
            }
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("↩️ " + tr("Reset role names and descriptions", "Réinitialiser les rôles"))
            }
            OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                Text("🗑️ " + tr("Clear the game history", "Effacer l'historique"), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (confirmClear) {
        ConfirmDialog(
            tr("Clear the history?", "Effacer l'historique ?"),
            tr("Every finished game is deleted. Players stay in the list.", "Toutes les parties terminées sont supprimées. Les joueurs restent dans la liste."),
            tr("Clear", "Effacer"),
            onConfirm = { vm.clearHistory() },
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            tr("Reset the roles?", "Réinitialiser les rôles ?"),
            tr("Built-in roles get their original names, icons and descriptions back. Custom roles are kept.", "Les rôles de base retrouvent leurs noms, icônes et descriptions d'origine. Les rôles personnalisés sont gardés."),
            tr("Reset", "Réinitialiser"),
            onConfirm = { vm.resetRoles() },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
fun RuleEditor(r: RuleSettings, onChange: (RuleSettings) -> Unit) {
    val nights = 1..4
    fun n(v: Int) = tr("night $v", "nuit $v")

    SectionTitle(tr("Game rules", "Règles du jeu"))
    SwitchRow(tr("Wolves kill on night 1", "Les loups tuent la nuit 1"), r.wolvesKillNight1) { onChange(r.copy(wolvesKillNight1 = it)) }
    ChoiceRow(
        tr("Reveal a dead player's role", "Révéler le rôle d'un mort"),
        listOf(RevealDead.FULL to tr("Full role", "Rôle complet"), RevealDead.TEAM to tr("Team only", "Camp seulement"), RevealDead.HIDDEN to tr("Hidden", "Caché")),
        r.revealDead,
    ) { onChange(r.copy(revealDead = it)) }
    SwitchRow(
        tr("Fake turns", "Faux tours"),
        r.fakeTurns,
        tr("Still call roles that are dead or powerless, so nobody notices.", "Appeler quand même les rôles morts ou sans pouvoir, pour ne rien révéler."),
    ) { onChange(r.copy(fakeTurns = it)) }
    SwitchRow(tr("A blocked player is still called", "Un joueur bloqué est quand même appelé"), r.blockedStillCalled) { onChange(r.copy(blockedStillCalled = it)) }
    SwitchRow(tr("Infected players count as wolves for checks", "Les infectés comptent comme loups pour les vérifications"), r.infectedCountAsWolves) { onChange(r.copy(infectedCountAsWolves = it)) }
    SwitchRow(tr("An infected player keeps their power", "Un infecté garde son pouvoir"), r.infectedKeepsPower) { onChange(r.copy(infectedKeepsPower = it)) }
    SwitchRow(tr("A muted player can vote", "Un joueur muet peut voter"), r.mutedCanVote) { onChange(r.copy(mutedCanVote = it)) }

    SectionTitle("🛡️ Salva")
    SwitchRow(tr("Can protect himself", "Peut se protéger"), r.salvaSelf) { onChange(r.copy(salvaSelf = it)) }
    SwitchRow(tr("Can protect the same player twice in a row", "Peut protéger le même joueur deux fois de suite"), r.salvaSameTwice) { onChange(r.copy(salvaSameTwice = it)) }

    SectionTitle("🔮 Voyante")
    ChoiceRow(tr("She sees", "Elle voit"), listOf(VoyanteSees.EXACT to tr("The exact role", "Le rôle exact"), VoyanteSees.TEAM to tr("The team only", "Le camp seulement")), r.voyanteSees) { onChange(r.copy(voyanteSees = it)) }

    SectionTitle("🧪 Sorcière")
    SwitchRow(tr("Can save herself", "Peut se sauver"), r.sorciereSelfSave) { onChange(r.copy(sorciereSelfSave = it)) }
    SwitchRow(tr("Can use both potions the same night", "Peut utiliser les deux potions la même nuit"), r.sorciereBothSameNight) { onChange(r.copy(sorciereBothSameNight = it)) }

    SectionTitle("👴 Ancien")
    StepperRow(tr("Extra lives against the wolves", "Vies en plus contre les loups"), r.ancienLives, 0..3) { onChange(r.copy(ancienLives = it)) }
    SwitchRow(tr("Villagers lose their powers if the village kills him", "Les villageois perdent leurs pouvoirs si le village le tue"), r.ancienPenalty) { onChange(r.copy(ancienPenalty = it)) }

    SectionTitle("🦊 Renard")
    SwitchRow(
        tr("Loses his power when he finds a wolf", "Perd son pouvoir quand il trouve un loup"),
        r.renardLosesOnWolf,
        tr("Off: the official rule (loses it on a no).", "Non : la règle officielle (le perd sur un non)."),
    ) { onChange(r.copy(renardLosesOnWolf = it)) }

    SectionTitle("🐑 Berger")
    StepperRow(tr("Sheep", "Moutons"), r.bergerSheep, 1..6) { onChange(r.copy(bergerSheep = it)) }
    StepperRow(tr("Sheep per night", "Moutons par nuit"), r.bergerPerNight, 0..6, { if (it == 0) tr("all", "tous") else "$it" }) { onChange(r.copy(bergerPerNight = it)) }

    SectionTitle("💘 Cupidon")
    SwitchRow(tr("Can choose himself", "Peut se choisir"), r.cupidonSelf) { onChange(r.copy(cupidonSelf = it)) }

    SectionTitle("🎯 Chasseur")
    SwitchRow(tr("Can shoot after being poisoned", "Peut tirer s'il est empoisonné"), r.chasseurShootsIfPoisoned) { onChange(r.copy(chasseurShootsIfPoisoned = it)) }

    SectionTitle("🎭 Voleur")
    StepperRow(tr("Extra cards in the middle", "Cartes au milieu"), r.voleurExtraCards, 1..3) { onChange(r.copy(voleurExtraCards = it)) }
    SwitchRow(tr("Must take a wolf if both cards are wolves", "Doit prendre un loup si les deux cartes sont des loups"), r.voleurMustTakeWolf) { onChange(r.copy(voleurMustTakeWolf = it)) }

    SectionTitle("👧 Petite fille")
    SwitchRow(tr("Can be caught by the wolves", "Peut être surprise par les loups"), r.petiteFilleCatchable) { onChange(r.copy(petiteFilleCatchable = it)) }

    SectionTitle("🦠 Loup père infecté")
    StepperRow(tr("Infections per game", "Infections par partie"), r.infections, 0..3) { onChange(r.copy(infections = it)) }
    StepperRow(tr("Can infect from", "Peut infecter à partir de"), r.infectFirstNight, nights, { n(it) }) { onChange(r.copy(infectFirstNight = it)) }

    SectionTitle("⛔ Loup rouge")
    StepperRow(tr("First night", "Première nuit"), r.loupRougeFirstNight, nights, { n(it) }) { onChange(r.copy(loupRougeFirstNight = it)) }
    SwitchRow(tr("Can block another wolf", "Peut bloquer un autre loup"), r.loupRougeCanBlockWolves) { onChange(r.copy(loupRougeCanBlockWolves = it)) }

    SectionTitle("🌑 Loup noir")
    StepperRow(tr("First night", "Première nuit"), r.loupNoirFirstNight, nights, { n(it) }) { onChange(r.copy(loupNoirFirstNight = it)) }

    SectionTitle("❄️ Loup blanc")
    SwitchRow(tr("Hidden from the Voyante, Ours and Grand ours", "Invisible pour la Voyante, l'Ours et le Grand ours"), r.loupBlancHidden) { onChange(r.copy(loupBlancHidden = it)) }

    SectionTitle("🎶 Joueur de flûte")
    StepperRow(tr("Players charmed per night", "Joueurs charmés par nuit"), r.fluteCharms, 1..3) { onChange(r.copy(fluteCharms = it)) }
    SwitchRow(tr("Can't die to the wolves", "Ne peut pas mourir des loups"), r.fluteImmuneToWolves) { onChange(r.copy(fluteImmuneToWolves = it)) }

    SectionTitle("👽 Alien")
    SwitchRow(
        tr("The guess must be the exact role", "La devinette doit être le rôle exact"),
        r.alienExact,
        tr("Off: “Loup” counts for any wolf.", "Non : « Loup » compte pour tout loup."),
    ) { onChange(r.copy(alienExact = it)) }

    SectionTitle("🐦 Corbeau")
    StepperRow(tr("First night", "Première nuit"), r.corbeauFirstNight, nights, { n(it) }) { onChange(r.copy(corbeauFirstNight = it)) }
    StepperRow(tr("Votes against his target", "Voix contre sa cible"), r.corbeauVotes, 1..4) { onChange(r.copy(corbeauVotes = it)) }

    SectionTitle("⚖️ Juge")
    StepperRow(tr("First night", "Première nuit"), r.jugeFirstNight, nights, { n(it) }) { onChange(r.copy(jugeFirstNight = it)) }
    SwitchRow(tr("Protection lasts until it is used", "La protection dure jusqu'à son utilisation"), r.jugeUntilUsed, tr("Off: it only covers the next day.", "Non : elle ne couvre que le lendemain.")) { onChange(r.copy(jugeUntilUsed = it)) }
}

@Composable
private fun NightOrderEditor(r: RuleSettings, roles: List<RoleDef>, onChange: (RuleSettings) -> Unit) {
    val customIds = roles.filter { it.custom }.map { StepIds.CUSTOM_PREFIX + it.id }
    val order = (r.nightOrder + StepIds.DEFAULT_ORDER.filter { it !in r.nightOrder } + customIds.filter { it !in r.nightOrder })
    Text(
        tr("Steps are skipped automatically when the role isn't in play. Turn a step off to never call it.", "Les étapes sont sautées si le rôle n'est pas en jeu. Désactive une étape pour ne jamais l'appeler."),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    order.forEachIndexed { i, id ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = id !in r.disabledSteps,
                onCheckedChange = { on -> onChange(r.copy(disabledSteps = if (on) r.disabledSteps - id else r.disabledSteps + id)) },
            )
            Text("${i + 1}. " + stepLabel(id, roles), Modifier.weight(1f).padding(start = 10.dp))
            IconButton(onClick = {
                val l = order.toMutableList(); l.add(i - 1, l.removeAt(i)); onChange(r.copy(nightOrder = l))
            }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = tr("Up", "Monter")) }
            IconButton(onClick = {
                val l = order.toMutableList(); l.add(i + 1, l.removeAt(i)); onChange(r.copy(nightOrder = l))
            }, enabled = i < order.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = tr("Down", "Descendre")) }
        }
        HorizontalDivider()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { onChange(r.copy(nightOrder = StepIds.DEFAULT_ORDER, disabledSteps = emptySet())) }) {
            Text(tr("Reset the order", "Réinitialiser l'ordre"))
        }
    }
}
