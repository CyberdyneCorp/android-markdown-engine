package com.cyberdyne.markdown.engine.mermaid

/** Parsed representation of a Mermaid diagram. Pure data; rendering is separate. */
sealed interface MermaidDiagram {
    data class Flowchart(
        val direction: FlowDirection,
        val nodes: List<FlowNode>,
        val edges: List<FlowEdge>,
    ) : MermaidDiagram

    data class Pie(val title: String?, val slices: List<PieSlice>) : MermaidDiagram

    data class Sequence(
        val participants: List<String>,
        val messages: List<SeqMessage>,
    ) : MermaidDiagram

    /** A recognized Mermaid diagram type that this engine does not yet lay out natively. */
    data class Unsupported(val type: String, val source: String) : MermaidDiagram
}

enum class FlowDirection { TB, BT, LR, RL }

enum class NodeShape { RECT, ROUNDED, STADIUM, CIRCLE, DIAMOND, HEXAGON, SUBROUTINE }

data class FlowNode(val id: String, val label: String, val shape: NodeShape)

enum class EdgeStyle { SOLID, DASHED, THICK }

data class FlowEdge(
    val from: String,
    val to: String,
    val label: String?,
    val style: EdgeStyle,
    val arrow: Boolean,
)

data class PieSlice(val label: String, val value: Double)

data class SeqMessage(val from: String, val to: String, val text: String, val dashed: Boolean)
