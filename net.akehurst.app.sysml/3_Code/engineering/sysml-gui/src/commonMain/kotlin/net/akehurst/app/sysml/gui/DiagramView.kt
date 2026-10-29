/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.app.sysml.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import net.akehurst.app.sysml.info.Diagram
import net.akehurst.app.sysml.info.DiagramEdge
import net.akehurst.app.sysml.info.RelationshipKind
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Draws a generated, laid out diagram. [REQ-9] */
@Composable
fun DiagramView(diagram: Diagram) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // the layout works in pixels of 7px wide monospace characters, so draw at 1 layout unit = 1 px
    val widthDp = with(density) { diagram.width.toFloat().toDp() }
    val heightDp = with(density) { diagram.height.toFloat().toDp() }
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = with(density) { 11.5f.toSp() }, color = Color.Black)
    Box(Modifier.horizontalScroll(rememberScrollState())) {
        Canvas(Modifier.size(widthDp, heightDp)) {
            drawRect(Color.White, size = size)
            diagram.edges.forEach { drawEdge(it) }
            diagram.nodes.forEach { n ->
                val topLeft = Offset(n.x.toFloat(), n.y.toFloat())
                val boxSize = Size(n.width.toFloat(), n.height.toFloat())
                drawRect(Color.White, topLeft, boxSize)
                drawRect(Color.Black, topLeft, boxSize, style = Stroke(1f))
                fun centred(text: String, y: Float, s: TextStyle) {
                    val m = measurer.measure(text, s)
                    drawText(m, topLeft = Offset((n.centreX - m.size.width / 2).toFloat(), y))
                }
                centred(n.kind, (n.y + 8).toFloat(), style)
                centred(n.title, (n.y + 8 + 16).toFloat(), style.copy(fontWeight = FontWeight.Bold))
                if (n.features.isNotEmpty()) {
                    val sepY = (n.y + 8 + 32 + 4).toFloat()
                    drawLine(Color.Black, Offset(n.x.toFloat(), sepY), Offset((n.x + n.width).toFloat(), sepY))
                    n.features.forEachIndexed { i, f ->
                        drawText(measurer.measure(f, style), topLeft = Offset((n.x + 8).toFloat(), sepY + 4 + i * 16))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawEdge(e: DiagramEdge) {
    val (x1, y1) = e.points.first()
    val (x2, y2) = e.points.last()
    val start = Offset(x1.toFloat(), y1.toFloat())
    val end = Offset(x2.toFloat(), y2.toFloat())
    val dashed = if (e.kind == RelationshipKind.SATISFY) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
    drawLine(Color.Black, start, end, pathEffect = dashed)
    val angle = atan2(end.y - start.y, end.x - start.x)
    fun at(o: Offset, len: Float, a: Float) = Offset(o.x + len * cos(a), o.y + len * sin(a))
    when (e.kind) {
        RelationshipKind.SPECIALISATION -> {
            // hollow triangle at the general end
            val p = Path().apply {
                moveTo(end.x, end.y)
                val l = at(end, 12f, angle + 2.7f); lineTo(l.x, l.y)
                val r = at(end, 12f, angle - 2.7f); lineTo(r.x, r.y)
                close()
            }
            drawPath(p, Color.White)
            drawPath(p, Color.Black, style = Stroke(1f))
        }

        RelationshipKind.TYPING, RelationshipKind.OWNERSHIP -> {
            // filled diamond at the owner end, open arrow at the other
            val p = Path().apply {
                moveTo(start.x, start.y)
                val a = at(start, 9f, angle + 0.45f); lineTo(a.x, a.y)
                val tip = at(start, 16f, angle); lineTo(tip.x, tip.y)
                val b = at(start, 9f, angle - 0.45f); lineTo(b.x, b.y)
                close()
            }
            drawPath(p, Color.Black)
            drawLine(Color.Black, end, at(end, 10f, angle + 2.6f))
            drawLine(Color.Black, end, at(end, 10f, angle - 2.6f))
        }

        RelationshipKind.SATISFY -> {
            drawLine(Color.Black, end, at(end, 10f, angle + 2.6f))
            drawLine(Color.Black, end, at(end, 10f, angle - 2.6f))
        }

        RelationshipKind.CONNECTION -> {}
    }
}
