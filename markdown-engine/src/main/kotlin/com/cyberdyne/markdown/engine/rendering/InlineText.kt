package com.cyberdyne.markdown.engine.rendering

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/** A styled inline string plus the inline-content composables it references (images, math). */
internal class InlineContent(val text: AnnotatedString, val inline: Map<String, InlineTextContent>)

/**
 * Builds a styled [AnnotatedString] from inline nodes. Links use `LinkAnnotation`
 * (handling taps via [onLinkClick]); inline images and inline math render as
 * `InlineTextContent`.
 */
internal fun buildInline(
    nodes: List<InlineNode>,
    theme: MarkdownTheme,
    services: MarkdownServices,
    onLinkClick: (String) -> Unit,
): InlineContent {
    val inline = LinkedHashMap<String, InlineTextContent>()
    val counter = intArrayOf(0)
    val text = buildAnnotatedString { appendInlines(nodes, theme, services, onLinkClick, inline, counter) }
    return InlineContent(text, inline)
}

private fun AnnotatedString.Builder.appendInlines(
    nodes: List<InlineNode>,
    theme: MarkdownTheme,
    services: MarkdownServices,
    onLinkClick: (String) -> Unit,
    inline: MutableMap<String, InlineTextContent>,
    counter: IntArray,
) {
    for (node in nodes) appendInline(node, theme, services, onLinkClick, inline, counter)
}

private fun AnnotatedString.Builder.appendInline(
    node: InlineNode,
    theme: MarkdownTheme,
    services: MarkdownServices,
    onLinkClick: (String) -> Unit,
    inline: MutableMap<String, InlineTextContent>,
    counter: IntArray,
) {
    fun children(list: List<InlineNode>) = appendInlines(list, theme, services, onLinkClick, inline, counter)
    when (node) {
        is InlineNode.Text -> append(node.content)
        is InlineNode.Html -> append(node.content)
        is InlineNode.LineBreak -> append("\n")
        is InlineNode.Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { children(node.children) }
        is InlineNode.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { children(node.children) }
        is InlineNode.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { children(node.children) }
        is InlineNode.Code -> withStyle(
            SpanStyle(fontFamily = FontFamily.Monospace, background = theme.inlineCodeBackground, color = theme.inlineCodeText),
        ) { append(node.content) }
        is InlineNode.Link -> link(node.destination, theme, onLinkClick) { children(node.children) }
        is InlineNode.Autolink -> link(node.url, theme, onLinkClick) { append(node.url.removePrefix("mailto:")) }
        is InlineNode.WikiLink -> {
            val dest = services.wikiLinkResolver.destination(node.target)
            val label = node.display ?: node.target
            if (dest != null) link(dest, theme, onLinkClick) { append(label) }
            else withStyle(SpanStyle(color = theme.accent)) { append(label) }
        }
        is InlineNode.FootnoteReference -> withStyle(
            SpanStyle(color = theme.accent, baselineShift = BaselineShift.Superscript),
        ) { append(node.identifier) }
        is InlineNode.InlineMath -> {
            val id = "m${counter[0]++}"
            appendInlineContent(id, node.body)
            val width = (node.body.length.coerceAtLeast(1) * 0.55f).em
            inline[id] = InlineTextContent(Placeholder(width, 1.4.em, PlaceholderVerticalAlign.Center)) {
                services.latexRenderer.Math(node.body, inline = true, modifier = Modifier)
            }
        }
        is InlineNode.Image -> {
            val id = "i${counter[0]++}"
            appendInlineContent(id, node.alt)
            inline[id] = InlineTextContent(Placeholder(8.em, 6.em, PlaceholderVerticalAlign.Center)) {
                Box(Modifier.fillMaxSize()) { services.imageProvider.Image(node.source, node.alt, Modifier.fillMaxSize()) }
            }
        }
    }
}

private fun AnnotatedString.Builder.link(
    destination: String,
    theme: MarkdownTheme,
    onLinkClick: (String) -> Unit,
    content: AnnotatedString.Builder.() -> Unit,
) {
    val annotation = LinkAnnotation.Clickable(
        tag = destination,
        styles = TextLinkStyles(SpanStyle(color = theme.linkColor, textDecoration = TextDecoration.Underline)),
        linkInteractionListener = LinkInteractionListener { onLinkClick(destination) },
    )
    withLink(annotation) { content() }
}

/** Renders an inline [InlineContent]; link taps are handled by their `LinkAnnotation`. */
@Composable
internal fun MarkdownText(content: InlineContent, baseStyle: TextStyle, modifier: Modifier = Modifier) {
    Text(text = content.text, modifier = modifier, style = baseStyle, inlineContent = content.inline)
}
