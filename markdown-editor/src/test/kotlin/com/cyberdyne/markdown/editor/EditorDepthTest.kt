package com.cyberdyne.markdown.editor

import com.cyberdyne.markdown.engine.model.TableAlignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorDepthTest {

    // Wiki-link completion
    @Test fun openWikiLinkOffersPrefix() {
        assertEquals("Pa", WikiCompletion.contextAt("see [[Pa", 8))
        assertEquals("", WikiCompletion.contextAt("x [[", 4))
    }

    @Test fun closedWikiLinkOffersNothing() {
        assertNull(WikiCompletion.contextAt("[[Page]] x", 10))
        assertNull(WikiCompletion.contextAt("no brackets", 5))
        assertNull(WikiCompletion.contextAt("[[a\nb", 5)) // newline breaks the link
    }

    @Test fun insertCompletesWikiLink() {
        val (text, cursor) = WikiCompletion.insert("see [[Pa", 8, "Page One")
        assertEquals("see [[Page One]]", text)
        assertEquals(text.length, cursor)
    }

    // Spellcheck suppression
    @Test fun cursorInCodeSpanSuppressed() {
        val text = "text `code` more"
        assertTrue(SpellcheckRegions.isSuppressed(text, 7)) // inside `code`
        assertFalse(SpellcheckRegions.isSuppressed(text, 1)) // in "text"
    }

    @Test fun mathAndWikiSuppressed() {
        assertTrue(SpellcheckRegions.isSuppressed("a \$x+y\$ b", 4))
        assertTrue(SpellcheckRegions.isSuppressed("a [[Link]] b", 5))
    }

    @Test fun fencedCodeSuppressed() {
        val text = "```\ncode here\n```"
        assertTrue(SpellcheckRegions.isSuppressed(text, 6))
    }

    // Table grid
    @Test fun tableGridRoundTrips() {
        val grid = TableGrid.fromMarkdown("| a | b |\n|:---|---:|\n| 1 | 2 |")!!
        assertEquals(listOf("a", "b"), grid.header)
        assertEquals(listOf(TableAlignment.LEFT, TableAlignment.RIGHT), grid.alignments)
        val reparsed = TableGrid.fromMarkdown(grid.toMarkdown())!!
        assertEquals(grid.header, reparsed.header)
        assertEquals(grid.rows, reparsed.rows)
        assertEquals(grid.alignments, reparsed.alignments)
    }

    @Test fun tableGridAddColumnAndRow() {
        val grid = TableGrid.fromMarkdown("| a |\n|---|\n| 1 |")!!.addColumn().addRow()
        assertEquals(2, grid.columns)
        val reparsed = TableGrid.fromMarkdown(grid.toMarkdown())!!
        assertEquals(2, reparsed.header.size)
        assertEquals(2, reparsed.rows.size)
    }

    @Test fun tableGridEditCellAndAlignment() {
        val grid = TableGrid.fromMarkdown("| a | b |\n|---|---|\n| 1 | 2 |")!!
            .setCell(0, 1, "X").setAlignment(0, TableAlignment.CENTER)
        val reparsed = TableGrid.fromMarkdown(grid.toMarkdown())!!
        assertEquals("X", reparsed.rows[0][1])
        assertEquals(TableAlignment.CENTER, reparsed.alignments[0])
    }
}
