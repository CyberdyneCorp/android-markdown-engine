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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
        is MathNode.Matrix -> matrixBox(node, m, style, color)
        is MathNode.Delimited -> delimitedBox(layout(node.body, m, style, color), node.left, node.right, color)
    }

    private fun matrixBox(node: MathNode.Matrix, m: TextMeasurer, style: TextStyle, color: Color): MathBox {
        val cells = node.rows.map { row -> row.map { layout(it, m, style, color) } }
        if (cells.isEmpty()) return MathBox(0f, 0f, 0f) { _, _ -> }
        val nCols = cells.maxOf { it.size }
        val colW = (0 until nCols).map { c -> cells.maxOf { r -> r.getOrNull(c)?.width ?: 0f } }
        val rowAsc = cells.map { r -> r.maxOfOrNull { it.ascent } ?: 0f }
        val rowDesc = cells.map { r -> r.maxOfOrNull { it.descent } ?: 0f }
        val gapX = 16f; val gapY = 10f
        val contentW = colW.sum() + gapX * (nCols - 1)
        val contentH = (rowAsc.sum() + rowDesc.sum()) + gapY * (cells.size - 1)
        val delimW = if (node.left.isEmpty()) 0f else 10f
        val width = contentW + 2 * delimW + 8f
        val ascent = contentH / 2 + 4f
        return MathBox(width, ascent, contentH / 2 + 4f) { x, baseline ->
            val top = baseline - contentH / 2
            if (node.left.isNotEmpty()) drawDelim(node.left, x, top, top + contentH, color)
            var ry = top
            for ((ri, row) in cells.withIndex()) {
                val rBaseline = ry + rowAsc[ri]
                var cx = x + delimW + if (node.left.isEmpty()) 0f else 4f
                for (c in 0 until nCols) {
                    val cell = row.getOrNull(c)
                    if (cell != null) cell.draw(this, cx + (colW[c] - cell.width) / 2, rBaseline)
                    cx += colW[c] + gapX
                }
                ry += rowAsc[ri] + rowDesc[ri] + gapY
            }
            if (node.right.isNotEmpty()) drawDelim(node.right, x + width - delimW, top, top + contentH, color)
        }
    }

    private fun delimitedBox(body: MathBox, left: String, right: String, color: Color): MathBox {
        val delimW = 9f
        val width = body.width + 2 * delimW + 6f
        return MathBox(width, body.ascent + 2f, body.descent + 2f) { x, baseline ->
            val top = baseline - body.ascent - 2f
            val bottom = baseline + body.descent + 2f
            if (left.isNotEmpty()) drawDelim(left, x, top, bottom, color)
            body.draw(this, x + delimW + 3f, baseline)
            if (right.isNotEmpty()) drawDelim(right, x + width - delimW, top, bottom, color)
        }
    }

    private fun DrawScope.drawDelim(glyph: String, x: Float, top: Float, bottom: Float, color: Color) {
        val w = 7f
        val mid = (top + bottom) / 2
        val stroke = Stroke(width = 1.6f)
        when (glyph) {
            "(" -> drawPath(Path().apply { moveTo(x + w, top); quadraticBezierTo(x, mid, x + w, bottom) }, color, style = stroke)
            ")" -> drawPath(Path().apply { moveTo(x, top); quadraticBezierTo(x + w, mid, x, bottom) }, color, style = stroke)
            "[" -> drawPath(Path().apply { moveTo(x + w, top); lineTo(x, top); lineTo(x, bottom); lineTo(x + w, bottom) }, color, style = stroke)
            "]" -> drawPath(Path().apply { moveTo(x, top); lineTo(x + w, top); lineTo(x + w, bottom); lineTo(x, bottom) }, color, style = stroke)
            "{" -> drawPath(Path().apply { moveTo(x + w, top); quadraticBezierTo(x, mid, x + w, bottom) }, color, style = stroke)
            "}" -> drawPath(Path().apply { moveTo(x, top); quadraticBezierTo(x + w, mid, x, bottom) }, color, style = stroke)
            else -> drawLine(color, Offset(x + w / 2, top), Offset(x + w / 2, bottom), strokeWidth = 1.6f)
        }
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
