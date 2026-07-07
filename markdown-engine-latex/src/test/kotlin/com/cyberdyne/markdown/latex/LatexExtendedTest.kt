package com.cyberdyne.markdown.latex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatexExtendedTest {

    @Test fun parsesMatrix() {
        val n = LatexParser("\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}").parse()
        assertTrue(n is MathNode.Matrix)
        val matrix = n as MathNode.Matrix
        assertEquals(2, matrix.rows.size)
        assertEquals(2, matrix.rows[0].size)
        assertEquals("(", matrix.left)
        assertEquals(")", matrix.right)
    }

    @Test fun bracketMatrixDelimiters() {
        val m = LatexParser("\\begin{bmatrix} 1 \\end{bmatrix}").parse() as MathNode.Matrix
        assertEquals("[", m.left)
        assertEquals("]", m.right)
    }

    @Test fun parsesLeftRightDelimiters() {
        val n = LatexParser("\\left( x + y \\right)").parse()
        assertTrue(n is MathNode.Delimited)
        val d = n as MathNode.Delimited
        assertEquals("(", d.left)
        assertEquals(")", d.right)
    }

    @Test fun operatorWithLimitsStillParses() {
        // \sum_{i=1}^{n} -> Sup over a Sub over the sum symbol
        val n = LatexParser("\\sum_{i=1}^{n}").parse()
        assertTrue(n is MathNode.Sup || n is MathNode.Sub)
    }

    @Test fun garbageMatrixDoesNotThrow() {
        LatexParser("\\begin{pmatrix} a & b").parse()
        LatexParser("\\left( x").parse()
    }
}
