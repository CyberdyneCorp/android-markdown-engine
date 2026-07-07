package com.cyberdyne.markdown.editor

/**
 * An item in the [MarkdownEditor] toolbar. Mirrors the iOS `MarkdownToolbarItem`
 * cases. Built-in items map to [MarkdownEditorController] commands; [Custom] runs
 * a host action receiving the controller.
 */
sealed interface MarkdownToolbarItem {
    data object Bold : MarkdownToolbarItem
    data object Italic : MarkdownToolbarItem
    data object Strikethrough : MarkdownToolbarItem
    data object InlineCode : MarkdownToolbarItem
    data object BulletList : MarkdownToolbarItem
    data object TaskList : MarkdownToolbarItem
    data object Quote : MarkdownToolbarItem
    data object Link : MarkdownToolbarItem
    data object Indent : MarkdownToolbarItem
    data object Outdent : MarkdownToolbarItem
    data object Divider : MarkdownToolbarItem

    data class Heading(val level: Int) : MarkdownToolbarItem

    data class Menu(val label: String, val items: List<MarkdownToolbarItem>) : MarkdownToolbarItem

    /**
     * A host-defined item. [icon] is a short glyph/emoji shown on the button;
     * [action] receives the editor's [MarkdownEditorController].
     */
    data class Custom(
        val id: String,
        val icon: String,
        val label: String,
        val action: (MarkdownEditorController) -> Unit,
    ) : MarkdownToolbarItem

    companion object {
        /** The default toolbar shown when no `toolbar` argument is supplied. */
        val defaults: List<MarkdownToolbarItem> = listOf(
            Bold, Italic, Strikethrough, InlineCode, Divider,
            Heading(1), Heading(2), Divider,
            BulletList, TaskList, Quote, Link, Divider,
            Outdent, Indent,
        )
    }
}
