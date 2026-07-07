package com.cyberdyne.markdown.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/**
 * Live syntax styling for the Markdown source editor. Applies heading/emphasis/
 * code styling while KEEPING every character (raw Markdown stays editable), so the
 * offset mapping is the identity. Mirrors the iOS editor's "live styling without
 * converting to rich text".
 */
class MarkdownStyler(private val theme: MarkdownTheme) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val styled = buildAnnotatedString {
            append(raw)
            styleHeadings(raw)
            styleFencedCode(raw)
            styleSpan(raw, BOLD) { SpanStyle(fontWeight = FontWeight.Bold) }
            styleSpan(raw, STRIKE) { SpanStyle(textDecoration = TextDecoration.LineThrough) }
            styleSpan(raw, ITALIC_STAR) { SpanStyle(fontStyle = FontStyle.Italic) }
            styleSpan(raw, ITALIC_USCORE) { SpanStyle(fontStyle = FontStyle.Italic) }
            styleSpan(raw, CODE) {
                SpanStyle(fontFamily = FontFamily.Monospace, background = theme.inlineCodeBackground, color = theme.inlineCodeText)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.styleHeadings(raw: String) {
        var offset = 0
        for (line in raw.split("\n")) {
            val m = HEADING.find(line)
            if (m != null) {
                val level = m.groupValues[1].length
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontSize = headingSize(level), color = theme.textPrimary),
                    offset, offset + line.length,
                )
            }
            offset += line.length + 1
        }
    }

    private fun AnnotatedString.Builder.styleFencedCode(raw: String) {
        FENCE_BLOCK.findAll(raw).forEach {
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = theme.codeText), it.range.first, it.range.last + 1)
        }
    }

    private inline fun AnnotatedString.Builder.styleSpan(raw: String, regex: Regex, style: () -> SpanStyle) {
        regex.findAll(raw).forEach { addStyle(style(), it.range.first, it.range.last + 1) }
    }

    private fun headingSize(level: Int): TextUnit = when (level) {
        1 -> 1.6.em
        2 -> 1.4.em
        3 -> 1.2.em
        else -> 1.05.em
    }

    private companion object {
        val HEADING = Regex("^(#{1,6})\\s+.*$")
        val BOLD = Regex("\\*\\*[^*\\n]+\\*\\*")
        val STRIKE = Regex("~~[^~\\n]+~~")
        val ITALIC_STAR = Regex("(?<!\\*)\\*(?!\\*)[^*\\n]+?\\*(?!\\*)")
        val ITALIC_USCORE = Regex("(?<![A-Za-z0-9_])_[^_\\n]+_(?![A-Za-z0-9_])")
        val CODE = Regex("`[^`\\n]+`")
        val FENCE_BLOCK = Regex("```[\\s\\S]*?```")
    }
}
