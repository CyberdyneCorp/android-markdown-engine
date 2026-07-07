package com.cyberdyne.markdown.codeblocks

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.cyberdyne.markdown.engine.code.LanguageAliases
import com.cyberdyne.markdown.engine.services.SyntaxHighlighter

/** Colors for [RegexSyntaxHighlighter]. Configurable; ships light/dark presets. */
data class CodeHighlightTheme(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val plain: Color,
) {
    companion object {
        val AtomOneDark = CodeHighlightTheme(
            keyword = Color(0xFFC678DD),
            string = Color(0xFF98C379),
            comment = Color(0xFF7F848E),
            number = Color(0xFFD19A66),
            plain = Color(0xFFABB2BF),
        )
        val AtomOneLight = CodeHighlightTheme(
            keyword = Color(0xFFA626A4),
            string = Color(0xFF50A14F),
            comment = Color(0xFFA0A1A7),
            number = Color(0xFF986801),
            plain = Color(0xFF383A42),
        )
    }
}

/**
 * A dependency-free [SyntaxHighlighter] bridge (mirrors the iOS Highlightr bridge
 * product) built on the lexical [CodeTokenizer]. No JavaScript runtime; the core
 * module stays free of it. Configure with a [CodeHighlightTheme].
 */
class RegexSyntaxHighlighter(
    private val theme: CodeHighlightTheme = CodeHighlightTheme.AtomOneDark,
) : SyntaxHighlighter {

    override fun highlight(code: String, language: String?): AnnotatedString {
        val canonical = LanguageAliases.normalize(language)
        val spec = LanguageSpecs.forName(canonical)
        val tokens = CodeTokenizer.tokenize(code, spec)
        return buildAnnotatedString {
            append(code)
            for (t in tokens) {
                val color = when (t.type) {
                    TokenType.KEYWORD -> theme.keyword
                    TokenType.STRING -> theme.string
                    TokenType.COMMENT -> theme.comment
                    TokenType.NUMBER -> theme.number
                    TokenType.PLAIN -> theme.plain
                }
                addStyle(SpanStyle(color = color), t.start, t.end)
            }
        }
    }
}
