package com.cyberdyne.markdown.editor

import com.cyberdyne.markdown.editor.builders.BuilderEdge
import com.cyberdyne.markdown.editor.builders.BuilderNode
import com.cyberdyne.markdown.editor.builders.BuilderShape
import com.cyberdyne.markdown.editor.builders.FlowchartSpec
import com.cyberdyne.markdown.editor.builders.PieSpec
import com.cyberdyne.markdown.editor.builders.SequenceSpec
import com.cyberdyne.markdown.engine.mermaid.FlowDirection
import com.cyberdyne.markdown.engine.mermaid.MermaidDiagram
import com.cyberdyne.markdown.engine.mermaid.MermaidParser
import com.cyberdyne.markdown.engine.mermaid.NodeShape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildersTest {

    @Test fun flowchartBuilderRoundTrips() {
        val spec = FlowchartSpec(
            direction = "LR",
            nodes = listOf(BuilderNode("A", "Start"), BuilderNode("B", "Decide", BuilderShape.DIAMOND)),
            edges = listOf(BuilderEdge("A", "B", "go")),
        )
        val d = MermaidParser.parse(spec.toMermaid()) as MermaidDiagram.Flowchart
        assertEquals(FlowDirection.LR, d.direction)
        assertEquals(NodeShape.DIAMOND, d.nodes.first { it.id == "B" }.shape)
        assertEquals("go", d.edges.single().label)
    }

    @Test fun pieBuilderRoundTrips() {
        val spec = PieSpec("Pets", listOf("Dogs" to 386.0, "Cats" to 85.0))
        val d = MermaidParser.parse(spec.toMermaid()) as MermaidDiagram.Pie
        assertEquals("Pets", d.title)
        assertEquals(2, d.slices.size)
        assertEquals(386.0, d.slices[0].value)
    }

    @Test fun sequenceBuilderRoundTrips() {
        val spec = SequenceSpec(
            participants = listOf("A", "B"),
            messages = listOf(Triple("A", "B", "Hello")),
        )
        val d = MermaidParser.parse(spec.toMermaid()) as MermaidDiagram.Sequence
        assertEquals(listOf("A", "B"), d.participants)
        assertEquals("Hello", d.messages.single().text)
    }

    @Test fun wysiwygSplitAndJoin() {
        val md = "# Title\n\nA paragraph.\n\n- a\n- b"
        val blocks = WysiwygOps.toBlocks(md)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0].startsWith("#"))
        // rejoin re-parses to an equivalent document
        val rejoined = WysiwygOps.join(blocks)
        assertEquals(3, WysiwygOps.toBlocks(rejoined).size)
    }

    @Test fun wysiwygMove() {
        val moved = WysiwygOps.moved(listOf("a", "b", "c"), 0, 2)
        assertEquals(listOf("b", "c", "a"), moved)
    }
}
