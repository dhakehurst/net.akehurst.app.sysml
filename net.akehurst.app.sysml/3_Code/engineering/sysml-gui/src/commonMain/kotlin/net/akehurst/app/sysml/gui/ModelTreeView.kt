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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.akehurst.app.sysml.info.ModelElement
import net.akehurst.app.sysml.info.SysmlModel
import net.akehurst.kotlin.compose.components.tree.TreeView
import net.akehurst.kotlin.compose.components.tree.TreeViewNode
import net.akehurst.kotlin.compose.components.tree.TreeViewStateHolder

private const val ELEMENT = "element"

/**
 * The composition (ownership) tree of the model, using the TreeView of net.akehurst.kotlin.compose components. [REQ-6.5]
 * Children are created when a node is first expanded.
 */
@Composable
fun ModelTreeView(model: SysmlModel, modifier: Modifier = Modifier, onSelect: (ModelElement) -> Unit = {}) {
    val stateHolder = remember { TreeViewStateHolder() }
    LaunchedEffect(model) {
        val roots = model.elements.filter { null == it.ownerQualifiedName || null == model.element(it.ownerQualifiedName!!) }
        stateHolder.updateItems(roots.map { treeNode(model, it) })
    }
    TreeView(
        stateHolder = stateHolder,
        onSelectItem = { item -> (item.data[ELEMENT] as? ModelElement)?.let(onSelect) },
        modifier = modifier
    )
}

private fun treeNode(model: SysmlModel, element: ModelElement): TreeViewNode {
    val children = model.childrenOf(element.qualifiedName)
    return TreeViewNode(element.qualifiedName).also { n ->
        n.data[ELEMENT] = element
        n.hasChildren = children.isNotEmpty()
        n.fetchChildren = { children.map { treeNode(model, it) } }
        n.content = {
            Row {
                Text(element.kind, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text(element.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                element.typeName?.let {
                    Text(" : $it", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
