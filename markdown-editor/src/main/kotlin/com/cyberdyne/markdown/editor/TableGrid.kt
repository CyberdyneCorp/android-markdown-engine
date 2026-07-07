package com.cyberdyne.markdown.editor

import com.cyberdyne.markdown.engine.Markdown
import com.cyberdyne.markdown.engine.model.BlockNode
import com.cyberdyne.markdown.engine.model.TableAlignment
import com.cyberdyne.markdown.engine.serialization.toMarkdown

/**
 * An editable GFM table grid backing the WYSIWYG visual table editor. Pure
 * (fromMarkdown/toMarkdown round-trip + row/column/alignment ops), unit-testable.
 */
data class TableGrid(
    val header: List<String>,
    val rows: List<List<String>>,
    val alignments: List<TableAlignment>,
) {
    val columns: Int get() = header.size

    fun setCell(row: Int, col: Int, value: String): TableGrid {
        val newRows = rows.mapIndexed { r, cells ->
            if (r == row) cells.mapIndexed { c, v -> if (c == col) value else v } else cells
        }
        return copy(rows = newRows)
    }

    fun setHeader(col: Int, value: String): TableGrid =
        copy(header = header.mapIndexed { c, v -> if (c == col) value else v })

    fun setAlignment(col: Int, alignment: TableAlignment): TableGrid =
        copy(alignments = alignments.mapIndexed { c, a -> if (c == col) alignment else a })

    fun addColumn(): TableGrid = copy(
        header = header + "",
        rows = rows.map { it + "" },
        alignments = alignments + TableAlignment.NONE,
    )

    fun removeColumn(col: Int): TableGrid {
        if (col !in header.indices || header.size <= 1) return this
        return copy(
            header = header.filterIndexed { c, _ -> c != col },
            rows = rows.map { it.filterIndexed { c, _ -> c != col } },
            alignments = alignments.filterIndexed { c, _ -> c != col },
        )
    }

    fun addRow(): TableGrid = copy(rows = rows + listOf(List(columns) { "" }))

    fun removeRow(row: Int): TableGrid =
        if (row in rows.indices) copy(rows = rows.filterIndexed { r, _ -> r != row }) else this

    fun toMarkdown(): String {
        fun row(cells: List<String>) = "| " + (0 until columns).joinToString(" | ") { cells.getOrElse(it) { "" }.ifBlank { "" } } + " |"
        val delim = "| " + (0 until columns).joinToString(" | ") {
            when (alignments.getOrElse(it) { TableAlignment.NONE }) {
                TableAlignment.LEFT -> ":---"
                TableAlignment.CENTER -> ":---:"
                TableAlignment.RIGHT -> "---:"
                TableAlignment.NONE -> "---"
            }
        } + " |"
        return buildString {
            append(row(header)).append('\n').append(delim)
            for (r in rows) append('\n').append(row(r))
        }
    }

    companion object {
        fun fromMarkdown(markdown: String): TableGrid? {
            val table = Markdown.parse(markdown).blocks.filterIsInstance<BlockNode.Table>().firstOrNull() ?: return null
            fun cellText(cell: List<com.cyberdyne.markdown.engine.model.InlineNode>) = cell.joinToString("") { it.toMarkdown() }.trim()
            return TableGrid(
                header = table.header.map { cellText(it) },
                rows = table.rows.map { r -> r.map { cellText(it) } },
                alignments = table.alignments,
            )
        }
    }
}
