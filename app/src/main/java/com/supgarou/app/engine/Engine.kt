package com.supgarou.app.engine

import com.supgarou.app.model.Action
import com.supgarou.app.model.CustomWake
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.RevealDead
import com.supgarou.app.model.RoleIds
import com.supgarou.app.model.Status
import com.supgarou.app.model.StepIds
import com.supgarou.app.model.Team
import com.supgarou.app.model.teamName
import com.supgarou.app.model.tr
import kotlin.math.min

data class StepTexts(val title: String, val icon: String, val question: String, val say: String)

/**
 * The rules engine. A game's state is never edited directly: it is rebuilt by replaying
 * the setup and every narrator action in order. Undo, redo and edits only change the log.
 */
object Engine {

    fun replay(record: GameRecord): GameState {
        val s = GameState(record)
        record.actions.forEachIndexed { i, a -> apply(s, a, i) }
        return s
    }

    fun stepRole(stepId: String): String? = when (stepId) {
        StepIds.VOLEUR -> RoleIds.VOLEUR
        StepIds.CUPIDON, StepIds.LOVERS -> RoleIds.CUPIDON
        StepIds.ALIEN_SIGN -> RoleIds.ALIEN
        StepIds.LOUP_ROUGE -> RoleIds.LOUP_ROUGE
        StepIds.SALVA -> RoleIds.SALVA
        StepIds.VOYANTE -> RoleIds.VOYANTE
        StepIds.RENARD -> RoleIds.RENARD
        StepIds.GRAND_OURS -> RoleIds.GRAND_OURS
        StepIds.BERGER -> RoleIds.BERGER
        StepIds.WOLVES -> null
        StepIds.LOUP_NOIR -> RoleIds.LOUP_NOIR
        StepIds.SORCIERE -> RoleIds.SORCIERE
        StepIds.FLUTE, StepIds.CHARMED -> RoleIds.FLUTE
        StepIds.CORBEAU -> RoleIds.CORBEAU
        StepIds.JUGE -> RoleIds.JUGE
        StepIds.ANCIEN -> RoleIds.ANCIEN
        StepIds.IDENTIFY_REST -> null
        else -> if (stepId.startsWith(StepIds.CUSTOM_PREFIX)) stepId.removePrefix(StepIds.CUSTOM_PREFIX) else null
    }

    // ---------------------------------------------------------------- night steps

    fun nightSteps(s: GameState): List<NightStep> {
        if (s.phase != Phase.NIGHT) return emptyList()
        val order = s.rules.nightOrder.toMutableList()
        StepIds.DEFAULT_ORDER.forEach { if (it !in order) order += it }
        s.roles.values.filter { it.custom && it.customWake != CustomWake.NEVER }.forEach {
            val id = StepIds.CUSTOM_PREFIX + it.id
            if (id !in order) order += id
        }
        val out = mutableListOf<NightStep>()
        for (id in order) {
            if (id in s.rules.disabledSteps && id !in s.done) continue
            build(s, id)?.let { out += it }
        }
        identifyRest(s)?.let { out += it }
        return out
    }

    private fun roleStep(
        s: GameState, id: String, role: String, kind: StepKind, min: Int, max: Int, cond: Boolean,
    ): NightStep? {
        val wasDone = id in s.done
        if (!wasDone && (!s.inPlay(role) || !cond)) return null
        val unknown = s.needsIdentify(role)
        val living = s.holders(role).filter { it.alive }
        val fake = (living.isEmpty() && !unknown) || s.villageLost(role) ||
            (living.isNotEmpty() && living.all { it.powerless })
        val blocked = s.blocked(role)
        if (!wasDone) {
            if (fake && !s.rules.fakeTurns) return null
            if (blocked && !s.rules.blockedStillCalled) return null
        }
        return NightStep(id, role, kind, if (unknown) listOf(role) else emptyList(), min, max, fake, blocked)
    }

    private fun build(s: GameState, id: String): NightStep? {
        val r = s.rules
        val n = s.night
        return when (id) {
            StepIds.VOLEUR -> roleStep(s, id, RoleIds.VOLEUR, StepKind.VOLEUR, 0, 0, n == 1)
            StepIds.CUPIDON -> roleStep(s, id, RoleIds.CUPIDON, StepKind.TARGETS, 2, 2, n == 1)
            StepIds.LOVERS -> when {
                n != 1 -> null
                id in s.done -> NightStep(id, RoleIds.CUPIDON, StepKind.INFO_LOVERS, emptyList(), 0, 0, false, false)
                !s.inPlay(RoleIds.CUPIDON) || StepIds.CUPIDON in r.disabledSteps -> null
                StepIds.CUPIDON in s.done && s.players.none { it.lover != null } -> null
                else -> NightStep(id, RoleIds.CUPIDON, StepKind.INFO_LOVERS, emptyList(), 0, 0, false, false)
            }
            StepIds.ALIEN_SIGN -> roleStep(s, id, RoleIds.ALIEN, StepKind.ALIEN_SIGN, 0, 0, n == 1)
            StepIds.LOUP_ROUGE -> roleStep(s, id, RoleIds.LOUP_ROUGE, StepKind.TARGETS, 1, 1, n >= r.loupRougeFirstNight)
            StepIds.SALVA -> roleStep(s, id, RoleIds.SALVA, StepKind.TARGETS, 1, 1, true)
            StepIds.VOYANTE -> roleStep(s, id, RoleIds.VOYANTE, StepKind.TARGETS, 1, 1, true)
            StepIds.RENARD -> roleStep(s, id, RoleIds.RENARD, StepKind.TARGETS, 1, 1, s.renardPower)
            StepIds.GRAND_OURS -> roleStep(s, id, RoleIds.GRAND_OURS, StepKind.TARGETS, 1, 1, true)
            StepIds.BERGER -> {
                val max = if (r.bergerPerNight > 0) min(r.bergerPerNight, s.sheepLeft) else s.sheepLeft
                roleStep(s, id, RoleIds.BERGER, StepKind.BERGER, 0, max, s.sheepLeft > 0)
            }
            StepIds.WOLVES -> {
                val pack = RoleIds.PACK.filter { s.inPlay(it) }
                if (pack.isEmpty() && id !in s.done) return null
                val unknown = pack.filter { s.needsIdentify(it) }
                val anyAlive = s.players.any { it.alive && (it.role in RoleIds.PACK || it.infected) }
                val fake = !anyAlive && unknown.isEmpty()
                if (fake && !r.fakeTurns && id !in s.done) return null
                val kill = !(n == 1 && !r.wolvesKillNight1)
                NightStep(id, RoleIds.LOUP, StepKind.WOLVES, unknown, 0, if (kill) 1 else 0, fake, false)
            }
            StepIds.LOUP_NOIR -> roleStep(s, id, RoleIds.LOUP_NOIR, StepKind.TARGETS, 1, 1, n >= r.loupNoirFirstNight)
            StepIds.SORCIERE -> roleStep(
                s, id, RoleIds.SORCIERE, StepKind.SORCIERE, 0, 1, !(s.lifeUsed && s.deathUsed) || r.fakeTurns,
            )
            StepIds.FLUTE -> {
                val left = s.players.count { it.alive && !it.charmed && it.role != RoleIds.FLUTE }
                val k = min(r.fluteCharms, left)
                roleStep(s, id, RoleIds.FLUTE, StepKind.TARGETS, k, k, true)
            }
            StepIds.CHARMED -> {
                val flute = s.inPlay(RoleIds.FLUTE) && (s.holdersAlive(RoleIds.FLUTE) || s.needsIdentify(RoleIds.FLUTE))
                if (id in s.done || (flute && s.players.any { it.alive && it.charmed })) {
                    NightStep(id, RoleIds.FLUTE, StepKind.INFO_CHARMED, emptyList(), 0, 0, false, false)
                } else null
            }
            StepIds.CORBEAU -> roleStep(s, id, RoleIds.CORBEAU, StepKind.TARGETS, 1, 1, n >= r.corbeauFirstNight)
            StepIds.JUGE -> roleStep(s, id, RoleIds.JUGE, StepKind.TARGETS, 1, 1, n >= r.jugeFirstNight)
            StepIds.ANCIEN -> roleStep(s, id, RoleIds.ANCIEN, StepKind.TARGETS, 1, 1, true)
            else -> {
                if (!id.startsWith(StepIds.CUSTOM_PREFIX)) return null
                val role = s.role(id.removePrefix(StepIds.CUSTOM_PREFIX)) ?: return null
                if (!role.custom) return null
                val cond = when (role.customWake) {
                    CustomWake.NEVER -> false
                    CustomWake.NIGHT_1 -> n == 1
                    CustomWake.EVERY_NIGHT -> true
                    CustomWake.FROM_NIGHT_2 -> n >= 2
                }
                roleStep(s, id, role.id, StepKind.CUSTOM, 0, role.customTargets, cond)
            }
        }
    }

    private fun identifyRest(s: GameState): NightStep? {
        if (s.night != 1) return null
        val id = StepIds.IDENTIFY_REST
        val roles = s.setup.roleCounts.keys.filter { it != RoleIds.VILLAGEOIS && s.needsIdentify(it) }
        if (roles.isEmpty() && id !in s.done) return null
        return NightStep(id, null, StepKind.IDENTIFY_REST, roles, 0, 0, false, false)
    }

    fun canInfect(s: GameState): Boolean =
        s.inPlay(RoleIds.LOUP_INFECT) && s.effective(RoleIds.LOUP_INFECT) &&
            s.infectionsUsed < s.rules.infections && s.night >= s.rules.infectFirstNight

    fun canCatchPetiteFille(s: GameState): Boolean =
        s.rules.petiteFilleCatchable && s.inPlay(RoleIds.PETITE_FILLE) &&
            (s.holdersAlive(RoleIds.PETITE_FILLE) || s.needsIdentify(RoleIds.PETITE_FILLE))

    // ---------------------------------------------------------------- texts

    fun stepTitle(s: GameState, stepId: String): String = when (stepId) {
        StepIds.LOVERS -> tr("Lovers", "Amoureux")
        StepIds.WOLVES -> tr("Wolves", "Loups-garous")
        StepIds.CHARMED -> tr("Charmed players", "Joueurs charmés")
        StepIds.IDENTIFY_REST -> tr("Other roles", "Autres rôles")
        StepIds.ALIEN_SIGN -> "Alien"
        else -> s.roleName(stepRole(stepId))
    }

    fun stepTexts(s: GameState, step: NightStep): StepTexts {
        val title = stepTitle(s, step.id)
        val icon = when (step.id) {
            StepIds.LOVERS -> "💞"
            StepIds.WOLVES -> s.roleIcon(RoleIds.LOUP)
            StepIds.CHARMED -> "🎶"
            StepIds.IDENTIFY_REST -> "🃏"
            else -> s.roleIcon(stepRole(step.id))
        }
        val (q, say) = when (step.id) {
            StepIds.VOLEUR -> tr("Voleur: keep your card or take one from the middle?", "Voleur : garder ta carte ou en prendre une au milieu ?") to
                tr("Voleur, wake up. You may swap your card with one of the cards in the middle.", "Voleur, réveille-toi. Tu peux échanger ta carte avec une des cartes du milieu.")
            StepIds.CUPIDON -> tr("Cupidon: who are the two lovers?", "Cupidon : qui sont les deux amoureux ?") to
                tr("Cupidon, wake up and choose two lovers.", "Cupidon, réveille-toi et désigne deux amoureux.")
            StepIds.LOVERS -> tr("The lovers wake up and see each other.", "Les amoureux se réveillent et se reconnaissent.") to
                tr("I touch the lovers: wake up, look at each other, then go back to sleep.", "Je touche les amoureux : réveillez-vous, regardez-vous, puis rendormez-vous.")
            StepIds.ALIEN_SIGN -> tr("Alien: note his secret sign.", "Alien : note son signe secret.") to
                tr("Alien, wake up and show me your secret sign.", "Alien, réveille-toi et montre-moi ton signe secret.")
            StepIds.LOUP_ROUGE -> tr("Loup rouge: whose power is blocked tonight?", "Loup rouge : quel pouvoir est bloqué cette nuit ?") to
                tr("Loup rouge, wake up and choose a player whose power is blocked tonight.", "Loup rouge, réveille-toi et choisis un joueur dont le pouvoir est bloqué cette nuit.")
            StepIds.SALVA -> tr("Salva: who do you protect?", "Salva : qui protèges-tu ?") to
                tr("Salva, wake up and choose someone to protect.", "Salva, réveille-toi et désigne quelqu'un à protéger.")
            StepIds.VOYANTE -> tr("Voyante: whose card do you look at?", "Voyante : quelle carte regardes-tu ?") to
                tr("Voyante, wake up and choose a player whose card you want to see.", "Voyante, réveille-toi et désigne le joueur dont tu veux voir la carte.")
            StepIds.RENARD -> tr("Renard: who do you sniff?", "Renard : qui flaires-tu ?") to
                tr("Renard, wake up and point at a player.", "Renard, réveille-toi et désigne un joueur.")
            StepIds.GRAND_OURS -> tr("Grand ours: which player?", "Grand ours : quel joueur ?") to
                tr("Grand ours, wake up and choose a player.", "Grand ours, réveille-toi et désigne un joueur.")
            StepIds.BERGER -> tr("Berger: who gets a sheep tonight? (${s.sheepLeft} left)", "Berger : qui reçoit un mouton ? (${s.sheepLeft} restants)") to
                tr("Berger, wake up. Do you send a sheep tonight?", "Berger, réveille-toi. Envoies-tu un mouton cette nuit ?")
            StepIds.WOLVES -> (if (step.maxTargets == 0) tr("Wolves: no kill tonight.", "Loups : pas de victime cette nuit.") else tr("Wolves: who do you eat?", "Loups : qui dévorez-vous ?")) to
                tr("Wolves, wake up and choose your victim.", "Loups-garous, réveillez-vous et choisissez votre victime.")
            StepIds.LOUP_NOIR -> tr("Loup noir: who is muted tomorrow?", "Loup noir : qui sera muet demain ?") to
                tr("Loup noir, wake up and choose a player to mute tomorrow.", "Loup noir, réveille-toi et choisis un joueur qui sera muet demain.")
            StepIds.SORCIERE -> tr("Sorcière: save the victim, poison someone, or nothing?", "Sorcière : sauver la victime, empoisonner quelqu'un, ou rien ?") to
                tr("Sorcière, wake up. Here is tonight's victim. Do you use a potion?", "Sorcière, réveille-toi. Voici la victime de cette nuit. Utilises-tu une potion ?")
            StepIds.FLUTE -> tr("Joueur de flûte: who do you charm?", "Joueur de flûte : qui charmes-tu ?") to
                tr("Joueur de flûte, wake up and choose two players to charm.", "Joueur de flûte, réveille-toi et désigne deux joueurs à charmer.")
            StepIds.CHARMED -> tr("The charmed players wake up and see each other.", "Les joueurs charmés se réveillent et se reconnaissent.") to
                tr("I touch the charmed players: wake up and look at each other.", "Je touche les joueurs charmés : réveillez-vous et regardez-vous.")
            StepIds.CORBEAU -> tr("Corbeau: who starts the vote with ${s.rules.corbeauVotes} votes?", "Corbeau : qui commence le vote avec ${s.rules.corbeauVotes} voix ?") to
                tr("Corbeau, wake up and choose a player who starts tomorrow's vote with ${s.rules.corbeauVotes} votes against them.", "Corbeau, réveille-toi et désigne un joueur qui aura ${s.rules.corbeauVotes} voix contre lui demain.")
            StepIds.JUGE -> tr("Juge: who is protected from tomorrow's vote?", "Juge : qui est protégé du vote de demain ?") to
                tr("Juge, wake up and choose a player to protect from tomorrow's vote.", "Juge, réveille-toi et désigne un joueur protégé du vote de demain.")
            StepIds.ANCIEN -> tr("Ancien: who starts talking and voting tomorrow?", "Ancien : qui commence à parler et à voter demain ?") to
                tr("Ancien, wake up and choose who starts talking and voting tomorrow.", "Ancien, réveille-toi et désigne qui commence à parler et à voter demain.")
            StepIds.IDENTIFY_REST -> tr("Point out the players who hold the remaining roles.", "Désigne les joueurs qui ont les rôles restants.") to
                tr("Roles that haven't woken up yet: open your eyes so I can see you.", "Rôles qui ne se sont pas encore réveillés : ouvrez les yeux pour que je vous voie.")
            else -> tr("$title: choose up to ${step.maxTargets} player(s).", "$title : choisis jusqu'à ${step.maxTargets} joueur(s).") to
                tr("$title, wake up.", "$title, réveille-toi.")
        }
        return StepTexts(title, icon, q, say)
    }

    fun identifyQuestion(s: GameState, role: String): String {
        val k = (s.activeCount(role) - s.assignedCount(role)).coerceAtLeast(1)
        val rn = s.roleName(role)
        return if (k == 1) tr("Who is the $rn? Tap them.", "Qui est $rn ? Touche le joueur.")
        else tr("Who are the $k $rn? Tap them.", "Qui sont les $k $rn ? Touche les joueurs.")
    }

    /** A narrator warning for an unusual pick, or null when the pick is normal. */
    fun targetWarning(s: GameState, stepId: String?, pid: Int, mode: String = ""): String? {
        val p = s.p(pid) ?: return null
        val r = s.rules
        if (mode == "identify") {
            return if (p.role != null) tr("${p.name} is already the ${s.roleName(p.role)}.", "${p.name} est déjà ${s.roleName(p.role)}.") else null
        }
        if (!p.alive) return tr("${p.name} is dead.", "${p.name} est mort.")
        return when (stepId) {
            StepIds.SALVA -> when {
                !r.salvaSameTwice && s.salvaHistory[s.night - 1] == pid ->
                    tr("${p.name} was protected last night.", "${p.name} a été protégé la nuit dernière.")
                !r.salvaSelf && p.role == RoleIds.SALVA -> tr("The Salva can't protect himself.", "Le Salva ne peut pas se protéger.")
                else -> null
            }
            StepIds.LOUP_ROUGE -> if (!r.loupRougeCanBlockWolves && s.isWolfForChecks(p)) tr("${p.name} is a wolf.", "${p.name} est un loup.") else null
            StepIds.WOLVES -> when {
                s.teamOf(p) == Team.WOLVES -> tr("${p.name} is a wolf.", "${p.name} est un loup.")
                p.role == RoleIds.FLUTE && r.fluteImmuneToWolves -> tr("The Joueur de flûte can't die to the wolves.", "Le Joueur de flûte ne peut pas mourir des loups.")
                else -> null
            }
            StepIds.FLUTE -> when {
                p.charmed -> tr("${p.name} is already charmed.", "${p.name} est déjà charmé.")
                p.role == RoleIds.FLUTE -> tr("That's the Joueur de flûte himself.", "C'est le Joueur de flûte lui-même.")
                else -> null
            }
            StepIds.CUPIDON -> if (!r.cupidonSelf && p.role == RoleIds.CUPIDON) tr("Cupidon can't choose himself.", "Cupidon ne peut pas se choisir.") else null
            StepIds.SORCIERE -> if (p.role == RoleIds.SORCIERE) tr("The Sorcière would poison herself.", "La Sorcière s'empoisonnerait.") else null
            StepIds.BERGER -> if (p.role == RoleIds.BERGER) tr("That's the Berger himself.", "C'est le Berger lui-même.") else null
            else -> null
        }
    }

    fun revealText(s: GameState, p: P): String = when (s.rules.revealDead) {
        RevealDead.FULL -> s.roleName(p.role)
        RevealDead.TEAM -> teamName(s.teamOf(p))
        RevealDead.HIDDEN -> tr("role hidden", "rôle caché")
    }

    private fun deathsText(s: GameState, deaths: List<Death>): String =
        deaths.joinToString("; ") { d -> "${s.name(d.pid)} (${s.roleName(s.p(d.pid)?.role)}) ${causeText(d.cause)}" }

    private fun cycle(s: GameState): Int = if (s.phase == Phase.NIGHT) s.night else s.day
    private fun at(s: GameState): String = if (s.phase == Phase.NIGHT) "N${s.night}" else "D${s.day}"

    // ---------------------------------------------------------------- applying actions

    fun apply(s: GameState, a: Action, i: Int) {
        val label = s.label()
        val (text, flag) = try {
            applyInner(s, a, i)
        } catch (e: Exception) {
            "?" to (e.message ?: "error")
        }
        if (flag != null) s.flags[i] = flag
        s.log += LogLine(i, label, text, flag)
    }

    private fun deadFlag(s: GameState, pids: List<Int>): String? =
        pids.firstOrNull { s.p(it)?.alive == false }?.let { tr("${s.name(it)} was already dead", "${s.name(it)} était déjà mort") }

    private fun applyInner(s: GameState, a: Action, i: Int): Pair<String, String?> {
        val notNight = tr("Not at night: ignored", "Pas la nuit : ignoré")
        val notDay = tr("Not during the day: ignored", "Pas le jour : ignoré")
        return when (a) {
            is Action.Assign -> {
                a.pids.forEach { pid ->
                    s.p(pid)?.let { p -> p.role = a.roleId; if (p.startRole == null) p.startRole = a.roleId }
                }
                s.absent.remove(a.roleId)
                "${s.roleName(a.roleId)} = ${s.names(a.pids)}" to null
            }
            is Action.Absent -> {
                s.absent += a.roleId
                tr("${s.roleName(a.roleId)} is not in play", "${s.roleName(a.roleId)} n'est pas en jeu") to null
            }
            is Action.Extras -> {
                s.extras.clear(); s.extras.addAll(a.roles); s.extrasKnown = true
                tr("Cards in the middle: ", "Cartes au milieu : ") + a.roles.joinToString(", ") { s.roleName(it) } to null
            }
            is Action.Step -> {
                if (s.phase != Phase.NIGHT) return Engine.stepTitle(s, a.stepId) to notNight
                val flag = deadFlag(s, a.targets)
                s.nightActs[a.stepId] = a
                s.done[a.stepId] = i
                applyStep(s, a) to flag
            }
            is Action.Dawn -> {
                if (s.phase != Phase.NIGHT) return tr("Dawn", "Aube") to notNight
                dawn(s)
            }
            is Action.Vote -> {
                if (s.phase != Phase.DAY) return tr("Vote", "Vote") to notDay
                s.votedToday = true
                val pid = a.pid
                if (pid == null) {
                    val t = tr("Vote: nobody was eliminated", "Vote : personne n'est éliminé")
                    s.dayEvents += t
                    return t to null
                }
                val p = s.p(pid) ?: return "?" to "?"
                if (!p.alive) return tr("Vote: ${p.name}", "Vote : ${p.name}") to deadFlag(s, listOf(pid))
                val jd = p.jugeDay
                if (jd != null && (s.rules.jugeUntilUsed || jd == s.day)) {
                    p.jugeDay = null
                    val t = tr("${p.name} was voted out, but the Juge protects them: they stay.", "${p.name} a été éliminé, mais le Juge le protège : il reste.")
                    s.dayEvents += t
                    return t to null
                }
                val out = mutableListOf<Death>()
                s.killChain(listOf(Death(pid, DeathCause.VOTE)), at(s), out)
                if (p.role == RoleIds.TROLL) s.trollWin = pid
                val t = deathsText(s, out)
                s.dayEvents += t
                t to null
            }
            is Action.Shot -> {
                s.pendingShots.remove(a.hunter)
                val target = a.target
                if (target == null) {
                    val t = tr("${s.name(a.hunter)} (Chasseur) did not shoot", "${s.name(a.hunter)} (Chasseur) n'a pas tiré")
                    if (s.phase == Phase.DAY) s.dayEvents += t
                    return t to null
                }
                val flag = deadFlag(s, listOf(target))
                val out = mutableListOf<Death>()
                s.killChain(listOf(Death(target, DeathCause.HUNTER)), at(s), out)
                val t = tr("${s.name(a.hunter)} (Chasseur) shot ${s.name(target)}", "${s.name(a.hunter)} (Chasseur) a tiré sur ${s.name(target)}") +
                    (if (out.size > 1) " — " + deathsText(s, out.drop(1)) else "")
                if (s.phase == Phase.DAY) s.dayEvents += t
                t to flag
            }
            is Action.AlienGuess -> {
                val target = s.p(a.target)
                val alien = s.p(a.alien)
                if (target == null || alien == null) return "Alien" to "?"
                val right = target.role == a.roleId || (!s.rules.alienExact && a.roleId == RoleIds.LOUP && s.teamOf(target) == Team.WOLVES)
                val out = mutableListOf<Death>()
                val death = if (right) Death(target.pid, DeathCause.ALIEN) else Death(alien.pid, DeathCause.ALIEN_MISS)
                s.killChain(listOf(death), at(s), out)
                val t = tr(
                    "Alien guessed ${target.name} is ${s.roleName(a.roleId)}: ${if (right) "right" else "wrong"}. ",
                    "L'Alien a dit que ${target.name} est ${s.roleName(a.roleId)} : ${if (right) "juste" else "faux"}. ",
                ) + deathsText(s, out)
                s.dayEvents += t
                t to (if (!target.alive && out.isEmpty()) deadFlag(s, listOf(target.pid)) else null)
            }
            is Action.Nightfall -> {
                if (s.phase != Phase.DAY) return tr("Night falls", "La nuit tombe") to notDay
                s.phase = Phase.NIGHT
                s.night = s.day + 1
                s.clearNight()
                s.votedToday = false
                s.dayEvents.clear()
                tr("Night ${s.night} falls", "La nuit ${s.night} tombe") to null
            }
            is Action.Kill -> {
                val out = mutableListOf<Death>()
                s.killChain(listOf(Death(a.pid, DeathCause.NARRATOR)), at(s), out)
                val t = tr("Narrator removed ", "Le narrateur a retiré ") + deathsText(s, out).ifEmpty { s.name(a.pid) }
                if (s.phase == Phase.DAY) s.dayEvents += t
                t to null
            }
            is Action.Revive -> {
                s.p(a.pid)?.let { it.alive = true; it.cause = null; it.diedAt = null }
                s.pendingShots.remove(a.pid)
                tr("${s.name(a.pid)} is back in the game", "${s.name(a.pid)} revient en jeu") to null
            }
            is Action.SetRole -> {
                s.p(a.pid)?.let { p -> p.role = a.roleId; if (p.startRole == null) p.startRole = a.roleId }
                s.absent.remove(a.roleId)
                tr("${s.name(a.pid)} is now ${s.roleName(a.roleId)}", "${s.name(a.pid)} est maintenant ${s.roleName(a.roleId)}") to null
            }
            is Action.SetStatus -> {
                val p = s.p(a.pid) ?: return "?" to "?"
                val c = cycle(s)
                val nextNight = if (s.phase == Phase.NIGHT) s.night else s.day + 1
                when (a.status) {
                    Status.PROTECTED -> p.protectedNight = if (a.on) nextNight else null
                    Status.BLOCKED -> p.blockedNight = if (a.on) nextNight else null
                    Status.MUTED -> p.mutedDay = if (a.on) c else null
                    Status.CHARMED -> p.charmed = a.on
                    Status.INFECTED -> p.infected = a.on
                    Status.JUGE -> p.jugeDay = if (a.on) c else null
                    Status.CORBEAU -> p.corbeauDay = if (a.on) c else null
                    Status.POWERLESS -> p.powerless = a.on
                }
                "${p.name}: ${statusName(a.status)} ${if (a.on) "✓" else "✗"}" to null
            }
            is Action.SetLovers -> {
                link(s, a.a, a.b)
                (if (a.b == null) tr("${s.name(a.a)} has no lover", "${s.name(a.a)} n'a plus d'amoureux")
                else tr("${s.name(a.a)} and ${s.name(a.b)} are lovers", "${s.name(a.a)} et ${s.name(a.b)} sont amoureux")) to null
            }
            is Action.MoveSeat -> {
                if (s.seating.remove(a.pid)) s.seating.add(a.toIndex.coerceIn(0, s.seating.size), a.pid)
                tr("${s.name(a.pid)} moved to seat ${a.toIndex + 1}", "${s.name(a.pid)} déplacé à la place ${a.toIndex + 1}") to null
            }
            is Action.Rename -> {
                val old = s.name(a.pid)
                s.p(a.pid)?.name = a.name
                tr("$old renamed to ${a.name}", "$old renommé en ${a.name}") to null
            }
            is Action.Rules -> {
                s.rules = a.rules
                tr("Game rules changed", "Règles de la partie modifiées") to null
            }
        }
    }

    fun statusName(st: Status): String = when (st) {
        Status.PROTECTED -> tr("protected", "protégé")
        Status.BLOCKED -> tr("blocked", "bloqué")
        Status.MUTED -> tr("muted", "muet")
        Status.CHARMED -> tr("charmed", "charmé")
        Status.INFECTED -> tr("infected", "infecté")
        Status.JUGE -> tr("Juge protection", "protection du Juge")
        Status.CORBEAU -> tr("Corbeau votes", "voix du Corbeau")
        Status.POWERLESS -> tr("no power", "sans pouvoir")
    }

    private fun link(s: GameState, a: Int, b: Int?) {
        val pa = s.p(a) ?: return
        pa.lover?.let { s.p(it)?.lover = null }
        pa.lover = null
        if (b != null && b != a) {
            val pb = s.p(b) ?: return
            pb.lover?.let { s.p(it)?.lover = null }
            pa.lover = b
            pb.lover = a
        }
    }

    private fun applyStep(s: GameState, a: Action.Step): String {
        val title = stepTitle(s, a.stepId)
        if (a.skipped) return tr("$title: skipped", "$title : passé")
        val role = stepRole(a.stepId)
        val eff = role == null || s.effective(role)
        val t = a.targets
        val who = s.names(t)
        val noEffectText = tr(" (no effect: blocked or no power)", " (sans effet : bloqué ou sans pouvoir)")
        fun noEffect(): String {
            s.results[a.stepId] = StepResult.Blocked
            return noEffectText
        }
        return when (a.stepId) {
            StepIds.LOUP_ROUGE -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.blockedNight = s.night }; "" } else noEffect()
                tr("Loup rouge blocked $who", "Le Loup rouge a bloqué $who") + sfx
            }
            StepIds.SALVA -> {
                val sfx = if (eff) {
                    t.forEach { s.p(it)?.protectedNight = s.night }
                    t.firstOrNull()?.let { s.salvaHistory[s.night] = it }
                    ""
                } else noEffect()
                tr("Salva protected $who", "Le Salva a protégé $who") + sfx
            }
            StepIds.VOYANTE -> {
                val p = s.p(t.firstOrNull())
                val sfx = when {
                    p == null -> ""
                    !eff -> noEffect()
                    else -> {
                        val hidden = p.role == RoleIds.LOUP_BLANC && s.rules.loupBlancHidden && !p.infected
                        val shown = if (hidden) RoleIds.VILLAGEOIS else p.role
                        val team = if (hidden) Team.VILLAGE else s.role(p.role)?.team
                        s.results[a.stepId] = StepResult.Seen(p.pid, shown, team, hidden)
                        p.seenNight = s.night
                        " (${s.roleName(shown)})"
                    }
                }
                tr("Voyante looked at $who", "La Voyante a regardé $who") + sfx
            }
            StepIds.RENARD -> {
                val center = t.firstOrNull() ?: return tr("Renard sniffed nobody", "Le Renard n'a flairé personne")
                if (!eff) return tr("Renard sniffed around $who", "Le Renard a flairé autour de $who") + noEffect()
                val group = listOf(center) + s.neighbors(center)
                val found = group.any { pid -> s.p(pid)?.let { s.isWolfForChecks(it) } == true }
                val lose = if (s.rules.renardLosesOnWolf) found else !found
                if (lose) s.renardPower = false
                s.results[a.stepId] = StepResult.Sniff(group, found, lose)
                tr("Renard sniffed around $who: ${if (found) "yes" else "no"}", "Le Renard a flairé autour de $who : ${if (found) "oui" else "non"}")
            }
            StepIds.GRAND_OURS -> {
                val sfx = if (eff) { s.nightGrandOurs = t.firstOrNull(); "" } else noEffect()
                tr("Grand ours chose $who", "Le Grand ours a désigné $who") + sfx
            }
            StepIds.BERGER -> {
                if (!eff) return tr("Berger sent sheep to $who", "Le Berger a envoyé des moutons chez $who") + noEffect()
                val outcomes = t.map { pid ->
                    val p = s.p(pid)
                    val died = p != null && s.isWolfForChecks(p)
                    p?.sheepNight = s.night
                    if (died) s.sheepLeft = (s.sheepLeft - 1).coerceAtLeast(0)
                    pid to died
                }
                s.results[a.stepId] = StepResult.Sheep(outcomes, s.sheepLeft)
                tr("Berger sent sheep to $who", "Le Berger a envoyé des moutons chez $who") +
                    outcomes.joinToString(", ", " (", ")") { (pid, died) -> s.name(pid) + if (died) tr(": died", " : mort") else tr(": came back", " : revenu") }
            }
            StepIds.WOLVES -> {
                val v = t.firstOrNull()
                s.nightVictim = v
                s.nightCaught = a.option == "caught" && v != null
                s.nightInfect = a.option == "infect" && v != null && canInfect(s)
                if (s.nightCaught) {
                    s.p(v)?.let { if (it.role == null) { it.role = RoleIds.PETITE_FILLE; it.startRole = RoleIds.PETITE_FILLE } }
                }
                val extra = when {
                    s.nightCaught -> tr(" (Petite fille caught)", " (Petite fille surprise)")
                    s.nightInfect -> tr(" (infection)", " (infection)")
                    a.option == "infect" -> tr(" (infection not possible)", " (infection impossible)")
                    else -> ""
                }
                tr("Wolves chose $who", "Les loups ont choisi $who") + extra
            }
            StepIds.LOUP_NOIR -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.mutedDay = s.night }; "" } else noEffect()
                tr("Loup noir muted $who", "Le Loup noir a rendu muet $who") + sfx
            }
            StepIds.SORCIERE -> {
                if (!eff) return "Sorcière" + noEffect()
                val parts = mutableListOf<String>()
                if (a.option == "save" && !s.lifeUsed) {
                    s.lifeUsed = true
                    s.nightSave = true
                    parts += tr("saved ${s.name(s.nightVictim)}", "a sauvé ${s.name(s.nightVictim)}")
                }
                t.firstOrNull()?.let {
                    if (!s.deathUsed) {
                        s.deathUsed = true
                        s.nightPoison = it
                        parts += tr("poisoned ${s.name(it)}", "a empoisonné ${s.name(it)}")
                    }
                }
                "Sorcière: " + parts.joinToString(", ").ifEmpty { tr("did nothing", "n'a rien fait") }
            }
            StepIds.FLUTE -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.charmed = true }; "" } else noEffect()
                tr("Joueur de flûte charmed $who", "Le Joueur de flûte a charmé $who") + sfx
            }
            StepIds.CORBEAU -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.corbeauDay = s.night }; "" } else noEffect()
                tr("Corbeau chose $who", "Le Corbeau a désigné $who") + sfx
            }
            StepIds.JUGE -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.jugeDay = s.night }; "" } else noEffect()
                tr("Juge protected $who from the vote", "Le Juge a protégé $who du vote") + sfx
            }
            StepIds.ANCIEN -> {
                val sfx = if (eff) { t.forEach { s.p(it)?.starterDay = s.night }; "" } else noEffect()
                tr("Ancien chose $who to start", "L'Ancien a désigné $who pour commencer") + sfx
            }
            StepIds.CUPIDON -> {
                if (eff && t.size == 2) link(s, t[0], t[1])
                tr("Cupidon linked $who", "Cupidon a uni $who")
            }
            StepIds.VOLEUR -> {
                val opt = a.option
                if (opt != null && opt.startsWith("take:")) {
                    val taken = opt.removePrefix("take:")
                    val thief = s.players.firstOrNull { it.role == RoleIds.VOLEUR && it.alive }
                    if (thief != null) {
                        thief.role = taken
                        val idx = s.extras.indexOf(taken)
                        if (idx >= 0) s.extras[idx] = RoleIds.VOLEUR else s.extras += RoleIds.VOLEUR
                    }
                    tr("Voleur took ${s.roleName(taken)}", "Le Voleur a pris ${s.roleName(taken)}")
                } else tr("Voleur kept his card", "Le Voleur a gardé sa carte")
            }
            StepIds.ALIEN_SIGN -> {
                s.alienSign = a.option
                tr("Alien's sign: ", "Signe de l'Alien : ") + (a.option?.ifBlank { null } ?: "—")
            }
            StepIds.LOVERS, StepIds.CHARMED, StepIds.IDENTIFY_REST -> tr("$title: done", "$title : fait")
            else -> {
                if (role != null) {
                    if (eff) t.forEach { s.p(it)?.badges?.put(role, s.night) } else noEffect()
                }
                "$title: $who"
            }
        }
    }

    private fun dawn(s: GameState): Pair<String, String?> {
        val rep = DawnReport(s.night)
        val deaths = mutableListOf<Death>()
        val v = s.p(s.nightVictim)
        if (v != null && v.alive) {
            when {
                s.nightCaught -> deaths += Death(v.pid, DeathCause.CAUGHT)
                s.nightInfect -> {
                    v.infected = true
                    s.infectionsUsed++
                    if (!s.rules.infectedKeepsPower) v.powerless = true
                    rep.notes += tr("${v.name} was infected and joins the wolves.", "${v.name} a été infecté et rejoint les loups.")
                }
                v.protectedNight == s.night -> rep.notes += tr("The Salva saved ${v.name}.", "Le Salva a sauvé ${v.name}.")
                s.nightSave -> rep.notes += tr("The Sorcière saved ${v.name}.", "La Sorcière a sauvé ${v.name}.")
                v.role == RoleIds.FLUTE && s.rules.fluteImmuneToWolves ->
                    rep.notes += tr("${v.name} (Joueur de flûte) can't die to the wolves.", "${v.name} (Joueur de flûte) ne meurt pas des loups.")
                v.role == RoleIds.ANCIEN && v.livesUsed < s.rules.ancienLives -> {
                    v.livesUsed++
                    rep.notes += tr("${v.name} (Ancien) used an extra life.", "${v.name} (Ancien) a utilisé une vie.")
                }
                else -> deaths += Death(v.pid, DeathCause.WOLVES)
            }
        }
        s.nightPoison?.let { deaths += Death(it, DeathCause.POISON) }
        if (s.night == 1) {
            s.players.filter { it.role == null }.forEach { it.role = RoleIds.VILLAGEOIS; it.startRole = RoleIds.VILLAGEOIS }
        }
        s.killChain(deaths, "N${s.night}", rep.deaths)

        s.holders(RoleIds.OURS).firstOrNull { it.alive && !it.powerless && !s.villageLost(RoleIds.OURS) }?.let { ours ->
            rep.oursGrowl = s.neighbors(ours.pid).any { s.looksLikeWolf(s.players[it]) }
        }
        s.nightGrandOurs?.let { target ->
            rep.grandOursTarget = target
            rep.grandOursGrowl = s.neighbors(target).any { s.looksLikeWolf(s.players[it]) }
        }
        rep.muted = s.players.filter { it.alive && it.mutedDay == s.night }.map { it.pid }
        rep.corbeau = s.players.filter { it.alive && it.corbeauDay == s.night }.map { it.pid }
        rep.juge = s.players.filter { it.alive && it.jugeDay == s.night }.map { it.pid }
        rep.starter = s.players.filter { it.alive && it.starterDay == s.night }.map { it.pid }
        (s.results[StepIds.BERGER] as? StepResult.Sheep)?.let { rep.sheep = it.outcomes }
        rep.seen = s.results[StepIds.VOYANTE] as? StepResult.Seen
        rep.blocked = s.players.filter { it.blockedNight == s.night }.map { it.pid }

        s.dawn = rep
        s.phase = Phase.DAY
        s.day = s.night
        s.votedToday = false
        s.dayEvents.clear()
        val text = if (rep.deaths.isEmpty()) tr("Dawn: nobody died", "Aube : personne n'est mort")
        else tr("Dawn: ", "Aube : ") + deathsText(s, rep.deaths)
        return text to null
    }
}
