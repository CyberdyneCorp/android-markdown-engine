package com.cyberdyne.markdown.engine.serialization

import com.cyberdyne.markdown.engine.Markdown
import com.cyberdyne.markdown.engine.model.BlockNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializationTest {

    /** Structural equality ignoring source ranges (which shift after re-serialization). */
    private fun structurallyEqual(a: com.cyberdyne.markdown.engine.model.MarkdownDocument,
                                  b: com.cyberdyne.markdown.engine.model.MarkdownDocument): Boolean =
        stripRanges(a) == stripRanges(b)

    private fun stripRanges(doc: com.cyberdyne.markdown.engine.model.MarkdownDocument): String =
        doc.blocks.joinToString("\n") { it.toMarkdown() }

    @Test fun representativeDocumentRoundTrips() {
        val src = """
            # Title

            Some **bold**, *italic*, ~~struck~~, `code`, and [link](https://x.com).

            - [x] done
            - [ ] todo

            | a | b |
            |:---|---:|
            | 1 | 2 |

            ```kotlin
            val x = 1
            ```

            $$
            E = mc^2
            $$

            ![alt](img.png)
        """.trimIndent()

        val once = Markdown.parse(src)
        val twice = Markdown.parse(once.toMarkdown())
        assertTrue(structurallyEqual(once, twice), "document should round-trip structurally")
    }

    @Test fun inlineDelimitersReproduced() {
        val src = "text with **bold** *italic* ~~strike~~ `code` and [a](b)"
        val roundTripped = Markdown.parse(Markdown.parse(src).toMarkdown()).toMarkdown()
        assertTrue(roundTripped.contains("**bold**"))
        assertTrue(roundTripped.contains("*italic*"))
        assertTrue(roundTripped.contains("~~strike~~"))
        assertTrue(roundTripped.contains("`code`"))
        assertTrue(roundTripped.contains("[a](b)"))
    }

    @Test fun singleTableBlockSerializes() {
        val table = Markdown.parse("| h1 | h2 |\n|---|---|\n| a | b |").blocks.single() as BlockNode.Table
        val md = table.toMarkdown()
        val reparsed = Markdown.parse(md).blocks.single()
        assertTrue(reparsed is BlockNode.Table)
        assertEquals(2, (reparsed as BlockNode.Table).header.size)
    }

    @Test fun headingRoundTrips() {
        assertEquals("## Hello", Markdown.parse("## Hello").blocks.single().toMarkdown())
    }
}
