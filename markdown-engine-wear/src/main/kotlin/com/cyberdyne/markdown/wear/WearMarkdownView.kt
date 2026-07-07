package com.cyberdyne.markdown.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.config.MarkdownExtension
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/**
 * A render-only Markdown surface for Wear OS. Uses a constrained configuration
 * suited to a small screen: the editor is intentionally not exposed, and heavy
 * constructs (Mermaid, math) degrade to source rather than render. Mirrors the
 * iOS watchOS constrained subset.
 */
@Composable
fun WearMarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.Dark,
) {
    MarkdownView(
        markdown = markdown,
        modifier = modifier,
        theme = theme,
        configuration = WEAR_CONFIGURATION,
    )
}

/** Extensions kept on Wear: math and Mermaid are disabled so they degrade to source. */
private val WEAR_CONFIGURATION = MarkdownConfiguration(
    enabledExtensions = setOf(
        MarkdownExtension.FOOTNOTES,
        MarkdownExtension.FRONTMATTER,
        MarkdownExtension.CALLOUTS,
        MarkdownExtension.WIKI_LINKS,
    ),
    codeCopyEnabled = false,
)
