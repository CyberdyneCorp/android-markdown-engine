package com.cyberdyne.markdown.engine.mermaid

/**
 * Parses Mermaid style color values to opaque ARGB `Long`s. Supports `#RGB`,
 * `#RRGGBB`, and a subset of CSS named colors. Pure and unit-testable (no Compose
 * dependency, so it lives in the model layer).
 */
object MermaidColors {

    /** Returns an opaque ARGB `0xFFRRGGBB` value, or null if unrecognized. */
    fun parse(token: String): Long? {
        val t = token.trim().lowercase()
        if (t.isEmpty()) return null
        return if (t.startsWith("#")) parseHex(t.substring(1)) else NAMED[t]
    }

    private fun parseHex(hex: String): Long? {
        val rgb = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return null
        }
        val value = rgb.toLongOrNull(16) ?: return null
        return 0xFF000000L or value
    }

    private val NAMED: Map<String, Long> = mapOf(
        "black" to 0xFF000000, "white" to 0xFFFFFFFF, "red" to 0xFFFF0000,
        "green" to 0xFF008000, "lime" to 0xFF00FF00, "blue" to 0xFF0000FF,
        "yellow" to 0xFFFFFF00, "cyan" to 0xFF00FFFF, "magenta" to 0xFFFF00FF,
        "gray" to 0xFF808080, "grey" to 0xFF808080, "silver" to 0xFFC0C0C0,
        "maroon" to 0xFF800000, "olive" to 0xFF808000, "purple" to 0xFF800080,
        "teal" to 0xFF008080, "navy" to 0xFF000080, "orange" to 0xFFFFA500,
        "tomato" to 0xFFFF6347, "gold" to 0xFFFFD700, "pink" to 0xFFFFC0CB,
        "lightblue" to 0xFFADD8E6, "lightgreen" to 0xFF90EE90, "lightgray" to 0xFFD3D3D3,
        "lightgrey" to 0xFFD3D3D3, "darkblue" to 0xFF00008B, "darkgreen" to 0xFF006400,
        "darkred" to 0xFF8B0000, "skyblue" to 0xFF87CEEB, "salmon" to 0xFFFA8072,
        "coral" to 0xFFFF7F50, "khaki" to 0xFFF0E68C, "violet" to 0xFFEE82EE,
        "indigo" to 0xFF4B0082, "crimson" to 0xFFDC143C, "transparent" to 0x00000000,
    )
}
