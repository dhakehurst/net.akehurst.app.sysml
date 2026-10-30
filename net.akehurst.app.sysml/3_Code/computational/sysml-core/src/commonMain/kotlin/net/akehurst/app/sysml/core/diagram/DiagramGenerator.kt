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

import net.akehurst.app.sysml.api.DiagramService
import net.akehurst.app.sysml.info.*
import net.akehurst.kotlin.compose.layout.graph.EdgeRouting
import net.akehurst.kotlin.compose.layout.graph.SugiyamaEdge
import net.akehurst.kotlin.compose.layout.graph.SugiyamaLayout
import kotlin.math.abs

/**
 * Generates diagrams from a selection of model elements and a view configuration, [REQ-9.1], [REQ-9.3]
 * and lays them out automatically with the SugiyamaLayout of net.akehurst.kotlin.compose layout-graph. [REQ-9.2]
 */
class DiagramGenerator(
    val charWidth: Double = 7.0,
    val lineHeight: Double = 16.0,
    val padding: Double = 8.0,
    val layerGap: Double = 60.0,
    val nodeGap: Double = 30.0,
    val margin: Double = 20.0
) : DiagramService {

    override fun generate(definition: DiagramDefinition, model: SysmlModel): Diagram {
        val selected = select(definition.selection, model)
        val config = definition.configuration
        val selectedNames = selected.map { it.qualifiedName }.toSet()

        // features are shown inside their owner's node, and are not nodes themselves
        val nodes = selected.map { el ->
            val features = if (config.showFeatures) {
                model.childrenOf(el.qualifiedName)
                    .filter { it.qualifiedName !in selectedNames }
                    .filter { config.featureKinds.isEmpty() || it.kind in config.featureKinds }
                    .map { f -> f.typeName?.let { "${f.kind} ${f.name} : $it" } ?: "${f.kind} ${f.name}" }
            } else emptyList()
            val title = el.name
            val keyword = "«${el.kind}»"
            val textWidth = (listOf(title, keyword) + features).maxOf { it.length } * charWidth
            val lines = 2 + features.size
            NodeBox(el, keyword, features, textWidth + 2 * padding, lines * lineHeight + 2 * padding + (if (features.isEmpty()) 0.0 else padding))
        }
        val edges = edgesFor(model, selectedNames, config)
        return layout(definition, nodes, edges, config.direction)
    }

    fun select(selection: ElementSelection, model: SysmlModel): List<ModelElement> {
        val result = linkedSetOf<ModelElement>()
        selection.elementNames.mapNotNull { model.resolve(it) }.forEach { result += it }
        if (selection.elementKinds.isNotEmpty()) {
            model.elements.filter { it.kind in selection.elementKinds }.forEach { result += it }
        }
        var frontier = result.toList()
        repeat(selection.nestingDepth) {
            frontier = frontier.flatMap { model.childrenOf(it.qualifiedName) }
            result += frontier
        }
        return result.toList()
    }

    private data class NodeBox(
        val element: ModelElement,
        val keyword: String,
        val features: List<String>,
        val width: Double,
        val height: Double
    )

    private data class Edge(val source: String, val target: String, val kind: RelationshipKind)

    private fun edgesFor(model: SysmlModel, nodeNames: Set<String>, config: ViewConfiguration): List<Edge> {
        // an edge whose end is a feature (not shown as a node) is lifted to the feature's owner
        fun lift(qn: String): String? {
            var cur: String? = qn
            while (null != cur) {
                if (cur in nodeNames) return cur
                cur = model.element(cur)?.ownerQualifiedName
            }
            return null
        }
        return model.relationships
            .filter { it.kind in config.relationshipKinds }
            .mapNotNull { r ->
                val s = lift(r.sourceQualifiedName)
                val t = lift(r.targetQualifiedName)
                if (null != s && null != t && s != t) Edge(s, t, r.kind) else null
            }
            .distinct()
    }

    private fun layout(definition: DiagramDefinition, boxes: List<NodeBox>, edges: List<Edge>, direction: LayoutDirection): Diagram {
        if (boxes.isEmpty()) return Diagram(definition, emptyList(), emptyList(), 2 * margin, 2 * margin)
        // SugiyamaLayout lays out top to bottom, for left to right the width and height are swapped, and so are the resulting coordinates
        val horizontal = direction == LayoutDirection.TOP_TO_BOTTOM
        val box = boxes.associateBy { it.element.qualifiedName }
        val sugiyama = SugiyamaLayout<String>(
            nodeWidth = { n -> box[n]!!.let { if (horizontal) it.width else it.height } },
            nodeHeight = { n -> box[n]!!.let { if (horizontal) it.height else it.width } },
            layerSpacing = layerGap,
            nodeSpacing = nodeGap,
            edgeRouting = EdgeRouting.DIRECT
        )
        // the 'upper' end of each edge: the general element is above the specific one, the owner above the owned / typed
        val layoutEdges = edges.mapIndexed { i, e ->
            val reversed = e.kind == RelationshipKind.SPECIALISATION || e.kind == RelationshipKind.SATISFY
            val le = if (reversed) SugiyamaEdge("e$i", e.target, e.source) else SugiyamaEdge("e$i", e.source, e.target)
            Triple(e, le, reversed)
        }
        val data = sugiyama.layoutGraph(boxes.map { it.element.qualifiedName }, layoutEdges.map { it.second })
        fun point(p: Pair<Double, Double>) = if (horizontal) (p.first + margin) to (p.second + margin) else (p.second + margin) to (p.first + margin)

        val nodes = boxes.map { b ->
            val p = point(data.nodePositions[b.element.qualifiedName] ?: (0.0 to 0.0))
            DiagramNode(b.element.qualifiedName, b.element.name, b.keyword, b.features, p.first, p.second, b.width, b.height)
        }
        val nodeByName = nodes.associateBy { it.elementQualifiedName }
        val diagramEdges = layoutEdges.map { (e, le, reversed) ->
            val route = data.edgeRoutes[le]?.map { point(it) }
            val points = if (null == route || route.size < 2) {
                val s = nodeByName[e.source]!!
                val t = nodeByName[e.target]!!
                listOf(borderPoint(s, t.centreX, t.centreY), borderPoint(t, s.centreX, s.centreY))
            } else if (reversed) route.reversed() else route
            DiagramEdge(e.source, e.target, e.kind, points)
        }
        val width = maxOf(nodes.maxOf { it.x + it.width }, diagramEdges.flatMap { it.points }.maxOfOrNull { it.first } ?: 0.0) + margin
        val height = maxOf(nodes.maxOf { it.y + it.height }, diagramEdges.flatMap { it.points }.maxOfOrNull { it.second } ?: 0.0) + margin
        return Diagram(definition, nodes, diagramEdges, width, height)
    }

    /** where the line from the centre of [n] towards (tx,ty) leaves the node's box */
    private fun borderPoint(n: DiagramNode, tx: Double, ty: Double): Pair<Double, Double> {
        val dx = tx - n.centreX
        val dy = ty - n.centreY
        if (dx == 0.0 && dy == 0.0) return n.centreX to n.centreY
        val sx = if (dx == 0.0) Double.MAX_VALUE else (n.width / 2) / abs(dx)
        val sy = if (dy == 0.0) Double.MAX_VALUE else (n.height / 2) / abs(dy)
        val s = minOf(sx, sy)
        return (n.centreX + dx * s) to (n.centreY + dy * s)
    }

    override fun toSvg(diagram: Diagram): String = SvgRenderer(lineHeight, padding).render(diagram)
}
