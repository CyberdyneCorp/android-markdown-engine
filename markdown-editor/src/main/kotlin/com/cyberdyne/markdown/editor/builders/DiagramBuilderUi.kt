package com.cyberdyne.markdown.editor.builders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cyberdyne.markdown.engine.mermaid.MermaidView
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme
import com.cyberdyne.markdown.engine.theming.MarkdownTheme

/**
 * A form-based visual flowchart builder: add nodes (id + label) and edges
 * (from → to + optional label), with a live native preview. Serializes to a
 * `flowchart` Mermaid block via [FlowchartSpec.toMermaid]. Mirrors the iOS
 * visual diagram builder.
 */
@Composable
fun FlowchartBuilder(
    initial: FlowchartSpec = FlowchartSpec(),
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = LocalMarkdownTheme.current,
    onChange: (FlowchartSpec) -> Unit = {},
) {
    var spec by remember { mutableStateOf(initial) }
    var nodeId by remember { mutableStateOf(TextFieldValue("")) }
    var nodeLabel by remember { mutableStateOf(TextFieldValue("")) }
    var edgeFrom by remember { mutableStateOf(TextFieldValue("")) }
    var edgeTo by remember { mutableStateOf(TextFieldValue("")) }
    var edgeLabel by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(spec) { onChange(spec) }

    Column(modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Nodes", color = theme.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(nodeId, { nodeId = it }, label = { Text("id") }, modifier = Modifier.weight(1f))
            OutlinedTextField(nodeLabel, { nodeLabel = it }, label = { Text("label") }, modifier = Modifier.weight(2f))
        }
        Button(
            onClick = {
                if (nodeId.text.isNotBlank()) {
                    spec = spec.copy(nodes = spec.nodes + BuilderNode(nodeId.text.trim(), nodeLabel.text.ifBlank { nodeId.text }.trim()))
                    nodeId = TextFieldValue(""); nodeLabel = TextFieldValue("")
                }
            },
        ) { Text("Add node") }

        Text("Edges", color = theme.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(edgeFrom, { edgeFrom = it }, label = { Text("from") }, modifier = Modifier.weight(1f))
            OutlinedTextField(edgeTo, { edgeTo = it }, label = { Text("to") }, modifier = Modifier.weight(1f))
            OutlinedTextField(edgeLabel, { edgeLabel = it }, label = { Text("label") }, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = {
                if (edgeFrom.text.isNotBlank() && edgeTo.text.isNotBlank()) {
                    spec = spec.copy(edges = spec.edges + BuilderEdge(edgeFrom.text.trim(), edgeTo.text.trim(), edgeLabel.text.ifBlank { null }?.trim()))
                    edgeFrom = TextFieldValue(""); edgeTo = TextFieldValue(""); edgeLabel = TextFieldValue("")
                }
            },
        ) { Text("Add edge") }

        Text("Preview", color = theme.accent)
        MermaidView(
            source = spec.toMermaid(),
            theme = theme,
            fallback = { Text(spec.toMermaid(), fontFamily = FontFamily.Monospace, color = theme.textSecondary) },
        )
    }
}
