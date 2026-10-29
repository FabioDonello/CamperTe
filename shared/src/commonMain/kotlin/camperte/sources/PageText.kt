package camperte.sources

private const val NEXT_DATA_TAG = "<script id=\"__NEXT_DATA__\""

private val whitespace = Regex("""\s+""")

internal fun nextDataJson(html: String, siteName: String): String {
    val start = html.indexOf(NEXT_DATA_TAG)
    check(start >= 0) { "$siteName page has no listings data" }
    val jsonStart = html.indexOf('>', start) + 1
    val jsonEnd = html.indexOf("</script>", jsonStart)
    check(jsonStart > 0 && jsonEnd > jsonStart) { "$siteName listings data is not closed" }
    return html.substring(jsonStart, jsonEnd)
}

internal fun String.normalizeSpaces(): String = whitespace.replace(this, " ").trim()
