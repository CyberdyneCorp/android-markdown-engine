package com.cyberdyne.markdown.engine.mermaid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MermaidParserTest {

    @Test fun parsesFlowchartNodesEdgesDirection() {
        val d = MermaidParser.parse(
            """
            flowchart TD
                A[Start] --> B{Decision}
                B -->|Yes| C[OK]
                B -->|No| D[Cancel]
            """.trimIndent(),
        ) as MermaidDiagram.Flowchart
        assertEquals(FlowDirection.TB, d.direction)
        assertEquals(4, d.nodes.size)
        assertEquals(NodeShape.DIAMOND, d.nodes.first { it.id == "B" }.shape)
        assertEquals("Start", d.nodes.first { it.id == "A" }.label)
        assertEquals(3, d.edges.size)
        assertEquals("Yes", d.edges.first { it.from == "B" && it.to == "C" }.label)
    }

    @Test fun parsesEdgeStyles() {
        val d = MermaidParser.parse("graph LR\n A --> B\n B -.-> C\n C ==> D") as MermaidDiagram.Flowchart
        assertEquals(FlowDirection.LR, d.direction)
        assertEquals(EdgeStyle.SOLID, d.edges[0].style)
        assertEquals(EdgeStyle.DASHED, d.edges[1].style)
        assertEquals(EdgeStyle.THICK, d.edges[2].style)
    }

    @Test fun parsesNodeShapes() {
        assertEquals(NodeShape.ROUNDED, MermaidParser.parseNode("A(round)").shape)
        assertEquals(NodeShape.STADIUM, MermaidParser.parseNode("A([stad])").shape)
        assertEquals(NodeShape.CIRCLE, MermaidParser.parseNode("A((circ))").shape)
        assertEquals(NodeShape.HEXAGON, MermaidParser.parseNode("A{{hex}}").shape)
        assertEquals(NodeShape.SUBROUTINE, MermaidParser.parseNode("A[[sub]]").shape)
        assertEquals(NodeShape.RECT, MermaidParser.parseNode("A[rect]").shape)
        assertEquals("bare", MermaidParser.parseNode("bare").label)
    }

    @Test fun parsesPie() {
        val d = MermaidParser.parse("pie title Pets\n \"Dogs\" : 386\n \"Cats\" : 85") as MermaidDiagram.Pie
        assertEquals("Pets", d.title)
        assertEquals(2, d.slices.size)
        assertEquals(386.0, d.slices[0].value)
    }

    @Test fun parsesSequence() {
        val d = MermaidParser.parse(
            "sequenceDiagram\n participant A\n participant B\n A->>B: Hello\n B-->>A: Hi",
        ) as MermaidDiagram.Sequence
        assertEquals(listOf("A", "B"), d.participants)
        assertEquals(2, d.messages.size)
        assertEquals("Hello", d.messages[0].text)
        assertTrue(d.messages[1].dashed)
    }

    @Test fun parsesMindmapHierarchy() {
        val d = MermaidParser.parse(
            "mindmap\n  root((Root))\n    A\n      A1\n    B",
        ) as MermaidDiagram.Mindmap
        assertEquals("Root", d.root.label)
        assertEquals(2, d.root.children.size)
        assertEquals("A1", d.root.children[0].children.single().label)
    }

    @Test fun parsesGanttSectionsAndTasks() {
        val d = MermaidParser.parse(
            "gantt\n title Plan\n section Build\n Design : d1, 2024-01-01, 3d\n Code : d2, 2024-01-04, 5d",
        ) as MermaidDiagram.Gantt
        assertEquals("Plan", d.title)
        assertEquals(1, d.sections.size)
        assertEquals(2, d.sections[0].tasks.size)
        assertEquals(5, d.sections[0].tasks[1].duration)
    }

    @Test fun parsesClassDiagram() {
        val d = MermaidParser.parse(
            "classDiagram\n class Animal {\n +String name\n +run()\n }\n Animal <|-- Dog",
        ) as MermaidDiagram.ClassDiagram
        val animal = d.classes.first { it.name == "Animal" }
        assertEquals(listOf("+String name"), animal.attributes)
        assertEquals(listOf("+run()"), animal.methods)
        assertEquals(1, d.relations.size)
    }

    @Test fun parsesStateDiagram() {
        val d = MermaidParser.parse(
            "stateDiagram-v2\n [*] --> Idle\n Idle --> Running : go",
        ) as MermaidDiagram.StateDiagram
        assertEquals(2, d.transitions.size)
        assertEquals("go", d.transitions[1].label)
    }

    @Test fun parsesErDiagram() {
        val d = MermaidParser.parse(
            "erDiagram\n CUSTOMER ||--o{ ORDER : places",
        ) as MermaidDiagram.ErDiagram
        assertEquals(2, d.entities.size)
        assertEquals("places", d.relations.single().label)
    }

    @Test fun parsesGitGraph() {
        val d = MermaidParser.parse(
            "gitGraph\n commit\n branch dev\n checkout dev\n commit\n checkout main\n merge dev",
        ) as MermaidDiagram.GitGraph
        assertTrue(d.commits.any { it.type == "branch" && it.branch == "dev" })
        assertTrue(d.commits.any { it.type == "merge" })
    }

    @Test fun parsesJourney() {
        val d = MermaidParser.parse(
            "journey\n title My Day\n section Morning\n Wake: 3: Me\n Commute: 2: Me,Bus",
        ) as MermaidDiagram.Journey
        assertEquals("My Day", d.title)
        assertEquals(3, d.sections[0].tasks[0].score)
        assertEquals(listOf("Me", "Bus"), d.sections[0].tasks[1].actors)
    }

    @Test fun parsesTimeline() {
        val d = MermaidParser.parse(
            "timeline\n title History\n 2002 : LinkedIn\n 2004 : Facebook : Google",
        ) as MermaidDiagram.Timeline
        assertEquals("History", d.title)
        assertEquals(listOf("Facebook", "Google"), d.periods.first { it.label == "2004" }.events)
    }

    @Test fun unsupportedTypeFallsBack() {
        val d = MermaidParser.parse("quadrantChart\n title Q")
        assertTrue(d is MermaidDiagram.Unsupported)
        assertEquals("quadrantchart", (d as MermaidDiagram.Unsupported).type)
    }
}
