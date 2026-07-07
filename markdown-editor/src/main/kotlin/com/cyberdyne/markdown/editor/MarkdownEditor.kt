package com.cyberdyne.markdown.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.engine.services.WikiLinkResolver
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/**
 * A native Markdown source editor with live syntax styling. The text remains
 * literal Markdown; formatting commands and a customizable toolbar mirror the iOS
 * `MarkdownEditor`.
 *
 * @param toolbar host-provided items; when null the [MarkdownToolbarItem.defaults] show.
 * @param showsToolbar set false to hide the toolbar entirely.
 */
@Composable
fun MarkdownEditor(
    state: MarkdownEditorState,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
    toolbar: List<MarkdownToolbarItem>? = null,
    showsToolbar: Boolean = true,
    wikiLinkResolver: WikiLinkResolver? = null,
) {
    val controller = remember(state) { MarkdownEditorController(state) }
    Column(modifier.fillMaxWidth().imePadding()) {
        if (showsToolbar) {
            EditorToolbar(toolbar ?: MarkdownToolbarItem.defaults, controller, theme)
        }
        if (wikiLinkResolver != null) WikiSuggestionBar(state, wikiLinkResolver, theme)
        val suppress = SpellcheckRegions.isSuppressed(state.value.text, state.value.selection.start)
        BasicTextField(
            value = state.value,
            onValueChange = { state.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp) // readable column
                .padding(8.dp)
                .onPreviewKeyEvent { event -> handleKey(event, state) },
            textStyle = TextStyle(color = theme.textPrimary, fontSize = theme.baseFontSize),
            visualTransformation = remember(theme) { MarkdownStyler(theme) },
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = !suppress),
            cursorBrush = SolidColor(theme.accent),
        )
    }
}

/** Suggestion chips shown while typing inside an unclosed `[[` wiki-link. */
@Composable
internal fun WikiSuggestionBar(state: MarkdownEditorState, resolver: WikiLinkResolver, theme: MarkdownTheme) {
    val text = state.value.text
    val cursor = state.value.selection.start
    val prefix = WikiCompletion.contextAt(text, cursor) ?: return
    val suggestions = remember(prefix) { resolver.completions(prefix) }.take(6)
    if (suggestions.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { s ->
            Text(
                s,
                color = theme.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.surface)
                    .clickable {
                        val (nt, nc) = WikiCompletion.insert(text, cursor, s)
                        state.value = androidx.compose.ui.text.input.TextFieldValue(nt, TextRange(nc))
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

internal fun handleKey(event: androidx.compose.ui.input.key.KeyEvent, state: MarkdownEditorState): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.Enter, Key.NumPadEnter -> {
            val v = state.value
            val r = EditOps.continueList(v.text, v.selection.min) ?: return false
            state.value = TextFieldValue(r.text, TextRange(r.selStart, r.selEnd))
            true
        }
        Key.Tab -> {
            val v = state.value
            val r = EditOps.indentLines(v.text, v.selection.min, v.selection.max, outdent = event.isShiftPressed)
            state.value = TextFieldValue(r.text, TextRange(r.selStart, r.selEnd))
            true
        }
        else -> false
    }
}

@Composable
internal fun EditorToolbar(items: List<MarkdownToolbarItem>, controller: MarkdownEditorController, theme: MarkdownTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item -> ToolbarEntry(item, controller, theme) }
    }
}

@Composable
private fun ToolbarEntry(item: MarkdownToolbarItem, controller: MarkdownEditorController, theme: MarkdownTheme) {
    when (item) {
        is MarkdownToolbarItem.Divider -> Box(
            Modifier.width(1.dp).height(20.dp).background(theme.border),
        )
        is MarkdownToolbarItem.Menu -> {
            var expanded by remember { mutableStateOf(false) }
            Box {
                ToolbarButton(item.label, theme) { expanded = true }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    item.items.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(glyph(sub)) },
                            onClick = { applyItem(sub, controller); expanded = false },
                        )
                    }
                }
            }
        }
        else -> {
            val bold = item is MarkdownToolbarItem.Bold
            ToolbarButton(glyph(item), theme, bold) { applyItem(item, controller) }
        }
    }
}

@Composable
private fun ToolbarButton(label: String, theme: MarkdownTheme, bold: Boolean = false, onClick: () -> Unit) {
    Text(
        text = label,
        color = theme.textPrimary,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun glyph(item: MarkdownToolbarItem): String = when (item) {
    MarkdownToolbarItem.Bold -> "B"
    MarkdownToolbarItem.Italic -> "I"
    MarkdownToolbarItem.Strikethrough -> "S"
    MarkdownToolbarItem.InlineCode -> "</>"
    MarkdownToolbarItem.BulletList -> "•"
    MarkdownToolbarItem.TaskList -> "☑"
    MarkdownToolbarItem.Quote -> "❝"
    MarkdownToolbarItem.Link -> "🔗"
    MarkdownToolbarItem.Indent -> "→"
    MarkdownToolbarItem.Outdent -> "←"
    MarkdownToolbarItem.Divider -> ""
    is MarkdownToolbarItem.Heading -> "H${item.level}"
    is MarkdownToolbarItem.Menu -> item.label
    is MarkdownToolbarItem.Custom -> item.icon.ifEmpty { item.label }
}

private fun applyItem(item: MarkdownToolbarItem, c: MarkdownEditorController) {
    when (item) {
        MarkdownToolbarItem.Bold -> c.toggleBold()
        MarkdownToolbarItem.Italic -> c.toggleItalic()
        MarkdownToolbarItem.Strikethrough -> c.toggleStrikethrough()
        MarkdownToolbarItem.InlineCode -> c.toggleInlineCode()
        MarkdownToolbarItem.BulletList -> c.toggleBulletList()
        MarkdownToolbarItem.TaskList -> c.toggleTaskList()
        MarkdownToolbarItem.Quote -> c.toggleQuote()
        MarkdownToolbarItem.Link -> c.insertLink("https://")
        MarkdownToolbarItem.Indent -> c.indent()
        MarkdownToolbarItem.Outdent -> c.outdent()
        is MarkdownToolbarItem.Heading -> c.setHeading(item.level)
        is MarkdownToolbarItem.Custom -> item.action(c)
        MarkdownToolbarItem.Divider, is MarkdownToolbarItem.Menu -> {}
    }
}
