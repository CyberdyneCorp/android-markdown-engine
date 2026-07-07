package com.cyberdyne.markdown.editor

/** Result of a pure text edit: the new [text] and the new selection `[selStart, selEnd)`. */
data class EditResult(val text: String, val selStart: Int, val selEnd: Int)

/**
 * Pure, Compose-free Markdown editing operations shared by [MarkdownEditorController].
 * All positions are UTF-16 char offsets (matching Compose selection). Fully
 * unit-testable.
 */
object EditOps {

    /** Toggles [delim] around the selection (wrap if absent, unwrap if present). */
    fun wrapSelection(text: String, s: Int, e: Int, delim: String): EditResult {
        if (s == e) {
            val nt = text.substring(0, s) + delim + delim + text.substring(s)
            val pos = s + delim.length
            return EditResult(nt, pos, pos)
        }
        val sel = text.substring(s, e)
        return if (sel.length >= 2 * delim.length && sel.startsWith(delim) && sel.endsWith(delim)) {
            val inner = sel.substring(delim.length, sel.length - delim.length)
            EditResult(text.substring(0, s) + inner + text.substring(e), s, s + inner.length)
        } else {
            EditResult(text.substring(0, s) + delim + sel + delim + text.substring(e), s + delim.length, e + delim.length)
        }
    }

    /** Toggles a line [prefix] on every line overlapping the selection. */
    fun toggleLinePrefix(text: String, s: Int, e: Int, prefix: String): EditResult {
        val lines = text.split("\n").toMutableList()
        val first = lineIndexAt(text, s)
        val last = lineIndexAt(text, e)
        val allHave = (first..last).all { lines[it].startsWith(prefix) }
        var delta = 0
        for (i in first..last) {
            if (allHave) {
                lines[i] = lines[i].removePrefix(prefix); delta -= prefix.length
            } else {
                lines[i] = prefix + lines[i]; delta += prefix.length
            }
        }
        val nt = lines.joinToString("\n")
        val ns = (s + if (allHave) -prefix.length else prefix.length).coerceIn(0, nt.length)
        return EditResult(nt, ns.coerceAtMost(nt.length), (e + delta).coerceIn(ns, nt.length))
    }

    /** Sets or toggles an ATX heading of [level] on the cursor's line. */
    fun setHeading(text: String, s: Int, e: Int, level: Int): EditResult {
        val (ls, le) = lineBounds(text, s)
        val line = text.substring(ls, le)
        val existing = Regex("^(#{1,6})\\s+").find(line)
        val stripped = line.replaceFirst(Regex("^#{1,6}\\s+"), "")
        val newLine = if (existing != null && existing.groupValues[1].length == level) stripped else "#".repeat(level) + " " + stripped
        val nt = text.substring(0, ls) + newLine + text.substring(le)
        val pos = ls + newLine.length
        return EditResult(nt, pos, pos)
    }

    private val LIST_LINE = Regex("^(\\s*)(- \\[[ xX]\\] |[-*+] |(\\d+)([.)]) )(.*)$")

    /** Enter behavior: continue the list, or remove an empty item's marker to end the list. */
    fun continueList(text: String, cursor: Int): EditResult? {
        val (ls, le) = lineBounds(text, cursor)
        val full = text.substring(ls, le)
        val m = LIST_LINE.find(full) ?: return null
        val indent = m.groupValues[1]
        val marker = m.groupValues[2]
        val content = m.groupValues[5]
        if (content.isBlank()) {
            val nt = text.substring(0, ls) + indent + text.substring(le)
            val pos = ls + indent.length
            return EditResult(nt, pos, pos)
        }
        val nextMarker = when {
            marker.startsWith("- [") -> "- [ ] "
            m.groupValues[3].isNotEmpty() -> "${m.groupValues[3].toInt() + 1}${m.groupValues[4]} "
            else -> marker
        }
        val insert = "\n" + indent + nextMarker
        val nt = text.substring(0, cursor) + insert + text.substring(cursor)
        val pos = cursor + insert.length
        return EditResult(nt, pos, pos)
    }

    /** Indents (or outdents) every line overlapping the selection by two spaces. */
    fun indentLines(text: String, s: Int, e: Int, outdent: Boolean): EditResult {
        val lines = text.split("\n").toMutableList()
        val first = lineIndexAt(text, s)
        val last = lineIndexAt(text, e)
        var delta = 0
        for (i in first..last) {
            if (outdent) {
                val removed = lines[i].takeWhile { it == ' ' }.take(2).length
                lines[i] = lines[i].substring(removed); delta -= removed
            } else {
                lines[i] = "  " + lines[i]; delta += 2
            }
        }
        val nt = lines.joinToString("\n")
        return EditResult(nt, s.coerceIn(0, nt.length), (e + delta).coerceIn(0, nt.length))
    }

    /** Inserts a Markdown link, wrapping the selection as the link text if present. */
    fun insertLink(text: String, s: Int, e: Int, url: String): EditResult {
        val label = text.substring(s, e)
        val snippet = "[$label]($url)"
        val nt = text.substring(0, s) + snippet + text.substring(e)
        val cursor = if (label.isEmpty()) s + 1 else s + snippet.length
        return EditResult(nt, cursor, cursor)
    }

    /** Toggles the checkbox state of a task item on the cursor's line, if any. */
    fun toggleTask(text: String, cursor: Int): EditResult? {
        val (ls, le) = lineBounds(text, cursor)
        val line = text.substring(ls, le)
        val m = Regex("^(\\s*[-*+] \\[)([ xX])(\\].*)$").find(line) ?: return null
        val toggled = if (m.groupValues[2].equals("x", ignoreCase = true)) " " else "x"
        val newLine = m.groupValues[1] + toggled + m.groupValues[3]
        val nt = text.substring(0, ls) + newLine + text.substring(le)
        return EditResult(nt, cursor, cursor)
    }

    // --- helpers ---

    private fun lineBounds(text: String, pos: Int): Pair<Int, Int> {
        val p = pos.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', (p - 1).coerceAtLeast(0)).let { if (it < 0 || p == 0) 0 else it + 1 }
        var end = text.indexOf('\n', p)
        if (end < 0) end = text.length
        return start to end
    }

    private fun lineIndexAt(text: String, pos: Int): Int =
        text.substring(0, pos.coerceIn(0, text.length)).count { it == '\n' }
}
