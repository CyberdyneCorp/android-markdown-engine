package com.cyberdyne.markdown.engine.parser

import com.cyberdyne.markdown.engine.Markdown
import com.cyberdyne.markdown.engine.model.BlockNode
import com.cyberdyne.markdown.engine.model.InlineNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Structural conformance corpus for CommonMark/GFM edge cases. */
class ParserConformanceTest {

    private fun para(src: String): List<InlineNode> =
        (Markdown.parse(src).blocks.single() as BlockNode.Paragraph).content

    private fun flatten(nodes: List<InlineNode>): List<InlineNode> =
        nodes.flatMap {
            listOf(it) + when (it) {
                is InlineNode.Emphasis -> flatten(it.children)
                is InlineNode.Strong -> flatten(it.children)
                is InlineNode.Strikethrough -> flatten(it.children)
                is InlineNode.Link -> flatten(it.children)
                else -> emptyList()
            }
        }

    private fun plainText(nodes: List<InlineNode>): String = buildString {
        fun walk(ns: List<InlineNode>) {
            for (n in ns) when (n) {
                is InlineNode.Text -> append(n.content)
                is InlineNode.Emphasis -> walk(n.children)
                is InlineNode.Strong -> walk(n.children)
                is InlineNode.Strikethrough -> walk(n.children)
                is InlineNode.Code -> append(n.content)
                is InlineNode.Link -> walk(n.children)
                else -> {}
            }
        }
        walk(nodes)
    }

    // --- Emphasis ---

    @Test fun strongContainingEmphasis() {
        val n = para("**a*b*c**")
        val strong = n.single() as InlineNode.Strong
        assertTrue(strong.children.any { it is InlineNode.Emphasis })
        assertEquals("abc", plainText(listOf(strong)))
    }

    @Test fun emphasisContainingStrong() {
        val n = para("*a**b**c*")
        val em = n.single() as InlineNode.Emphasis
        assertTrue(em.children.any { it is InlineNode.Strong })
    }

    @Test fun tripleIsStrongAndEmphasis() {
        val n = para("***boom***")
        // Either Strong>Emphasis or Emphasis>Strong; both contain the word.
        assertEquals("boom", plainText(n))
        assertTrue(flatten(n).any { it is InlineNode.Strong })
        assertTrue(flatten(n).any { it is InlineNode.Emphasis })
    }

    @Test fun intrawordUnderscoreIsNotEmphasis() {
        assertTrue(flatten(para("a_b_c")).none { it is InlineNode.Emphasis })
    }

    @Test fun intrawordAsteriskIsEmphasis() {
        assertTrue(flatten(para("a*b*c")).any { it is InlineNode.Emphasis })
    }

    @Test fun escapedDelimitersAreLiteral() {
        val n = para("\\*\\*not bold\\*\\*")
        assertTrue(flatten(n).none { it is InlineNode.Strong })
        assertEquals("**not bold**", plainText(n))
    }

    @Test fun underscoreEmphasisWithBoundaries() {
        assertTrue(flatten(para("_hi_")).any { it is InlineNode.Emphasis })
    }

    @Test fun unmatchedDelimitersAreLiteral() {
        assertEquals("*a", plainText(para("*a")))
        assertEquals("a**b", plainText(para("a**b")))
    }

    // --- Headings ---

    @Test fun atxRequiresSpace() {
        assertTrue(Markdown.parse("#notaheading").blocks.single() is BlockNode.Paragraph)
    }

    @Test fun atxTrailingHashesStripped() {
        val h = Markdown.parse("## Title ##").blocks.single() as BlockNode.Heading
        assertEquals("Title", plainText(h.content))
    }

    @Test fun sevenHashesIsNotHeading() {
        assertTrue(Markdown.parse("####### too many").blocks.single() is BlockNode.Paragraph)
    }

    @Test fun setextBeatsThematicBreak() {
        val h = Markdown.parse("Title\n---").blocks.single() as BlockNode.Heading
        assertEquals(2, h.level)
    }

    // --- Code ---

    @Test fun fencedInfoStringFirstWordIsLanguage() {
        val cb = Markdown.parse("```python title=foo\nx=1\n```").blocks.single() as BlockNode.CodeBlock
        assertEquals("python", cb.language)
    }

    @Test fun fencedCodeStripsFenceIndent() {
        val cb = Markdown.parse("  ```\n  indented\n    deeper\n  ```").blocks.single() as BlockNode.CodeBlock
        assertEquals("indented\n  deeper", cb.content)
    }

    @Test fun tildeFenceWorks() {
        val cb = Markdown.parse("~~~\ncode\n~~~").blocks.single() as BlockNode.CodeBlock
        assertEquals("code", cb.content)
    }

    // --- Links ---

    @Test fun referenceLinkCaseInsensitive() {
        val link = flatten(para("[text][REF]\n\n[ref]: https://x.com"))
            .filterIsInstance<InlineNode.Link>().single()
        assertEquals("https://x.com", link.destination)
    }

    @Test fun inlineLinkWithTitle() {
        val link = para("[a](https://x.com \"T\")").filterIsInstance<InlineNode.Link>().single()
        assertEquals("https://x.com", link.destination)
        assertEquals("T", link.title)
    }

    // --- GFM tables ---

    @Test fun raggedTableRowsPadded() {
        val t = Markdown.parse("| a | b | c |\n|---|---|---|\n| 1 | 2 |").blocks.single() as BlockNode.Table
        assertEquals(3, t.rows.single().size) // padded to 3 columns
    }

    @Test fun longTableRowsTruncated() {
        val t = Markdown.parse("| a | b |\n|---|---|\n| 1 | 2 | 3 |").blocks.single() as BlockNode.Table
        assertEquals(2, t.rows.single().size)
    }
}
