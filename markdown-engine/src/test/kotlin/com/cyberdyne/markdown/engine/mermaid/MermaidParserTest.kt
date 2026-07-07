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

    @Test fun unsupportedTypeFallsBack() {
        val d = MermaidParser.parse("classDiagram\n class Animal")
        assertTrue(d is MermaidDiagram.Unsupported)
        assertEquals("classdiagram", (d as MermaidDiagram.Unsupported).type)
    }
}
