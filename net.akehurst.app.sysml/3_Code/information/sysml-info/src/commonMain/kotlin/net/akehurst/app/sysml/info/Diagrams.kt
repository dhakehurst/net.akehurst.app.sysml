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

package net.akehurst.app.sysml.info

/** What a diagram shows: a selection of elements plus a view configuration. [REQ-9.3] */
data class DiagramDefinition(
    val id: String,
    val name: String,
    val selection: ElementSelection,
    val configuration: ViewConfiguration = ViewConfiguration()
)

/** [REQ-9.3.1] */
data class ElementSelection(
    /** elements explicitly selected, by qualified (or unique simple) name */
    val elementNames: List<String> = emptyList(),
    /** select every element of these kinds, e.g. "part def" */
    val elementKinds: Set<String> = emptySet(),
    /** also include elements owned by selected elements, to this depth */
    val nestingDepth: Int = 0
)

enum class LayoutDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT }

/** [REQ-9.3.2] */
data class ViewConfiguration(
    val relationshipKinds: Set<RelationshipKind> = setOf(RelationshipKind.SPECIALISATION, RelationshipKind.TYPING, RelationshipKind.CONNECTION),
    /** show owned features (attributes, ports, ...) inside the node */
    val showFeatures: Boolean = true,
    /** feature kinds to show when [showFeatures] is true, empty means all */
    val featureKinds: Set<String> = setOf("attribute", "port", "part"),
    val direction: LayoutDirection = LayoutDirection.TOP_TO_BOTTOM
)

/** A generated and laid out diagram. [REQ-9.1], [REQ-9.2] */
data class Diagram(
    val definition: DiagramDefinition,
    val nodes: List<DiagramNode>,
    val edges: List<DiagramEdge>,
    val width: Double,
    val height: Double
)

data class DiagramNode(
    val elementQualifiedName: String,
    val title: String,
    val kind: String,
    val features: List<String>,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
) {
    val centreX: Double get() = x + width / 2
    val centreY: Double get() = y + height / 2
}

data class DiagramEdge(
    val sourceQualifiedName: String,
    val targetQualifiedName: String,
    val kind: RelationshipKind,
    /** poly-line through these points, (x,y) pairs */
    val points: List<Pair<Double, Double>>
)
