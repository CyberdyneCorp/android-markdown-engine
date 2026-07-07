package com.cyberdyne.markdown.engine.mermaid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MermaidStyleTest {

    @Test fun parsesHexColors() {
        assertEquals(0xFFFF0000L, MermaidColors.parse("#ff0000"))
        assertEquals(0xFFFF0000L, MermaidColors.parse("#f00"))
        assertEquals(0xFF00FF00L, MermaidColors.parse("#00FF00"))
    }

    @Test fun parsesNamedColors() {
        assertEquals(0xFFFF6347L, MermaidColors.parse("tomato"))
        assertEquals(0xFF000000L, MermaidColors.parse("black"))
        assertNull(MermaidColors.parse("notacolor"))
    }

    @Test fun styleDirectiveAppliesFill() {
        val d = MermaidParser.parse(
            "flowchart TD\n    A[Start] --> B[End]\n    style A fill:#ff0000,stroke:tomato",
        ) as MermaidDiagram.Flowchart
        val a = d.nodes.first { it.id == "A" }
        assertEquals(0xFFFF0000L, a.fill)
        assertEquals(0xFFFF6347L, a.stroke)
    }

    @Test fun classDefAndClassApply() {
        val d = MermaidParser.parse(
            "flowchart TD\n    A --> B\n    classDef hot fill:#f00\n    class A,B hot",
        ) as MermaidDiagram.Flowchart
        assertEquals(0xFFFF0000L, d.nodes.first { it.id == "A" }.fill)
        assertEquals(0xFFFF0000L, d.nodes.first { it.id == "B" }.fill)
    }
}
