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
import kotlin.math.abs
import kotlin.math.max

/**
 * Generates diagrams from a selection of model elements and a view configuration, [REQ-9.1], [REQ-9.3]
 * and lays them out automatically with a simple layered layout. [REQ-9.2]
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
        val names = boxes.map { it.element.qualifiedName }
        // 'upper' end of each edge: the general element is above the specific one, the owner above the owned / typed
        val down = edges.map { e ->
            if (e.kind == RelationshipKind.SPECIALISATION || e.kind == RelationshipKind.SATISFY) e.target to e.source else e.source to e.target
        }.filter { it.first != it.second }
        val layer = assignLayers(names, down)
        val layers = names.groupBy { layer[it] ?: 0 }.entries.sortedBy { it.key }.map { it.value.toMutableList() }
        orderLayers(layers, down)

        val box = boxes.associateBy { it.element.qualifiedName }
        val positions = mutableMapOf<String, Pair<Double, Double>>()
        var along = margin
        var maxAcross = 0.0
        val horizontal = direction == LayoutDirection.TOP_TO_BOTTOM
        for (l in layers) {
            var across = margin
            var depth = 0.0
            for (n in l) {
                val b = box[n]!!
                positions[n] = if (horizontal) across to along else along to across
                across += (if (horizontal) b.width else b.height) + nodeGap
                depth = max(depth, if (horizontal) b.height else b.width)
            }
            maxAcross = max(maxAcross, across)
            along += depth + layerGap
        }
        // centre each layer
        for (l in layers) {
            val last = box[l.last()]!!
            val end = positions[l.last()]!!.let { if (horizontal) it.first + last.width else it.second + last.height }
            val shift = (maxAcross - nodeGap - end) / 2
            l.forEach { n ->
                val p = positions[n]!!
                positions[n] = if (horizontal) (p.first + shift) to p.second else p.first to (p.second + shift)
            }
        }
        val nodes = boxes.map { b ->
            val p = positions[b.element.qualifiedName]!!
            DiagramNode(b.element.qualifiedName, b.element.name, b.keyword, b.features, p.first, p.second, b.width, b.height)
        }
        val nodeByName = nodes.associateBy { it.elementQualifiedName }
        val diagramEdges = edges.map { e ->
            val s = nodeByName[e.source]!!
            val t = nodeByName[e.target]!!
            DiagramEdge(e.source, e.target, e.kind, listOf(borderPoint(s, t.centreX, t.centreY), borderPoint(t, s.centreX, s.centreY)))
        }
        val width = nodes.maxOf { it.x + it.width } + margin
        val height = nodes.maxOf { it.y + it.height } + margin
        return Diagram(definition, nodes, diagramEdges, width, height)
    }

    /** longest path layering, ignoring edges that would make a cycle */
    private fun assignLayers(names: List<String>, down: List<Pair<String, String>>): Map<String, Int> {
        val outgoing = down.groupBy({ it.first }, { it.second })
        val layer = mutableMapOf<String, Int>()
        val visiting = mutableSetOf<String>()
        fun visit(n: String, depth: Int) {
            if (n in visiting) return // cycle
            if ((layer[n] ?: -1) >= depth) return
            layer[n] = depth
            visiting += n
            outgoing[n]?.forEach { visit(it, depth + 1) }
            visiting -= n
        }
        val hasIncoming = down.map { it.second }.toSet()
        names.filter { it !in hasIncoming }.forEach { visit(it, 0) }
        names.filter { it !in layer }.forEach { visit(it, 0) }
        return layer
    }

    /** one down and one up pass of barycentre ordering, to reduce crossings */
    private fun orderLayers(layers: List<MutableList<String>>, down: List<Pair<String, String>>) {
        fun pass(indices: IntProgression, neighbours: (String) -> List<String>) {
            for (i in indices) {
                val ref = layers[i - indices.step.let { if (it > 0) 1 else -1 }]
                val pos = ref.withIndex().associate { it.value to it.index.toDouble() }
                val keyed = layers[i].withIndex().map { (idx, n) ->
                    val ns = neighbours(n).mapNotNull { pos[it] }
                    n to (if (ns.isEmpty()) idx.toDouble() else ns.average())
                }
                layers[i].clear()
                layers[i].addAll(keyed.sortedBy { it.second }.map { it.first })
            }
        }
        if (layers.size < 2) return
        pass(1 until layers.size) { n -> down.filter { it.second == n }.map { it.first } }
        pass(layers.size - 2 downTo 0) { n -> down.filter { it.first == n }.map { it.second } }
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
