package com.cyberdyne.markdown.editor.builders

/** Node shape choices offered by the visual flowchart builder. */
enum class BuilderShape { RECT, ROUNDED, STADIUM, CIRCLE, DIAMOND, HEXAGON }

data class BuilderNode(val id: String, val label: String, val shape: BuilderShape = BuilderShape.RECT)

data class BuilderEdge(val from: String, val to: String, val label: String? = null)

/**
 * Form model for the visual flowchart builder. [toMermaid] serializes to a
 * `flowchart` block that the Mermaid engine renders and re-parses.
 */
data class FlowchartSpec(
    val direction: String = "TD",
    val nodes: List<BuilderNode> = emptyList(),
    val edges: List<BuilderEdge> = emptyList(),
) {
    fun toMermaid(): String = buildString {
        append("flowchart ").append(direction).append('\n')
        for (n in nodes) append("    ").append(shapeToken(n)).append('\n')
        for (e in edges) {
            append("    ").append(e.from).append(" -->")
            if (e.label != null) append("|").append(e.label).append("|")
            append(' ').append(e.to).append('\n')
        }
    }

    private fun shapeToken(n: BuilderNode): String = when (n.shape) {
        BuilderShape.RECT -> "${n.id}[${n.label}]"
        BuilderShape.ROUNDED -> "${n.id}(${n.label})"
        BuilderShape.STADIUM -> "${n.id}([${n.label}])"
        BuilderShape.CIRCLE -> "${n.id}((${n.label}))"
        BuilderShape.DIAMOND -> "${n.id}{${n.label}}"
        BuilderShape.HEXAGON -> "${n.id}{{${n.label}}}"
    }
}

/** Form model for the visual pie-chart builder. */
data class PieSpec(val title: String? = null, val slices: List<Pair<String, Double>> = emptyList()) {
    fun toMermaid(): String = buildString {
        append("pie")
        if (!title.isNullOrBlank()) append(" title ").append(title)
        append('\n')
        for ((label, value) in slices) append("    \"").append(label).append("\" : ").append(trim(value)).append('\n')
    }

    private fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
}

/** Form model for the visual sequence-diagram builder. */
data class SequenceSpec(
    val participants: List<String> = emptyList(),
    val messages: List<Triple<String, String, String>> = emptyList(),
) {
    fun toMermaid(): String = buildString {
        append("sequenceDiagram\n")
        for (p in participants) append("    participant ").append(p).append('\n')
        for ((from, to, text) in messages) append("    ").append(from).append("->>").append(to).append(": ").append(text).append('\n')
    }
}
