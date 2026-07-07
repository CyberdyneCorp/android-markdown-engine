package com.cyberdyne.markdown.engine.code

/**
 * Normalizes common fenced-code language aliases to their canonical names before
 * a [com.cyberdyne.markdown.engine.services.SyntaxHighlighter] is invoked. Mirrors
 * the iOS alias table.
 */
object LanguageAliases {
    private val aliases = mapOf(
        "py" to "python",
        "c++" to "cpp",
        "cxx" to "cpp",
        "sh" to "bash",
        "shell" to "bash",
        "zsh" to "bash",
        "ts" to "typescript",
        "js" to "javascript",
        "kt" to "kotlin",
        "kts" to "kotlin",
        "rb" to "ruby",
        "yml" to "yaml",
        "md" to "markdown",
        "objc" to "objectivec",
        "cs" to "csharp",
        "golang" to "go",
        "rs" to "rust",
    )

    /** Returns the canonical language name, or the trimmed lowercase input if unknown, or null. */
    fun normalize(language: String?): String? {
        val key = language?.trim()?.lowercase()?.ifEmpty { null } ?: return null
        return aliases[key] ?: key
    }
}
