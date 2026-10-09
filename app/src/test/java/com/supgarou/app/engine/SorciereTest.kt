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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SorciereTest {
    private fun game(handout: Handout, assigned: Map<Int, String>, actions: List<Action>): GameState {
        val players = (0 until 6).map { SetupPlayer("p$it", "P$it") }
        val counts = mapOf(RoleIds.LOUP to 1, RoleIds.SORCIERE to 1, RoleIds.VILLAGEOIS to 4)
        val setup = GameSetup(players, counts, handout, assigned, emptyList(), DefaultRoles.all, RuleSettings())
        return Engine.replay(GameRecord("g", 0, setup = setup, actions = actions))
    }

    private val manual = mapOf(0 to RoleIds.LOUP, 1 to RoleIds.SORCIERE) + (2..5).associateWith { RoleIds.VILLAGEOIS }

    @Test
    fun saveKeepsVictimAlive_manual() {
        val s = game(
            Handout.MANUAL, manual,
            listOf(Action.Step(StepIds.WOLVES, listOf(2)), Action.Step(StepIds.SORCIERE, emptyList(), "save"), Action.Dawn),
        )
        println(s.log.joinToString("\n") { "${it.label} ${it.text} ${it.flag ?: ""}" })
        assertTrue("victim should survive", s.players[2].alive)
    }

    @Test
    fun saveKeepsVictimAlive_physical() {
        val s = game(
            Handout.PHYSICAL, emptyMap(),
            listOf(
                Action.Assign(RoleIds.LOUP, listOf(0)),
                Action.Step(StepIds.WOLVES, listOf(2)),
                Action.Assign(RoleIds.SORCIERE, listOf(1)),
                Action.Step(StepIds.SORCIERE, emptyList(), "save"),
                Action.Dawn,
            ),
        )
        println(s.log.joinToString("\n") { "${it.label} ${it.text} ${it.flag ?: ""}" })
        assertTrue("victim should survive", s.players[2].alive)
    }

    @Test
    fun saveAndPoisonSameNight() {
        val s = game(
            Handout.MANUAL, manual,
            listOf(Action.Step(StepIds.WOLVES, listOf(2)), Action.Step(StepIds.SORCIERE, listOf(3), "save"), Action.Dawn),
        )
        assertTrue(s.players[2].alive)
        assertFalse(s.players[3].alive)
        assertEquals(DeathCause.POISON, s.players[3].cause)
    }
}
