package com.supgarou.app.data

import com.supgarou.app.engine.Engine
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.Team
import java.text.Normalizer
import kotlin.math.max

data class PlayerStats(
    val games: Int = 0,
    val wins: Int = 0,
    val winsByTeam: Map<Team, Int> = emptyMap(),
    val roles: Map<String, Int> = emptyMap(),
    val last: Long = 0,
) {
    val winRate: Int get() = if (games == 0) 0 else (wins * 100) / games
    val topRole: String? get() = roles.maxByOrNull { it.value }?.key
}

/** Stats per player profile, from finished games. */
fun computeStats(games: List<GameRecord>): Map<String, PlayerStats> {
    val acc = mutableMapOf<String, PlayerStats>()
    for (g in games) {
        if (g.endedAt == null) continue
        val s = try {
            Engine.replay(g)
        } catch (e: Exception) {
            continue
        }
        for (p in s.players) {
            val cur = acc[p.profileId] ?: PlayerStats()
            val won = p.pid in g.winners
            val team = s.teamOf(p)
            val role = p.startRole ?: p.role
            acc[p.profileId] = cur.copy(
                games = cur.games + 1,
                wins = cur.wins + if (won) 1 else 0,
                winsByTeam = if (won && team != null) cur.winsByTeam + (team to (cur.winsByTeam[team] ?: 0) + 1) else cur.winsByTeam,
                roles = if (role != null) cur.roles + (role to (cur.roles[role] ?: 0) + 1) else cur.roles,
                last = max(cur.last, g.startedAt),
            )
        }
    }
    return acc
}

/** Lowercase, trimmed, accents removed: "Inès " and "ines" match. */
fun normName(s: String): String =
    Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
