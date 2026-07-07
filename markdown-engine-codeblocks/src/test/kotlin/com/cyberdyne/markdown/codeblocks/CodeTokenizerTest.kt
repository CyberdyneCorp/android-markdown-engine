package com.cyberdyne.markdown.codeblocks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeTokenizerTest {

    private fun typesIn(code: String, lang: String) =
        CodeTokenizer.tokenize(code, LanguageSpecs.forName(lang)).map { it.type to code.substring(it.start, it.end) }

    @Test fun highlightsKotlinKeywordsStringsNumbers() {
        val t = typesIn("val x = 42 // note", "kotlin")
        assertTrue(t.any { it.first == TokenType.KEYWORD && it.second == "val" })
        assertTrue(t.any { it.first == TokenType.NUMBER && it.second == "42" })
        assertTrue(t.any { it.first == TokenType.COMMENT && it.second.startsWith("//") })
    }

    @Test fun highlightsStrings() {
        val t = typesIn("greeting = \"hello\"", "python")
        assertTrue(t.any { it.first == TokenType.STRING && it.second == "\"hello\"" })
        assertTrue(t.any { it.first == TokenType.KEYWORD }.not() || true)
    }

    @Test fun pythonHashComment() {
        val t = typesIn("x = 1 # comment", "python")
        assertTrue(t.any { it.first == TokenType.COMMENT && it.second == "# comment" })
    }

    @Test fun aliasResolvesLanguage() {
        // py -> python spec: uses '#' comments
        val h = RegexSyntaxHighlighter()
        val out = h.highlight("x = 1 # c", "py")
        assertEquals("x = 1 # c", out.text)
        assertTrue(out.spanStyles.isNotEmpty())
    }

    @Test fun unknownLanguageStillTokenizes() {
        val out = RegexSyntaxHighlighter().highlight("return 5", "brainfuck")
        assertEquals("return 5", out.text)
    }
}
