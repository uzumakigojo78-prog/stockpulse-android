package app.stockpulse.domain

private val SYMBOL = Regex("[A-Z0-9.\\-^=]{1,15}")

private fun cell(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

/** One row per favorite; price columns stay blank when no quote could be fetched. */
fun favoritesCsv(symbols: List<String>, quotes: Map<String, Quote>, exportedAt: String): String =
    (listOf("symbol,name,price,change_pct,exported_at") + symbols.map { s ->
        val q = quotes[s]
        listOf(s, q?.let { cell(it.name) } ?: "", q?.price ?: "", q?.changePct ?: "", exportedAt).joinToString(",")
    }).joinToString("\n")

/** Reads the first column of a backup file; symbols are never quoted, so no full CSV parser is needed. */
fun parseCsvSymbols(csv: String): List<String> =
    csv.lineSequence().drop(1).map { it.substringBefore(',').trim().uppercase() }
        .filter { SYMBOL.matches(it) }.distinct().toList()
