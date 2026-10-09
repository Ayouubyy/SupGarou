package com.supgarou.app.ui

/**
 * What the Sorcière is about to do tonight. Tapping the wolves' victim revives them,
 * tapping anyone else poisons them; [poisonMode] (the Poison chip) lets a tap on the victim poison them instead.
 */
data class WitchChoice(val save: Boolean = false, val poisonMode: Boolean = false, val poison: Int? = null) {

    fun toggleSave(bothAllowed: Boolean): WitchChoice =
        if (save) copy(save = false)
        else if (bothAllowed) copy(save = true)
        else copy(save = true, poisonMode = false, poison = null)

    /** The Poison chip: arms the next tap as a poison, or cancels the poison. */
    fun togglePoisonMode(bothAllowed: Boolean): WitchChoice =
        if (poisonMode || poison != null) copy(poisonMode = false, poison = null)
        else copy(poisonMode = true, save = save && bothAllowed)

    /** The new choice after tapping [pid], or null when the tap does nothing for the Sorcière. */
    fun tap(pid: Int, victim: Int?, lifeLeft: Boolean, deathLeft: Boolean, bothAllowed: Boolean): WitchChoice? = when {
        pid == victim && !poisonMode -> if (lifeLeft) toggleSave(bothAllowed) else null
        deathLeft -> {
            val target = if (poison == pid) null else pid
            copy(poisonMode = false, poison = target, save = save && (target == null || bothAllowed))
        }
        else -> null
    }
}
