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

package net.akehurst.app.sysml.core.diagram

import net.akehurst.app.sysml.info.Diagram
import net.akehurst.app.sysml.info.RelationshipKind

/** Renders a laid out [Diagram] as SVG, e.g. for documents. [REQ-10] */
class SvgRenderer(
    private val lineHeight: Double = 16.0,
    private val padding: Double = 8.0
) {

    fun render(diagram: Diagram): String = buildString {
        appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="${n(diagram.width)}" height="${n(diagram.height)}" viewBox="0 0 ${n(diagram.width)} ${n(diagram.height)}" font-family="monospace" font-size="12">""")
        appendLine("""  <defs>""")
        appendLine("""    <marker id="general" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="12" markerHeight="12" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="white" stroke="black"/></marker>""")
        appendLine("""    <marker id="arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10" fill="none" stroke="black"/></marker>""")
        appendLine("""    <marker id="composite" viewBox="0 0 20 10" refX="0" refY="5" markerWidth="16" markerHeight="8" orient="auto-start-reverse"><path d="M0,5 L10,0 L20,5 L10,10 z" fill="black"/></marker>""")
        appendLine("""  </defs>""")
        diagram.edges.forEach { e ->
            val (x1, y1) = e.points.first()
            val (x2, y2) = e.points.last()
            val style = when (e.kind) {
                RelationshipKind.SPECIALISATION -> """marker-end="url(#general)""""
                RelationshipKind.TYPING, RelationshipKind.OWNERSHIP -> """marker-start="url(#composite)" marker-end="url(#arrow)""""
                RelationshipKind.CONNECTION -> ""
                RelationshipKind.SATISFY -> """marker-end="url(#arrow)" stroke-dasharray="6,4""""
            }
            appendLine("""  <line x1="${n(x1)}" y1="${n(y1)}" x2="${n(x2)}" y2="${n(y2)}" stroke="black" $style/>""")
            if (e.kind == RelationshipKind.SATISFY) {
                appendLine("""  <text x="${n((x1 + x2) / 2)}" y="${n((y1 + y2) / 2)}" text-anchor="middle">«satisfy»</text>""")
            }
        }
        diagram.nodes.forEach { node ->
            appendLine("""  <g>""")
            appendLine("""    <rect x="${n(node.x)}" y="${n(node.y)}" width="${n(node.width)}" height="${n(node.height)}" fill="white" stroke="black" rx="${if (node.kind.contains("def")) 0 else 6}"/>""")
            val cx = node.x + node.width / 2
            appendLine("""    <text x="${n(cx)}" y="${n(node.y + padding + lineHeight * 0.8)}" text-anchor="middle">${esc(node.kind)}</text>""")
            appendLine("""    <text x="${n(cx)}" y="${n(node.y + padding + lineHeight * 1.8)}" text-anchor="middle" font-weight="bold">${esc(node.title)}</text>""")
            if (node.features.isNotEmpty()) {
                val sepY = node.y + padding + lineHeight * 2 + padding / 2
                appendLine("""    <line x1="${n(node.x)}" y1="${n(sepY)}" x2="${n(node.x + node.width)}" y2="${n(sepY)}" stroke="black"/>""")
                node.features.forEachIndexed { i, f ->
                    appendLine("""    <text x="${n(node.x + padding)}" y="${n(sepY + padding / 2 + lineHeight * (i + 0.8))}">${esc(f)}</text>""")
                }
            }
            appendLine("""  </g>""")
        }
        appendLine("</svg>")
    }

    private fun n(d: Double): String = ((d * 10).toLong() / 10.0).toString()

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
