package com.cyberdyne.markdown.engine.parser

import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.config.MarkdownExtension
import com.cyberdyne.markdown.engine.model.MarkdownDocument
import com.cyberdyne.markdown.engine.model.Utf8Offsets

/**
 * Parses Markdown source into an immutable [MarkdownDocument]. Dependency-free and
 * deterministic; safe to run off the main thread (see [Markdown.parse]).
 *
 * Mirrors the iOS parser: CommonMark + GFM plus gated extensions (math, mermaid,
 * footnotes, frontmatter, callouts, wiki-links).
 */
class MarkdownParser(private val config: MarkdownConfiguration = MarkdownConfiguration.Default) {

    fun parse(source: String): MarkdownDocument {
        return try {
            parseInternal(source)
        } catch (t: Throwable) {
            // Resilience contract: never crash on arbitrary input. Fall back to a
            // single paragraph of literal text.
            fallback(source)
        }
    }

    private fun parseInternal(source: String): MarkdownDocument {
        val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
        val offsets = Utf8Offsets(normalized)
        val allLines = toLines(normalized)

        var startIndex = 0
        val metadata = LinkedHashMap<String, String>()
        if (config.isEnabled(MarkdownExtension.FRONTMATTER)) {
            startIndex = extractFrontmatter(allLines, metadata)
        }

        val working = allLines.toMutableList()
        val linkRefs = collectLinkDefinitions(working, startIndex)

        val body = working.subList(startIndex, working.size)
        val blocks = BlockParser(config, offsets, linkRefs).parseBlocks(body)
        return MarkdownDocument(blocks, metadata)
    }

    private fun fallback(source: String): MarkdownDocument {
        val offsets = Utf8Offsets(source)
        val inline = InlineParser(config, offsets, emptyMap())
        val nodes = inline.parse(source, 0)
        return MarkdownDocument(
            listOf(
                com.cyberdyne.markdown.engine.model.BlockNode.Paragraph(
                    nodes,
                    offsets.range(0, source.length),
                ),
            ),
        )
    }

    private fun toLines(source: String): List<Line> {
        val lines = mutableListOf<Line>()
        var start = 0
        var i = 0
        while (i <= source.length) {
            if (i == source.length || source[i] == '\n') {
                lines += Line(source.substring(start, i), start)
                start = i + 1
            }
            i++
        }
        // A trailing newline produces a final empty line we can drop; keep at least one line.
        if (lines.size > 1 && lines.last().text.isEmpty() && source.endsWith("\n")) {
            lines.removeAt(lines.size - 1)
        }
        return lines
    }

    /** Returns the index of the first body line after any frontmatter block. */
    private fun extractFrontmatter(lines: List<Line>, into: MutableMap<String, String>): Int {
        if (lines.isEmpty() || lines[0].text.trim() != "---") return 0
        var j = 1
        while (j < lines.size && lines[j].text.trim() != "---") {
            val t = lines[j].text
            val colon = t.indexOf(':')
            if (colon > 0) {
                val key = t.substring(0, colon).trim()
                val value = t.substring(colon + 1).trim().trim('"', '\'')
                if (key.isNotEmpty()) into[key] = value
            }
            j++
        }
        return if (j < lines.size) j + 1 else 0 // unterminated frontmatter -> treat as body
    }

    /** Collects `[label]: url "title"` definitions, blanking their lines so they don't render. */
    private fun collectLinkDefinitions(lines: MutableList<Line>, from: Int): Map<String, LinkDefinition> {
        val refs = HashMap<String, LinkDefinition>()
        var inFence = false
        for (idx in from until lines.size) {
            val t = lines[idx].text
            if (FENCE_LINE.matches(t)) { inFence = !inFence; continue }
            if (inFence) continue
            val m = LINK_DEF.find(t) ?: continue
            val label = m.groupValues[1].trim().lowercase()
            if (label.isEmpty() || label.startsWith("^")) continue // '^' is a footnote definition, not a link def
            refs.putIfAbsent(label, LinkDefinition(m.groupValues[2], m.groupValues[3].ifEmpty { null }))
            lines[idx] = Line("", lines[idx].start)
        }
        return refs
    }

    private companion object {
        val LINK_DEF = Regex("^ {0,3}\\[([^\\]]+)\\]:\\s*(\\S+?)(?:\\s+\"([^\"]*)\")?\\s*$")
        val FENCE_LINE = Regex("^ {0,3}(`{3,}|~{3,}).*$")
    }
}
