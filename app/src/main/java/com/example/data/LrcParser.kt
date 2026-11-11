package com.example.data

/**
 * Parses LRC formatted lyrics ("[01:23.45] some line") into timed [LyricLine]s.
 * Plain lyrics (without timestamps) return an empty list, so callers can fall back to plain text.
 */
object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]""")

    fun hasTimestamps(text: String?): Boolean {
        return !text.isNullOrBlank() && timeTag.containsMatchIn(text)
    }

    fun parse(text: String?): List<LyricLine> {
        if (text.isNullOrBlank() || !timeTag.containsMatchIn(text)) return emptyList()
        val result = ArrayList<LyricLine>()
        for (rawLine in text.lines()) {
            val matches = timeTag.findAll(rawLine).toList()
            if (matches.isEmpty()) continue
            val content = timeTag.replace(rawLine, "").trim()
            if (content.isEmpty()) continue
            for (m in matches) {
                val minutes = m.groupValues[1].toIntOrNull() ?: continue
                val seconds = m.groupValues[2].toIntOrNull() ?: continue
                result.add(LyricLine(timeSec = minutes * 60 + seconds, text = content))
            }
        }
        return result.sortedBy { it.timeSec }
    }

    fun stripTimestamps(text: String): String {
        return timeTag.replace(text, "")
    }
}
