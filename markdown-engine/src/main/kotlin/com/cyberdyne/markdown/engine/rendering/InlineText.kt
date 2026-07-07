package com.cyberdyne.markdown.engine.rendering

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.cyberdyne.markdown.engine.model.InlineNode
import com.cyberdyne.markdown.engine.services.MarkdownServices
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

private const val TAG_URL = "url"

/** Builds a styled [AnnotatedString] from inline nodes, tagging links for click handling. */
internal fun buildInlineString(
    nodes: List<InlineNode>,
    theme: MarkdownTheme,
    services: MarkdownServices,
): AnnotatedString = buildAnnotatedString {
    appendInlines(nodes, theme, services)
}

private fun AnnotatedString.Builder.appendInlines(
    nodes: List<InlineNode>,
    theme: MarkdownTheme,
    services: MarkdownServices,
) {
    for (node in nodes) appendInline(node, theme, services)
}

private fun AnnotatedString.Builder.appendInline(
    node: InlineNode,
    theme: MarkdownTheme,
    services: MarkdownServices,
) {
    when (node) {
        is InlineNode.Text -> append(node.content)
        is InlineNode.Html -> append(node.content)
        is InlineNode.LineBreak -> append("\n")
        is InlineNode.Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            appendInlines(node.children, theme, services)
        }
        is InlineNode.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            appendInlines(node.children, theme, services)
        }
        is InlineNode.Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            appendInlines(node.children, theme, services)
        }
        is InlineNode.Code -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = theme.inlineCodeBackground,
                color = theme.inlineCodeText,
            ),
        ) { append(node.content) }
        is InlineNode.InlineMath -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(node.body)
        }
        is InlineNode.Link -> appendLink(node.destination, theme) {
            appendInlines(node.children, theme, services)
        }
        is InlineNode.Autolink -> appendLink(node.url, theme) { append(node.url.removePrefix("mailto:")) }
        is InlineNode.WikiLink -> {
            val dest = services.wikiLinkResolver.destination(node.target)
            val label = node.display ?: node.target
            if (dest != null) appendLink(dest, theme) { append(label) }
            else withStyle(SpanStyle(color = theme.accent)) { append(label) }
        }
        is InlineNode.FootnoteReference -> withStyle(
            SpanStyle(color = theme.accent, baselineShift = BaselineShift.Superscript),
        ) { append(node.identifier) }
        is InlineNode.Image -> append(node.alt) // block-level images are rendered separately
    }
}

private inline fun AnnotatedString.Builder.appendLink(
    destination: String,
    theme: MarkdownTheme,
    content: AnnotatedString.Builder.() -> Unit,
) {
    pushStringAnnotation(TAG_URL, destination)
    withStyle(SpanStyle(color = theme.linkColor, textDecoration = TextDecoration.Underline)) { content() }
    pop()
}

/**
 * Renders an inline [AnnotatedString] with clickable links. A link tap invokes
 * [onLinkClick]; the default opens the URL externally via the platform URI handler.
 */
@Composable
internal fun MarkdownText(
    text: AnnotatedString,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = text,
        modifier = modifier,
        style = baseStyle,
    ) { offset ->
        text.getStringAnnotations(TAG_URL, offset, offset).firstOrNull()?.let { ann ->
            if (onLinkClick != null) onLinkClick(ann.item) else runCatching { uriHandler.openUri(ann.item) }
        }
    }
}
