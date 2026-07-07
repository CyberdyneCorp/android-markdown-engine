package com.cyberdyne.markdown.engine.mermaid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberdyne.markdown.engine.config.DiagramSizing
import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Prepared diagram geometry: intrinsic size plus a draw closure over a [DrawScope]. */
private class DiagramContent(val width: Float, val height: Float, val draw: DrawScope.() -> Unit)

/**
 * Renders a Mermaid diagram natively on a Compose [Canvas] (no WebView/JS).
 * Supports flowchart, pie, and sequence diagrams; unsupported types call
 * [fallback] (typically a source code block).
 */
@Composable
fun MermaidView(
    source: String,
    theme: MarkdownTheme,
    configuration: MarkdownConfiguration = MarkdownConfiguration.Default,
    fallback: @Composable () -> Unit,
) {
    val diagram = remember(source) { MermaidParser.parse(source) }
    val measurer = rememberTextMeasurer()
    val content: DiagramContent? = when (diagram) {
        is MermaidDiagram.Flowchart -> flowchartContent(diagram, theme, measurer)
        is MermaidDiagram.Pie -> pieContent(diagram, theme, measurer)
        is MermaidDiagram.Sequence -> sequenceContent(diagram, theme, measurer)
        is MermaidDiagram.Unsupported -> null
    }
    if (content == null || content.width <= 0f) {
        fallback()
        return
    }
    DiagramHost(content, configuration)
}

@Composable
private fun DiagramHost(content: DiagramContent, config: MarkdownConfiguration) {
    val density = LocalDensity.current
    when (config.diagramSizing) {
        DiagramSizing.SCROLL -> {
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Canvas(
                    Modifier.width(with(density) { content.width.toDp() })
                        .height(with(density) { content.height.toDp() }),
                ) { content.draw(this) }
            }
        }
        DiagramSizing.FIT_TO_WIDTH -> {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val available = constraints.maxWidth.toFloat()
                val scale = if (available > 0f) min(1f, available / content.width) else 1f
                Canvas(
                    Modifier.width(with(density) { (content.width * scale).toDp() })
                        .height(with(density) { (content.height * scale).toDp() }),
                ) { scale(scale, scale, pivot = Offset.Zero) { content.draw(this) } }
            }
        }
    }
}

// --- Flowchart ---

private class Positioned(val node: FlowNode, val x: Float, val y: Float, val w: Float, val h: Float) {
    val cx get() = x + w / 2
    val cy get() = y + h / 2
}

private fun flowchartContent(d: MermaidDiagram.Flowchart, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    val labelStyle = TextStyle(fontSize = 13.sp, color = theme.textPrimary)
    val padX = 24f; val padY = 16f; val layerGap = 64f; val nodeGap = 28f; val margin = 16f
    val measured = d.nodes.associate { it.id to measurer.measure(AnnotatedString(it.label), labelStyle) }
    fun w(id: String) = max(48f, (measured[id]?.size?.width?.toFloat() ?: 40f) + padX * 2)
    fun h(id: String) = max(36f, (measured[id]?.size?.height?.toFloat() ?: 20f) + padY * 2)

    // Longest-path layering (relaxation; safe for cycles by iteration cap).
    val layer = HashMap<String, Int>().apply { d.nodes.forEach { this[it.id] = 0 } }
    repeat(d.nodes.size) {
        for (e in d.edges) {
            val nl = (layer[e.from] ?: 0) + 1
            if (nl > (layer[e.to] ?: 0)) layer[e.to] = nl
        }
    }
    val horizontal = d.direction == FlowDirection.LR || d.direction == FlowDirection.RL
    val byLayer = d.nodes.groupBy { layer[it.id] ?: 0 }.toSortedMap()

    // Cross extents per layer to center them.
    fun cross(id: String) = if (horizontal) h(id) else w(id)
    fun primary(id: String) = if (horizontal) w(id) else h(id)
    val layerCross = byLayer.mapValues { (_, ns) -> ns.sumOf { cross(it.id).toDouble() }.toFloat() + nodeGap * (ns.size - 1) }
    val maxCross = (layerCross.values.maxOrNull() ?: 0f)

    val positions = HashMap<String, Positioned>()
    var primaryOffset = margin
    for ((_, ns) in byLayer) {
        val layerPrimary = ns.maxOf { primary(it.id) }
        var crossPos = margin + (maxCross - (layerCross[layer[ns.first().id]] ?: 0f)) / 2
        for (n in ns) {
            val nw = w(n.id); val nh = h(n.id)
            val (x, y) = if (horizontal) {
                primaryOffset + (layerPrimary - nw) / 2 to crossPos
            } else {
                crossPos to primaryOffset + (layerPrimary - nh) / 2
            }
            positions[n.id] = Positioned(n, x, y, nw, nh)
            crossPos += cross(n.id) + nodeGap
        }
        primaryOffset += layerPrimary + layerGap
    }
    val totalW = (positions.values.maxOfOrNull { it.x + it.w } ?: 0f) + margin
    val totalH = (positions.values.maxOfOrNull { it.y + it.h } ?: 0f) + margin

    return DiagramContent(totalW, totalH) {
        for (e in d.edges) {
            val a = positions[e.from] ?: continue
            val b = positions[e.to] ?: continue
            drawEdge(a, b, e, theme, measurer)
        }
        for (p in positions.values) drawNode(p, theme, measured[p.node.id])
    }
}

private fun DrawScope.drawEdge(a: Positioned, b: Positioned, e: FlowEdge, theme: MarkdownTheme, measurer: TextMeasurer) {
    val start = Offset(a.cx, a.cy)
    val end = Offset(b.cx, b.cy)
    val color = theme.textSecondary
    val stroke = when (e.style) {
        EdgeStyle.THICK -> Stroke(width = 3.5f)
        EdgeStyle.DASHED -> Stroke(width = 1.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        EdgeStyle.SOLID -> Stroke(width = 1.8f)
    }
    // Clip endpoint to the target node border.
    val angle = atan2(end.y - start.y, end.x - start.x)
    val tip = Offset(b.cx - cos(angle) * (b.w / 2), b.cy - sin(angle) * (b.h / 2))
    drawLine(color, start, tip, strokeWidth = stroke.width, pathEffect = stroke.pathEffect)
    if (e.arrow) drawArrowHead(tip, angle, color)
    e.label?.let { label ->
        val mid = Offset((start.x + tip.x) / 2, (start.y + tip.y) / 2)
        val lr = measurer.measure(AnnotatedString(label), TextStyle(fontSize = 11.sp, color = theme.textSecondary))
        drawRect(theme.background, topLeft = Offset(mid.x - lr.size.width / 2f, mid.y - lr.size.height / 2f), size = androidx.compose.ui.geometry.Size(lr.size.width.toFloat(), lr.size.height.toFloat()))
        drawText(lr, topLeft = Offset(mid.x - lr.size.width / 2f, mid.y - lr.size.height / 2f))
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, angle: Float, color: Color) {
    val size = 10f
    val left = Offset(tip.x - cos(angle - 0.4f) * size, tip.y - sin(angle - 0.4f) * size)
    val right = Offset(tip.x - cos(angle + 0.4f) * size, tip.y - sin(angle + 0.4f) * size)
    val path = Path().apply { moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(right.x, right.y); close() }
    drawPath(path, color)
}

private fun DrawScope.drawNode(p: Positioned, theme: MarkdownTheme, label: TextLayoutResult?) {
    val fill = theme.surface
    val stroke = theme.accent
    val tl = Offset(p.x, p.y)
    val size = androidx.compose.ui.geometry.Size(p.w, p.h)
    when (p.node.shape) {
        NodeShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(p.cx, p.y); lineTo(p.x + p.w, p.cy); lineTo(p.cx, p.y + p.h); lineTo(p.x, p.cy); close()
            }
            drawPath(path, fill); drawPath(path, stroke, style = Stroke(width = 1.8f))
        }
        NodeShape.HEXAGON -> {
            val inset = p.w * 0.15f
            val path = Path().apply {
                moveTo(p.x + inset, p.y); lineTo(p.x + p.w - inset, p.y); lineTo(p.x + p.w, p.cy)
                lineTo(p.x + p.w - inset, p.y + p.h); lineTo(p.x + inset, p.y + p.h); lineTo(p.x, p.cy); close()
            }
            drawPath(path, fill); drawPath(path, stroke, style = Stroke(width = 1.8f))
        }
        NodeShape.CIRCLE -> {
            val r = min(p.w, p.h) / 2
            drawCircle(fill, r, Offset(p.cx, p.cy)); drawCircle(stroke, r, Offset(p.cx, p.cy), style = Stroke(1.8f))
        }
        else -> {
            val corner = when (p.node.shape) {
                NodeShape.STADIUM -> p.h / 2
                NodeShape.ROUNDED, NodeShape.SUBROUTINE -> 10f
                else -> 2f
            }
            drawRoundRect(fill, tl, size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner))
            drawRoundRect(stroke, tl, size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner), style = Stroke(1.8f))
        }
    }
    if (label != null) {
        drawText(label, topLeft = Offset(p.cx - label.size.width / 2f, p.cy - label.size.height / 2f))
    }
}

// --- Pie ---

private val PIE_PALETTE = listOf(
    Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFF84CC16),
)

private fun pieContent(d: MermaidDiagram.Pie, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    if (d.slices.isEmpty()) return DiagramContent(0f, 0f) {}
    val total = d.slices.sumOf { it.value }.takeIf { it > 0 } ?: 1.0
    val diameter = 220f; val margin = 16f
    val legendStyle = TextStyle(fontSize = 13.sp, color = theme.textPrimary)
    val legend = d.slices.map { measurer.measure(AnnotatedString("${it.label}  ${(it.value / total * 100).toInt()}%"), legendStyle) }
    val legendW = (legend.maxOfOrNull { it.size.width }?.toFloat() ?: 0f) + 28f
    val titleLr = d.title?.let { measurer.measure(AnnotatedString(it), TextStyle(fontSize = 16.sp, color = theme.textPrimary)) }
    val topOffset = (titleLr?.size?.height?.toFloat() ?: 0f) + (if (titleLr != null) 8f else 0f)
    val width = margin * 3 + diameter + legendW
    val height = margin * 2 + topOffset + max(diameter, legend.size * 26f)

    return DiagramContent(width, height) {
        titleLr?.let { drawText(it, topLeft = Offset(margin, margin)) }
        val center = Offset(margin + diameter / 2, margin + topOffset + diameter / 2)
        var startAngle = -90f
        d.slices.forEachIndexed { i, slice ->
            val sweep = (slice.value / total * 360.0).toFloat()
            val color = PIE_PALETTE[i % PIE_PALETTE.size]
            drawArc(color, startAngle, sweep, useCenter = true, topLeft = Offset(margin, margin + topOffset), size = androidx.compose.ui.geometry.Size(diameter, diameter))
            startAngle += sweep
        }
        val legendX = margin * 2 + diameter
        legend.forEachIndexed { i, lr ->
            val y = margin + topOffset + i * 26f
            drawRect(PIE_PALETTE[i % PIE_PALETTE.size], topLeft = Offset(legendX, y + 2f), size = androidx.compose.ui.geometry.Size(16f, 16f))
            drawText(lr, topLeft = Offset(legendX + 24f, y))
        }
    }
}

// --- Sequence ---

private fun sequenceContent(d: MermaidDiagram.Sequence, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    if (d.participants.isEmpty()) return DiagramContent(0f, 0f) {}
    val nameStyle = TextStyle(fontSize = 13.sp, color = theme.textPrimary)
    val margin = 16f; val boxH = 36f; val msgGap = 44f; val colGap = 160f
    val boxes = d.participants.associateWith { measurer.measure(AnnotatedString(it), nameStyle) }
    val colX = d.participants.mapIndexed { i, _ -> margin + 60f + i * colGap }.toFloatArray()
    val width = margin * 2 + 120f + (d.participants.size - 1) * colGap
    val height = margin * 2 + boxH + d.messages.size * msgGap + 20f
    val idx = d.participants.withIndex().associate { it.value to it.index }

    return DiagramContent(width, height) {
        // Participant boxes + lifelines
        d.participants.forEachIndexed { i, name ->
            val lr = boxes[name]!!
            val bx = colX[i] - lr.size.width / 2f - 12f
            drawRoundRect(theme.surface, Offset(bx, margin), androidx.compose.ui.geometry.Size(lr.size.width + 24f, boxH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            drawRoundRect(theme.accent, Offset(bx, margin), androidx.compose.ui.geometry.Size(lr.size.width + 24f, boxH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(1.5f))
            drawText(lr, topLeft = Offset(colX[i] - lr.size.width / 2f, margin + (boxH - lr.size.height) / 2f))
            drawLine(theme.border, Offset(colX[i], margin + boxH), Offset(colX[i], height - margin), strokeWidth = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        }
        // Messages
        d.messages.forEachIndexed { i, msg ->
            val fromX = colX[idx[msg.from] ?: 0]
            val toX = colX[idx[msg.to] ?: 0]
            val y = margin + boxH + 30f + i * msgGap
            val effect = if (msg.dashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
            drawLine(theme.textSecondary, Offset(fromX, y), Offset(toX, y), strokeWidth = 1.6f, pathEffect = effect)
            drawArrowHead(Offset(toX, y), atan2(0f, toX - fromX), theme.textSecondary)
            val lr = measurer.measure(AnnotatedString(msg.text), TextStyle(fontSize = 11.sp, color = theme.textPrimary, textAlign = TextAlign.Center))
            drawText(lr, topLeft = Offset((fromX + toX) / 2 - lr.size.width / 2f, y - lr.size.height - 4f))
        }
    }
}
