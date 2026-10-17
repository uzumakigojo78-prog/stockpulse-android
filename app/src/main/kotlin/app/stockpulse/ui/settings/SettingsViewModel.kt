package app.stockpulse.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.stockpulse.data.AppFont
import app.stockpulse.data.AppSettings
import app.stockpulse.data.BackupManager
import app.stockpulse.data.SettingsStore
import app.stockpulse.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore,
    private val backup: BackupManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun messageShown() {
        _message.value = null
    }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { store.update(transform) }
    }

    fun setTheme(t: ThemeMode) = edit { it.copy(theme = t) }
    fun setDynamicColor(on: Boolean) = edit { it.copy(dynamicColor = on) }
    fun setFont(f: AppFont) = edit { it.copy(font = f) }
    fun setFolder(uri: Uri) = edit { it.copy(backupFolder = uri.toString()) }

    fun setBackupHours(hours: Int) {
        edit { it.copy(backupHours = hours) }
        backup.schedule(hours)
    }

    fun backupNow() {
        viewModelScope.launch {
            _message.value = backup.backup().fold({ "Saved $it" }, { it.message ?: "Backup failed" })
        }
    }

    fun restore(file: Uri) {
        viewModelScope.launch {
            _message.value = backup.restore(file).fold(
                { "Restored $it favorites from the file" },
                { "Could not read that file" },
            )
        }
    }
}
