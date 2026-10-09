package com.supgarou.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class Language { ENGLISH_FR_ROLES, FULL_FRENCH }

@Serializable
enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Serializable
enum class RevealDead { FULL, TEAM, HIDDEN }

@Serializable
enum class VoyanteSees { EXACT, TEAM }

@Serializable
enum class Handout { PHYSICAL, APP_DEALS, MANUAL }

object StepIds {
    const val VOLEUR = "voleur"
    const val CUPIDON = "cupidon"
    const val LOVERS = "lovers"
    const val ALIEN_SIGN = "alien_sign"
    const val LOUP_ROUGE = "loup_rouge"
    const val SALVA = "salva"
    const val VOYANTE = "voyante"
    const val RENARD = "renard"
    const val GRAND_OURS = "grand_ours"
    const val BERGER = "berger"
    const val WOLVES = "wolves"
    const val LOUP_NOIR = "loup_noir"
    const val SORCIERE = "sorciere"
    const val FLUTE = "flute"
    const val CHARMED = "charmed"
    const val CORBEAU = "corbeau"
    const val JUGE = "juge"
    const val ANCIEN = "ancien"
    const val IDENTIFY_REST = "identify_rest"
    const val CUSTOM_PREFIX = "custom:"

    val DEFAULT_ORDER = listOf(
        VOLEUR, CUPIDON, LOVERS, ALIEN_SIGN,
        LOUP_ROUGE, SALVA, VOYANTE, RENARD, GRAND_OURS, BERGER, WOLVES, LOUP_NOIR,
        SORCIERE, FLUTE, CHARMED, CORBEAU, JUGE, ANCIEN,
    )
}

/** Game rules. Each game keeps its own copy, editable mid-game. */
@Serializable
data class RuleSettings(
    val wolvesKillNight1: Boolean = true,
    val revealDead: RevealDead = RevealDead.FULL,
    val salvaSelf: Boolean = true,
    val salvaSameTwice: Boolean = false,
    val voyanteSees: VoyanteSees = VoyanteSees.EXACT,
    val sorciereSelfSave: Boolean = true,
    val sorciereBothSameNight: Boolean = true,
    val ancienLives: Int = 0,
    val ancienPenalty: Boolean = true,
    val infectedCountAsWolves: Boolean = true,
    val infectedKeepsPower: Boolean = false,
    val renardLosesOnWolf: Boolean = true,
    val bergerSheep: Int = 3,
    /** 0 = as many as he has left. */
    val bergerPerNight: Int = 0,
    val cupidonSelf: Boolean = true,
    val chasseurShootsIfPoisoned: Boolean = true,
    val voleurExtraCards: Int = 2,
    val voleurMustTakeWolf: Boolean = true,
    val petiteFilleCatchable: Boolean = true,
    val infections: Int = 1,
    val infectFirstNight: Int = 2,
    val loupRougeFirstNight: Int = 2,
    val loupRougeCanBlockWolves: Boolean = false,
    val blockedStillCalled: Boolean = true,
    val loupNoirFirstNight: Int = 2,
    val mutedCanVote: Boolean = false,
    val loupBlancHidden: Boolean = true,
    val fluteCharms: Int = 2,
    val fluteImmuneToWolves: Boolean = true,
    val alienExact: Boolean = true,
    val corbeauFirstNight: Int = 2,
    val corbeauVotes: Int = 2,
    val jugeFirstNight: Int = 2,
    val jugeUntilUsed: Boolean = false,
    val fakeTurns: Boolean = false,
    val nightOrder: List<String> = StepIds.DEFAULT_ORDER,
    val disabledSteps: Set<String> = emptySet(),
)

@Serializable
data class AppSettings(
    val language: Language = Language.ENGLISH_FR_ROLES,
    val theme: ThemeMode = ThemeMode.DARK,
    val keepScreenOn: Boolean = true,
    val haptics: Boolean = true,
    val textScale: Float = 1f,
    val showRoleIcons: Boolean = true,
    val confirmDestructive: Boolean = true,
    val showScript: Boolean = true,
    val readAloud: Boolean = false,
    val autoAdvance: Boolean = true,
    val warnings: Boolean = true,
    val dayTimerMinutes: Int = 0,
    val nightTimerSeconds: Int = 0,
    val vibrateOnTimer: Boolean = true,
    val handout: Handout = Handout.PHYSICAL,
    val suggestMix: Boolean = true,
    val startFromLast: Boolean = true,
    val suggestNames: Boolean = true,
    val rules: RuleSettings = RuleSettings(),
    val currentGameId: String? = null,
)
