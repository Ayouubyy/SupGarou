package com.supgarou.app.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.supgarou.app.data.Store
import com.supgarou.app.data.normName
import com.supgarou.app.engine.Engine
import com.supgarou.app.engine.GameState
import com.supgarou.app.model.Action
import com.supgarou.app.model.AppSettings
import com.supgarou.app.model.Backup
import com.supgarou.app.model.DefaultRoles
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.GameSetup
import com.supgarou.app.model.Handout
import com.supgarou.app.model.Lang
import com.supgarou.app.model.Language
import com.supgarou.app.model.PlayerProfile
import com.supgarou.app.model.Preset
import com.supgarou.app.model.RoleDef
import com.supgarou.app.model.RoleIds
import com.supgarou.app.model.SetupPlayer
import com.supgarou.app.model.Team
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

sealed interface Screen {
    data object Home : Screen
    data object Setup : Screen
    data object Reveal : Screen
    data object Game : Screen
    data object History : Screen
    data class GameDetail(val id: String) : Screen
    data object Players : Screen
    data object Roles : Screen
    data object Settings : Screen
}

data class DraftPlayer(val profileId: String, val name: String)

data class SetupDraft(
    val players: List<DraftPlayer> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val handout: Handout = Handout.PHYSICAL,
    /** Manual handout: profile id -> role id. */
    val manual: Map<String, String> = emptyMap(),
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app.filesDir)
    private val io = Executors.newSingleThreadExecutor()

    var settings by mutableStateOf(store.loadSettings()); private set
    var roles by mutableStateOf(store.loadRoles()); private set
    var profiles by mutableStateOf(store.loadProfiles()); private set
    var presets by mutableStateOf(store.loadPresets()); private set
    var games by mutableStateOf(store.loadGames()); private set

    val nav = mutableStateListOf<Screen>(Screen.Home)
    var draft by mutableStateOf(SetupDraft())

    var record by mutableStateOf<GameRecord?>(null); private set
    var state by mutableStateOf<GameState?>(null); private set

    /** Step whose result (Voyante card, Renard answer, sheep) should pop up. */
    var resultFor by mutableStateOf<String?>(null)
    var dawnSeenDay by mutableIntStateOf(0)
    var toast by mutableStateOf<String?>(null)

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null

    init {
        Lang.french = settings.language == Language.FULL_FRENCH
        settings.currentGameId?.let { id ->
            games.firstOrNull { it.id == id && it.endedAt == null }?.let { open(it) }
        }
    }

    // ------------------------------------------------------------ navigation

    val screen: Screen get() = nav.last()
    fun go(s: Screen) { nav.add(s) }
    fun back() { if (nav.size > 1) nav.removeAt(nav.lastIndex) }
    fun home() { nav.clear(); nav.add(Screen.Home) }
    private fun replaceTop(s: Screen) { if (nav.size > 1) nav.removeAt(nav.lastIndex); nav.add(s) }

    // ------------------------------------------------------------ settings & roles

    fun updateSettings(f: (AppSettings) -> AppSettings) {
        val old = settings
        settings = f(settings)
        Lang.french = settings.language == Language.FULL_FRENCH
        val s = settings
        io.execute { store.saveSettings(s) }
        if (old.language != s.language) record?.let { state = Engine.replay(it) }
    }

    private fun persistRoles() { val r = roles; io.execute { store.saveRoles(r) } }

    fun saveRole(r: RoleDef) {
        roles = if (roles.any { it.id == r.id }) roles.map { if (it.id == r.id) r else it } else roles + r
        persistRoles()
    }

    fun deleteRole(id: String) {
        roles = roles.filter { it.id != id }
        persistRoles()
    }

    fun resetRoles() {
        roles = DefaultRoles.all + roles.filter { it.custom }
        persistRoles()
    }

    fun role(id: String?): RoleDef? = roles.firstOrNull { it.id == id }

    // ------------------------------------------------------------ players

    private fun persistProfiles() { val p = profiles; io.execute { store.saveProfiles(p) } }

    fun gamesPlayed(profileId: String): Int = games.count { g -> g.setup.players.any { it.profileId == profileId } }

    fun suggestions(query: String, exclude: Set<String>): List<PlayerProfile> {
        val q = normName(query)
        if (q.isEmpty()) return emptyList()
        return profiles
            .filter { it.id !in exclude && normName(it.name).let { n -> n.startsWith(q) || n.contains(" $q") || (q.length >= 2 && n.contains(q)) } }
            .sortedWith(compareByDescending<PlayerProfile> { normName(it.name).startsWith(q) }.thenByDescending { gamesPlayed(it.id) }.thenByDescending { it.lastPlayed })
            .take(6)
    }

    fun renameProfile(id: String, name: String) {
        if (name.isBlank()) return
        profiles = profiles.map { if (it.id == id) it.copy(name = name.trim()) else it }
        persistProfiles()
    }

    fun deleteProfile(id: String) {
        profiles = profiles.filter { it.id != id }
        persistProfiles()
    }

    /** Moves every game of [from] onto [into] and removes [from]. */
    fun mergeProfiles(from: String, into: String) {
        if (from == into) return
        val changed = games.map { g ->
            if (g.setup.players.none { it.profileId == from }) g
            else g.copy(setup = g.setup.copy(players = g.setup.players.map { if (it.profileId == from) it.copy(profileId = into) else it }))
        }
        val toSave = changed.filterIndexed { i, g -> g !== games[i] }
        games = changed
        profiles = profiles.filter { it.id != from }
        persistProfiles()
        io.execute { toSave.forEach { store.saveGame(it) } }
        record?.let { r -> games.firstOrNull { it.id == r.id }?.let { open(it) } }
    }

    // ------------------------------------------------------------ setup

    private fun lastGame(): GameRecord? = games.maxByOrNull { it.startedAt }

    fun startNewSetup() {
        val last = lastGame()
        draft = if (settings.startFromLast && last != null) {
            SetupDraft(
                players = last.setup.players.map { DraftPlayer(it.profileId, profileName(it.profileId, it.name)) },
                counts = last.setup.roleCounts,
                handout = settings.handout,
            )
        } else SetupDraft(handout = settings.handout)
        go(Screen.Setup)
    }

    private fun profileName(id: String, fallback: String) = profiles.firstOrNull { it.id == id }?.name ?: fallback

    fun addPlayer(name: String, profileId: String?) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val existing = profileId?.let { id -> profiles.firstOrNull { it.id == id } }
            ?: profiles.firstOrNull { normName(it.name) == normName(clean) }
        val id = existing?.id ?: UUID.randomUUID().toString()
        if (draft.players.any { it.profileId == id }) { toast = "$clean ✓"; return }
        draft = draft.copy(players = draft.players + DraftPlayer(id, existing?.name ?: clean))
    }

    fun removePlayer(i: Int) {
        val p = draft.players.getOrNull(i) ?: return
        draft = draft.copy(players = draft.players.filterIndexed { j, _ -> j != i }, manual = draft.manual - p.profileId)
    }

    fun movePlayer(from: Int, to: Int) {
        val l = draft.players.toMutableList()
        if (from !in l.indices || to !in l.indices) return
        l.add(to, l.removeAt(from))
        draft = draft.copy(players = l)
    }

    fun addLastGamePlayers() {
        val last = lastGame() ?: return
        last.setup.players.forEach { addPlayer(profileName(it.profileId, it.name), it.profileId) }
    }

    fun setCount(role: String, n: Int) {
        draft = draft.copy(counts = (draft.counts + (role to n.coerceIn(0, 40))).filterValues { it > 0 })
    }

    fun neededCards(): Int =
        draft.players.size + if ((draft.counts[RoleIds.VOLEUR] ?: 0) > 0) settings.rules.voleurExtraCards else 0

    fun fillVillagers() {
        val total = draft.counts.values.sum()
        val missing = neededCards() - total
        if (missing > 0) setCount(RoleIds.VILLAGEOIS, (draft.counts[RoleIds.VILLAGEOIS] ?: 0) + missing)
        else if (missing < 0) setCount(RoleIds.VILLAGEOIS, (draft.counts[RoleIds.VILLAGEOIS] ?: 0) + missing)
    }

    fun suggestMix() {
        val n = draft.players.size.coerceAtLeast(4)
        val wolves = (n / 4).coerceAtLeast(1)
        val c = mutableMapOf(RoleIds.LOUP to wolves, RoleIds.VOYANTE to 1, RoleIds.SALVA to 1, RoleIds.SORCIERE to 1)
        if (n >= 8) c[RoleIds.CHASSEUR] = 1
        if (n >= 10) c[RoleIds.CUPIDON] = 1
        if (n >= 12) c[RoleIds.PETITE_FILLE] = 1
        if (n >= 14) c[RoleIds.RENARD] = 1
        if (n >= 16) { c[RoleIds.LOUP] = wolves - 1; c[RoleIds.LOUP_NOIR] = 1 }
        if (n >= 18) c[RoleIds.FLUTE] = 1
        val rest = draft.players.size - c.values.sum()
        if (rest > 0) c[RoleIds.VILLAGEOIS] = rest
        draft = draft.copy(counts = c.filterValues { it > 0 })
    }

    fun sameAsLastGame() { lastGame()?.let { draft = draft.copy(counts = it.setup.roleCounts) } }
    fun clearCounts() { draft = draft.copy(counts = emptyMap()) }

    fun savePreset(name: String) {
        if (name.isBlank()) return
        presets = presets.filter { it.name != name } + Preset(name.trim(), draft.counts)
        val p = presets
        io.execute { store.savePresets(p) }
    }

    fun loadPreset(p: Preset) { draft = draft.copy(counts = p.counts) }

    fun deletePreset(p: Preset) {
        presets = presets - p
        val l = presets
        io.execute { store.savePresets(l) }
    }

    fun assignManual(profileId: String, role: String?) {
        draft = draft.copy(manual = if (role == null) draft.manual - profileId else draft.manual + (profileId to role))
    }

    fun startGame() {
        val d = draft
        if (d.players.size < 3) return
        val now = System.currentTimeMillis()
        val profs = profiles.toMutableList()
        val players = d.players.map { dp ->
            val existing = profs.firstOrNull { it.id == dp.profileId } ?: profs.firstOrNull { normName(it.name) == normName(dp.name) }
            val prof = existing ?: PlayerProfile(dp.profileId, dp.name, now).also { profs += it }
            SetupPlayer(prof.id, dp.name)
        }
        profiles = profs.map { p -> if (players.any { it.profileId == p.id }) p.copy(lastPlayed = now) else p }
        persistProfiles()

        val pool = d.counts.flatMap { (r, c) -> List(c.coerceAtLeast(0)) { r } }
        var assigned: Map<Int, String> = emptyMap()
        var extras: List<String> = emptyList()
        when (d.handout) {
            Handout.APP_DEALS -> {
                val sh = pool.shuffled()
                assigned = players.indices.filter { it < sh.size }.associateWith { sh[it] }
                extras = sh.drop(players.size)
            }
            Handout.MANUAL -> {
                assigned = players.indices.mapNotNull { i -> d.manual[players[i].profileId]?.let { i to it } }.toMap()
                if (assigned.size == players.size) {
                    val remaining = pool.toMutableList()
                    assigned.values.forEach { remaining.remove(it) }
                    extras = remaining
                }
            }
            Handout.PHYSICAL -> Unit
        }
        val rec = GameRecord(
            id = UUID.randomUUID().toString(),
            startedAt = now,
            setup = GameSetup(players, d.counts.filterValues { it > 0 }, d.handout, assigned, extras, roles, settings.rules),
        )
        games = listOf(rec) + games
        open(rec)
        io.execute { store.saveGame(rec) }
        updateSettings { it.copy(currentGameId = rec.id, handout = d.handout) }
        replaceTop(if (d.handout == Handout.APP_DEALS) Screen.Reveal else Screen.Game)
    }

    fun finishReveal() { replaceTop(Screen.Game) }

    fun playAgain(g: GameRecord) {
        draft = SetupDraft(
            players = g.setup.players.map { DraftPlayer(it.profileId, profileName(it.profileId, it.name)) },
            counts = g.setup.roleCounts,
            handout = settings.handout,
        )
        go(Screen.Setup)
    }

    // ------------------------------------------------------------ game

    private fun open(rec: GameRecord) {
        record = rec
        val s = Engine.replay(rec)
        state = s
        dawnSeenDay = s.day
    }

    private fun commit(rec: GameRecord) {
        record = rec
        state = Engine.replay(rec)
        games = if (games.any { it.id == rec.id }) games.map { if (it.id == rec.id) rec else it } else listOf(rec) + games
        io.execute { store.saveGame(rec) }
    }

    private fun showResultOf(a: Action) {
        if (a is Action.Step && state?.results?.containsKey(a.stepId) == true) resultFor = a.stepId
    }

    fun dispatch(a: Action) {
        val r = record ?: return
        commit(r.copy(actions = r.actions + a, redo = emptyList()))
        showResultOf(a)
    }

    fun replaceAction(i: Int, a: Action) {
        val r = record ?: return
        if (i !in r.actions.indices) return
        commit(r.copy(actions = r.actions.mapIndexed { j, old -> if (j == i) a else old }, redo = emptyList()))
        showResultOf(a)
    }

    fun deleteAction(i: Int) {
        val r = record ?: return
        commit(r.copy(actions = r.actions.filterIndexed { j, _ -> j != i }, redo = emptyList()))
    }

    /** Undoes every action from [i] on; they stay available for redo. */
    fun undoFrom(i: Int) {
        val r = record ?: return
        if (i !in r.actions.indices) return
        commit(r.copy(actions = r.actions.take(i), redo = r.actions.drop(i) + r.redo))
    }

    fun undo() {
        val r = record ?: return
        if (r.actions.isEmpty()) return
        commit(r.copy(actions = r.actions.dropLast(1), redo = listOf(r.actions.last()) + r.redo))
    }

    fun redo() {
        val r = record ?: return
        if (r.redo.isEmpty()) return
        commit(r.copy(actions = r.actions + r.redo.first(), redo = r.redo.drop(1)))
    }

    fun endGame(winners: List<Int>, label: String) {
        val r = record ?: return
        val done = r.copy(endedAt = System.currentTimeMillis(), winners = winners, winnerLabel = label)
        commit(done)
        record = null
        state = null
        updateSettings { it.copy(currentGameId = null) }
        home()
        go(Screen.GameDetail(done.id))
    }

    fun abandonGame() {
        val r = record ?: return
        deleteGame(r.id)
        home()
    }

    fun deleteGame(id: String) {
        games = games.filter { it.id != id }
        io.execute { store.deleteGame(id) }
        if (record?.id == id) {
            record = null
            state = null
            updateSettings { it.copy(currentGameId = null) }
        }
    }

    fun clearHistory() {
        val keep = record?.id
        val gone = games.filter { it.id != keep }
        games = games.filter { it.id == keep }
        io.execute { gone.forEach { store.deleteGame(it.id) } }
    }

    // ------------------------------------------------------------ voice

    fun speak(text: String) {
        if (!settings.readAloud || text.isBlank()) return
        val engine = tts
        if (engine == null) {
            pendingSpeech = text
            tts = TextToSpeech(getApplication()) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                pendingSpeech?.let { speakNow(it) }
                pendingSpeech = null
            }
        } else if (ttsReady) speakNow(text) else pendingSpeech = text
    }

    private fun speakNow(text: String) {
        val t = tts ?: return
        t.language = if (Lang.french) Locale.FRENCH else Locale.ENGLISH
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step")
    }

    // ------------------------------------------------------------ backup

    fun exportJson(): String = store.exportBackup(Backup(profiles, roles, presets, games))

    fun importJson(text: String?): Boolean {
        if (text == null) return false
        return try {
            val b = store.parseBackup(text)
            val ids = profiles.map { it.id }.toSet()
            profiles = profiles + b.players.filter { it.id !in ids }
            val roleIds = b.roles.map { it.id }.toSet()
            if (b.roles.isNotEmpty()) roles = roles.filter { it.id !in roleIds } + b.roles
            presets = presets.filter { p -> b.presets.none { it.name == p.name } } + b.presets
            val gameIds = games.map { it.id }.toSet()
            val newGames = b.games.filter { it.id !in gameIds }
            games = (games + newGames).sortedByDescending { it.startedAt }
            val (p, r, pr) = Triple(profiles, roles, presets)
            io.execute {
                store.saveProfiles(p); store.saveRoles(r); store.savePresets(pr)
                newGames.forEach { store.saveGame(it) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun teamOfRole(id: String): Team? = role(id)?.team

    override fun onCleared() {
        tts?.shutdown()
        io.shutdown()
        super.onCleared()
    }
}
