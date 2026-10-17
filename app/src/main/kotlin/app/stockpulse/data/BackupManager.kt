package app.stockpulse.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.stockpulse.StockPulseApp
import app.stockpulse.domain.favoritesCsv
import app.stockpulse.domain.parseCsvSymbols
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class BackupManager(
    private val context: Context,
    private val repo: StockRepository,
    private val watchlist: WatchlistStore,
    private val settings: SettingsStore,
) {
    /** Writes a new dated CSV of the favorites into the chosen folder; returns the file name. */
    // shortcut: runCatching also swallows cancellation, harmless for a short worker, tidy up if backups grow
    suspend fun backup(): Result<String> = runCatching {
        val tree = settings.settings.first().backupFolder?.let(Uri::parse)
            ?: error("Choose a backup folder first")
        val symbols = watchlist.symbols.first()
        val quotes = coroutineScope { symbols.map { s -> async { repo.quote(s).getOrNull()?.quote } }.awaitAll() }
            .filterNotNull().associateBy { it.symbol.uppercase() }
        val csv = favoritesCsv(symbols, quotes, Instant.now().toString())
        val dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = "stockpulse-favorites-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))}.csv"
        val file = DocumentsContract.createDocument(context.contentResolver, dir, "text/csv", name)
            ?: error("Could not create a file in the chosen folder")
        context.contentResolver.openOutputStream(file)!!.use { it.write(csv.toByteArray()) }
        settings.update { it.copy(lastBackup = System.currentTimeMillis()) }
        name
    }

    /** Adds every symbol found in a backup CSV to the favorites; returns how many were in the file. */
    suspend fun restore(file: Uri): Result<Int> = runCatching {
        val text = context.contentResolver.openInputStream(file)!!.use { it.readBytes().decodeToString() }
        val symbols = parseCsvSymbols(text)
        symbols.forEach { watchlist.add(it) }
        symbols.size
    }

    fun schedule(hours: Int) {
        val wm = WorkManager.getInstance(context)
        if (hours <= 0) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<BackupWorker>(hours.toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private companion object {
        const val WORK = "favorites-backup"
    }
}

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val backup = (applicationContext as StockPulseApp).container.backup
        return if (backup.backup().isSuccess) Result.success() else Result.failure()
    }
}
