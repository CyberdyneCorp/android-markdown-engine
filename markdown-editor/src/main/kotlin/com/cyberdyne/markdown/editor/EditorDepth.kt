package com.cyberdyne.markdown.editor

/** Detects an open `[[` wiki-link context at the cursor for completion. Pure. */
object WikiCompletion {

    /**
     * Returns the partial target typed after an unclosed `[[` on the cursor's
     * segment, or null if the cursor is not inside a wiki-link being typed.
     */
    fun contextAt(text: String, cursor: Int): String? {
        val c = cursor.coerceIn(0, text.length)
        val open = text.lastIndexOf("[[", (c - 1).coerceAtLeast(0))
        if (open < 0 || open + 2 > c) return null
        val between = text.substring(open + 2, c)
        if (between.contains("]]") || between.contains("\n") || between.contains("[[")) return null
        return between
    }

    /** Replaces the partial `[[prefix` at the cursor with `[[target]]`; returns (text, newCursor). */
    fun insert(text: String, cursor: Int, target: String): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val open = text.lastIndexOf("[[", (c - 1).coerceAtLeast(0))
        if (open < 0) return text to cursor
        val newText = text.substring(0, open + 2) + target + "]]" + text.substring(c)
        return newText to (open + 2 + target.length + 2)
    }
}

/**
 * Computes the character regions where system spellcheck/autocorrect is
 * suppressed: inline & fenced code, math, and wiki-links. Pure and unit-testable.
 */
object SpellcheckRegions {

    private val FENCE = Regex("(?s)(```.*?```|~~~.*?~~~)")
    private val BLOCK_MATH = Regex("(?s)\\$\\$.*?\\$\\$")
    private val INLINE_CODE = Regex("`[^`\\n]+`")
    private val INLINE_MATH = Regex("\\$[^$\\n]+\\$")
    private val WIKI = Regex("\\[\\[[^\\]\\n]*\\]\\]")

    fun suppressed(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        for (regex in listOf(FENCE, BLOCK_MATH, INLINE_CODE, INLINE_MATH, WIKI)) {
            regex.findAll(text).forEach { ranges += it.range.first..(it.range.last + 1) }
        }
        return merge(ranges)
    }

    fun isSuppressed(text: String, cursor: Int): Boolean =
        suppressed(text).any { cursor in it }

    private fun merge(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return ranges
        val sorted = ranges.sortedBy { it.first }
        val out = mutableListOf(sorted.first())
        for (r in sorted.drop(1)) {
            val last = out.last()
            if (r.first <= last.last) out[out.size - 1] = last.first..maxOf(last.last, r.last) else out += r
        }
        return out
    }
}
