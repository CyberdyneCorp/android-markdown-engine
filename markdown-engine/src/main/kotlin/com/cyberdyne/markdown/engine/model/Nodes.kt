package com.cyberdyne.markdown.engine.model

/**
 * Per-column alignment of a GFM table, derived from the delimiter row
 * (`:---`, `:---:`, `---:`, `---`).
 */
enum class TableAlignment { NONE, LEFT, CENTER, RIGHT }

/** The block variant, used by custom block renderers to key overrides. */
enum class BlockKind {
    HEADING, PARAGRAPH, BLOCK_QUOTE, THEMATIC_BREAK, CODE_BLOCK, BLOCK_MATH,
    MERMAID, LIST, TABLE, CALLOUT, HTML_BLOCK, FOOTNOTE_DEFINITION
}

/**
 * Immutable, thread-safe representation of a parsed Markdown document. Safe to
 * build off the main thread and hand to the UI thread (mirrors the iOS
 * `Sendable` `MarkdownDocument`).
 *
 * @property blocks top-level block nodes, in document order.
 * @property metadata parsed YAML frontmatter key/value pairs (empty if none).
 */
data class MarkdownDocument(
    val blocks: List<BlockNode>,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        val EMPTY = MarkdownDocument(emptyList())
    }
}

/** A GFM table cell: a sequence of inline nodes. */
typealias TableCell = List<InlineNode>

/** Block-level AST node. Every node exposes its UTF-8 [range]. */
sealed interface BlockNode {
    val range: SourceRange
    val kind: BlockKind

    data class Heading(
        val level: Int,
        val content: List<InlineNode>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.HEADING
    }

    data class Paragraph(
        val content: List<InlineNode>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.PARAGRAPH
    }

    data class BlockQuote(
        val children: List<BlockNode>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.BLOCK_QUOTE
    }

    data class ThematicBreak(
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.THEMATIC_BREAK
    }

    data class CodeBlock(
        val language: String?,
        val content: String,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.CODE_BLOCK
    }

    data class BlockMath(
        val body: String,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.BLOCK_MATH
    }

    data class Mermaid(
        val source: String,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.MERMAID
    }

    /**
     * An ordered or unordered list. Named [ListBlock] (not `List`) to avoid
     * colliding with `kotlin.collections.List`.
     *
     * @property ordered true for `1.`/`1)` lists.
     * @property start starting ordinal for ordered lists (ignored when unordered).
     * @property tight true when items have no blank lines between them.
     */
    data class ListBlock(
        val ordered: Boolean,
        val start: Int,
        val tight: Boolean,
        val items: List<ListItem>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.LIST
    }

    data class Table(
        val alignments: List<TableAlignment>,
        val header: List<TableCell>,
        val rows: List<List<TableCell>>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.TABLE
    }

    /**
     * A callout/admonition block (`> [!NOTE]`). [kindLabel] is the lowercased
     * type name, e.g. `note`, `warning`.
     */
    data class Callout(
        val kindLabel: String,
        val title: List<InlineNode>,
        val children: List<BlockNode>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.CALLOUT
    }

    data class HtmlBlock(
        val content: String,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.HTML_BLOCK
    }

    data class FootnoteDefinition(
        val identifier: String,
        val children: List<BlockNode>,
        override val range: SourceRange,
    ) : BlockNode {
        override val kind: BlockKind get() = BlockKind.FOOTNOTE_DEFINITION
    }
}

/**
 * An item within a [BlockNode.ListBlock].
 *
 * @property checked `null` for a plain item, `true`/`false` for a GFM task item.
 */
data class ListItem(
    val children: List<BlockNode>,
    val checked: Boolean?,
    val range: SourceRange,
)

/** Inline-level AST node. Every node exposes its UTF-8 [range]. */
sealed interface InlineNode {
    val range: SourceRange

    data class Text(val content: String, override val range: SourceRange) : InlineNode

    data class Emphasis(val children: List<InlineNode>, override val range: SourceRange) : InlineNode

    data class Strong(val children: List<InlineNode>, override val range: SourceRange) : InlineNode

    data class Strikethrough(val children: List<InlineNode>, override val range: SourceRange) : InlineNode

    /** Inline code span. */
    data class Code(val content: String, override val range: SourceRange) : InlineNode

    data class Link(
        val destination: String,
        val title: String?,
        val children: List<InlineNode>,
        override val range: SourceRange,
    ) : InlineNode

    data class Image(
        val source: String,
        val alt: String,
        val title: String?,
        override val range: SourceRange,
    ) : InlineNode

    data class Autolink(val url: String, override val range: SourceRange) : InlineNode

    data class LineBreak(val hard: Boolean, override val range: SourceRange) : InlineNode

    data class InlineMath(val body: String, override val range: SourceRange) : InlineNode

    data class WikiLink(
        val target: String,
        val display: String?,
        override val range: SourceRange,
    ) : InlineNode

    data class FootnoteReference(val identifier: String, override val range: SourceRange) : InlineNode

    /** Raw inline HTML. */
    data class Html(val content: String, override val range: SourceRange) : InlineNode
}
