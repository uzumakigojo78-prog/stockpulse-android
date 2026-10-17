package app.stockpulse.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class AppFont(val label: String) { DEFAULT("Default"), SERIF("Serif"), MONOSPACE("Mono"), CURSIVE("Cursive") }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val font: AppFont = AppFont.DEFAULT,
    val backupHours: Int = 0,
    val backupFolder: String? = null,
    val lastBackup: Long = 0,
)

private val Context.settingsDataStore by preferencesDataStore("settings")
private val THEME = stringPreferencesKey("theme")
private val DYNAMIC = booleanPreferencesKey("dynamic")
private val FONT = stringPreferencesKey("font")
private val HOURS = intPreferencesKey("backup_hours")
private val FOLDER = stringPreferencesKey("backup_folder")
private val LAST = longPreferencesKey("last_backup")

private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
    this[key]?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

private fun Preferences.toSettings() = AppSettings(
    theme = enum(THEME, ThemeMode.SYSTEM),
    dynamicColor = this[DYNAMIC] ?: true,
    font = enum(FONT, AppFont.DEFAULT),
    backupHours = this[HOURS] ?: 0,
    backupFolder = this[FOLDER],
    lastBackup = this[LAST] ?: 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val s = transform(p.toSettings())
            p[THEME] = s.theme.name
            p[DYNAMIC] = s.dynamicColor
            p[FONT] = s.font.name
            p[HOURS] = s.backupHours
            p[LAST] = s.lastBackup
            s.backupFolder?.let { p[FOLDER] = it } ?: p.remove(FOLDER)
        }
    }
}
