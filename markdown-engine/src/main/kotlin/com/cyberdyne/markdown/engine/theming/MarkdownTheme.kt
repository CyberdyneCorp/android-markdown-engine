package com.cyberdyne.markdown.engine.theming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Semantic theme tokens for Markdown rendering. Immutable value type; mirrors the
 * iOS `MarkdownTheme`. Code highlighting, math, and Mermaid derive their default
 * colors from these tokens.
 */
data class MarkdownTheme(
    // Backgrounds & surfaces
    val background: Color,
    val surface: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Accents & lines
    val accent: Color,
    val border: Color,
    val linkColor: Color,
    // Code
    val codeBackground: Color,
    val codeText: Color,
    val inlineCodeBackground: Color,
    val inlineCodeText: Color,
    // Block quote / callout
    val blockQuoteBar: Color,
    val blockQuoteText: Color,
    // Table
    val tableBorder: Color,
    val tableStripe: Color,
    // Typography
    val baseFontSize: TextUnit = 16.sp,
    val headingScale: List<TextUnit> = listOf(30.sp, 24.sp, 20.sp, 18.sp, 16.sp, 14.sp),
    val codeFontFamily: FontFamily = FontFamily.Monospace,
    val paragraphSpacing: Dp = 12.dp,
    val listIndent: Dp = 20.dp,
) {
    /** Type size for a heading [level] (1-6), clamped to the scale. */
    fun headingSize(level: Int): TextUnit = headingScale[(level - 1).coerceIn(0, headingScale.lastIndex)]

    companion object {
        val Light = MarkdownTheme(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF6F8FA),
            textPrimary = Color(0xFF1F2328),
            textSecondary = Color(0xFF57606A),
            textTertiary = Color(0xFF8C959F),
            accent = Color(0xFF0969DA),
            border = Color(0xFFD0D7DE),
            linkColor = Color(0xFF0969DA),
            codeBackground = Color(0xFFF6F8FA),
            codeText = Color(0xFF1F2328),
            inlineCodeBackground = Color(0x1F1F2328),
            inlineCodeText = Color(0xFF1F2328),
            blockQuoteBar = Color(0xFFD0D7DE),
            blockQuoteText = Color(0xFF57606A),
            tableBorder = Color(0xFFD0D7DE),
            tableStripe = Color(0xFFF6F8FA),
        )

        val Dark = MarkdownTheme(
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            textPrimary = Color(0xFFE6EDF3),
            textSecondary = Color(0xFF9198A1),
            textTertiary = Color(0xFF6E7681),
            accent = Color(0xFF2F81F7),
            border = Color(0xFF30363D),
            linkColor = Color(0xFF2F81F7),
            codeBackground = Color(0xFF161B22),
            codeText = Color(0xFFE6EDF3),
            inlineCodeBackground = Color(0x33FFFFFF),
            inlineCodeText = Color(0xFFE6EDF3),
            blockQuoteBar = Color(0xFF30363D),
            blockQuoteText = Color(0xFF9198A1),
            tableBorder = Color(0xFF30363D),
            tableStripe = Color(0xFF161B22),
        )
    }
}

/**
 * Injects a [MarkdownTheme] through the composition so nested `MarkdownView` /
 * `MarkdownEditor` composables inherit it. Mirrors the iOS SwiftUI environment.
 */
val LocalMarkdownTheme = staticCompositionLocalOf { MarkdownTheme.Light }

/** Resolves a theme argument, falling back to the composition-local theme. */
@Composable
internal fun rememberMarkdownTheme(explicit: MarkdownTheme?): MarkdownTheme =
    explicit ?: LocalMarkdownTheme.current
