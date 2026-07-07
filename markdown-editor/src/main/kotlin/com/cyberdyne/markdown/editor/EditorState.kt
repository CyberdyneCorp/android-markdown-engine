package com.cyberdyne.markdown.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Editable Markdown source state for [MarkdownEditor]. The underlying text is
 * always literal Markdown (e.g. `**bold**`). Mirrors the iOS editor's
 * `text: Binding<String>` while carrying selection for formatting commands.
 */
@Stable
class MarkdownEditorState(initial: String = "") {
    var value by mutableStateOf(TextFieldValue(initial))

    /** The current Markdown source text. */
    val text: String get() = value.text
}

@Composable
fun rememberMarkdownEditorState(initial: String = ""): MarkdownEditorState =
    remember { MarkdownEditorState(initial) }

/**
 * Applies formatting commands to a [MarkdownEditorState]. Mirrors the iOS
 * `MarkdownEditorController`. Every command keeps the document as literal Markdown.
 */
class MarkdownEditorController(internal val state: MarkdownEditorState) {

    private fun apply(op: (String, Int, Int) -> EditResult?) {
        val v = state.value
        val r = op(v.text, v.selection.min, v.selection.max) ?: return
        state.value = TextFieldValue(r.text, TextRange(r.selStart, r.selEnd))
    }

    fun toggleBold() = apply { t, s, e -> EditOps.wrapSelection(t, s, e, "**") }
    fun toggleItalic() = apply { t, s, e -> EditOps.wrapSelection(t, s, e, "*") }
    fun toggleStrikethrough() = apply { t, s, e -> EditOps.wrapSelection(t, s, e, "~~") }
    fun toggleInlineCode() = apply { t, s, e -> EditOps.wrapSelection(t, s, e, "`") }
    fun setHeading(level: Int) = apply { t, s, e -> EditOps.setHeading(t, s, e, level) }
    fun toggleBulletList() = apply { t, s, e -> EditOps.toggleLinePrefix(t, s, e, "- ") }
    fun toggleTaskList() = apply { t, s, e -> EditOps.toggleLinePrefix(t, s, e, "- [ ] ") }
    fun toggleQuote() = apply { t, s, e -> EditOps.toggleLinePrefix(t, s, e, "> ") }
    fun insertLink(url: String) = apply { t, s, e -> EditOps.insertLink(t, s, e, url) }
    fun indent() = apply { t, s, e -> EditOps.indentLines(t, s, e, outdent = false) }
    fun outdent() = apply { t, s, e -> EditOps.indentLines(t, s, e, outdent = true) }
    fun toggleTask() = apply { t, s, _ -> EditOps.toggleTask(t, s) }

    fun toggleCodeBlock() = apply { t, s, e ->
        val sel = t.substring(s, e)
        EditResult(t.substring(0, s) + "```\n" + sel + "\n```" + t.substring(e), s + 4, e + 4)
    }
}
