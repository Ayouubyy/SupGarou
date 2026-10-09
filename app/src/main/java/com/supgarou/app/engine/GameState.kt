package com.supgarou.app.engine

import com.supgarou.app.model.Action
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.Handout
import com.supgarou.app.model.RoleDef
import com.supgarou.app.model.RoleIds
import com.supgarou.app.model.RuleSettings
import com.supgarou.app.model.Team
import com.supgarou.app.model.tr

enum class Phase { NIGHT, DAY }

enum class DeathCause { WOLVES, POISON, VOTE, HUNTER, GRIEF, ALIEN, ALIEN_MISS, NARRATOR, CAUGHT }

fun causeText(c: DeathCause?): String = when (c) {
    DeathCause.WOLVES -> tr("eaten by the wolves", "dévoré par les loups")
    DeathCause.POISON -> tr("poisoned by the Sorcière", "empoisonné par la Sorcière")
    DeathCause.VOTE -> tr("voted out", "éliminé par le vote")
    DeathCause.HUNTER -> tr("shot by the Chasseur", "abattu par le Chasseur")
    DeathCause.GRIEF -> tr("died of grief", "mort de chagrin")
    DeathCause.ALIEN -> tr("taken by the Alien", "enlevé par l'Alien")
    DeathCause.ALIEN_MISS -> tr("wrong Alien guess", "mauvaise devinette de l'Alien")
    DeathCause.NARRATOR -> tr("removed by the narrator", "retiré par le narrateur")
    DeathCause.CAUGHT -> tr("caught spying by the wolves", "surprise par les loups")
    null -> ""
}

/** One player during a game. Mutable only while the engine replays the log. */
class P(val pid: Int, var name: String, val profileId: String) {
    var role: String? = null
    var startRole: String? = null
    var alive = true
    var cause: DeathCause? = null
    var diedAt: String? = null
    var infected = false
    var powerless = false
    var lover: Int? = null
    var charmed = false
    var mutedDay: Int? = null
    var corbeauDay: Int? = null
    var jugeDay: Int? = null
    var blockedNight: Int? = null
    var protectedNight: Int? = null
    var sheepNight: Int? = null
    var starterDay: Int? = null
    /** Night the Voyante looked at this player's card. */
    var seenNight: Int? = null
    var livesUsed = 0
    val badges = mutableMapOf<String, Int>()
}

data class Death(val pid: Int, val cause: DeathCause)

class DawnReport(val night: Int) {
    val deaths = mutableListOf<Death>()
    var oursGrowl: Boolean? = null
    var grandOursTarget: Int? = null
    var grandOursGrowl: Boolean? = null
    var muted: List<Int> = emptyList()
    var corbeau: List<Int> = emptyList()
    var juge: List<Int> = emptyList()
    var starter: List<Int> = emptyList()
    var sheep: List<Pair<Int, Boolean>> = emptyList()
    /** What the Voyante saw tonight, to tell at dawn. */
    var seen: StepResult.Seen? = null
    /** Players the Loup rouge blocked tonight. */
    var blocked: List<Int> = emptyList()
    /** Narrator-only notes, such as who was saved. */
    val notes = mutableListOf<String>()
}

data class LogLine(val index: Int, val label: String, val text: String, val flag: String?)

enum class StepKind { TARGETS, VOLEUR, WOLVES, SORCIERE, BERGER, INFO_LOVERS, INFO_CHARMED, ALIEN_SIGN, IDENTIFY_REST, CUSTOM }

data class NightStep(
    val id: String,
    val roleId: String?,
    val kind: StepKind,
    /** Roles whose players the narrator must point out first (physical cards). */
    val identify: List<String>,
    val minTargets: Int,
    val maxTargets: Int,
    /** Nobody can act (dead or powerless): call the role anyway so nobody notices. */
    val fake: Boolean,
    /** The Loup rouge blocked this role tonight. */
    val blocked: Boolean,
)

sealed class StepResult {
    data class Seen(val pid: Int, val shownRole: String?, val shownTeam: Team?, val hidden: Boolean) : StepResult()
    data class Sniff(val pids: List<Int>, val found: Boolean, val lostPower: Boolean) : StepResult()
    data class Sheep(val outcomes: List<Pair<Int, Boolean>>, val left: Int) : StepResult()
    data object Blocked : StepResult()
}

class GameState(val record: GameRecord) {
    val setup = record.setup
    var rules: RuleSettings = setup.rules
    val roles: Map<String, RoleDef> = setup.roles.associateBy { it.id }
    val players: List<P> = setup.players.mapIndexed { i, sp -> P(i, sp.name, sp.profileId) }
    val seating: MutableList<Int> = players.indices.toMutableList()

    var phase = Phase.NIGHT
    var night = 1
    var day = 0

    val extras: MutableList<String> = setup.extras.toMutableList()
    /** False while the cards left in the middle are unknown (physical cards with a Voleur). */
    var extrasKnown = setup.extras.isNotEmpty() || setup.roleCounts.values.sum() <= setup.players.size
    val absent = mutableSetOf<String>()

    /** Steps done this night: step id -> index of its action in the log. */
    val done = mutableMapOf<String, Int>()
    val nightActs = mutableMapOf<String, Action.Step>()
    val results = mutableMapOf<String, StepResult>()

    var nightVictim: Int? = null
    var nightInfect = false
    var nightCaught = false
    var nightSave = false
    var nightPoison: Int? = null
    var nightGrandOurs: Int? = null

    var lifeUsed = false
    var deathUsed = false
    var sheepLeft = rules.bergerSheep
    var renardPower = true
    var infectionsUsed = 0
    val salvaHistory = mutableMapOf<Int, Int>()
    var villagePowersLost = false

    var votedToday = false
    val pendingShots = mutableListOf<Int>()
    var dawn: DawnReport? = null
    var trollWin: Int? = null
    var alienSign: String? = null
    val dayEvents = mutableListOf<String>()

    val log = mutableListOf<LogLine>()
    val flags = mutableMapOf<Int, String>()

    init {
        setup.assigned.forEach { (pid, role) ->
            players.getOrNull(pid)?.let { it.role = role; it.startRole = role }
        }
    }

    fun p(pid: Int?): P? = pid?.let { players.getOrNull(it) }
    fun role(id: String?): RoleDef? = id?.let { roles[it] }
    fun name(pid: Int?): String = p(pid)?.name ?: "?"
    fun roleName(id: String?): String = role(id)?.name ?: tr("Unknown", "Inconnu")
    fun roleIcon(id: String?): String = role(id)?.icon ?: "❓"
    fun names(pids: List<Int>): String = if (pids.isEmpty()) tr("nobody", "personne") else pids.joinToString(", ") { name(it) }

    fun label(): String = if (phase == Phase.NIGHT) tr("Night $night", "Nuit $night") else tr("Day $day", "Jour $day")

    fun teamOf(p: P): Team? = if (p.infected) Team.WOLVES else role(p.role)?.team

    /** Wolf for the Renard and the Berger. */
    fun isWolfForChecks(p: P): Boolean =
        if (p.infected) rules.infectedCountAsWolves || role(p.role)?.team == Team.WOLVES
        else role(p.role)?.team == Team.WOLVES

    /** Wolf as seen by the Voyante, the Ours and the Grand ours. */
    fun looksLikeWolf(p: P): Boolean =
        if (p.role == RoleIds.LOUP_BLANC && rules.loupBlancHidden && !p.infected) false else isWolfForChecks(p)

    fun activeCount(role: String): Int = (setup.roleCounts[role] ?: 0) - extras.count { it == role }
    fun assignedCount(role: String): Int = players.count { it.role == role }
    val hasUnknown: Boolean get() = players.any { it.role == null }
    fun needsIdentify(role: String): Boolean =
        role !in absent && hasUnknown && assignedCount(role) < activeCount(role)

    fun inPlay(role: String): Boolean = role !in absent && activeCount(role) > 0
    fun holders(role: String): List<P> = players.filter { it.role == role }
    fun holdersAlive(role: String): Boolean = holders(role).any { it.alive }

    fun villageLost(role: String): Boolean = villagePowersLost && role(role)?.team == Team.VILLAGE

    /** True when at least one holder of the role can use it tonight. */
    fun effective(role: String): Boolean =
        holders(role).any { it.alive && !it.powerless && it.blockedNight != night && !villageLost(role) } ||
            (needsIdentify(role) && !villageLost(role))

    fun blocked(role: String): Boolean = holders(role).any { it.alive && it.blockedNight == night }

    /** The two closest living neighbors in seat order. */
    fun neighbors(pid: Int): List<Int> {
        val ring = seating.filter { players[it].alive || it == pid }
        val i = ring.indexOf(pid)
        if (i < 0 || ring.size < 2) return emptyList()
        val prev = ring[(i - 1 + ring.size) % ring.size]
        val next = ring[(i + 1) % ring.size]
        return listOf(prev, next).filter { it != pid }.distinct()
    }

    fun aliveCount(team: Team): Int = players.count { it.alive && teamOf(it) == team }

    /** Kills players and everyone their deaths drag along (lovers, Ancien penalty, Chasseur shot). */
    fun killChain(initial: List<Death>, at: String, out: MutableList<Death>) {
        val queue = ArrayDeque(initial)
        while (queue.isNotEmpty()) {
            val d = queue.removeFirst()
            val p = p(d.pid) ?: continue
            if (!p.alive) continue
            p.alive = false
            p.cause = d.cause
            p.diedAt = at
            out += d
            p.lover?.let { l -> if (players[l].alive) queue.addLast(Death(l, DeathCause.GRIEF)) }
            if (p.role == RoleIds.ANCIEN && rules.ancienPenalty &&
                d.cause in setOf(DeathCause.VOTE, DeathCause.POISON, DeathCause.HUNTER)
            ) villagePowersLost = true
            if (p.role == RoleIds.CHASSEUR && !p.powerless && !villageLost(RoleIds.CHASSEUR) &&
                !(d.cause == DeathCause.POISON && !rules.chasseurShootsIfPoisoned)
            ) pendingShots += p.pid
        }
    }

    fun clearNight() {
        done.clear(); nightActs.clear(); results.clear()
        nightVictim = null; nightInfect = false; nightCaught = false
        nightSave = false; nightPoison = null; nightGrandOurs = null
    }
}
