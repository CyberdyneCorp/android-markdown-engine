package com.cyberdyne.markdown.latex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatexParserTest {

    @Test fun parsesSuperscript() {
        val n = LatexParser("x^2").parse()
        assertTrue(n is MathNode.Sup)
        assertEquals("x", ((n as MathNode.Sup).base as MathNode.Symbol).text)
        assertEquals("2", (n.exponent as MathNode.Symbol).text)
    }

    @Test fun parsesFraction() {
        val n = LatexParser("\\frac{x^2}{y}").parse()
        assertTrue(n is MathNode.Frac)
        val frac = n as MathNode.Frac
        assertTrue(frac.numerator is MathNode.Sup)
        assertEquals("y", (frac.denominator as MathNode.Symbol).text)
    }

    @Test fun parsesGreekAndOperators() {
        val n = LatexParser("\\alpha + \\beta").parse() as MathNode.Row
        val symbols = n.items.filterIsInstance<MathNode.Symbol>().map { it.text }
        assertTrue(symbols.contains("α"))
        assertTrue(symbols.contains("β"))
    }

    @Test fun parsesSqrtAndSubscript() {
        val sqrt = LatexParser("\\sqrt{2}").parse()
        assertTrue(sqrt is MathNode.Sqrt)
        val sub = LatexParser("a_i").parse()
        assertTrue(sub is MathNode.Sub)
    }

    @Test fun parsesGroupedExponent() {
        val n = LatexParser("e^{-x}").parse() as MathNode.Sup
        assertTrue(n.exponent is MathNode.Row)
    }

    @Test fun doesNotThrowOnGarbage() {
        // Unbalanced braces / stray commands must not crash.
        LatexParser("\\frac{a}{").parse()
        LatexParser("^^__{}").parse()
        LatexParser("").parse()
    }
}
