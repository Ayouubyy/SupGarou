package com.supgarou.app.data

import com.supgarou.app.model.AppSettings
import com.supgarou.app.model.Backup
import com.supgarou.app.model.DefaultRoles
import com.supgarou.app.model.GameRecord
import com.supgarou.app.model.PlayerProfile
import com.supgarou.app.model.Preset
import com.supgarou.app.model.RoleDef
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Everything is stored as small JSON files in the app's private folder. */
class Store(private val dir: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
    private val gamesDir = File(dir, "games").also { it.mkdirs() }

    private fun <T> read(name: String, ser: KSerializer<T>): T? = try {
        val f = File(dir, name)
        if (f.exists()) json.decodeFromString(ser, f.readText()) else null
    } catch (e: Exception) {
        null
    }

    private fun <T> write(file: File, ser: KSerializer<T>, value: T) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(ser, value))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun loadSettings(): AppSettings = read("settings.json", AppSettings.serializer()) ?: AppSettings()
    fun saveSettings(s: AppSettings) = write(File(dir, "settings.json"), AppSettings.serializer(), s)

    fun loadRoles(): List<RoleDef> {
        val saved = read("roles.json", ListSerializer(RoleDef.serializer())) ?: return DefaultRoles.all
        val ids = saved.map { it.id }.toSet()
        return saved + DefaultRoles.all.filter { it.id !in ids }
    }
    fun saveRoles(r: List<RoleDef>) = write(File(dir, "roles.json"), ListSerializer(RoleDef.serializer()), r)

    fun loadProfiles(): List<PlayerProfile> = read("players.json", ListSerializer(PlayerProfile.serializer())) ?: emptyList()
    fun saveProfiles(p: List<PlayerProfile>) = write(File(dir, "players.json"), ListSerializer(PlayerProfile.serializer()), p)

    fun loadPresets(): List<Preset> = read("presets.json", ListSerializer(Preset.serializer())) ?: emptyList()
    fun savePresets(p: List<Preset>) = write(File(dir, "presets.json"), ListSerializer(Preset.serializer()), p)

    fun loadGames(): List<GameRecord> =
        gamesDir.listFiles { f -> f.name.endsWith(".json") }
            ?.mapNotNull { f ->
                try {
                    json.decodeFromString(GameRecord.serializer(), f.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.startedAt }
            ?: emptyList()

    fun saveGame(g: GameRecord) = write(File(gamesDir, "${g.id}.json"), GameRecord.serializer(), g)
    fun deleteGame(id: String) {
        File(gamesDir, "$id.json").delete()
    }

    fun exportBackup(b: Backup): String = json.encodeToString(Backup.serializer(), b)
    fun parseBackup(text: String): Backup = json.decodeFromString(Backup.serializer(), text)
}
