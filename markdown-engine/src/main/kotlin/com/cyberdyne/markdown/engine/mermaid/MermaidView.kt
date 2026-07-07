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
        is MermaidDiagram.StateDiagram -> stateContent(diagram, theme, measurer)
        is MermaidDiagram.ClassDiagram -> classContent(diagram, theme, measurer)
        is MermaidDiagram.ErDiagram -> erContent(diagram, theme, measurer)
        is MermaidDiagram.Mindmap -> mindmapContent(diagram, theme, measurer)
        is MermaidDiagram.Gantt -> ganttContent(diagram, theme, measurer)
        is MermaidDiagram.GitGraph -> gitGraphContent(diagram, theme, measurer)
        is MermaidDiagram.Journey -> journeyContent(diagram, theme, measurer)
        is MermaidDiagram.Timeline -> timelineContent(diagram, theme, measurer)
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

// --- Shared layered-graph layout (state / generic box graphs) ---

private class Box4(val id: String, val x: Float, val y: Float, val w: Float, val h: Float) {
    val cx get() = x + w / 2
    val cy get() = y + h / 2
}

private fun layeredLayout(
    ids: List<String>,
    edges: List<Pair<String, String>>,
    sizeOf: (String) -> Pair<Float, Float>,
    horizontal: Boolean,
    margin: Float = 16f,
    layerGap: Float = 64f,
    nodeGap: Float = 28f,
): Pair<Map<String, Box4>, Pair<Float, Float>> {
    val layer = HashMap<String, Int>().apply { ids.forEach { this[it] = 0 } }
    repeat(ids.size) {
        for ((f, t) in edges) {
            val nl = (layer[f] ?: 0) + 1
            if (nl > (layer[t] ?: 0)) layer[t] = nl
        }
    }
    val byLayer = ids.groupBy { layer[it] ?: 0 }.toSortedMap()
    fun cross(id: String) = if (horizontal) sizeOf(id).second else sizeOf(id).first
    fun primary(id: String) = if (horizontal) sizeOf(id).first else sizeOf(id).second
    val layerCross = byLayer.mapValues { (_, ns) -> ns.sumOf { cross(it).toDouble() }.toFloat() + nodeGap * (ns.size - 1) }
    val maxCross = layerCross.values.maxOrNull() ?: 0f
    val pos = HashMap<String, Box4>()
    var primaryOff = margin
    for ((ly, ns) in byLayer) {
        val layerPrimary = ns.maxOf { primary(it) }
        var crossPos = margin + (maxCross - (layerCross[ly] ?: 0f)) / 2
        for (id in ns) {
            val (w, h) = sizeOf(id)
            val x: Float; val y: Float
            if (horizontal) { x = primaryOff + (layerPrimary - w) / 2; y = crossPos } else { x = crossPos; y = primaryOff + (layerPrimary - h) / 2 }
            pos[id] = Box4(id, x, y, w, h)
            crossPos += cross(id) + nodeGap
        }
        primaryOff += layerPrimary + layerGap
    }
    val tw = (pos.values.maxOfOrNull { it.x + it.w } ?: 0f) + margin
    val th = (pos.values.maxOfOrNull { it.y + it.h } ?: 0f) + margin
    return pos to (tw to th)
}

private fun DrawScope.edgeBetween(a: Box4, b: Box4, theme: MarkdownTheme, label: String?, measurer: TextMeasurer, dashed: Boolean = false) {
    val start = Offset(a.cx, a.cy)
    val angle = atan2(b.cy - a.cy, b.cx - a.cx)
    val tip = Offset(b.cx - cos(angle) * (b.w / 2), b.cy - sin(angle) * (b.h / 2))
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(9f, 7f)) else null
    drawLine(theme.textSecondary, start, tip, strokeWidth = 1.8f, pathEffect = effect)
    drawArrowHead(tip, angle, theme.textSecondary)
    label?.let {
        val mid = Offset((start.x + tip.x) / 2, (start.y + tip.y) / 2)
        val lr = measurer.measure(AnnotatedString(it), TextStyle(fontSize = 11.sp, color = theme.textSecondary))
        drawRect(theme.background, topLeft = Offset(mid.x - lr.size.width / 2f, mid.y - lr.size.height / 2f), size = androidx.compose.ui.geometry.Size(lr.size.width.toFloat(), lr.size.height.toFloat()))
        drawText(lr, topLeft = Offset(mid.x - lr.size.width / 2f, mid.y - lr.size.height / 2f))
    }
}

// --- State diagram ---

private fun stateContent(d: MermaidDiagram.StateDiagram, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    if (d.states.isEmpty()) return DiagramContent(0f, 0f) {}
    val style = TextStyle(fontSize = 13.sp, color = theme.textPrimary)
    val labels = d.states.associateWith { if (it == "[*]") null else measurer.measure(AnnotatedString(it), style) }
    fun size(id: String): Pair<Float, Float> = if (id == "[*]") 20f to 20f
    else (max(56f, (labels[id]?.size?.width ?: 0).toFloat() + 32f)) to 40f
    val (pos, total) = layeredLayout(d.states, d.transitions.map { it.from to it.to }, ::size, horizontal = false)
    return DiagramContent(total.first, total.second) {
        for (t in d.transitions) {
            val a = pos[t.from] ?: continue; val b = pos[t.to] ?: continue
            edgeBetween(a, b, theme, t.label, measurer)
        }
        for (s in d.states) {
            val p = pos[s] ?: continue
            if (s == "[*]") {
                drawCircle(theme.accent, p.w / 2, Offset(p.cx, p.cy))
            } else {
                drawRoundRect(theme.surface, Offset(p.x, p.y), androidx.compose.ui.geometry.Size(p.w, p.h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(p.h / 2, p.h / 2))
                drawRoundRect(theme.accent, Offset(p.x, p.y), androidx.compose.ui.geometry.Size(p.w, p.h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(p.h / 2, p.h / 2), style = Stroke(1.8f))
                labels[s]?.let { drawText(it, topLeft = Offset(p.cx - it.size.width / 2f, p.cy - it.size.height / 2f)) }
            }
        }
    }
}

// --- Class & ER (compartment boxes in a grid) ---

private class CompartmentBox(val header: String, val rows: List<String>)

private fun compartmentContent(
    boxes: List<CompartmentBox>,
    relations: List<Triple<String, String, String?>>,
    theme: MarkdownTheme,
    measurer: TextMeasurer,
): DiagramContent {
    if (boxes.isEmpty()) return DiagramContent(0f, 0f) {}
    val headStyle = TextStyle(fontSize = 14.sp, color = theme.textPrimary)
    val rowStyle = TextStyle(fontSize = 12.sp, color = theme.textSecondary)
    val rowH = 22f; val headH = 30f; val pad = 12f; val gapX = 40f; val gapY = 40f; val margin = 16f
    data class Placed(val box: CompartmentBox, val x: Float, val y: Float, val w: Float, val h: Float, val head: TextLayoutResult, val rows: List<TextLayoutResult>)
    val measured = boxes.map { b ->
        val head = measurer.measure(AnnotatedString(b.header), headStyle)
        val rows = b.rows.map { measurer.measure(AnnotatedString(it), rowStyle) }
        val widest = (listOf(head.size.width) + rows.map { it.size.width }).maxOrNull() ?: 0
        val w = max(120f, widest + pad * 2)
        val h = headH + rows.size * rowH + pad
        Triple(b, w to h, head to rows)
    }
    val cols = kotlin.math.ceil(kotlin.math.sqrt(boxes.size.toDouble())).toInt().coerceAtLeast(1)
    val colWidth = measured.maxOf { it.second.first } + gapX
    val placed = HashMap<String, Placed>()
    var maxRowH = 0f; var x = margin; var y = margin; var col = 0
    val ordered = mutableListOf<Placed>()
    for ((b, size, texts) in measured) {
        val p = Placed(b, x, y, size.first, size.second, texts.first, texts.second)
        placed[b.header] = p; ordered += p
        maxRowH = max(maxRowH, size.second)
        col++; x += colWidth
        if (col >= cols) { col = 0; x = margin; y += maxRowH + gapY; maxRowH = 0f }
    }
    val totalW = ordered.maxOf { it.x + it.w } + margin
    val totalH = ordered.maxOf { it.y + it.h } + margin
    return DiagramContent(totalW, totalH) {
        for ((from, to, label) in relations) {
            val a = placed[from] ?: continue; val b = placed[to] ?: continue
            edgeBetween(Box4(from, a.x, a.y, a.w, a.h), Box4(to, b.x, b.y, b.w, b.h), theme, label, measurer)
        }
        for (p in ordered) {
            drawRoundRect(theme.surface, Offset(p.x, p.y), androidx.compose.ui.geometry.Size(p.w, p.h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
            drawRoundRect(theme.accent, Offset(p.x, p.y), androidx.compose.ui.geometry.Size(p.w, p.h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(1.6f))
            drawText(p.head, topLeft = Offset(p.x + pad, p.y + 6f))
            drawLine(theme.border, Offset(p.x, p.y + headH), Offset(p.x + p.w, p.y + headH), strokeWidth = 1f)
            p.rows.forEachIndexed { i, lr -> drawText(lr, topLeft = Offset(p.x + pad, p.y + headH + 4f + i * rowH)) }
        }
    }
}

private fun classContent(d: MermaidDiagram.ClassDiagram, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent =
    compartmentContent(
        d.classes.map { CompartmentBox(it.name, it.attributes + it.methods) },
        d.relations.map { Triple(it.from, it.to, it.label) },
        theme, measurer,
    )

private fun erContent(d: MermaidDiagram.ErDiagram, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent =
    compartmentContent(
        d.entities.map { CompartmentBox(it.name, it.attributes) },
        d.relations.map { Triple(it.from, it.to, it.cardinality + (it.label?.let { l -> " $l" } ?: "")) },
        theme, measurer,
    )

// --- Mindmap (indented tree) ---

private fun mindmapContent(d: MermaidDiagram.Mindmap, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    val style = TextStyle(fontSize = 13.sp, color = theme.textPrimary)
    val colW = 180f; val rowH = 40f; val margin = 16f
    class Placed(val label: String, val x: Float, val y: Float, val lr: TextLayoutResult)
    val placed = mutableListOf<Placed>()
    val edges = mutableListOf<Pair<Offset, Offset>>()
    var leafRow = 0
    fun walk(node: MindNode, depth: Int): Offset {
        val lr = measurer.measure(AnnotatedString(node.label), style)
        val x = margin + depth * colW
        if (node.children.isEmpty()) {
            val y = margin + leafRow * rowH + rowH / 2
            leafRow++
            placed += Placed(node.label, x, y, lr)
            return Offset(x, y)
        }
        val childAnchors = node.children.map { walk(it, depth + 1) }
        val y = childAnchors.map { it.y.toDouble() }.average().toFloat()
        placed += Placed(node.label, x, y, lr)
        val myRight = Offset(x + lr.size.width + 20f, y)
        childAnchors.forEach { edges += myRight to it }
        return Offset(x, y)
    }
    walk(d.root, 0)
    val totalW = (placed.maxOfOrNull { it.x + it.lr.size.width } ?: 0f) + margin + 24f
    val totalH = (placed.maxOfOrNull { it.y } ?: 0f) + margin + rowH
    return DiagramContent(totalW, totalH) {
        for ((a, b) in edges) drawLine(theme.border, a, b, strokeWidth = 1.4f)
        for (p in placed) {
            val w = p.lr.size.width + 20f; val h = 26f
            drawRoundRect(theme.surface, Offset(p.x, p.y - h / 2), androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(13f, 13f))
            drawRoundRect(theme.accent, Offset(p.x, p.y - h / 2), androidx.compose.ui.geometry.Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(13f, 13f), style = Stroke(1.4f))
            drawText(p.lr, topLeft = Offset(p.x + 10f, p.y - p.lr.size.height / 2f))
        }
    }
}

// --- Gantt (cumulative bars) ---

private fun ganttContent(d: MermaidDiagram.Gantt, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    val nameStyle = TextStyle(fontSize = 12.sp, color = theme.textPrimary)
    val secStyle = TextStyle(fontSize = 13.sp, color = theme.accent)
    val margin = 16f; val rowH = 30f; val unit = 26f; val labelW = 160f
    val titleLr = d.title?.let { measurer.measure(AnnotatedString(it), TextStyle(fontSize = 16.sp, color = theme.textPrimary)) }
    val totalUnits = d.sections.sumOf { s -> s.tasks.sumOf { it.duration } }.coerceAtLeast(1)
    var rows = 0
    d.sections.forEach { rows += 1 + it.tasks.size }
    val width = margin * 2 + labelW + totalUnits * unit + 40f
    val height = margin * 2 + (titleLr?.let { it.size.height + 8f } ?: 0f) + rows * rowH
    return DiagramContent(width, height) {
        var y = margin + (titleLr?.let { drawText(it, topLeft = Offset(margin, margin)); it.size.height + 8f } ?: 0f)
        var cursor = 0
        for (section in d.sections) {
            drawText(measurer.measure(AnnotatedString(section.name), secStyle), topLeft = Offset(margin, y + 6f))
            y += rowH
            for (task in section.tasks) {
                drawText(measurer.measure(AnnotatedString(task.name), nameStyle), topLeft = Offset(margin, y + 6f))
                val barX = margin + labelW + cursor * unit
                val barW = task.duration * unit
                drawRoundRect(theme.accent, Offset(barX, y + 4f), androidx.compose.ui.geometry.Size(barW, rowH - 12f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
                cursor += task.duration
                y += rowH
            }
        }
    }
}

// --- Git graph (branch lanes) ---

private fun gitGraphContent(d: MermaidDiagram.GitGraph, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    val branches = d.commits.map { it.branch }.distinct().ifEmpty { listOf("main") }
    val laneY = branches.withIndex().associate { it.value to (40f + it.index * 60f) }
    val margin = 16f; val stepX = 70f
    val nameStyle = TextStyle(fontSize = 12.sp, color = theme.textSecondary)
    val width = margin * 2 + 120f + d.commits.size * stepX
    val height = margin * 2 + branches.size * 60f + 20f
    return DiagramContent(width, height) {
        branches.forEach { b ->
            val y = laneY[b]!!
            drawLine(theme.border, Offset(margin + 100f, y), Offset(width - margin, y), strokeWidth = 2f)
            drawText(measurer.measure(AnnotatedString(b), nameStyle), topLeft = Offset(margin, y - 8f))
        }
        var x = margin + 120f
        d.commits.forEach { c ->
            val y = laneY[c.branch] ?: 40f
            when (c.type) {
                "merge" -> {
                    val target = laneY[c.id] ?: y
                    drawLine(theme.accent, Offset(x, target), Offset(x, y), strokeWidth = 2f)
                    drawCircle(theme.accent, 7f, Offset(x, y))
                }
                "branch" -> drawCircle(theme.textTertiary, 5f, Offset(x, y))
                else -> drawCircle(theme.accent, 7f, Offset(x, y))
            }
            x += stepX
        }
    }
}

// --- Journey (scored steps) ---

private fun journeyContent(d: MermaidDiagram.Journey, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    val tasks = d.sections.flatMap { s -> s.tasks.map { s.name to it } }
    if (tasks.isEmpty()) return DiagramContent(0f, 0f) {}
    val style = TextStyle(fontSize = 11.sp, color = theme.textPrimary, textAlign = TextAlign.Center)
    val margin = 20f; val stepX = 120f; val maxScore = 5f; val chartH = 140f
    val titleLr = d.title?.let { measurer.measure(AnnotatedString(it), TextStyle(fontSize = 16.sp, color = theme.textPrimary)) }
    val top = margin + (titleLr?.let { it.size.height + 8f } ?: 0f)
    val width = margin * 2 + tasks.size * stepX
    val height = top + chartH + 50f
    return DiagramContent(width, height) {
        titleLr?.let { drawText(it, topLeft = Offset(margin, margin)) }
        val baseY = top + chartH
        var prev: Offset? = null
        tasks.forEachIndexed { i, (_, task) ->
            val x = margin + stepX / 2 + i * stepX
            val y = baseY - (task.score.coerceIn(0, 5) / maxScore) * chartH
            prev?.let { drawLine(theme.accent, it, Offset(x, y), strokeWidth = 2f) }
            drawCircle(theme.accent, 8f, Offset(x, y))
            val lr = measurer.measure(AnnotatedString(task.name), style)
            drawText(lr, topLeft = Offset(x - lr.size.width / 2f, baseY + 8f))
            prev = Offset(x, y)
        }
    }
}

// --- Timeline (periods with events) ---

private fun timelineContent(d: MermaidDiagram.Timeline, theme: MarkdownTheme, measurer: TextMeasurer): DiagramContent {
    if (d.periods.isEmpty()) return DiagramContent(0f, 0f) {}
    val periodStyle = TextStyle(fontSize = 14.sp, color = theme.accent, textAlign = TextAlign.Center)
    val eventStyle = TextStyle(fontSize = 12.sp, color = theme.textPrimary, textAlign = TextAlign.Center)
    val margin = 20f; val colW = 160f
    val titleLr = d.title?.let { measurer.measure(AnnotatedString(it), TextStyle(fontSize = 16.sp, color = theme.textPrimary)) }
    val top = margin + (titleLr?.let { it.size.height + 8f } ?: 0f)
    val maxEvents = d.periods.maxOf { it.events.size }
    val width = margin * 2 + d.periods.size * colW
    val height = top + 60f + maxEvents * 30f + margin
    return DiagramContent(width, height) {
        titleLr?.let { drawText(it, topLeft = Offset(margin, margin)) }
        val lineY = top + 30f
        drawLine(theme.border, Offset(margin, lineY), Offset(width - margin, lineY), strokeWidth = 2f)
        d.periods.forEachIndexed { i, period ->
            val cx = margin + colW / 2 + i * colW
            drawCircle(theme.accent, 6f, Offset(cx, lineY))
            val pl = measurer.measure(AnnotatedString(period.label), periodStyle)
            drawText(pl, topLeft = Offset(cx - pl.size.width / 2f, top))
            period.events.forEachIndexed { j, e ->
                val el = measurer.measure(AnnotatedString(e), eventStyle)
                drawText(el, topLeft = Offset(cx - el.size.width / 2f, lineY + 16f + j * 30f))
            }
        }
    }
}
