package com.cyberdyne.markdown.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/**
 * A continuous "Live" Markdown editor: inline formatting is rendered live and
 * syntax markers collapse to zero width off the cursor's line (reveal-on-active-
 * line), while the underlying text stays literal Markdown. Mirrors the iOS
 * continuous live editor's text behavior.
 */
@Composable
fun MarkdownLiveEditor(
    state: MarkdownEditorState,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
    toolbar: List<MarkdownToolbarItem>? = null,
    showsToolbar: Boolean = true,
) {
    val controller = remember(state) { MarkdownEditorController(state) }
    Column(modifier.fillMaxWidth()) {
        if (showsToolbar) EditorToolbar(toolbar ?: MarkdownToolbarItem.defaults, controller, theme)
        val cursor = state.value.selection.start
        val transformation = remember(theme, state.value.text, cursor) { LiveTransformation(theme, cursor) }
        BasicTextField(
            value = state.value,
            onValueChange = { state.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .onPreviewKeyEvent { event -> handleKey(event, state, controller) },
            textStyle = TextStyle(color = theme.textPrimary, fontSize = theme.baseFontSize),
            visualTransformation = transformation,
            cursorBrush = SolidColor(theme.accent),
        )
    }
}

/**
 * Hides Markdown markers off the active line and styles the visible content.
 * Offset mapping is provided by the unit-tested [OffsetMap].
 */
internal class LiveTransformation(private val theme: MarkdownTheme, private val cursor: Int) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val hidden = LiveMarkers.hiddenRanges(raw, cursor)
        val map = OffsetMap(hidden)
        val visible = map.transform(raw)
        val styled = buildAnnotatedString {
            append(visible)
            applyMappedStyles(raw, map, visible.length)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = map.originalToTransformed(offset).coerceIn(0, visible.length)
            override fun transformedToOriginal(offset: Int) = map.transformedToOriginal(offset).coerceIn(0, raw.length)
        }
        return TransformedText(styled, mapping)
    }

    private fun AnnotatedString.Builder.applyMappedStyles(raw: String, map: OffsetMap, visLen: Int) {
        fun style(sOrig: Int, eOrig: Int, span: SpanStyle) {
            val s = map.originalToTransformed(sOrig).coerceIn(0, visLen)
            val e = map.originalToTransformed(eOrig).coerceIn(s, visLen)
            if (s < e) addStyle(span, s, e)
        }
        // Headings: whole-line bold + scaled size.
        var lineStart = 0
        for (line in raw.split("\n")) {
            val m = HEADING.find(line)
            if (m != null) {
                val size = when (m.groupValues[1].length) { 1 -> 1.6.em; 2 -> 1.4.em; 3 -> 1.2.em; else -> 1.05.em }
                style(lineStart, lineStart + line.length, SpanStyle(fontWeight = FontWeight.Bold, fontSize = size, color = theme.textPrimary))
            }
            lineStart += line.length + 1
        }
        BOLD.findAll(raw).forEach { style(it.range.first + 2, it.range.last - 1, SpanStyle(fontWeight = FontWeight.Bold)) }
        STRIKE.findAll(raw).forEach { style(it.range.first + 2, it.range.last - 1, SpanStyle(textDecoration = TextDecoration.LineThrough)) }
        ITALIC.findAll(raw).forEach { style(it.range.first + 1, it.range.last, SpanStyle(fontStyle = FontStyle.Italic)) }
        CODE.findAll(raw).forEach {
            style(it.range.first + 1, it.range.last, SpanStyle(fontFamily = FontFamily.Monospace, background = theme.inlineCodeBackground))
        }
    }

    private companion object {
        val HEADING = Regex("^(#{1,6}) ")
        val BOLD = Regex("\\*\\*[^*\\n]+\\*\\*")
        val STRIKE = Regex("~~[^~\\n]+~~")
        val ITALIC = Regex("(?<!\\*)\\*(?!\\*)[^*\\n]+?\\*(?!\\*)")
        val CODE = Regex("`[^`\\n]+`")
    }
}
