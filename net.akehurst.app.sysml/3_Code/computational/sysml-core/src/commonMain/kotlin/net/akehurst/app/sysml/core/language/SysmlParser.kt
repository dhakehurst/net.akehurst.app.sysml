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

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault

/** a problem found when reading SysML text */
data class TextIssue(
    val file: String,
    val line: Int,
    val column: Int,
    val message: String
)

class ParseOutcome(
    val file: String,
    /** the RootNamespace node, null when the text could not be parsed */
    val root: CstNode?,
    val issues: List<TextIssue>
)

/** Parses SysML v2 textual notation with the agl grammar [SysmlGrammar]. */
object SysmlParser {

    // building the parser from the grammar takes a while, so it is done once
    private val processor by lazy {
        val res = Agl.processorFromStringSimple(GrammarString(SysmlGrammar.text))
        res.processor ?: error("SysML grammar has errors: ${res.issues}")
    }

    fun parse(file: String, text: String): ParseOutcome {
        val result = processor.parse(text, ParseOptionsDefault(goalRuleName = SysmlGrammar.GOAL_RULE))
        val issues = result.issues.errors.map {
            TextIssue(file, it.location?.line ?: 0, it.location?.column ?: 0, it.message)
        }
        val sppt = result.sppt
        val root = if (null == sppt) null else {
            val b = CstBuilder(text)
            sppt.traverseTreeDepthFirst(b, false)
            b.root.child(SysmlGrammar.GOAL_RULE)
        }
        return ParseOutcome(file, root, issues)
    }
}
