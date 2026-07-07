package com.cyberdyne.markdown.engine.parser

import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.config.MarkdownExtension
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.model.SourceRange
import com.cyberdyne.markdown.engine.model.Utf8Offsets

/** Link reference definitions collected during the block phase (`[id]: url "title"`). */
data class LinkDefinition(val destination: String, val title: String?)

/**
 * Parses a run of inline Markdown text into [InlineNode]s. Emphasis/strong/
 * strikethrough are resolved with a delimiter-stack pass; other inline constructs
 * are recognized as atomic tokens first.
 *
 * @param base absolute UTF-16 char index in the original source where [text] begins.
 */
internal class InlineParser(
    private val config: MarkdownConfiguration,
    private val offsets: Utf8Offsets,
    private val linkRefs: Map<String, LinkDefinition>,
) {
    private sealed interface Token {
        val startChar: Int

        data class Literal(val text: String, override val startChar: Int) : Token
        data class Node(val node: InlineNode) : Token {
            override val startChar: Int get() = 0
        }

        data class Delim(
            val ch: Char,
            var count: Int,
            val canOpen: Boolean,
            val canClose: Boolean,
            override val startChar: Int,
            val originalCount: Int,
        ) : Token
    }

    fun parse(text: String, base: Int): List<InlineNode> {
        val tokens = tokenize(text, base)
        resolveEmphasis(tokens, base, text)
        return materialize(tokens, base)
    }

    // --- Pass 1: tokenize into literals, atomic nodes, and delimiter runs ---

    private fun tokenize(text: String, base: Int): MutableList<Token> {
        val tokens = mutableListOf<Token>()
        val literal = StringBuilder()
        var literalStart = 0
        var i = 0

        fun flush() {
            if (literal.isNotEmpty()) {
                tokens += Token.Literal(literal.toString(), base + literalStart)
                literal.clear()
            }
        }

        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' && i + 1 < text.length && text[i + 1] == '\n' -> {
                    flush(); tokens += Token.Node(InlineNode.LineBreak(true, rng(base + i, base + i + 2)))
                    i += 2
                }
                c == '\\' && i + 1 < text.length && isEscapable(text[i + 1]) -> {
                    if (literal.isEmpty()) literalStart = i + 1
                    literal.append(text[i + 1]); i += 2
                }
                c == '\n' -> {
                    val hard = literal.endsWith("  ") || literal.endsWith("\t")
                    while (literal.isNotEmpty() && literal.last() == ' ') literal.deleteCharAt(literal.length - 1)
                    flush(); tokens += Token.Node(InlineNode.LineBreak(hard, rng(base + i, base + i + 1)))
                    i += 1
                }
                c == '`' -> {
                    val consumed = readCodeSpan(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '$' && config.isEnabled(MarkdownExtension.MATH) -> {
                    val consumed = readInlineMath(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '\\' && i + 1 < text.length && text[i + 1] == '(' && config.isEnabled(MarkdownExtension.MATH) -> {
                    val consumed = readDelimitedMath(text, i, "\\(", "\\)", base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '<' -> {
                    val consumed = readAngle(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '[' && i + 1 < text.length && text[i + 1] == '[' && config.isEnabled(MarkdownExtension.WIKI_LINKS) -> {
                    val consumed = readWikiLink(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '[' && i + 1 < text.length && text[i + 1] == '^' && config.isEnabled(MarkdownExtension.FOOTNOTES) -> {
                    val consumed = readFootnoteRef(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '!' && i + 1 < text.length && text[i + 1] == '[' -> {
                    val consumed = readImage(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '[' -> {
                    val consumed = readLink(text, i, base, tokens, ::flush)
                    if (consumed > 0) i += consumed else { if (literal.isEmpty()) literalStart = i; literal.append(c); i++ }
                }
                c == '*' || c == '_' || c == '~' -> {
                    flush()
                    val run = runLength(text, i, c)
                    if (c == '~' && run < 2) {
                        tokens += Token.Literal("~".repeat(run), base + i)
                    } else {
                        tokens += Token.Delim(c, run, canOpen(text, i, run, c), canClose(text, i, run, c), base + i, run)
                    }
                    i += run
                }
                else -> {
                    val consumed = readBareAutolink(text, i, base, tokens, ::flush)
                    if (consumed > 0) {
                        i += consumed
                    } else {
                        if (literal.isEmpty()) literalStart = i
                        literal.append(c); i++
                    }
                }
            }
        }
        flush()
        return tokens
    }

    private fun rng(startChar: Int, endChar: Int) = offsets.range(startChar, endChar)

    private fun readCodeSpan(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val ticks = runLength(text, i, '`')
        val close = findRun(text, i + ticks, '`', ticks)
        if (close < 0) return 0
        val content = text.substring(i + ticks, close).replace('\n', ' ').let {
            if (it.length > 1 && it.first() == ' ' && it.last() == ' ' && it.isNotBlank()) it.substring(1, it.length - 1) else it
        }
        flush()
        val end = close + ticks
        out += Token.Node(InlineNode.Code(content, rng(base + i, base + end)))
        return end - i
    }

    private fun readInlineMath(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        if (i + 1 < text.length && text[i + 1] == '$') return 0 // `$$` is block math, not inline
        val next = text.getOrNull(i + 1) ?: return 0
        if (next.isWhitespace() || next.isDigit()) return 0 // currency guard
        var j = i + 1
        while (j < text.length) {
            if (text[j] == '$' && text[j - 1] != ' ' && text[j - 1] != '\\') {
                val after = text.getOrNull(j + 1)
                if (after == null || !after.isDigit()) break
            }
            if (text[j] == '\n') return 0
            j++
        }
        if (j >= text.length || text[j] != '$') return 0
        flush()
        out += Token.Node(InlineNode.InlineMath(text.substring(i + 1, j), rng(base + i, base + j + 1)))
        return j + 1 - i
    }

    private fun readDelimitedMath(text: String, i: Int, open: String, close: String, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val end = text.indexOf(close, i + open.length)
        if (end < 0) return 0
        flush()
        out += Token.Node(InlineNode.InlineMath(text.substring(i + open.length, end), rng(base + i, base + end + close.length)))
        return end + close.length - i
    }

    private fun readAngle(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val end = text.indexOf('>', i + 1)
        if (end < 0) return 0
        val inner = text.substring(i + 1, end)
        flush()
        val node: InlineNode = when {
            URL_AUTOLINK.matches(inner) -> InlineNode.Autolink(inner, rng(base + i, base + end + 1))
            EMAIL.matches(inner) -> InlineNode.Autolink("mailto:$inner", rng(base + i, base + end + 1))
            inner.startsWith("/") || inner.firstOrNull()?.isLetter() == true ->
                InlineNode.Html(text.substring(i, end + 1), rng(base + i, base + end + 1))
            else -> return 0
        }
        out += Token.Node(node)
        return end + 1 - i
    }

    private fun readBareAutolink(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        if (!text.startsWith("http://", i) && !text.startsWith("https://", i)) return 0
        if (i > 0 && (text[i - 1].isLetterOrDigit())) return 0
        var j = i
        while (j < text.length && !text[j].isWhitespace() && text[j] != '<') j++
        while (j > i && text[j - 1] in ".,;:!?)") j--
        if (j <= i + 8) return 0
        flush()
        out += Token.Node(InlineNode.Autolink(text.substring(i, j), rng(base + i, base + j)))
        return j - i
    }

    private fun readWikiLink(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val end = text.indexOf("]]", i + 2)
        if (end < 0) return 0
        val inner = text.substring(i + 2, end)
        val pipe = inner.indexOf('|')
        val target = (if (pipe >= 0) inner.substring(0, pipe) else inner).trim()
        val display = if (pipe >= 0) inner.substring(pipe + 1).trim() else null
        flush()
        out += Token.Node(InlineNode.WikiLink(target, display, rng(base + i, base + end + 2)))
        return end + 2 - i
    }

    private fun readFootnoteRef(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val end = text.indexOf(']', i + 2)
        if (end < 0) return 0
        val id = text.substring(i + 2, end)
        if (id.isEmpty() || id.any { it.isWhitespace() }) return 0
        flush()
        out += Token.Node(InlineNode.FootnoteReference(id, rng(base + i, base + end + 1)))
        return end + 1 - i
    }

    private fun readImage(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val labelEnd = matchBracket(text, i + 1)
        if (labelEnd < 0) return 0
        val alt = text.substring(i + 2, labelEnd)
        val paren = parseInlineTarget(text, labelEnd + 1) ?: return 0
        flush()
        out += Token.Node(InlineNode.Image(paren.first, alt, paren.second, rng(base + i, base + paren.third)))
        return paren.third - i
    }

    private fun readLink(text: String, i: Int, base: Int, out: MutableList<Token>, flush: () -> Unit): Int {
        val labelEnd = matchBracket(text, i)
        if (labelEnd < 0) return 0
        val label = text.substring(i + 1, labelEnd)
        // Inline link: [text](url)
        parseInlineTarget(text, labelEnd + 1)?.let { (dest, title, end) ->
            flush()
            out += Token.Node(InlineNode.Link(dest, title, parse(label, base + i + 1), rng(base + i, base + end)))
            return end - i
        }
        // Reference link: [text][id] or [text][] or [text]
        var refId = label
        var end = labelEnd + 1
        if (end < text.length && text[end] == '[') {
            val refEnd = text.indexOf(']', end + 1)
            if (refEnd < 0) return 0
            val explicit = text.substring(end + 1, refEnd)
            if (explicit.isNotBlank()) refId = explicit
            end = refEnd + 1
        }
        val def = linkRefs[refId.trim().lowercase()] ?: return 0
        flush()
        out += Token.Node(InlineNode.Link(def.destination, def.title, parse(label, base + i + 1), rng(base + i, base + end)))
        return end - i
    }

    /** Parses `(url "title")` starting at [open]; returns (dest, title, endIndexExclusive) or null. */
    private fun parseInlineTarget(text: String, open: Int): Triple<String, String?, Int>? {
        if (open >= text.length || text[open] != '(') return null
        val close = text.indexOf(')', open + 1)
        if (close < 0) return null
        val inside = text.substring(open + 1, close).trim()
        val titleStart = inside.indexOf('"')
        return if (titleStart >= 0 && inside.endsWith("\"")) {
            Triple(inside.substring(0, titleStart).trim(), inside.substring(titleStart + 1, inside.length - 1), close + 1)
        } else {
            Triple(inside, null, close + 1)
        }
    }

    private fun matchBracket(text: String, openBracket: Int): Int {
        var depth = 0
        var i = openBracket
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i++
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return -1
    }

    // --- Pass 2: emphasis / strong / strikethrough via delimiter stack ---

    /**
     * CommonMark `process_emphasis`: scan closers left-to-right, match the nearest
     * valid opener (respecting the rule of three and an openers-bottom watermark
     * per delimiter char), and wrap the enclosed content.
     */
    private fun resolveEmphasis(tokens: MutableList<Token>, base: Int, text: String) {
        // Keyed by (delimiter char, run length % 3) so a blocked short run does not
        // prevent a longer run of the same char from matching (CommonMark buckets).
        val openersBottom = HashMap<String, Int>()
        var closerIdx = 0
        while (closerIdx < tokens.size) {
            val closer = tokens[closerIdx]
            if (closer !is Token.Delim || !closer.canClose) { closerIdx++; continue }

            val key = "${closer.ch}:${closer.originalCount % 3}"
            val bottom = openersBottom[key] ?: -1
            var found = -1
            var openerIdx = closerIdx - 1
            while (openerIdx > bottom) {
                val opener = tokens[openerIdx]
                if (opener is Token.Delim && opener.canOpen && opener.ch == closer.ch && emphasisRuleOk(opener, closer)) {
                    found = openerIdx; break
                }
                openerIdx--
            }
            if (found == -1) {
                openersBottom[key] = closerIdx - 1
                closerIdx++
                continue
            }
            closerIdx = wrap(tokens, found, closerIdx, closer.ch, base)
        }
    }

    /** The CommonMark "rule of three" for delimiter compatibility. */
    private fun emphasisRuleOk(opener: Token.Delim, closer: Token.Delim): Boolean {
        val eitherBothSided = (opener.canOpen && opener.canClose) || (closer.canOpen && closer.canClose)
        if (eitherBothSided && (opener.originalCount + closer.originalCount) % 3 == 0 &&
            !(opener.originalCount % 3 == 0 && closer.originalCount % 3 == 0)
        ) {
            return false
        }
        return true
    }

    /** Wraps content between [openerIdx] and [closerIdx]; returns the index to continue scanning from. */
    private fun wrap(tokens: MutableList<Token>, openerIdx: Int, closerIdx: Int, ch: Char, base: Int): Int {
        val opener = tokens[openerIdx] as Token.Delim
        val closer = tokens[closerIdx] as Token.Delim
        val use = if (ch == '~') 2 else if (opener.count >= 2 && closer.count >= 2) 2 else 1
        val openCharEnd = opener.startChar + opener.count
        val innerNodes = materialize(tokens.subList(openerIdx + 1, closerIdx).toMutableList(), base)
        val startChar = openCharEnd - use
        val endChar = closer.startChar + use
        val node: InlineNode = when {
            ch == '~' -> InlineNode.Strikethrough(innerNodes, rng(startChar, endChar))
            use == 2 -> InlineNode.Strong(innerNodes, rng(startChar, endChar))
            else -> InlineNode.Emphasis(innerNodes, rng(startChar, endChar))
        }
        opener.count -= use
        closer.count -= use

        for (k in closerIdx downTo openerIdx + 1) tokens.removeAt(k)
        var insertAt = openerIdx + 1
        tokens.add(insertAt, Token.Node(node))

        var continueAt = insertAt + 1
        if (closer.count > 0) {
            val leftover = Token.Delim(closer.ch, closer.count, closer.canOpen, closer.canClose, closer.startChar + use, closer.originalCount)
            tokens.add(insertAt + 1, leftover)
            continueAt = insertAt + 1 // re-examine the leftover closer against earlier openers
        }
        if (opener.count <= 0) {
            tokens.removeAt(openerIdx)
            insertAt--
            continueAt--
        }
        return continueAt
    }

    // --- Pass 3: turn remaining tokens into inline nodes ---

    private fun materialize(tokens: MutableList<Token>, base: Int): List<InlineNode> {
        val result = mutableListOf<InlineNode>()
        val pending = StringBuilder()
        var pendingStart = -1

        fun flushText() {
            if (pending.isNotEmpty()) {
                val end = pendingStart + pending.length
                result += InlineNode.Text(pending.toString(), rng(pendingStart, end))
                pending.clear(); pendingStart = -1
            }
        }

        for (t in tokens) {
            when (t) {
                is Token.Node -> { flushText(); result += t.node }
                is Token.Literal -> {
                    if (pending.isEmpty()) pendingStart = t.startChar
                    pending.append(t.text)
                }
                is Token.Delim -> {
                    if (t.count > 0) {
                        if (pending.isEmpty()) pendingStart = t.startChar
                        pending.append(t.ch.toString().repeat(t.count))
                    }
                }
            }
        }
        flushText()
        return result
    }

    // --- delimiter flanking rules (simplified CommonMark) ---

    private fun canOpen(text: String, i: Int, run: Int, ch: Char): Boolean {
        val next = text.getOrNull(i + run) ?: return false
        if (next.isWhitespace()) return false
        if (ch == '_') {
            val prev = text.getOrNull(i - 1)
            if (prev != null && prev.isLetterOrDigit()) return false // no intraword underscore open
        }
        return true
    }

    private fun canClose(text: String, i: Int, run: Int, ch: Char): Boolean {
        val prev = text.getOrNull(i - 1) ?: return false
        if (prev.isWhitespace()) return false
        if (ch == '_') {
            val next = text.getOrNull(i + run)
            if (next != null && next.isLetterOrDigit()) return false // no intraword underscore close
        }
        return true
    }

    private fun runLength(text: String, i: Int, ch: Char): Int {
        var j = i
        while (j < text.length && text[j] == ch) j++
        return j - i
    }

    private fun findRun(text: String, from: Int, ch: Char, len: Int): Int {
        var i = from
        while (i < text.length) {
            if (text[i] == ch) {
                val r = runLength(text, i, ch)
                if (r == len) return i
                i += r
            } else i++
        }
        return -1
    }

    private fun isEscapable(c: Char): Boolean = c in "\\`*_{}[]()#+-.!>~|\"$/^&"

    private companion object {
        val URL_AUTOLINK = Regex("[a-zA-Z][a-zA-Z0-9+.-]*://\\S+")
        val EMAIL = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
    }
}
