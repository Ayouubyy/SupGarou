package com.supgarou.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WitchChoiceTest {
    private val victim = 2

    private fun WitchChoice.t(pid: Int, life: Boolean = true, death: Boolean = true, both: Boolean = true) =
        tap(pid, victim, life, death, both)

    @Test
    fun tappingTheVictimRevivesThem() {
        val c = WitchChoice().t(victim)!!
        assertTrue(c.save)
        assertNull(c.poison)
    }

    @Test
    fun tappingTheVictimAgainCancelsTheRevive() {
        val c = WitchChoice().t(victim)!!.t(victim)!!
        assertFalse(c.save)
    }

    @Test
    fun tappingSomeoneElsePoisonsThem() {
        val c = WitchChoice().t(4)!!
        assertEquals(4, c.poison)
        assertFalse(c.save)
    }

    @Test
    fun reviveAndPoisonInTheSameNight() {
        val c = WitchChoice().t(victim)!!.t(4)!!
        assertTrue(c.save)
        assertEquals(4, c.poison)
        val d = WitchChoice().t(4)!!.t(victim)!!
        assertTrue(d.save)
        assertEquals(4, d.poison)
    }

    @Test
    fun usedLifePotionCannotRevive() {
        assertNull(WitchChoice().t(victim, life = false))
    }

    @Test
    fun usedDeathPotionCannotPoison() {
        assertNull(WitchChoice().t(4, death = false))
    }

    @Test
    fun poisonModeLetsHerPoisonTheVictim() {
        val c = WitchChoice(poisonMode = true).t(victim)!!
        assertEquals(victim, c.poison)
        assertFalse(c.save)
    }

    @Test
    fun withoutBothRuleThePoisonReplacesTheRevive() {
        val c = WitchChoice().t(victim, both = false)!!.t(4, both = false)!!
        assertFalse(c.save)
        assertEquals(4, c.poison)
    }
}
