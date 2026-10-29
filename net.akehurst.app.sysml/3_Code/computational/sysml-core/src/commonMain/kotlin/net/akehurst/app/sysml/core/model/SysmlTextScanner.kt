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

package net.akehurst.app.sysml.core.model

import net.akehurst.app.sysml.api.SysmlModelReader
import net.akehurst.app.sysml.info.ModelElement
import net.akehurst.app.sysml.info.ModelRelationship
import net.akehurst.app.sysml.info.RelationshipKind
import net.akehurst.app.sysml.info.SysmlModel

/**
 * First cut reader of SysML v2 textual notation.
 * It finds declarations (packages, definitions and usages), their owners, types, specialisations,
 * `connect a to b` and `satisfy r by x`. It does not check the text is valid SysML.
 * To be replaced by a full parser (agl) producing the omg sysml model.
 */
class SysmlTextScanner : SysmlModelReader {

    companion object {
        /** keywords that start a declaration and make up its kind */
        val KIND_KEYWORDS = setOf(
            "package", "library", "standard",
            "part", "attribute", "port", "item", "action", "state", "transition", "constraint", "requirement",
            "concern", "case", "analysis", "verification", "use", "view", "viewpoint", "rendering",
            "connection", "interface", "allocation", "flow", "occurrence", "individual", "enum", "metadata",
            "calc", "def", "actor", "subject", "stakeholder", "objective"
        )

        /** keywords that may come before the kind and are not part of it */
        val MODIFIER_KEYWORDS = setOf(
            "abstract", "ref", "in", "out", "inout", "private", "public", "protected", "readonly",
            "derived", "end", "variation", "variant", "composite", "portion", "snapshot", "timeslice"
        )

        /** statements that are not declarations */
        val IGNORED_KEYWORDS = setOf("import", "alias", "doc", "comment", "language", "dependency", "filter", "expose", "render")
    }

    private sealed class Token {
        data class Word(val text: String) : Token()
        data class Symbol(val text: String) : Token()
    }

    override fun read(files: Map<String, String>): SysmlModel {
        val elements = mutableListOf<ModelElement>()
        val pending = mutableListOf<PendingRelationship>()
        files.forEach { (path, text) -> scanFile(path, text, elements, pending) }
        val model = SysmlModel(elements, emptyList())
        val relationships = mutableListOf<ModelRelationship>()
        elements.forEach { el ->
            el.ownerQualifiedName?.let { relationships += ModelRelationship(it, el.qualifiedName, RelationshipKind.OWNERSHIP) }
        }
        pending.forEach { p ->
            val source = resolve(model, p.source, p.contextOwner)
            val target = resolve(model, p.target, p.contextOwner)
            if (null != source && null != target) {
                relationships += ModelRelationship(source.qualifiedName, target.qualifiedName, p.kind)
            }
        }
        return SysmlModel(elements, relationships.distinct())
    }

    private data class PendingRelationship(
        val source: String,
        val target: String,
        val kind: RelationshipKind,
        /** owner in which the names appeared, used to resolve relative names */
        val contextOwner: String?
    )

    /** resolve a (possibly qualified, possibly dotted feature chain) name, searching outwards from the context */
    private fun resolve(model: SysmlModel, name: String, context: String?): ModelElement? {
        val qn = name.replace(".", "::")
        var ctx = context
        while (null != ctx) {
            model.element("$ctx::$qn")?.let { return it }
            ctx = model.element(ctx)?.ownerQualifiedName
        }
        return model.element(qn) ?: model.elements.filter { it.name == qn.substringAfterLast("::") }.firstOrNull()
    }

    private fun scanFile(path: String, text: String, elements: MutableList<ModelElement>, pending: MutableList<PendingRelationship>) {
        val tokens = tokenise(text)
        // stack of owner qualified names; null entries are anonymous blocks
        val owners = ArrayDeque<String?>()
        fun currentOwner(): String? = owners.lastOrNull { null != it }
        var statement = mutableListOf<Token>()
        var anonCount = 0
        for (tok in tokens) {
            when {
                tok is Token.Symbol && tok.text == ";" -> {
                    processStatement(path, statement, currentOwner(), elements, pending, opensBlock = false)
                    statement = mutableListOf()
                }

                tok is Token.Symbol && tok.text == "{" -> {
                    val el = processStatement(path, statement, currentOwner(), elements, pending, opensBlock = true)
                    owners.addLast(el?.qualifiedName ?: run { anonCount++; null })
                    statement = mutableListOf()
                }

                tok is Token.Symbol && tok.text == "}" -> {
                    if (statement.isNotEmpty()) processStatement(path, statement, currentOwner(), elements, pending, opensBlock = false)
                    statement = mutableListOf()
                    owners.removeLastOrNull()
                }

                else -> statement += tok
            }
        }
    }

    private fun processStatement(
        path: String,
        statement: List<Token>,
        owner: String?,
        elements: MutableList<ModelElement>,
        pending: MutableList<PendingRelationship>,
        opensBlock: Boolean
    ): ModelElement? {
        if (statement.isEmpty()) return null
        var i = 0
        fun word(at: Int) = (statement.getOrNull(at) as? Token.Word)?.text
        fun symbol(at: Int) = (statement.getOrNull(at) as? Token.Symbol)?.text

        // skip modifiers
        while (word(i) in MODIFIER_KEYWORDS) i++
        val first = word(i) ?: return null
        when (first) {
            in IGNORED_KEYWORDS -> return null
            "connect" -> {
                // connect a to b
                val toIdx = statement.indexOfFirst { it is Token.Word && it.text == "to" }
                val a = word(i + 1)
                val b = if (toIdx > 0) word(toIdx + 1) else null
                if (null != a && null != b) pending += PendingRelationship(a, b, RelationshipKind.CONNECTION, owner)
                return null
            }

            "satisfy" -> {
                // satisfy [requirement] R by X
                var j = i + 1
                if (word(j) == "requirement") j++
                val r = word(j)
                val byIdx = statement.indexOfFirst { it is Token.Word && it.text == "by" }
                val x = if (byIdx > 0) word(byIdx + 1) else owner
                if (null != r && null != x) pending += PendingRelationship(x, r, RelationshipKind.SATISFY, owner)
                return null
            }
        }
        if (first !in KIND_KEYWORDS) return null
        val kindWords = mutableListOf<String>()
        while (word(i) in KIND_KEYWORDS) {
            kindWords += word(i)!!
            i++
        }
        val kind = kindWords.joinToString(" ")
        // short name <id> is ignored
        if (symbol(i) == "<") {
            while (i < statement.size && symbol(i) != ">") i++
            i++
        }
        val name = word(i)
        if (null == name) {
            return null
        }
        i++
        val qualifiedName = if (null == owner) name else "$owner::$name"
        var typeName: String? = null
        while (i < statement.size) {
            when (symbol(i)) {
                ":", "defined" -> {
                    word(i + 1)?.let { typeName = it }
                    i += 2
                }

                ":>", "specializes", ":>>", "redefines", "subsets" -> {
                    word(i + 1)?.let { pending += PendingRelationship(qualifiedName, it, RelationshipKind.SPECIALISATION, owner) }
                    i += 2
                }

                "=", ":=" -> break

                else -> {
                    when (word(i)) {
                        "specializes", "subsets", "redefines" -> {
                            word(i + 1)?.let { pending += PendingRelationship(qualifiedName, it, RelationshipKind.SPECIALISATION, owner) }
                            i += 2
                        }

                        "defined" -> {
                            // defined by T
                            word(i + 2)?.let { typeName = it }
                            i += 3
                        }

                        else -> i++
                    }
                }
            }
        }
        val el = ModelElement(qualifiedName, name, kind, typeName, owner, path)
        elements += el
        typeName?.let { pending += PendingRelationship(qualifiedName, it, RelationshipKind.TYPING, owner) }
        return el
    }

    private fun tokenise(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                text.startsWith("//", i) -> {
                    while (i < n && text[i] != '\n') i++
                }

                text.startsWith("/*", i) -> {
                    val end = text.indexOf("*/", i + 2)
                    i = if (end < 0) n else end + 2
                }

                c == '"' -> {
                    i++
                    while (i < n && text[i] != '"') {
                        if (text[i] == '\\') i++
                        i++
                    }
                    i++
                    tokens += Token.Symbol("\"string\"")
                }

                c == '\'' -> {
                    val start = i + 1
                    i++
                    while (i < n && text[i] != '\'') i++
                    tokens += Token.Word(text.substring(start, minOf(i, n)))
                    i++
                }

                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (text[i].isLetterOrDigit() || text[i] == '_' || (text[i] == ':' && text.startsWith("::", i)) || (text[i] == '.' && i + 1 < n && text[i + 1].isLetter()))) {
                        i += if (text.startsWith("::", i)) 2 else 1
                    }
                    tokens += Token.Word(text.substring(start, i))
                }

                text.startsWith(":>>", i) -> {
                    tokens += Token.Symbol(":>>"); i += 3
                }

                text.startsWith(":>", i) -> {
                    tokens += Token.Symbol(":>"); i += 2
                }

                text.startsWith(":=", i) -> {
                    tokens += Token.Symbol(":="); i += 2
                }

                else -> {
                    tokens += Token.Symbol(c.toString()); i++
                }
            }
        }
        return tokens
    }
}
