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

    data class Mindmap(val root: MindNode) : MermaidDiagram

    data class Gantt(val title: String?, val sections: List<GanttSection>) : MermaidDiagram

    data class ClassDiagram(val classes: List<UmlClass>, val relations: List<UmlRelation>) : MermaidDiagram

    data class StateDiagram(val states: List<String>, val transitions: List<StateTransition>) : MermaidDiagram

    data class ErDiagram(val entities: List<ErEntity>, val relations: List<ErRelation>) : MermaidDiagram

    data class GitGraph(val commits: List<GitCommit>) : MermaidDiagram

    data class Journey(val title: String?, val sections: List<JourneySection>) : MermaidDiagram

    data class Timeline(val title: String?, val periods: List<TimelinePeriod>) : MermaidDiagram

    /** A recognized Mermaid diagram type that this engine does not yet lay out natively. */
    data class Unsupported(val type: String, val source: String) : MermaidDiagram
}

// --- Flowchart ---

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

// --- Pie / sequence ---

data class PieSlice(val label: String, val value: Double)

data class SeqMessage(val from: String, val to: String, val text: String, val dashed: Boolean)

// --- Mindmap ---

data class MindNode(val label: String, val children: List<MindNode>)

// --- Gantt ---

data class GanttSection(val name: String, val tasks: List<GanttTask>)

data class GanttTask(val name: String, val duration: Int)

// --- Class diagram ---

data class UmlClass(val name: String, val attributes: List<String>, val methods: List<String>)

/** [kind] is the raw relation operator, e.g. `<|--`, `-->`, `..>`, `*--`, `o--`. */
data class UmlRelation(val from: String, val to: String, val label: String?, val kind: String)

// --- State diagram ---

data class StateTransition(val from: String, val to: String, val label: String?)

// --- ER diagram ---

data class ErEntity(val name: String, val attributes: List<String>)

data class ErRelation(val from: String, val to: String, val label: String?, val cardinality: String)

// --- Git graph ---

/** [type] is one of `commit`, `branch`, `checkout`, `merge`. */
data class GitCommit(val branch: String, val type: String, val id: String?)

// --- Journey ---

data class JourneySection(val name: String, val tasks: List<JourneyTask>)

data class JourneyTask(val name: String, val score: Int, val actors: List<String>)

// --- Timeline ---

data class TimelinePeriod(val label: String, val events: List<String>)
