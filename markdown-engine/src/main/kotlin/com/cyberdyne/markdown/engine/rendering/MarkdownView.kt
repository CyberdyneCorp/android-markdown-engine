package com.cyberdyne.markdown.engine.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.engine.Markdown
import com.cyberdyne.markdown.engine.code.LanguageAliases
import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.model.BlockKind
import com.cyberdyne.markdown.engine.model.BlockNode
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.model.MarkdownDocument
import com.cyberdyne.markdown.engine.model.SourceRange
import com.cyberdyne.markdown.engine.model.TableAlignment
import com.cyberdyne.markdown.engine.model.TableCell
import com.cyberdyne.markdown.engine.services.LocalMarkdownServices
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import com.cyberdyne.markdown.engine.video.VideoKind
import com.cyberdyne.markdown.engine.video.VideoUrls

/** A host-provided renderer overriding the built-in rendering of a [BlockKind]. */
typealias MarkdownBlockRenderer = @Composable (BlockNode, MarkdownTheme) -> Unit

/** Internal bag of rendering inputs, threaded through the block composables. */
internal class RenderScope(
    val theme: MarkdownTheme,
    val config: MarkdownConfiguration,
    val services: MarkdownServices,
    val onLinkClick: ((String) -> Unit)?,
    val onCheckboxToggle: (SourceRange, Boolean) -> Unit,
    val blockRenderers: Map<BlockKind, MarkdownBlockRenderer>,
) {
    val baseStyle: TextStyle get() = TextStyle(color = theme.textPrimary, fontSize = theme.baseFontSize)
}

/**
 * Renders raw Markdown [markdown] natively with Jetpack Compose. No WebView, no
 * JavaScript. Mirrors the iOS `MarkdownView`.
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
    configuration: MarkdownConfiguration = MarkdownConfiguration.Default,
    services: MarkdownServices = LocalMarkdownServices.current,
    onLinkClick: ((String) -> Unit)? = null,
    onCheckboxToggle: (SourceRange, Boolean) -> Unit = { _, _ -> },
    blockRenderers: Map<BlockKind, MarkdownBlockRenderer> = emptyMap(),
) {
    val document = remember(markdown, configuration) { Markdown.parse(markdown, configuration) }
    MarkdownView(document, modifier, theme, configuration, services, onLinkClick, onCheckboxToggle, blockRenderers)
}

/** Renders a pre-parsed [document] without re-parsing. */
@Composable
fun MarkdownView(
    document: MarkdownDocument,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
    configuration: MarkdownConfiguration = MarkdownConfiguration.Default,
    services: MarkdownServices = LocalMarkdownServices.current,
    onLinkClick: ((String) -> Unit)? = null,
    onCheckboxToggle: (SourceRange, Boolean) -> Unit = { _, _ -> },
    blockRenderers: Map<BlockKind, MarkdownBlockRenderer> = emptyMap(),
) {
    val scope = RenderScope(theme, configuration, services, onLinkClick, onCheckboxToggle, blockRenderers)
    val content: @Composable () -> Unit = {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(theme.paragraphSpacing),
        ) {
            for (block in document.blocks) RenderBlock(block, scope)
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMarkdownTheme provides theme,
        LocalMarkdownServices provides services,
        content = content,
    )
}

@Composable
internal fun RenderBlock(block: BlockNode, scope: RenderScope) {
    scope.blockRenderers[block.kind]?.let { override ->
        override(block, scope.theme)
        return
    }
    when (block) {
        is BlockNode.Heading -> HeadingView(block, scope)
        is BlockNode.Paragraph -> ParagraphView(block, scope)
        is BlockNode.ThematicBreak -> HorizontalDivider(color = scope.theme.border)
        is BlockNode.CodeBlock -> CodeBlockView(block.language, block.content, scope)
        is BlockNode.Mermaid -> CodeBlockView("mermaid", block.source, scope) // native renderer lands in a later slice
        is BlockNode.BlockMath -> scope.services.latexRenderer.Math(block.body, inline = false, Modifier.fillMaxWidth())
        is BlockNode.BlockQuote -> BlockQuoteView(block.children, scope)
        is BlockNode.Callout -> CalloutView(block, scope)
        is BlockNode.ListBlock -> ListView(block, scope)
        is BlockNode.Table -> TableView(block, scope)
        is BlockNode.HtmlBlock -> Text(block.content, fontFamily = FontFamily.Monospace, color = scope.theme.textSecondary)
        is BlockNode.FootnoteDefinition -> FootnoteDefinitionView(block, scope)
    }
}

@Composable
private fun HeadingView(block: BlockNode.Heading, scope: RenderScope) {
    val style = scope.baseStyle.copy(
        fontSize = scope.theme.headingSize(block.level),
        fontWeight = FontWeight.Bold,
    )
    MarkdownText(
        text = buildInlineString(block.content, scope.theme, scope.services),
        baseStyle = style,
        modifier = Modifier.semantics { heading() },
        onLinkClick = scope.onLinkClick,
    )
}

@Composable
private fun ParagraphView(block: BlockNode.Paragraph, scope: RenderScope) {
    val media = paragraphMedia(block)
    if (media != null) {
        MediaView(media, scope)
        return
    }
    MarkdownText(
        text = buildInlineString(block.content, scope.theme, scope.services),
        baseStyle = scope.baseStyle,
        onLinkClick = scope.onLinkClick,
    )
}

// --- Code ---

@Composable
private fun CodeBlockView(language: String?, code: String, scope: RenderScope) {
    val theme = scope.theme
    val clipboard = LocalClipboardManager.current
    val highlighted = remember(code, language) {
        scope.services.syntaxHighlighter.highlight(code, LanguageAliases.normalize(language))
    }
    val lines = remember(code) { code.split("\n") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(theme.codeBackground)
            .padding(8.dp),
    ) {
        if (scope.config.codeCopyEnabled || language != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(language ?: "", color = theme.textTertiary, fontSize = scope.theme.baseFontSize)
                if (scope.config.codeCopyEnabled) {
                    Text(
                        "Copy",
                        color = theme.accent,
                        modifier = Modifier.clickable { clipboard.setText(AnnotatedString(code)) },
                    )
                }
            }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            if (scope.config.showCodeLineNumbers) {
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    for (n in 1..lines.size) {
                        Text("$n", color = theme.textTertiary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Text(highlighted, color = theme.codeText, fontFamily = FontFamily.Monospace)
        }
    }
}

// --- Quote / callout / footnote ---

@Composable
private fun BlockQuoteView(children: List<BlockNode>, scope: RenderScope) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .background(scope.theme.blockQuoteBar),
        ) { Text(" ") }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(scope.theme.paragraphSpacing),
        ) {
            for (child in children) RenderBlock(child, scope)
        }
    }
}

@Composable
private fun CalloutView(block: BlockNode.Callout, scope: RenderScope) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(scope.theme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(scope.theme.paragraphSpacing),
    ) {
        val header = block.kindLabel.replaceFirstChar { it.uppercase() }
        Text(header, color = scope.theme.accent, fontWeight = FontWeight.Bold)
        if (block.title.isNotEmpty()) {
            MarkdownText(buildInlineString(block.title, scope.theme, scope.services), scope.baseStyle, onLinkClick = scope.onLinkClick)
        }
        for (child in block.children) RenderBlock(child, scope)
    }
}

@Composable
private fun FootnoteDefinitionView(block: BlockNode.FootnoteDefinition, scope: RenderScope) {
    Row {
        Text("[${block.identifier}] ", color = scope.theme.textSecondary, fontWeight = FontWeight.Bold)
        Column { for (child in block.children) RenderBlock(child, scope) }
    }
}

// --- Lists ---

@Composable
private fun ListView(block: BlockNode.ListBlock, scope: RenderScope) {
    Column(
        verticalArrangement = Arrangement.spacedBy(if (block.tight) 2.dp else scope.theme.paragraphSpacing),
    ) {
        block.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                when (item.checked) {
                    null -> Text(
                        if (block.ordered) "${block.start + index}. " else "•  ",
                        color = scope.theme.textSecondary,
                    )
                    else -> Checkbox(
                        checked = item.checked,
                        onCheckedChange = if (scope.config.interactiveCheckboxes) {
                            { scope.onCheckboxToggle(item.range, it) }
                        } else {
                            null
                        },
                        enabled = scope.config.interactiveCheckboxes,
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (child in item.children) RenderBlock(child, scope)
                }
            }
        }
    }
}

// --- Table ---

@Composable
private fun TableView(block: BlockNode.Table, scope: RenderScope) {
    val theme = scope.theme
    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Column {
            TableRow(block.header, block.alignments, scope, header = true, stripe = false)
            block.rows.forEachIndexed { index, row ->
                TableRow(row, block.alignments, scope, header = false, stripe = index % 2 == 1)
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<TableCell>,
    alignments: List<TableAlignment>,
    scope: RenderScope,
    header: Boolean,
    stripe: Boolean,
) {
    val theme = scope.theme
    Row(modifier = Modifier.background(if (stripe) theme.tableStripe else theme.background)) {
        cells.forEachIndexed { i, cell ->
            val align = when (alignments.getOrNull(i) ?: TableAlignment.NONE) {
                TableAlignment.CENTER -> TextAlign.Center
                TableAlignment.RIGHT -> TextAlign.End
                else -> TextAlign.Start
            }
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .padding(0.5.dp)
                    .background(theme.background)
                    .padding(8.dp),
            ) {
                MarkdownText(
                    text = buildInlineString(cell, theme, scope.services),
                    baseStyle = scope.baseStyle.copy(
                        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                        textAlign = align,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onLinkClick = scope.onLinkClick,
                )
            }
        }
    }
}

// --- Media (images & video) ---

private sealed interface Media {
    data class Img(val image: InlineNode.Image) : Media
    data class LinkedImg(val destination: String, val image: InlineNode.Image) : Media
}

private fun paragraphMedia(p: BlockNode.Paragraph): Media? {
    val meaningful = p.content.filterNot { it is InlineNode.Text && it.content.isBlank() }
    (meaningful.singleOrNull() as? InlineNode.Image)?.let { return Media.Img(it) }
    (meaningful.singleOrNull() as? InlineNode.Link)?.let { link ->
        val inner = link.children.filterNot { it is InlineNode.Text && it.content.isBlank() }
        (inner.singleOrNull() as? InlineNode.Image)?.let { img -> return Media.LinkedImg(link.destination, img) }
    }
    return null
}

@Composable
private fun MediaView(media: Media, scope: RenderScope) {
    when (media) {
        is Media.Img -> {
            if (VideoUrls.classify(media.image.source) == VideoKind.DIRECT_FILE) {
                InlineVideoPlayer(media.image.source)
            } else {
                scope.services.imageProvider.Image(media.image.source, media.image.alt, Modifier)
            }
        }
        is Media.LinkedImg -> {
            val kind = VideoUrls.classify(media.destination)
            if (kind == VideoKind.NOT_VIDEO) {
                val uriHandler = LocalUriHandler.current
                Box(
                    modifier = Modifier.clickable {
                        scope.onLinkClick?.invoke(media.destination) ?: runCatching { uriHandler.openUri(media.destination) }
                    },
                ) { scope.services.imageProvider.Image(media.image.source, media.image.alt, Modifier) }
            } else {
                VideoEmbed(media.image, media.destination, kind, scope)
            }
        }
    }
}

@Composable
private fun VideoEmbed(thumbnail: InlineNode.Image, videoUrl: String, kind: VideoKind, scope: RenderScope) {
    var active by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val embedder = scope.services.videoEmbedder

    if (active && kind == VideoKind.DIRECT_FILE) {
        InlineVideoPlayer(videoUrl)
        return
    }
    if (active && kind == VideoKind.PROVIDER && embedder != null) {
        val handled = embedder.Player(videoUrl, Modifier.fillMaxWidth())
        if (handled) return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    kind == VideoKind.DIRECT_FILE -> active = true
                    kind == VideoKind.PROVIDER && embedder != null -> active = true
                    else -> scope.onLinkClick?.invoke(videoUrl) ?: runCatching { uriHandler.openUri(videoUrl) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        scope.services.imageProvider.Image(thumbnail.source, thumbnail.alt, Modifier)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(scope.theme.surface)
                .widthIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text("▶", color = scope.theme.textPrimary) }
    }
}
