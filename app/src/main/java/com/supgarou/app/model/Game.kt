package com.supgarou.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerProfile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastPlayed: Long = 0,
)

@Serializable
data class Preset(val name: String, val counts: Map<String, Int>)

@Serializable
data class SetupPlayer(val profileId: String, val name: String)

@Serializable
data class GameSetup(
    /** Players in seat order; a player's index here is their id (pid) for the whole game. */
    val players: List<SetupPlayer>,
    val roleCounts: Map<String, Int>,
    val handout: Handout,
    /** Roles known at the start (pid -> role id). Empty with physical cards. */
    val assigned: Map<Int, String> = emptyMap(),
    /** Cards left in the middle (for the Voleur). */
    val extras: List<String> = emptyList(),
    val roles: List<RoleDef>,
    val rules: RuleSettings,
)

@Serializable
enum class Status { PROTECTED, BLOCKED, MUTED, CHARMED, INFECTED, JUGE, CORBEAU, POWERLESS }

/** One thing the narrator did. A game is its setup plus the ordered list of these. */
@Serializable
sealed class Action {
    @Serializable @SerialName("assign")
    data class Assign(val roleId: String, val pids: List<Int>) : Action()

    @Serializable @SerialName("absent")
    data class Absent(val roleId: String) : Action()

    @Serializable @SerialName("extras")
    data class Extras(val roles: List<String>) : Action()

    @Serializable @SerialName("step")
    data class Step(
        val stepId: String,
        val targets: List<Int> = emptyList(),
        val option: String? = null,
        val skipped: Boolean = false,
    ) : Action()

    @Serializable @SerialName("dawn")
    data object Dawn : Action()

    @Serializable @SerialName("vote")
    data class Vote(val pid: Int?) : Action()

    @Serializable @SerialName("shot")
    data class Shot(val hunter: Int, val target: Int?) : Action()

    @Serializable @SerialName("alien")
    data class AlienGuess(val alien: Int, val target: Int, val roleId: String) : Action()

    @Serializable @SerialName("nightfall")
    data object Nightfall : Action()

    @Serializable @SerialName("kill")
    data class Kill(val pid: Int) : Action()

    @Serializable @SerialName("revive")
    data class Revive(val pid: Int) : Action()

    @Serializable @SerialName("setRole")
    data class SetRole(val pid: Int, val roleId: String) : Action()

    @Serializable @SerialName("status")
    data class SetStatus(val pid: Int, val status: Status, val on: Boolean) : Action()

    @Serializable @SerialName("lovers")
    data class SetLovers(val a: Int, val b: Int?) : Action()

    @Serializable @SerialName("seat")
    data class MoveSeat(val pid: Int, val toIndex: Int) : Action()

    @Serializable @SerialName("rename")
    data class Rename(val pid: Int, val name: String) : Action()

    @Serializable @SerialName("rules")
    data class Rules(val rules: RuleSettings) : Action()
}

@Serializable
data class GameRecord(
    val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val setup: GameSetup,
    val actions: List<Action> = emptyList(),
    val redo: List<Action> = emptyList(),
    val winners: List<Int> = emptyList(),
    val winnerLabel: String? = null,
)

@Serializable
data class Backup(
    val players: List<PlayerProfile> = emptyList(),
    val roles: List<RoleDef> = emptyList(),
    val presets: List<Preset> = emptyList(),
    val games: List<GameRecord> = emptyList(),
)
