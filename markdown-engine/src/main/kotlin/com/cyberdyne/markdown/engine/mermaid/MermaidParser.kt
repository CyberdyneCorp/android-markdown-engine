package com.cyberdyne.markdown.engine.mermaid

/**
 * Parses Mermaid source into a [MermaidDiagram]. Pure and dependency-free.
 * Natively supports flowchart, pie, and sequence diagrams; other recognized types
 * become [MermaidDiagram.Unsupported] so the renderer can fall back to source.
 */
object MermaidParser {

    fun parse(source: String): MermaidDiagram {
        val lines = source.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("%%") }
        val header = lines.firstOrNull().orEmpty()
        val type = header.substringBefore(' ').lowercase()
        val body = lines.drop(1)
        return when (type) {
            "flowchart", "graph" -> parseFlowchart(header, body)
            "pie" -> parsePie(header, body)
            "sequencediagram" -> parseSequence(body)
            else -> MermaidDiagram.Unsupported(type.ifEmpty { "unknown" }, source)
        }
    }

    // --- Flowchart ---

    private val EDGE = Regex("^(.+?)\\s*(-\\.->|-\\.-|<-->|==>|===|-->|---|--[xo])\\s*(?:\\|([^|]*)\\|)?\\s*(.+)$")

    private fun parseFlowchart(header: String, body: List<String>): MermaidDiagram.Flowchart {
        val dir = when (header.substringAfter(' ', "").trim().uppercase()) {
            "LR" -> FlowDirection.LR
            "RL" -> FlowDirection.RL
            "BT" -> FlowDirection.BT
            else -> FlowDirection.TB
        }
        val nodes = LinkedHashMap<String, FlowNode>()
        val edges = mutableListOf<FlowEdge>()

        fun register(node: FlowNode) {
            val existing = nodes[node.id]
            // Prefer an explicit label/shape over an implicit bare reference.
            if (existing == null || (existing.label == existing.id && node.label != node.id)) {
                nodes[node.id] = node
            }
        }

        for (raw in body) {
            val line = raw.removeSuffix(";").trim()
            if (line.isEmpty() || line.startsWith("subgraph") || line == "end" ||
                line.startsWith("style") || line.startsWith("classDef") || line.startsWith("class ") ||
                line.startsWith("linkStyle") || line.startsWith("direction")
            ) {
                continue
            }
            val m = EDGE.find(line)
            if (m != null) {
                val from = parseNode(m.groupValues[1].trim())
                val to = parseNode(m.groupValues[4].trim())
                register(from)
                register(to)
                val connector = m.groupValues[2]
                val style = when {
                    connector.contains('.') -> EdgeStyle.DASHED
                    connector.contains('=') -> EdgeStyle.THICK
                    else -> EdgeStyle.SOLID
                }
                val arrow = connector.endsWith(">") || connector.endsWith("x") || connector.endsWith("o")
                edges += FlowEdge(from.id, to.id, m.groupValues[3].ifEmpty { null }, style, arrow)
            } else {
                register(parseNode(line))
            }
        }
        return MermaidDiagram.Flowchart(dir, nodes.values.toList(), edges)
    }

    private val NODE = Regex("^([A-Za-z0-9_]+)(.*)$")

    /** Parses a node token like `A[Label]`, `B{Decision}`, `C((Circle))`, or bare `D`. */
    internal fun parseNode(token: String): FlowNode {
        val m = NODE.find(token.trim()) ?: return FlowNode(token.trim(), token.trim(), NodeShape.RECT)
        val id = m.groupValues[1]
        val rest = m.groupValues[2].trim()
        val (shape, label) = when {
            rest.startsWith("([") && rest.endsWith("])") -> NodeShape.STADIUM to rest.drop(2).dropLast(2)
            rest.startsWith("[[") && rest.endsWith("]]") -> NodeShape.SUBROUTINE to rest.drop(2).dropLast(2)
            rest.startsWith("((") && rest.endsWith("))") -> NodeShape.CIRCLE to rest.drop(2).dropLast(2)
            rest.startsWith("{{") && rest.endsWith("}}") -> NodeShape.HEXAGON to rest.drop(2).dropLast(2)
            rest.startsWith("[") && rest.endsWith("]") -> NodeShape.RECT to rest.drop(1).dropLast(1)
            rest.startsWith("(") && rest.endsWith(")") -> NodeShape.ROUNDED to rest.drop(1).dropLast(1)
            rest.startsWith("{") && rest.endsWith("}") -> NodeShape.DIAMOND to rest.drop(1).dropLast(1)
            else -> NodeShape.RECT to id
        }
        return FlowNode(id, unquote(label).ifBlank { id }, shape)
    }

    // --- Pie ---

    private val PIE_SLICE = Regex("^\"([^\"]*)\"\\s*:\\s*([0-9.]+)$")

    private fun parsePie(header: String, body: List<String>): MermaidDiagram.Pie {
        val title = header.substringAfter("title", "").trim().ifEmpty { null }
        val slices = body.mapNotNull { line ->
            PIE_SLICE.find(line)?.let { PieSlice(it.groupValues[1], it.groupValues[2].toDoubleOrNull() ?: 0.0) }
        }
        return MermaidDiagram.Pie(title, slices)
    }

    // --- Sequence ---

    private val SEQ_MSG = Regex("^(\\w+)\\s*(-?->>?|--?>>?)\\s*(\\w+)\\s*:\\s*(.*)$")

    private fun parseSequence(body: List<String>): MermaidDiagram.Sequence {
        val participants = LinkedHashSet<String>()
        val messages = mutableListOf<SeqMessage>()
        for (line in body) {
            when {
                line.startsWith("participant ") || line.startsWith("actor ") -> {
                    participants += line.substringAfter(' ').substringBefore(" as ").trim()
                }
                else -> SEQ_MSG.find(line)?.let { m ->
                    val from = m.groupValues[1]
                    val to = m.groupValues[3]
                    participants += from
                    participants += to
                    messages += SeqMessage(from, to, m.groupValues[4].trim(), dashed = m.groupValues[2].contains("--"))
                }
            }
        }
        return MermaidDiagram.Sequence(participants.toList(), messages)
    }

    private fun unquote(s: String): String =
        s.trim().removeSurrounding("\"").removeSurrounding("'").trim()
}
