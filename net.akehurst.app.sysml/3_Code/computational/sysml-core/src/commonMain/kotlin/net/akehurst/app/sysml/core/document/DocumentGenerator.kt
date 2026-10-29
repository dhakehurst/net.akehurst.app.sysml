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

package net.akehurst.app.sysml.core.document

import net.akehurst.app.sysml.api.DiagramService
import net.akehurst.app.sysml.api.DocumentService
import net.akehurst.app.sysml.info.*

/**
 * Generates Markdown documents from [DocumentTemplate]s. [REQ-10]
 *
 * Placeholders:
 * - `{{project.name}}`, `{{project.description}}`, `{{process.name}}`, `{{process.description}}`, `{{date}}`
 * - `{{elements:<kind>}}`  a table of the elements of that kind, e.g. `{{elements:requirement def}}`
 * - `{{diagram:<name>}}`   the project diagram with that name, as SVG
 * - `{{process.steps}}`    a list of the process steps
 * - `{{model}}`            all SysML v2 text of the project
 */
class DocumentGenerator(
    private val diagrams: DiagramService,
    private val today: () -> String
) : DocumentService {

    private val placeholder = Regex("""\{\{\s*([^}:]+?)\s*(?::\s*([^}]+?)\s*)?\}\}""")

    override fun generate(template: DocumentTemplate, project: Project, process: ProcessDefinition, model: SysmlModel, modelText: Map<String, String>): String =
        placeholder.replace(template.body) { m ->
            val key = m.groupValues[1]
            val arg = m.groupValues[2]
            when (key) {
                "project.name" -> project.name
                "project.description" -> project.description
                "process.name" -> process.name
                "process.description" -> process.description
                "process.steps" -> process.steps.joinToString("\n") { "1. **${it.name}**: ${it.instructions}" }
                "date" -> today()
                "elements" -> elementsTable(model, arg)
                "diagram" -> diagram(project, model, arg)
                "model" -> modelText.entries.joinToString("\n\n") { (path, text) -> "`$path`\n\n```sysml\n$text\n```" }
                else -> m.value // unknown placeholders are left as they are
            }
        }

    private fun elementsTable(model: SysmlModel, kind: String): String {
        val els = model.elements.filter { it.kind == kind }
        if (els.isEmpty()) return "_No $kind elements._"
        return buildString {
            appendLine("| Name | Qualified name | Type |")
            appendLine("|---|---|---|")
            els.forEach { appendLine("| ${it.name} | ${it.qualifiedName} | ${it.typeName ?: ""} |") }
        }.trimEnd()
    }

    private fun diagram(project: Project, model: SysmlModel, name: String): String {
        val def = project.diagrams.firstOrNull { it.name == name } ?: return "_No diagram named '$name'._"
        return diagrams.toSvg(diagrams.generate(def, model))
    }
}
