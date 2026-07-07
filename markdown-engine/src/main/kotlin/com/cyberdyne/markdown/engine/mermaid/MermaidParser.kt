package com.cyberdyne.markdown.engine.mermaid

/**
 * Parses Mermaid source into a [MermaidDiagram]. Pure and dependency-free.
 * Natively supports flowchart, pie, and sequence diagrams; other recognized types
 * become [MermaidDiagram.Unsupported] so the renderer can fall back to source.
 */
object MermaidParser {

    fun parse(source: String): MermaidDiagram {
        val rawLines = source.lines().filter { it.isNotBlank() && !it.trim().startsWith("%%") }
        val lines = rawLines.map { it.trim() }
        val header = lines.firstOrNull().orEmpty()
        val type = header.substringBefore(' ').lowercase()
        val body = lines.drop(1)
        return when (type) {
            "flowchart", "graph" -> parseFlowchart(header, body)
            "pie" -> parsePie(header, body)
            "sequencediagram" -> parseSequence(body)
            "mindmap" -> parseMindmap(rawLines.drop(1))
            "gantt" -> parseGantt(body)
            "classdiagram", "classdiagram-v2" -> parseClassDiagram(body)
            "statediagram", "statediagram-v2" -> parseStateDiagram(body)
            "erdiagram" -> parseErDiagram(body)
            "gitgraph" -> parseGitGraph(body)
            "journey" -> parseJourney(body)
            "timeline" -> parseTimeline(body)
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

    // --- Mindmap (indentation hierarchy) ---

    private fun parseMindmap(rawBody: List<String>): MermaidDiagram {
        if (rawBody.isEmpty()) return MermaidDiagram.Mindmap(MindNode("", emptyList()))
        data class Mut(val label: String, val indent: Int, val children: MutableList<Mut> = mutableListOf())
        val indentOf = { s: String ->
            s.takeWhile { it == ' ' || it == '\t' }.fold(0) { acc, c -> acc + if (c == '\t') 4 else 1 }
        }
        val rootLabel = cleanShapeLabel(rawBody.first().trim())
        val root = Mut(rootLabel, indentOf(rawBody.first()))
        val stack = ArrayDeque<Mut>().apply { addLast(root) }
        for (line in rawBody.drop(1)) {
            val indent = indentOf(line)
            val node = Mut(cleanShapeLabel(line.trim()), indent)
            while (stack.size > 1 && stack.last().indent >= indent) stack.removeLast()
            stack.last().children += node
            stack.addLast(node)
        }
        fun freeze(m: Mut): MindNode = MindNode(m.label, m.children.map { freeze(it) })
        return MermaidDiagram.Mindmap(freeze(root))
    }

    // --- Gantt ---

    private val GANTT_DURATION = Regex("(\\d+)\\s*([dwh])")

    private fun parseGantt(body: List<String>): MermaidDiagram.Gantt {
        var title: String? = null
        val sections = mutableListOf<GanttSection>()
        var current: MutableList<GanttTask>? = null
        var currentName = "Tasks"
        fun flush() { if (current != null) sections += GanttSection(currentName, current!!) }
        for (line in body) {
            when {
                line.startsWith("title ") -> title = line.substringAfter("title ").trim()
                line.startsWith("dateFormat") || line.startsWith("axisFormat") || line.startsWith("excludes") -> {}
                line.startsWith("section ") -> { flush(); currentName = line.substringAfter("section ").trim(); current = mutableListOf() }
                line.contains(':') -> {
                    if (current == null) current = mutableListOf()
                    val name = line.substringBefore(':').trim()
                    val meta = line.substringAfter(':')
                    val duration = GANTT_DURATION.find(meta)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    current!! += GanttTask(name, duration)
                }
            }
        }
        flush()
        return MermaidDiagram.Gantt(title, sections)
    }

    // --- Class diagram ---

    private val CLASS_REL = Regex("^(\\S+)\\s*(<\\|--|--\\|>|\\*--|o--|--\\*|--o|\\.\\.>|<\\.\\.|-->|<--|--|\\.\\.)\\s*(\\S+)\\s*(?::\\s*(.*))?$")

    private fun parseClassDiagram(body: List<String>): MermaidDiagram.ClassDiagram {
        val classes = LinkedHashMap<String, Pair<MutableList<String>, MutableList<String>>>()
        val relations = mutableListOf<UmlRelation>()
        fun cls(name: String) = classes.getOrPut(name.trim()) { mutableListOf<String>() to mutableListOf() }
        var currentBlock: String? = null
        for (raw in body) {
            val line = raw.trim()
            when {
                line == "}" -> currentBlock = null
                line.startsWith("class ") && line.endsWith("{") -> currentBlock = line.removePrefix("class ").removeSuffix("{").trim()
                line.startsWith("class ") -> cls(line.removePrefix("class ").trim())
                currentBlock != null -> addMember(cls(currentBlock!!), line)
                CLASS_REL.matches(line) -> {
                    val m = CLASS_REL.find(line)!!
                    cls(m.groupValues[1]); cls(m.groupValues[3])
                    relations += UmlRelation(m.groupValues[1], m.groupValues[3], m.groupValues[4].ifBlank { null }, m.groupValues[2])
                }
                line.contains(":") -> addMember(cls(line.substringBefore(":").trim()), line.substringAfter(":").trim())
            }
        }
        return MermaidDiagram.ClassDiagram(classes.map { (n, m) -> UmlClass(n, m.first, m.second) }, relations)
    }

    private fun addMember(cls: Pair<MutableList<String>, MutableList<String>>, member: String) {
        val m = member.trim().removeSuffix(";")
        if (m.isEmpty()) return
        if (m.contains("(")) cls.second += m else cls.first += m
    }

    // --- State diagram ---

    private val STATE_TRANS = Regex("^(\\[\\*\\]|\\w+)\\s*-->\\s*(\\[\\*\\]|\\w+)\\s*(?::\\s*(.*))?$")

    private fun parseStateDiagram(body: List<String>): MermaidDiagram.StateDiagram {
        val states = LinkedHashSet<String>()
        val transitions = mutableListOf<StateTransition>()
        for (raw in body) {
            val line = raw.trim()
            STATE_TRANS.find(line)?.let { m ->
                states += m.groupValues[1]; states += m.groupValues[2]
                transitions += StateTransition(m.groupValues[1], m.groupValues[2], m.groupValues[3].ifBlank { null })
            } ?: run {
                if (line.startsWith("state ")) states += line.removePrefix("state ").substringBefore(' ').trim()
            }
        }
        return MermaidDiagram.StateDiagram(states.toList(), transitions)
    }

    // --- ER diagram ---

    private val ER_REL = Regex("^(\\w+)\\s*([|}o][|}o.-]*[|{o.-]*)\\s*(\\w+)\\s*:\\s*(.*)$")

    private fun parseErDiagram(body: List<String>): MermaidDiagram.ErDiagram {
        val entities = LinkedHashMap<String, MutableList<String>>()
        val relations = mutableListOf<ErRelation>()
        fun ent(name: String) = entities.getOrPut(name.trim()) { mutableListOf() }
        var block: String? = null
        for (raw in body) {
            val line = raw.trim()
            when {
                line == "}" -> block = null
                line.endsWith("{") && !line.contains("--") -> block = line.removeSuffix("{").trim().also { ent(it) }
                block != null -> if (line.isNotEmpty()) ent(block!!) += line
                ER_REL.matches(line) -> {
                    val m = ER_REL.find(line)!!
                    ent(m.groupValues[1]); ent(m.groupValues[3])
                    relations += ErRelation(m.groupValues[1], m.groupValues[3], m.groupValues[4].trim().ifBlank { null }, m.groupValues[2].trim())
                }
            }
        }
        return MermaidDiagram.ErDiagram(entities.map { (n, a) -> ErEntity(n, a) }, relations)
    }

    // --- Git graph ---

    private fun parseGitGraph(body: List<String>): MermaidDiagram.GitGraph {
        val commits = mutableListOf<GitCommit>()
        var branch = "main"
        for (raw in body) {
            val line = raw.trim()
            val kw = line.substringBefore(' ').lowercase()
            when (kw) {
                "commit" -> commits += GitCommit(branch, "commit", idOf(line))
                "branch" -> { branch = line.substringAfter("branch ").trim(); commits += GitCommit(branch, "branch", null) }
                "checkout", "switch" -> branch = line.substringAfter(' ').trim()
                "merge" -> commits += GitCommit(branch, "merge", line.substringAfter("merge ").trim())
            }
        }
        return MermaidDiagram.GitGraph(commits)
    }

    private fun idOf(line: String): String? = Regex("id:\\s*\"([^\"]*)\"").find(line)?.groupValues?.get(1)

    // --- Journey ---

    private fun parseJourney(body: List<String>): MermaidDiagram.Journey {
        var title: String? = null
        val sections = mutableListOf<JourneySection>()
        var current: MutableList<JourneyTask>? = null
        var currentName = "Journey"
        fun flush() { if (current != null) sections += JourneySection(currentName, current!!) }
        for (line in body) {
            when {
                line.startsWith("title ") -> title = line.substringAfter("title ").trim()
                line.startsWith("section ") -> { flush(); currentName = line.substringAfter("section ").trim(); current = mutableListOf() }
                line.contains(':') -> {
                    if (current == null) current = mutableListOf()
                    val name = line.substringBefore(':').trim()
                    val parts = line.substringAfter(':').split(':').map { it.trim() }
                    val score = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val actors = parts.getOrNull(1)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    current!! += JourneyTask(name, score, actors)
                }
            }
        }
        flush()
        return MermaidDiagram.Journey(title, sections)
    }

    // --- Timeline ---

    private fun parseTimeline(body: List<String>): MermaidDiagram.Timeline {
        var title: String? = null
        val periods = mutableListOf<TimelinePeriod>()
        for (line in body) {
            when {
                line.startsWith("title ") -> title = line.substringAfter("title ").trim()
                line.startsWith("section ") -> periods += TimelinePeriod(line.substringAfter("section ").trim(), emptyList())
                line.contains(':') -> {
                    val parts = line.split(':').map { it.trim() }
                    periods += TimelinePeriod(parts.first(), parts.drop(1).filter { it.isNotEmpty() })
                }
                else -> periods += TimelinePeriod(line.trim(), emptyList())
            }
        }
        return MermaidDiagram.Timeline(title, periods)
    }

    private fun cleanShapeLabel(token: String): String {
        // Strip an optional id prefix and shape brackets: `root((X))`, `id[X]`, `(X)`, plain.
        val afterId = Regex("^[A-Za-z0-9_]+((\\(\\(|\\[|\\(|\\{\\{|\\{).*)$").find(token)?.groupValues?.get(1) ?: token
        return unquote(afterId.trim().trim('(', ')', '[', ']', '{', '}').trim())
    }

    private fun unquote(s: String): String =
        s.trim().removeSurrounding("\"").removeSurrounding("'").trim()
}
