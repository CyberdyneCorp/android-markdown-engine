package com.cyberdyne.markdown.engine.model

/**
 * A half-open range `[start, end)` of **UTF-8 byte offsets** into the original
 * Markdown source. Mirrors the iOS engine's source-range contract so editing and
 * incremental updates line up byte-for-byte across platforms.
 *
 * Note: Kotlin `String` indexing is UTF-16; the parser converts char indices to
 * UTF-8 byte offsets via [Utf8Offsets] before constructing ranges.
 */
data class SourceRange(val start: Int, val end: Int) {
    init {
        require(start in 0..end) { "Invalid SourceRange [$start, $end)" }
    }

    val length: Int get() = end - start

    companion object {
        val EMPTY = SourceRange(0, 0)
    }
}

/**
 * Maps UTF-16 char indices (Kotlin `String` positions) to UTF-8 byte offsets.
 * Pre-computes a prefix sum so lookups are O(1). For pure-ASCII source the map is
 * the identity function.
 */
class Utf8Offsets(source: String) {
    // prefix[i] = number of UTF-8 bytes for source[0 until i]
    private val prefix: IntArray = IntArray(source.length + 1)

    init {
        var bytes = 0
        for (i in source.indices) {
            prefix[i] = bytes
            bytes += utf8Length(source[i], source, i)
        }
        prefix[source.length] = bytes
    }

    /** Byte offset for a char index (clamped to valid bounds). */
    fun byteOffset(charIndex: Int): Int {
        val i = charIndex.coerceIn(0, prefix.size - 1)
        return prefix[i]
    }

    fun range(startChar: Int, endChar: Int): SourceRange =
        SourceRange(byteOffset(startChar), byteOffset(endChar))

    private fun utf8Length(c: Char, source: String, i: Int): Int = when {
        c.code < 0x80 -> 1
        c.code < 0x800 -> 2
        // Surrogate pair -> one 4-byte code point; count 2 on the high surrogate, 2 on the low.
        c.isHighSurrogate() && i + 1 < source.length && source[i + 1].isLowSurrogate() -> 2
        c.isLowSurrogate() -> 2
        else -> 3
    }
}
