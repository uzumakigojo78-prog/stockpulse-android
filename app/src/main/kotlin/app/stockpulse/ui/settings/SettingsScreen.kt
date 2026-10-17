package app.stockpulse.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.stockpulse.data.AppFont
import app.stockpulse.data.ThemeMode
import app.stockpulse.ui.components.AppLogo
import app.stockpulse.ui.theme.family

const val GITHUB_URL = "https://github.com/uzumakigojo78-prog/stockpulse-android"

private val INTERVALS = listOf("Off" to 0, "Daily" to 24, "Weekly" to 168)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val s by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setFolder(it)
        }
    }
    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restore)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                subtitle = { Text("Look, backup and about") },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section("Theme")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = s.theme == mode,
                        onClick = { viewModel.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Dynamic color") },
                    supportingContent = { Text("Use your wallpaper's colors") },
                    trailingContent = { Switch(s.dynamicColor, viewModel::setDynamicColor) },
                )
            }

            Section("Font")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFont.entries.forEach { f ->
                    FilterChip(
                        selected = s.font == f,
                        onClick = { viewModel.setFont(f) },
                        label = { Text(f.label, fontFamily = f.family()) },
                    )
                }
            }

            Section("Backup favorites (CSV)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                INTERVALS.forEach { (label, hours) ->
                    FilterChip(
                        selected = s.backupHours == hours,
                        onClick = { viewModel.setBackupHours(hours) },
                        label = { Text(label) },
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Backup folder") },
                supportingContent = { Text(s.backupFolder?.let(::folderName) ?: "Not chosen yet") },
                modifier = Modifier.clickable { pickFolder.launch(null) },
            )
            Text(
                if (s.lastBackup == 0L) "No backup yet" else "Last backup ${
                    DateUtils.getRelativeTimeSpanString(s.lastBackup)
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::backupNow) { Text("Back up now") }
                OutlinedButton(onClick = { pickBackup.launch(arrayOf("*/*")) }) { Text("Restore from CSV") }
            }

            Section("About")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(56.dp)
                Column(Modifier.padding(start = 16.dp)) {
                    Text("StockPulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Version ${versionName(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val uriHandler = LocalUriHandler.current
            ListItem(
                headlineContent = { Text("StockPulse on GitHub") },
                supportingContent = { Text("Source code, releases and issues") },
                modifier = Modifier.clickable { uriHandler.openUri(GITHUB_URL) },
            )
            Text(
                "Forecasts are statistical estimates, not financial advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String) =
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

private fun folderName(uri: String) = Uri.parse(uri).lastPathSegment?.substringAfterLast(':')?.ifBlank { null } ?: uri

private fun versionName(context: Context) =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
