package com.cyberdyne.markdown.engine.config

/**
 * Optional Markdown extensions beyond CommonMark + core GFM. Mirrors the iOS
 * `enabledExtensions` cases. Core GFM (tables, strikethrough, task lists,
 * extended autolinks) is always parsed and is not gated here.
 */
enum class MarkdownExtension {
    MATH,
    MERMAID,
    FOOTNOTES,
    WIKI_LINKS,
    FRONTMATTER,
    CALLOUTS,
    ;

    companion object {
        /** All extensions enabled — the default, matching a batteries-included view. */
        val ALL: Set<MarkdownExtension> = entries.toSet()
    }
}

/** How a diagram behaves when it is larger than the viewport. */
enum class DiagramSizing {
    /** Pan/scroll within the container at intrinsic size (default). */
    SCROLL,

    /** Scale to fit the available width. */
    FIT_TO_WIDTH,
}

/**
 * Controls parsing and rendering behavior. Immutable value type; mirrors the iOS
 * `MarkdownConfiguration`.
 *
 * @property interactiveCheckboxes render task items as toggleable controls.
 * @property showCodeLineNumbers show line numbers on fenced code blocks.
 * @property codeCopyEnabled show a copy-to-clipboard affordance on code blocks.
 * @property enabledExtensions which optional extensions the parser recognizes.
 * @property diagramSizing overflow behavior for diagrams.
 * @property readingWidthDp optional max content width (dp) for prose; null = fill.
 */
data class MarkdownConfiguration(
    val interactiveCheckboxes: Boolean = false,
    val showCodeLineNumbers: Boolean = false,
    val codeCopyEnabled: Boolean = true,
    val enabledExtensions: Set<MarkdownExtension> = MarkdownExtension.ALL,
    val diagramSizing: DiagramSizing = DiagramSizing.SCROLL,
    val readingWidthDp: Int? = null,
) {
    fun isEnabled(extension: MarkdownExtension): Boolean = extension in enabledExtensions

    companion object {
        val Default = MarkdownConfiguration()
    }
}
