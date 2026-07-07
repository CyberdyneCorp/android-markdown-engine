package com.cyberdyne.markdown.editor

/**
 * Maps offsets between the original text and a transformed text produced by
 * deleting [hidden] character ranges (used to collapse Markdown markers to zero
 * width off the active line). Pure and unit-testable — correctness here is what
 * keeps the text field from crashing.
 *
 * @param hidden inclusive `[first, last]` ranges in ORIGINAL coordinates, sorted
 *   ascending and non-overlapping.
 */
class OffsetMap(private val hidden: List<IntRange>) {

    /** Original offset → transformed offset. Offsets inside a hidden range collapse to its start. */
    fun originalToTransformed(offset: Int): Int {
        var transformed = 0
        var cursor = 0
        for (r in hidden) {
            if (offset <= r.first) return transformed + (offset - cursor)
            transformed += r.first - cursor
            cursor = r.last + 1
            if (offset < cursor) return transformed // inside the hidden range
        }
        return transformed + (offset - cursor)
    }

    /** Transformed offset → original offset, skipping hidden ranges. */
    fun transformedToOriginal(offset: Int): Int {
        var remaining = offset
        var cursor = 0
        for (r in hidden) {
            val visibleBefore = r.first - cursor
            if (remaining < visibleBefore) return cursor + remaining
            remaining -= visibleBefore
            cursor = r.last + 1
        }
        return cursor + remaining
    }

    /** Builds the transformed string by removing the hidden ranges from [text]. */
    fun transform(text: String): String {
        if (hidden.isEmpty()) return text
        val sb = StringBuilder(text.length)
        var cursor = 0
        for (r in hidden) {
            if (r.first > cursor) sb.append(text, cursor, r.first)
            cursor = r.last + 1
        }
        if (cursor < text.length) sb.append(text, cursor, text.length)
        return sb.toString()
    }
}

/**
 * Computes the Markdown-marker character ranges to hide on every line EXCEPT the
 * one containing the cursor (reveal-on-active-line). Pure and unit-testable.
 */
object LiveMarkers {

    private val BOLD = Regex("\\*\\*[^*\\n]+\\*\\*")
    private val ITALIC = Regex("(?<!\\*)\\*(?!\\*)[^*\\n]+?\\*(?!\\*)")
    private val STRIKE = Regex("~~[^~\\n]+~~")
    private val CODE = Regex("`[^`\\n]+`")
    private val HEADING = Regex("^(#{1,6} )")

    /** Returns sorted, non-overlapping marker ranges to hide given the cursor position. */
    fun hiddenRanges(text: String, cursor: Int): List<IntRange> {
        val activeLine = lineBounds(text, cursor)
        val ranges = mutableListOf<IntRange>()
        var lineStart = 0
        for (line in text.split("\n")) {
            val lineEnd = lineStart + line.length
            val isActive = cursor in lineStart..lineEnd
            if (!isActive) {
                HEADING.find(line)?.let { ranges += lineStart until (lineStart + it.groupValues[1].length) }
                addMarkers(line, lineStart, BOLD, 2, ranges)
                addMarkers(line, lineStart, STRIKE, 2, ranges)
                addMarkers(line, lineStart, CODE, 1, ranges)
                addMarkers(line, lineStart, ITALIC, 1, ranges)
            }
            lineStart = lineEnd + 1
        }
        return normalize(ranges)
    }

    private fun addMarkers(line: String, base: Int, regex: Regex, markerLen: Int, out: MutableList<IntRange>) {
        for (m in regex.findAll(line)) {
            val s = base + m.range.first
            val e = base + m.range.last // inclusive
            out += s until (s + markerLen)
            out += (e - markerLen + 1)..e
        }
    }

    private fun normalize(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return ranges
        val sorted = ranges.filter { it.first <= it.last }.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        for (r in sorted) {
            val last = merged.lastOrNull()
            if (last != null && r.first <= last.last + 1) {
                merged[merged.size - 1] = last.first..maxOf(last.last, r.last)
            } else {
                merged += r
            }
        }
        return merged
    }

    private fun lineBounds(text: String, pos: Int): IntRange {
        val p = pos.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', (p - 1).coerceAtLeast(0)).let { if (it < 0 || p == 0) 0 else it + 1 }
        var end = text.indexOf('\n', p)
        if (end < 0) end = text.length
        return start..end
    }
}
