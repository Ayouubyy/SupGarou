package com.supgarou.app.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** App language. Role names are always French; the rest of the interface follows this flag. */
object Lang {
    var french by mutableStateOf(false)
}

/** Picks the English or French text for the current language. */
fun tr(en: String, fr: String): String = if (Lang.french) fr else en
