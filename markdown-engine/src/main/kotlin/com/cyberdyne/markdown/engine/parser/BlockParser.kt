package com.cyberdyne.markdown.engine.parser

import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.config.MarkdownExtension
import com.cyberdyne.markdown.engine.model.BlockNode
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.model.ListItem
import com.cyberdyne.markdown.engine.model.SourceRange
import com.cyberdyne.markdown.engine.model.TableAlignment
import com.cyberdyne.markdown.engine.model.TableCell
import com.cyberdyne.markdown.engine.model.Utf8Offsets

/** A source line: its [text] (without the trailing newline) and the char index [start] of its first char. */
internal data class Line(val text: String, val start: Int) {
    val end: Int get() = start + text.length
    val blank: Boolean get() = text.isBlank()
}

internal class BlockParser(
    private val config: MarkdownConfiguration,
    private val offsets: Utf8Offsets,
    private val linkRefs: Map<String, LinkDefinition>,
) {
    private val inline = InlineParser(config, offsets, linkRefs)

    fun parseBlocks(lines: List<Line>): List<BlockNode> {
        val blocks = mutableListOf<BlockNode>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].blank) { i++; continue }
            val (block, next) = parseBlock(lines, i)
            if (block != null) blocks += block
            i = if (next > i) next else i + 1 // guarantee progress
        }
        return blocks
    }

    private fun parseBlock(lines: List<Line>, i: Int): Pair<BlockNode?, Int> {
        val line = lines[i]
        val text = line.text
        return when {
            indentOf(text) >= 4 -> parseIndentedCode(lines, i)
            THEMATIC.matches(text) -> BlockNode.ThematicBreak(rangeOf(line, line)) to i + 1
            ATX.matches(text) -> parseAtxHeading(line, i)
            FENCE.matches(text) -> parseFence(lines, i)
            config.isEnabled(MarkdownExtension.MATH) && BLOCK_MATH_OPEN.matches(text) -> parseBlockMath(lines, i)
            config.isEnabled(MarkdownExtension.FOOTNOTES) && FOOTNOTE_DEF.matches(text) -> parseFootnoteDef(lines, i)
            text.trimStart().startsWith(">") -> parseBlockQuote(lines, i)
            listMarker(text) != null -> parseList(lines, i)
            isTableStart(lines, i) -> parseTable(lines, i)
            isHtmlStart(text) -> parseHtmlBlock(lines, i)
            else -> parseParagraphOrSetext(lines, i)
        }
    }

    // --- Headings ---

    private fun parseAtxHeading(line: Line, i: Int): Pair<BlockNode, Int> {
        val m = ATX.find(line.text)!!
        val level = m.groupValues[1].length
        val content = m.groups[2]?.value ?: ""
        val contentStart = line.start + (m.groups[2]?.range?.first ?: line.text.length)
        val nodes = inline.parse(content, contentStart)
        return BlockNode.Heading(level, nodes, rangeOf(line, line)) to i + 1
    }

    // --- Code ---

    private fun parseFence(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val open = FENCE.find(lines[i].text)!!
        val fenceChar = open.groupValues[1][0]
        val fenceLen = open.groupValues[1].length
        val fenceIndent = indentOf(lines[i].text)
        // CommonMark: the info string's first word is the language.
        val info = open.groupValues[2].trim().substringBefore(' ').substringBefore('\t')
        val body = StringBuilder()
        var j = i + 1
        var closed = false
        while (j < lines.size) {
            val t = lines[j].text
            val cm = CLOSE_FENCE.find(t)
            if (cm != null && cm.groupValues[1].isNotEmpty() && cm.groupValues[1][0] == fenceChar && cm.groupValues[1].length >= fenceLen) {
                closed = true; j++; break
            }
            // CommonMark: strip up to the opening fence's indentation from each content line.
            val strip = minOf(fenceIndent, t.length - t.trimStart().length)
            body.append(t.substring(strip)).append('\n')
            j++
        }
        val endLine = lines[(if (closed) j - 1 else j - 1).coerceIn(i, lines.size - 1)]
        val content = body.toString().removeSuffix("\n")
        val range = rangeOf(lines[i], endLine)
        val block: BlockNode =
            if (config.isEnabled(MarkdownExtension.MERMAID) && info.equals("mermaid", ignoreCase = true)) {
                BlockNode.Mermaid(content, range)
            } else {
                BlockNode.CodeBlock(info.ifEmpty { null }, content, range)
            }
        return block to j
    }

    private fun parseIndentedCode(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val body = StringBuilder()
        var j = i
        var lastNonBlank = i
        while (j < lines.size && (lines[j].blank || indentOf(lines[j].text) >= 4)) {
            body.append(if (lines[j].blank) "" else lines[j].text.substring(4)).append('\n')
            if (!lines[j].blank) lastNonBlank = j
            j++
        }
        val content = body.toString().trimEnd('\n')
        return BlockNode.CodeBlock(null, content, rangeOf(lines[i], lines[lastNonBlank])) to (lastNonBlank + 1)
    }

    private fun parseBlockMath(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val firstRest = lines[i].text.trimStart().removePrefix("$$")
        // Single-line `$$ ... $$`
        if (firstRest.trimEnd().endsWith("$$") && firstRest.trim() != "") {
            val body = firstRest.trimEnd().removeSuffix("$$").trim()
            return BlockNode.BlockMath(body, rangeOf(lines[i], lines[i])) to i + 1
        }
        val body = StringBuilder()
        if (firstRest.isNotBlank()) body.append(firstRest.trim()).append('\n')
        var j = i + 1
        var closed = false
        while (j < lines.size) {
            if (lines[j].text.trimEnd().endsWith("$$")) {
                val pre = lines[j].text.trimEnd().removeSuffix("$$")
                if (pre.isNotBlank()) body.append(pre).append('\n')
                closed = true; j++; break
            }
            body.append(lines[j].text).append('\n')
            j++
        }
        val end = if (closed) j - 1 else lines.size - 1
        return BlockNode.BlockMath(body.toString().trim(), rangeOf(lines[i], lines[end])) to j
    }

    // --- Block quote / callout ---

    private fun parseBlockQuote(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val inner = mutableListOf<Line>()
        var j = i
        while (j < lines.size) {
            val t = lines[j].text
            if (t.trimStart().startsWith(">")) {
                val idx = t.indexOf('>')
                val afterMarker = idx + 1 + (if (idx + 1 < t.length && t[idx + 1] == ' ') 1 else 0)
                inner += Line(t.substring(afterMarker), lines[j].start + afterMarker)
                j++
            } else if (!lines[j].blank && listMarker(t) == null && !THEMATIC.matches(t)) {
                inner += lines[j] // lazy continuation
                j++
            } else break
        }
        val range = rangeOf(lines[i], lines[j - 1])

        val firstText = inner.firstOrNull()?.text?.trimStart().orEmpty()
        val callout = CALLOUT.find(firstText)
        if (config.isEnabled(MarkdownExtension.CALLOUTS) && callout != null) {
            val kindLabel = callout.groupValues[1].lowercase()
            val titleText = callout.groupValues[2].trim()
            val titleBase = inner.first().start + (inner.first().text.length - inner.first().text.trimStart().length) + callout.range.first + callout.groupValues[1].length + 3
            val title = if (titleText.isEmpty()) emptyList() else inline.parse(titleText, titleBase)
            val rest = inner.drop(1)
            return BlockNode.Callout(kindLabel, title, parseBlocks(rest), range) to j
        }
        return BlockNode.BlockQuote(parseBlocks(inner), range) to j
    }

    // --- Lists ---

    private data class Marker(val ordered: Boolean, val start: Int, val markerText: String, val contentCol: Int)

    private fun parseList(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val first = listMarker(lines[i].text)!!
        val items = mutableListOf<ListItem>()
        var j = i
        var sawBlankBetween = false
        var lastLine = lines[i]

        while (j < lines.size) {
            // Skip blank lines separating items; blanks make the list loose.
            var skippedBlank = false
            while (j < lines.size && lines[j].blank) { j++; skippedBlank = true }
            if (j >= lines.size) break

            val marker = listMarker(lines[j].text)
            if (marker == null || marker.ordered != first.ordered) break
            if (skippedBlank) sawBlankBetween = true

            val itemLines = mutableListOf<Line>()
            val markerLine = lines[j]
            val contentCol = marker.contentCol
            // First line content after the marker.
            val firstCol = contentCol.coerceAtMost(markerLine.text.length)
            itemLines += Line(markerLine.text.substring(firstCol), markerLine.start + firstCol)
            var k = j + 1
            while (k < lines.size) {
                val t = lines[k].text
                if (lines[k].blank) {
                    // peek: blank then either continued indented content or next item / end
                    if (k + 1 < lines.size && !lines[k + 1].blank &&
                        indentOf(lines[k + 1].text) >= contentCol && listMarker(lines[k + 1].text) == null
                    ) {
                        itemLines += Line("", lines[k].start)
                        sawBlankBetween = true
                        k++
                    } else {
                        if (k + 1 < lines.size && listMarker(lines[k + 1].text)?.ordered == first.ordered) sawBlankBetween = true
                        break
                    }
                } else if (indentOf(t) >= contentCol) {
                    itemLines += Line(t.substring(contentCol.coerceAtMost(t.length)), lines[k].start + contentCol.coerceAtMost(t.length))
                    k++
                } else if (listMarker(t) != null) {
                    break
                } else {
                    itemLines += lines[k] // lazy continuation of the item's paragraph
                    k++
                }
            }

            val (checked, contentLines) = extractTask(itemLines)
            val children = parseBlocks(contentLines)
            val itemRange = rangeOf(markerLine, lines[(k - 1).coerceAtLeast(j)])
            items += ListItem(children, checked, itemRange)
            lastLine = lines[(k - 1).coerceAtLeast(j)]
            j = k
        }

        val block = BlockNode.ListBlock(
            ordered = first.ordered,
            start = first.start,
            tight = !sawBlankBetween,
            items = items,
            range = rangeOf(lines[i], lastLine),
        )
        return block to j
    }

    private fun extractTask(itemLines: List<Line>): Pair<Boolean?, List<Line>> {
        if (itemLines.isEmpty()) return null to itemLines
        val firstLine = itemLines.first()
        val m = TASK.find(firstLine.text) ?: return null to itemLines
        val checked = m.groupValues[1].equals("x", ignoreCase = true)
        val consumed = m.value.length
        val stripped = Line(firstLine.text.substring(consumed), firstLine.start + consumed)
        return checked to (listOf(stripped) + itemLines.drop(1))
    }

    // --- Tables ---

    private fun isTableStart(lines: List<Line>, i: Int): Boolean {
        if (!lines[i].text.contains('|')) return false
        val next = lines.getOrNull(i + 1) ?: return false
        return TABLE_DELIM.matches(next.text.trim())
    }

    private fun parseTable(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val header = splitRow(lines[i])
        val alignments = splitCells(lines[i + 1].text).map { cell ->
            val c = cell.trim()
            val left = c.startsWith(":")
            val right = c.endsWith(":")
            when {
                left && right -> TableAlignment.CENTER
                right -> TableAlignment.RIGHT
                left -> TableAlignment.LEFT
                else -> TableAlignment.NONE
            }
        }
        val colCount = header.size
        val rows = mutableListOf<List<TableCell>>()
        var j = i + 2
        while (j < lines.size && lines[j].text.contains('|') && !lines[j].blank) {
            // GFM: pad short rows and truncate long rows to the header column count.
            val cells = splitRow(lines[j])
            rows += (0 until colCount).map { cells.getOrElse(it) { emptyList() } }
            j++
        }
        return BlockNode.Table(alignments, header, rows, rangeOf(lines[i], lines[j - 1])) to j
    }

    private fun splitRow(line: Line): List<TableCell> {
        val cells = mutableListOf<TableCell>()
        var searchFrom = 0
        for (p in splitCells(line.text)) {
            val trimmed = p.trim()
            val idx = if (trimmed.isEmpty()) searchFrom
            else line.text.indexOf(trimmed, searchFrom).let { if (it < 0) searchFrom else it }
            cells += inline.parse(trimmed, line.start + idx)
            searchFrom = (idx + trimmed.length).coerceAtMost(line.text.length)
        }
        return cells
    }

    private fun splitCells(row: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var content = row.trim().removePrefix("|").removeSuffix("|")
        while (i < content.length) {
            val c = content[i]
            when {
                c == '\\' && i + 1 < content.length -> { sb.append(c).append(content[i + 1]); i += 2 }
                c == '|' -> { result += sb.toString(); sb.clear(); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        result += sb.toString()
        return result
    }

    // --- HTML block ---

    private fun isHtmlStart(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith("<") && (t.startsWith("</") || (t.length > 1 && t[1].isLetter()))
    }

    private fun parseHtmlBlock(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val body = StringBuilder()
        var j = i
        while (j < lines.size && !lines[j].blank) { body.append(lines[j].text).append('\n'); j++ }
        return BlockNode.HtmlBlock(body.toString().trimEnd('\n'), rangeOf(lines[i], lines[j - 1])) to j
    }

    // --- Footnote definition ---

    private fun parseFootnoteDef(lines: List<Line>, i: Int): Pair<BlockNode, Int> {
        val m = FOOTNOTE_DEF.find(lines[i].text)!!
        val id = m.groupValues[1]
        val firstRest = m.groupValues[2]
        val contentLines = mutableListOf<Line>()
        val firstStart = lines[i].start + (m.groups[2]?.range?.first ?: lines[i].text.length)
        contentLines += Line(firstRest, firstStart)
        var j = i + 1
        while (j < lines.size && (lines[j].blank || indentOf(lines[j].text) >= 4)) {
            if (lines[j].blank) break
            contentLines += Line(lines[j].text.substring(4), lines[j].start + 4)
            j++
        }
        return BlockNode.FootnoteDefinition(id, parseBlocks(contentLines), rangeOf(lines[i], lines[j - 1])) to j
    }

    // --- Paragraph / setext ---

    private fun parseParagraphOrSetext(lines: List<Line>, i: Int): Pair<BlockNode?, Int> {
        val para = mutableListOf<Line>()
        var j = i
        while (j < lines.size && !lines[j].blank) {
            val t = lines[j].text
            // A setext underline following a paragraph line takes precedence over a thematic break.
            if (j > i && para.isNotEmpty() && SETEXT.matches(t)) {
                val level = if (t.trim().startsWith("=")) 1 else 2
                val content = para.joinToString("\n") { it.text }
                val node = BlockNode.Heading(level, inline.parse(content, para.first().start), rangeOf(para.first(), lines[j]))
                return node to (j + 1)
            }
            if (j > i && (THEMATIC.matches(t) || ATX.matches(t) || t.trimStart().startsWith(">") ||
                    listMarker(t) != null || FENCE.matches(t))
            ) break
            para += lines[j]
            j++
        }
        if (para.isEmpty()) return null to (i + 1)
        val content = para.joinToString("\n") { it.text }
        val nodes = inline.parse(content, para.first().start)
        return BlockNode.Paragraph(nodes, rangeOf(para.first(), para.last())) to j
    }

    // --- helpers ---

    private fun rangeOf(from: Line, to: Line): SourceRange = offsets.range(from.start, to.end)

    private fun indentOf(text: String): Int {
        var n = 0
        for (c in text) {
            when (c) {
                ' ' -> n += 1
                '\t' -> n += 4
                else -> return n
            }
        }
        return n
    }

    private fun listMarker(text: String): Marker? {
        BULLET.find(text)?.let { m ->
            val markerText = m.groupValues[2]
            val spaces = m.groupValues[3]
            val contentCol = m.groupValues[1].length + markerText.length + spaces.length.coerceAtLeast(1)
            return Marker(false, 1, markerText, contentCol)
        }
        ORDERED.find(text)?.let { m ->
            val num = m.groupValues[2].toIntOrNull() ?: 1
            val markerText = m.groupValues[2] + m.groupValues[3]
            val spaces = m.groupValues[4]
            val contentCol = m.groupValues[1].length + markerText.length + spaces.length.coerceAtLeast(1)
            return Marker(true, num, markerText, contentCol)
        }
        return null
    }

    private companion object {
        val THEMATIC = Regex("^ {0,3}([-*_])[ \\t]*(?:\\1[ \\t]*){2,}$")
        val ATX = Regex("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?[ \\t]*#*[ \\t]*$")
        val FENCE = Regex("^ {0,3}(`{3,}|~{3,})[ \\t]*([^`]*)$")
        val CLOSE_FENCE = Regex("^ {0,3}(`{3,}|~{3,})[ \\t]*$")
        val BLOCK_MATH_OPEN = Regex("^ {0,3}\\$\\$.*$")
        val BULLET = Regex("^( {0,3})([-+*])([ \\t]+|$)(.*)$")
        val ORDERED = Regex("^( {0,3})(\\d{1,9})([.)])([ \\t]+|$)(.*)$")
        val TASK = Regex("^\\[([ xX])\\][ \\t]+")
        val SETEXT = Regex("^ {0,3}(=+|-+)[ \\t]*$")
        val TABLE_DELIM = Regex("^\\|?[ \\t]*:?-+:?[ \\t]*(\\|[ \\t]*:?-+:?[ \\t]*)*\\|?$")
        val FOOTNOTE_DEF = Regex("^ {0,3}\\[\\^([^\\]\\s]+)\\]:[ \\t]*(.*)$")
        val CALLOUT = Regex("^\\[!([A-Za-z]+)\\][ \\t]*(.*)$")
    }
}
