package com.cyberdyne.markdown.editor

import com.cyberdyne.markdown.engine.Markdown
import com.cyberdyne.markdown.engine.serialization.toMarkdown

/**
 * Pure block-management helpers for the WYSIWYG editor. Splits a document into
 * per-block Markdown fragments (via the parser + serializer, so code blocks with
 * blank lines survive) and rejoins them. Unit-testable.
 */
object WysiwygOps {

    /** Splits [markdown] into one Markdown fragment per top-level block. */
    fun toBlocks(markdown: String): List<String> =
        Markdown.parse(markdown).blocks.map { it.toMarkdown() }.filter { it.isNotBlank() }

    /** Rejoins block fragments into a single Markdown document. */
    fun join(blocks: List<String>): String =
        blocks.filter { it.isNotBlank() }.joinToString("\n\n")

    fun moved(blocks: List<String>, from: Int, to: Int): List<String> {
        if (from !in blocks.indices || to !in blocks.indices || from == to) return blocks
        val list = blocks.toMutableList()
        list.add(to, list.removeAt(from))
        return list
    }
}
