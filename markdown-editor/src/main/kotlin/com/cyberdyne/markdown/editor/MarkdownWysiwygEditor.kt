package com.cyberdyne.markdown.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/** Block-stack document state for [MarkdownWysiwygEditor]. Source stays Markdown. */
@Stable
class WysiwygState(initial: String) {
    val blocks: SnapshotStateList<String> = mutableStateListOf<String>().apply { addAll(WysiwygOps.toBlocks(initial)) }

    /** The current document serialized back to Markdown. */
    val markdown: String get() = WysiwygOps.join(blocks)
}

@Composable
fun rememberWysiwygState(initial: String): WysiwygState = remember { WysiwygState(initial) }

private val INSERT_TEMPLATES = listOf(
    "Paragraph" to "New paragraph.",
    "Heading" to "## Heading",
    "Bullet list" to "- item",
    "Task" to "- [ ] task",
    "Quote" to "> quote",
    "Code" to "```\ncode\n```",
    "Table" to "| a | b |\n|---|---|\n| 1 | 2 |",
    "Math" to "$$\nE = mc^2\n$$",
    "Diagram" to "```mermaid\nflowchart TD\n    A --> B\n```",
)

/**
 * A block-based WYSIWYG editor: the document is a stack of blocks rendered with
 * the Markdown renderer (no raw syntax) and tapped to edit; the underlying
 * document stays Markdown. Mirrors the iOS WYSIWYG editor.
 */
@Composable
fun MarkdownWysiwygEditor(
    state: WysiwygState,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
) {
    var editing by remember { mutableIntStateOf(-1) }
    LazyColumn(modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(state.blocks) { index, block ->
            BlockCard(
                block = block,
                editing = editing == index,
                theme = theme,
                onToggleEdit = { editing = if (editing == index) -1 else index },
                onChange = { state.blocks[index] = it },
                onMoveUp = { if (index > 0) state.blocks.move(index, index - 1) },
                onMoveDown = { if (index < state.blocks.size - 1) state.blocks.move(index, index + 1) },
                onDelete = { state.blocks.removeAt(index); editing = -1 },
            )
        }
        item { InsertMenu(onInsert = { state.blocks.add(it) }) }
    }
}

private fun SnapshotStateList<String>.move(from: Int, to: Int) {
    add(to, removeAt(from))
}

@Composable
private fun BlockCard(
    block: String,
    editing: Boolean,
    theme: MarkdownTheme,
    onToggleEdit: () -> Unit,
    onChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton("▲", onMoveUp)
            TextButton("▼", onMoveDown)
            TextButton(if (editing) "✓" else "✎", onToggleEdit)
            TextButton("🗑", onDelete)
        }
        if (editing) {
            when {
                TableGrid.fromMarkdown(block) != null -> TableGridEditor(block, onChange, theme)
                block.trimStart().startsWith("```") -> CodeBlockEditor(block, onChange, theme)
                else -> OutlinedTextField(
                    value = block,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = theme.textPrimary),
                )
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                MarkdownView(markdown = block, theme = theme)
            }
        }
    }
}

@Composable
private fun InsertMenu(onInsert: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton("＋ Insert block") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            INSERT_TEMPLATES.forEach { (label, template) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onInsert(template); expanded = false })
            }
        }
    }
}

@Composable
private fun TextButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

/** Visual grid editor for a GFM table block. */
@Composable
private fun TableGridEditor(markdown: String, onChange: (String) -> Unit, theme: MarkdownTheme) {
    var grid by remember(markdown) { mutableStateOf(TableGrid.fromMarkdown(markdown)!!) }
    fun update(g: TableGrid) { grid = g; onChange(g.toMarkdown()) }
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            grid.header.forEachIndexed { c, h ->
                OutlinedTextField(h, { update(grid.setHeader(c, it)) }, Modifier.weight(1f), label = { Text("col ${c + 1}") })
            }
        }
        grid.rows.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { c, v ->
                    OutlinedTextField(v, { update(grid.setCell(r, c, it)) }, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton("+ Row") { update(grid.addRow()) }
            TextButton("+ Column") { update(grid.addColumn()) }
        }
    }
}

/** Visual editor for a fenced code block: language field + monospace body. */
@Composable
private fun CodeBlockEditor(markdown: String, onChange: (String) -> Unit, theme: MarkdownTheme) {
    val lines = markdown.trim().lines()
    var lang by remember(markdown) { mutableStateOf(lines.firstOrNull()?.trimStart('`', '~')?.trim().orEmpty()) }
    var code by remember(markdown) {
        mutableStateOf(lines.drop(1).dropLastWhile { it.trimStart().startsWith("```") || it.trimStart().startsWith("~~~") }.joinToString("\n"))
    }
    fun emit() = onChange("```$lang\n$code\n```")
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(lang, { lang = it; emit() }, label = { Text("language") })
        OutlinedTextField(
            value = code,
            onValueChange = { code = it; emit() },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = theme.textPrimary),
        )
    }
}
