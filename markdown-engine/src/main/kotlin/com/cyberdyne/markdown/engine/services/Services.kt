package com.cyberdyne.markdown.engine.services

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme

/**
 * Colors a fenced code block. Injectable; the default renders plain monospaced
 * text. Mirrors the iOS `SyntaxHighlighter` protocol. Callers pass the canonical
 * language (see [com.cyberdyne.markdown.engine.code.LanguageAliases]).
 */
interface SyntaxHighlighter {
    fun highlight(code: String, language: String?): AnnotatedString
}

/** Default highlighter: no coloring, plain monospaced text. */
object PlainSyntaxHighlighter : SyntaxHighlighter {
    override fun highlight(code: String, language: String?): AnnotatedString =
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(code) }
        }
}

/**
 * Renders LaTeX math. Injectable; the default renders the raw source in
 * monospaced text. Mirrors the iOS `LatexRenderer` protocol. Implementations MUST
 * NOT use a WebView.
 */
interface LatexRenderer {
    @Composable
    fun Math(latex: String, inline: Boolean, modifier: Modifier)
}

/** Default renderer: shows the raw LaTeX as monospaced text (never crashes). */
object RawLatexRenderer : LatexRenderer {
    @Composable
    override fun Math(latex: String, inline: Boolean, modifier: Modifier) {
        val theme = LocalMarkdownTheme.current
        Text(
            text = latex,
            modifier = modifier,
            fontFamily = FontFamily.Monospace,
            color = theme.textPrimary,
        )
    }
}

/** Resolves a wiki-link `target` to a destination URL, or null to render as plain text. */
interface WikiLinkResolver {
    fun destination(target: String): String?

    /** Candidate targets for editor completion (empty by default). */
    fun completions(prefix: String): List<String> = emptyList()
}

/** Default resolver: wiki-links render as plain text (no destination). */
object PlainWikiLinkResolver : WikiLinkResolver {
    override fun destination(target: String): String? = null
}

/**
 * Loads and renders an embedded image. Injectable; the default renders an
 * accessible placeholder (the core stays dependency-free, so hosts inject a real
 * image loader such as Coil). Mirrors the iOS `EmbeddedImageProvider`.
 */
interface EmbeddedImageProvider {
    @Composable
    fun Image(source: String, contentDescription: String?, modifier: Modifier)
}

/** Default provider: an accessible placeholder box (no network dependency in core). */
object PlaceholderImageProvider : EmbeddedImageProvider {
    @Composable
    override fun Image(source: String, contentDescription: String?, modifier: Modifier) {
        val theme = LocalMarkdownTheme.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = contentDescription?.ifBlank { source } ?: source, color = theme.textTertiary)
        }
    }
}

/**
 * Provides an inline player for a provider (e.g. YouTube/Vimeo) video. Injectable;
 * when absent, provider videos open externally. Mirrors the iOS `VideoEmbedder`.
 * Any WebView-backed embedder lives in the host app, never in the core.
 *
 * @return true if the embedder handled the URL inline; false to fall back to
 *   external open.
 */
interface VideoEmbedder {
    @Composable
    fun Player(url: String, modifier: Modifier): Boolean
}

/**
 * Bundles the configured services. Suppliable directly to `MarkdownView` /
 * `MarkdownEditor` or via [LocalMarkdownServices]. Mirrors the iOS
 * `MarkdownServices` container. Every service has a sensible default.
 */
data class MarkdownServices(
    val syntaxHighlighter: SyntaxHighlighter = PlainSyntaxHighlighter,
    val latexRenderer: LatexRenderer = RawLatexRenderer,
    val wikiLinkResolver: WikiLinkResolver = PlainWikiLinkResolver,
    val imageProvider: EmbeddedImageProvider = PlaceholderImageProvider,
    val videoEmbedder: VideoEmbedder? = null,
) {
    companion object {
        val Default = MarkdownServices()
    }
}

/** Injects [MarkdownServices] through the composition. Mirrors the iOS environment. */
val LocalMarkdownServices = staticCompositionLocalOf { MarkdownServices.Default }
