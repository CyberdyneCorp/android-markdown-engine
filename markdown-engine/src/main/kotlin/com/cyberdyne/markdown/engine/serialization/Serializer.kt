package com.cyberdyne.markdown.engine.serialization

import com.cyberdyne.markdown.engine.model.BlockNode
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.model.ListItem
import com.cyberdyne.markdown.engine.model.MarkdownDocument
import com.cyberdyne.markdown.engine.model.TableAlignment
import com.cyberdyne.markdown.engine.model.TableCell

/** Serialize a whole document back to Markdown. Round-trips with the parser. */
fun MarkdownDocument.toMarkdown(): String {
    val sb = StringBuilder()
    if (metadata.isNotEmpty()) {
        sb.append("---\n")
        for ((k, v) in metadata) sb.append(k).append(": ").append(v).append('\n')
        sb.append("---\n\n")
    }
    sb.append(blocks.joinToString("\n\n") { it.toMarkdown() })
    return sb.toString()
}

/** Serialize a single block to its Markdown fragment. */
fun BlockNode.toMarkdown(): String = when (this) {
    is BlockNode.Heading -> "#".repeat(level) + " " + content.toMarkdown()
    is BlockNode.Paragraph -> content.toMarkdown()
    is BlockNode.ThematicBreak -> "---"
    is BlockNode.CodeBlock -> "```" + (language ?: "") + "\n" + content + "\n```"
    is BlockNode.BlockMath -> "$$\n" + body + "\n$$"
    is BlockNode.Mermaid -> "```mermaid\n" + source + "\n```"
    is BlockNode.HtmlBlock -> content
    is BlockNode.BlockQuote -> prefixLines(children.joinToString("\n\n") { it.toMarkdown() }, "> ")
    is BlockNode.Callout -> {
        val head = "> [!" + kindLabel.uppercase() + "]" + (title.toMarkdown().let { if (it.isBlank()) "" else " $it" })
        val bodyMd = children.joinToString("\n\n") { it.toMarkdown() }
        if (bodyMd.isBlank()) head else head + "\n" + prefixLines(bodyMd, "> ")
    }
    is BlockNode.FootnoteDefinition -> "[^" + identifier + "]: " + children.joinToString("\n\n") { it.toMarkdown() }.trim()
    is BlockNode.ListBlock -> serializeList(this)
    is BlockNode.Table -> serializeTable(this)
}

/** Serialize a list of inline nodes to Markdown. */
fun List<InlineNode>.toMarkdown(): String = joinToString("") { it.toMarkdown() }

/** Serialize a single inline node to Markdown. */
fun InlineNode.toMarkdown(): String = when (this) {
    is InlineNode.Text -> content
    is InlineNode.Emphasis -> "*" + children.toMarkdown() + "*"
    is InlineNode.Strong -> "**" + children.toMarkdown() + "**"
    is InlineNode.Strikethrough -> "~~" + children.toMarkdown() + "~~"
    is InlineNode.Code -> {
        val ticks = longestBacktickRun(content) + 1
        val fence = "`".repeat(ticks)
        val pad = if (content.startsWith("`") || content.endsWith("`")) " " else ""
        fence + pad + content + pad + fence
    }
    is InlineNode.Link -> "[" + children.toMarkdown() + "](" + destination + titleSuffix(title) + ")"
    is InlineNode.Image -> "![" + alt + "](" + source + titleSuffix(title) + ")"
    is InlineNode.Autolink -> "<" + url.removePrefix("mailto:") + ">"
    is InlineNode.LineBreak -> if (hard) "  \n" else "\n"
    is InlineNode.InlineMath -> "$" + body + "$"
    is InlineNode.WikiLink -> "[[" + target + (display?.let { "|$it" } ?: "") + "]]"
    is InlineNode.FootnoteReference -> "[^" + identifier + "]"
    is InlineNode.Html -> content
}

private fun titleSuffix(title: String?): String = title?.let { " \"$it\"" } ?: ""

private fun longestBacktickRun(s: String): Int {
    var max = 0
    var cur = 0
    for (c in s) {
        if (c == '`') { cur++; max = maxOf(max, cur) } else cur = 0
    }
    return max
}

private fun prefixLines(text: String, prefix: String): String =
    text.split("\n").joinToString("\n") { if (it.isEmpty()) prefix.trimEnd() else prefix + it }

private fun serializeList(list: BlockNode.ListBlock): String {
    val sb = StringBuilder()
    var ordinal = list.start
    for ((index, item) in list.items.withIndex()) {
        val marker = if (list.ordered) "$ordinal. " else "- "
        val task = when (item.checked) {
            true -> "[x] "
            false -> "[ ] "
            null -> ""
        }
        val content = item.children.joinToString("\n\n") { it.toMarkdown() }
        val indented = indentContinuation(content, marker.length)
        sb.append(marker).append(task).append(indented)
        if (index < list.items.size - 1) sb.append(if (list.tight) "\n" else "\n\n")
        ordinal++
    }
    return sb.toString()
}

/** Indents continuation lines of a list item's content by [width] spaces. */
private fun indentContinuation(content: String, width: Int): String {
    val lines = content.split("\n")
    if (lines.size <= 1) return content
    val pad = " ".repeat(width)
    return lines.first() + "\n" + lines.drop(1).joinToString("\n") { if (it.isEmpty()) "" else pad + it }
}

private fun serializeTable(table: BlockNode.Table): String {
    val cols = table.alignments.size.coerceAtLeast(table.header.size)
    fun row(cells: List<TableCell>): String =
        "| " + (0 until cols).joinToString(" | ") { cells.getOrNull(it)?.toMarkdown()?.trim() ?: "" } + " |"

    val delim = "| " + (0 until cols).joinToString(" | ") {
        when (table.alignments.getOrNull(it) ?: TableAlignment.NONE) {
            TableAlignment.LEFT -> ":---"
            TableAlignment.CENTER -> ":---:"
            TableAlignment.RIGHT -> "---:"
            TableAlignment.NONE -> "---"
        }
    } + " |"

    val sb = StringBuilder()
    sb.append(row(table.header)).append('\n').append(delim)
    for (r in table.rows) sb.append('\n').append(row(r))
    return sb.toString()
}
