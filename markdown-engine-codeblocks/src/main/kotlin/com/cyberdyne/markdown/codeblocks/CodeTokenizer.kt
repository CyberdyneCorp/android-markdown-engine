package com.cyberdyne.markdown.codeblocks

/** Kind of a highlighted code token. */
enum class TokenType { PLAIN, KEYWORD, STRING, COMMENT, NUMBER }

/** A contiguous run `[start, end)` of [code] classified as [type]. */
data class CodeToken(val start: Int, val end: Int, val type: TokenType)

/** Per-language lexical rules used by [CodeTokenizer]. Pure data. */
data class LanguageSpec(
    val keywords: Set<String>,
    val lineComment: List<String> = listOf("//"),
    val blockComment: Pair<String, String>? = "/*" to "*/",
    val stringDelims: List<Char> = listOf('"', '\''),
)

/**
 * A tiny, dependency-free lexical highlighter. Not a full grammar — it classifies
 * comments, strings, numbers, and keywords, which is enough for readable code
 * blocks. Pure and unit-testable.
 */
object CodeTokenizer {

    fun tokenize(code: String, spec: LanguageSpec): List<CodeToken> {
        val tokens = mutableListOf<CodeToken>()
        var i = 0
        val n = code.length
        while (i < n) {
            val c = code[i]
            when {
                spec.blockComment != null && code.startsWith(spec.blockComment.first, i) -> {
                    val end = code.indexOf(spec.blockComment.second, i + spec.blockComment.first.length)
                        .let { if (it < 0) n else it + spec.blockComment.second.length }
                    tokens += CodeToken(i, end, TokenType.COMMENT); i = end
                }
                spec.lineComment.any { code.startsWith(it, i) } -> {
                    val end = code.indexOf('\n', i).let { if (it < 0) n else it }
                    tokens += CodeToken(i, end, TokenType.COMMENT); i = end
                }
                c in spec.stringDelims -> {
                    var j = i + 1
                    while (j < n && code[j] != c) { if (code[j] == '\\') j++; j++ }
                    val end = (j + 1).coerceAtMost(n)
                    tokens += CodeToken(i, end, TokenType.STRING); i = end
                }
                c.isDigit() -> {
                    var j = i + 1
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                    tokens += CodeToken(i, j, TokenType.NUMBER); i = j
                }
                c.isLetter() || c == '_' -> {
                    var j = i + 1
                    while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    if (word in spec.keywords) tokens += CodeToken(i, j, TokenType.KEYWORD)
                    i = j
                }
                else -> i++
            }
        }
        return tokens
    }
}

/** Built-in [LanguageSpec]s keyed by canonical language name. */
object LanguageSpecs {
    private val C_FAMILY = setOf(
        "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",
        "class", "struct", "enum", "void", "int", "long", "float", "double", "char", "bool",
        "public", "private", "protected", "static", "const", "new", "delete", "this", "true", "false", "null",
    )
    private val KOTLIN = setOf(
        "fun", "val", "var", "if", "else", "when", "for", "while", "return", "class", "object",
        "interface", "data", "sealed", "enum", "override", "private", "public", "internal", "protected",
        "import", "package", "is", "as", "in", "out", "by", "companion", "true", "false", "null", "this", "suspend",
    )
    private val PYTHON = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as",
        "try", "except", "finally", "with", "lambda", "yield", "pass", "break", "continue", "in", "is",
        "not", "and", "or", "True", "False", "None", "self", "async", "await",
    )
    private val JS = setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "return", "class",
        "import", "export", "from", "default", "new", "this", "true", "false", "null", "undefined",
        "async", "await", "typeof", "instanceof", "interface", "type", "enum", "extends", "implements",
    )

    fun forName(language: String?): LanguageSpec = when (language?.lowercase()) {
        "kotlin" -> LanguageSpec(KOTLIN)
        "python" -> LanguageSpec(PYTHON, lineComment = listOf("#"), blockComment = null, stringDelims = listOf('"', '\''))
        "javascript", "typescript" -> LanguageSpec(JS)
        "c", "cpp", "java", "swift", "go", "rust", "csharp" -> LanguageSpec(C_FAMILY)
        "bash" -> LanguageSpec(emptySet(), lineComment = listOf("#"), blockComment = null, stringDelims = listOf('"', '\''))
        else -> LanguageSpec(C_FAMILY + KOTLIN)
    }
}
