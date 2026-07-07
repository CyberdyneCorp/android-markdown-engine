package com.cyberdyne.markdown.latex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import com.cyberdyne.markdown.engine.services.LatexRenderer
import com.cyberdyne.markdown.engine.theming.LocalMarkdownTheme

/**
 * A native LaTeX math renderer (mirrors the iOS SwiftMath bridge product). Lays
 * out and draws math on a Compose Canvas — no WebView, no MathJax. Covers the
 * common subset; unparseable input falls back to raw monospaced source.
 */
class NativeLatexRenderer : LatexRenderer {

    @Composable
    override fun Math(latex: String, inline: Boolean, modifier: Modifier) {
        val theme = LocalMarkdownTheme.current
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val base = if (inline) theme.baseFontSize else theme.baseFontSize * 1.4f
        val style = TextStyle(fontSize = base, color = theme.textPrimary, fontFamily = FontFamily.Serif)

        val box = remember(latex, base) {
            runCatching { layout(LatexParser(latex).parse(), measurer, style, theme.textPrimary) }.getOrNull()
        }
        if (box == null || box.width <= 0f) {
            BasicText(
                text = latex,
                modifier = modifier,
                style = TextStyle(fontFamily = FontFamily.Monospace, color = theme.textPrimary, fontSize = base),
            )
            return
        }
        val canvas = @Composable {
            Canvas(
                Modifier.width(with(density) { box.width.toDp() }).height(with(density) { box.height.toDp() }),
            ) { box.draw(this, 0f, box.ascent) }
        }
        if (inline) canvas() else Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { canvas() }
    }

    private class MathBox(
        val width: Float,
        val ascent: Float,
        val descent: Float,
        val draw: DrawScope.(x: Float, baseline: Float) -> Unit,
    ) {
        val height get() = ascent + descent
    }

    private fun layout(node: MathNode, m: TextMeasurer, style: TextStyle, color: Color): MathBox = when (node) {
        is MathNode.Symbol -> symbolBox(node.text, m, style, color)
        is MathNode.Row -> rowBox(node.items.map { layout(it, m, style, color) })
        is MathNode.Sup -> scriptBox(layout(node.base, m, style, color), layout(node.exponent, m, scaled(style), color), sup = true)
        is MathNode.Sub -> scriptBox(layout(node.base, m, style, color), layout(node.subscript, m, scaled(style), color), sup = false)
        is MathNode.Frac -> fracBox(layout(node.numerator, m, scaled(style), color), layout(node.denominator, m, scaled(style), color), m, style, color)
        is MathNode.Sqrt -> sqrtBox(layout(node.body, m, style, color), color)
    }

    private fun scaled(style: TextStyle) = style.copy(fontSize = style.fontSize * 0.72f)

    private fun symbolBox(text: String, m: TextMeasurer, style: TextStyle, color: Color): MathBox {
        val tl = m.measure(AnnotatedString(text.ifEmpty { " " }), style.copy(color = color))
        val ascent = tl.firstBaseline
        val descent = tl.size.height - tl.firstBaseline
        return MathBox(tl.size.width.toFloat(), ascent, descent) { x, baseline ->
            drawText(tl, topLeft = Offset(x, baseline - ascent))
        }
    }

    private fun rowBox(children: List<MathBox>): MathBox {
        if (children.isEmpty()) return MathBox(0f, 0f, 0f) { _, _ -> }
        val width = children.sumOf { it.width.toDouble() }.toFloat()
        val ascent = children.maxOf { it.ascent }
        val descent = children.maxOf { it.descent }
        return MathBox(width, ascent, descent) { x, baseline ->
            var cx = x
            for (child in children) { child.draw(this, cx, baseline); cx += child.width }
        }
    }

    private fun scriptBox(base: MathBox, script: MathBox, sup: Boolean): MathBox {
        val width = base.width + script.width
        return if (sup) {
            val shift = base.ascent * 0.5f
            val ascent = maxOf(base.ascent, shift + script.ascent)
            MathBox(width, ascent, base.descent) { x, baseline ->
                base.draw(this, x, baseline)
                script.draw(this, x + base.width, baseline - shift)
            }
        } else {
            val shift = base.descent * 0.5f + script.ascent * 0.4f
            val descent = maxOf(base.descent, shift + script.descent)
            MathBox(width, base.ascent, descent) { x, baseline ->
                base.draw(this, x, baseline)
                script.draw(this, x + base.width, baseline + shift)
            }
        }
    }

    private fun fracBox(num: MathBox, den: MathBox, m: TextMeasurer, style: TextStyle, color: Color): MathBox {
        val ref = m.measure(AnnotatedString("x"), style)
        val axis = ref.firstBaseline * 0.5f
        val gap = 4f
        val width = maxOf(num.width, den.width) + 8f
        val ascent = axis + gap + num.height
        val descent = -axis + gap + den.height
        return MathBox(width, ascent, descent) { x, baseline ->
            val barY = baseline - axis
            drawLine(color, Offset(x, barY), Offset(x + width, barY), strokeWidth = 1.4f)
            val numBaseline = barY - gap - num.descent
            val denBaseline = barY + gap + den.ascent
            num.draw(this, x + (width - num.width) / 2, numBaseline)
            den.draw(this, x + (width - den.width) / 2, denBaseline)
        }
    }

    private fun sqrtBox(body: MathBox, color: Color): MathBox {
        val radW = 12f
        val topPad = 4f
        val width = body.width + radW + 4f
        val ascent = body.ascent + topPad
        return MathBox(width, ascent, body.descent) { x, baseline ->
            val top = baseline - body.ascent - topPad
            val bottom = baseline + body.descent
            drawLine(color, Offset(x, (top + bottom) / 2), Offset(x + radW * 0.4f, bottom), strokeWidth = 1.4f)
            drawLine(color, Offset(x + radW * 0.4f, bottom), Offset(x + radW * 0.8f, top), strokeWidth = 1.4f)
            drawLine(color, Offset(x + radW * 0.8f, top), Offset(x + width, top), strokeWidth = 1.4f)
            body.draw(this, x + radW + 2f, baseline)
        }
    }
}
