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

package net.akehurst.app.sysml.core.language

import net.akehurst.language.sppt.api.PathFunction
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.api.SpptWalker
import net.akehurst.language.sppt.api.TreeData

/**
 * A concrete syntax tree node, made from an agl shared packed parse tree.
 * Nodes are named by the grammar rule that matched them; leaves (literals and patterns) have [text].
 */
class CstNode(
    val rule: String,
    val start: Int,
    val end: Int,
    val text: String?
) {
    val children = mutableListOf<CstNode>()

    val isLeaf: Boolean get() = null != text

    /** the first child matching [rule] */
    fun child(rule: String): CstNode? = children.firstOrNull { it.rule == rule }

    fun children(rule: String): List<CstNode> = children.filter { it.rule == rule }

    /** depth first search of the descendants, not descending into nodes for which [stopAt] is true */
    fun find(rule: String, stopAt: (CstNode) -> Boolean = { false }): CstNode? {
        for (c in children) {
            if (c.rule == rule) return c
            if (!stopAt(c)) c.find(rule, stopAt)?.let { return it }
        }
        return null
    }

    fun findAll(rule: String, stopAt: (CstNode) -> Boolean = { false }): List<CstNode> {
        val result = mutableListOf<CstNode>()
        fun walk(n: CstNode) {
            for (c in n.children) {
                if (c.rule == rule) result += c
                else if (!stopAt(c)) walk(c)
            }
        }
        walk(this)
        return result
    }

    /** the text this node matched, without skipped white space and comments between its leaves */
    fun leafText(): String = if (isLeaf) text!! else children.joinToString("") { it.leafText() }

    fun dump(indent: String = "", sb: StringBuilder = StringBuilder()): StringBuilder {
        sb.append(indent).append(rule)
        if (isLeaf) sb.append(" '").append(text).append("'")
        sb.append('\n')
        children.forEach { it.dump("$indent  ", sb) }
        return sb
    }

    override fun toString(): String = if (isLeaf) "$rule '$text'" else rule
}

/**
 * Builds a [CstNode] tree while walking an agl parse tree.
 * Where the parse is ambiguous only the first alternative is kept; pseudo rules (groups and lists) are flattened.
 */
class CstBuilder(private val input: String) : SpptWalker {
    val root = CstNode("<root>", 0, input.length, null)

    // null entries mark subtrees that are ignored
    private val stack = ArrayDeque<CstNode?>().apply { addLast(root) }

    private fun keep(ni: SpptDataNodeInfo) = ni.parentAlt.index == 0 && null != stack.last()

    override fun beginTree() {}
    override fun endTree() {}
    override fun skip(skipData: TreeData) {}

    override fun leaf(nodeInfo: SpptDataNodeInfo) {
        if (!keep(nodeInfo)) return
        val n = nodeInfo.node
        if (n.rule.isEmptyTerminal || n.rule.isEmptyListTerminal) return
        stack.last()!!.children += CstNode(n.rule.tag, n.startPosition, n.nextInputNoSkip, input.substring(n.startPosition, n.nextInputNoSkip))
    }

    override fun beginBranch(nodeInfo: SpptDataNodeInfo) {
        if (!keep(nodeInfo)) {
            stack.addLast(null); return
        }
        val n = nodeInfo.node
        if (n.rule.isPseudo) {
            stack.addLast(stack.last()); return
        }
        val c = CstNode(n.rule.tag, n.startPosition, n.nextInputNoSkip, null)
        stack.last()!!.children += c
        stack.addLast(c)
    }

    override fun endBranch(nodeInfo: SpptDataNodeInfo) {
        stack.removeLast()
    }

    override fun beginEmbedded(nodeInfo: SpptDataNodeInfo) = beginBranch(nodeInfo)
    override fun endEmbedded(nodeInfo: SpptDataNodeInfo) = endBranch(nodeInfo)
    override fun treeError(msg: String, path: PathFunction) = error(msg)
}
