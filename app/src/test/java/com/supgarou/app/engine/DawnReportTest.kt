package com.supgarou.app.engine

import com.supgarou.app.model.Action
import com.supgarou.app.model.DefaultRoles
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.GameSetup
import com.supgarou.app.model.Handout
import com.supgarou.app.model.RoleIds
import com.supgarou.app.model.RuleSettings
import com.supgarou.app.model.SetupPlayer
import com.supgarou.app.model.StepIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DawnReportTest {
    private val roles = listOf(
        RoleIds.LOUP, RoleIds.LOUP_ROUGE, RoleIds.VOYANTE, RoleIds.SALVA,
        RoleIds.VILLAGEOIS, RoleIds.VILLAGEOIS, RoleIds.VILLAGEOIS,
    )

    private fun game(actions: List<Action>, rules: RuleSettings = RuleSettings(loupRougeFirstNight = 1)): GameState {
        val players = roles.indices.map { SetupPlayer("p$it", "P$it") }
        val counts = roles.groupingBy { it }.eachCount()
        val assigned = roles.withIndex().associate { it.index to it.value }
        val setup = GameSetup(players, counts, Handout.MANUAL, assigned, emptyList(), DefaultRoles.all, rules)
        return Engine.replay(GameRecord("g", 0, setup = setup, actions = actions))
    }

    @Test
    fun voyanteTargetIsMarkedAndReportedAtDawn() {
        val s = game(listOf(Action.Step(StepIds.VOYANTE, listOf(0)), Action.Dawn))
        assertEquals(1, s.players[0].seenNight)
        val seen = s.dawn!!.seen!!
        assertEquals(0, seen.pid)
        assertEquals(RoleIds.LOUP, seen.shownRole)
    }

    @Test
    fun blockedPlayersAreReportedAtDawn() {
        val s = game(
            listOf(
                Action.Step(StepIds.LOUP_ROUGE, listOf(3)),
                Action.Step(StepIds.SALVA, listOf(4)),
                Action.Step(StepIds.WOLVES, listOf(4)),
                Action.Dawn,
            ),
        )
        assertEquals(listOf(3), s.dawn!!.blocked)
        // The Salva was blocked, so his protection did nothing.
        assertTrue(!s.players[4].alive)
    }
}
