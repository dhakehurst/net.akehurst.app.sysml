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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.akehurst.app.sysml.info.*
import kotlin.random.Random

private enum class AdminTab(val label: String) {
    PROCESSES("Engineering processes"), PROJECT_TEMPLATES("Project templates"), DOCUMENT_TEMPLATES("Document templates"), USERS("Users")
}

private fun newId(prefix: String) = prefix + "-" + Random.nextLong().toULong().toString(16)

/** Administration: only shown to, and only usable by, admins. [REQ-3.3], [REQ-4] */
@Composable
fun AdminScreen(services: GuiServices, session: Session) {
    var tab by remember { mutableStateOf(AdminTab.PROCESSES) }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            AdminTab.entries.forEach { t -> Tab(selected = t == tab, onClick = { tab = t }, text = { Text(t.label) }) }
        }
        Box(Modifier.weight(1f).padding(8.dp)) {
            when (tab) {
                AdminTab.PROCESSES -> ProcessesAdmin(services, session)
                AdminTab.PROJECT_TEMPLATES -> ProjectTemplatesAdmin(services, session)
                AdminTab.DOCUMENT_TEMPLATES -> DocumentTemplatesAdmin(services, session)
                AdminTab.USERS -> UsersAdmin(services, session)
            }
        }
    }
}

@Composable
private fun ProcessesAdmin(services: GuiServices, session: Session) {
    var processes by remember { mutableStateOf(services.admin.processes()) }
    var selected by remember { mutableStateOf(processes.firstOrNull()) }
    var error by remember { mutableStateOf<String?>(null) }
    MasterDetail(processes, selected, { it.name }, { selected = it }, actions = {
        Button(onClick = {
            selected = ProcessDefinition(newId("process"), "New process", "", emptyList(), services.admin.processes().firstOrNull()?.agentStrategy ?: AgentStrategy("", "{{request}}"))
        }) { Text("New process") }
    }) {
        val p = selected
        if (null != p) {
            key(p.id) {
                ProcessEditor(p, onSave = { changed ->
                    try {
                        services.admin.saveProcess(session, changed)
                        processes = services.admin.processes()
                        selected = changed
                        error = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }, onDelete = {
                    services.admin.deleteProcess(session, p.id)
                    processes = services.admin.processes()
                    selected = processes.firstOrNull()
                })
            }
            ErrorText(error)
        }
    }
}

@Composable
private fun ProcessEditor(process: ProcessDefinition, onSave: (ProcessDefinition) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(process.name) }
    var description by remember { mutableStateOf(process.description) }
    var systemPrompt by remember { mutableStateOf(process.agentStrategy.systemPrompt) }
    var stepPrompt by remember { mutableStateOf(process.agentStrategy.stepPromptTemplate) }
    var steps by remember { mutableStateOf(process.steps) }
    var diagramKinds by remember { mutableStateOf(process.diagrams.joinToString("; ") { d -> d.name + "=" + d.selection.elementKinds.joinToString(",") }) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Field("Name", name, { name = it })
        Field("Description", description, { description = it }, singleLine = false)
        SectionTitle("Steps")
        steps.forEachIndexed { i, s ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    fun update(c: ProcessStep) {
                        steps = steps.toMutableList().also { it[i] = c }
                    }
                    Field("Step name", s.name, { update(s.copy(name = it)) })
                    Field("Instructions", s.instructions, { update(s.copy(instructions = it)) }, singleLine = false)
                    Field("Expected element kinds (comma separated)", s.expectedElementKinds.joinToString(", "), {
                        update(s.copy(expectedElementKinds = it.split(',').map { k -> k.trim() }.filter { k -> k.isNotEmpty() }))
                    })
                    Field("Target file", s.targetFile, { update(s.copy(targetFile = it)) })
                    Field("Extra prompt for the AI agent", s.agentPrompt, { update(s.copy(agentPrompt = it)) }, singleLine = false)
                    Row {
                        TextButton(enabled = i > 0, onClick = { steps = steps.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) } }) { Text("Up") }
                        TextButton(enabled = i < steps.size - 1, onClick = { steps = steps.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) } }) { Text("Down") }
                        TextButton(onClick = { steps = steps.toMutableList().also { l -> l.removeAt(i) } }) { Text("Remove") }
                    }
                }
            }
        }
        OutlinedButton(onClick = { steps = steps + ProcessStep(newId("step"), "New step", "") }) { Text("Add step") }
        SectionTitle("AI agent strategy")
        Field("System prompt", systemPrompt, { systemPrompt = it }, singleLine = false, minLines = 4)
        Field("Prompt for each request", stepPrompt, { stepPrompt = it }, singleLine = false, minLines = 6)
        Text("Placeholders: {{step.name}} {{step.instructions}} {{step.agentPrompt}} {{step.targetFile}} {{step.expectedElementKinds}} {{model}} {{request}}", style = MaterialTheme.typography.bodySmall)
        SectionTitle("Pre-selected diagrams")
        Field("name=kind,kind; name=kind …", diagramKinds, { diagramKinds = it })
        Row {
            Button(onClick = {
                onSave(
                    process.copy(
                        name = name,
                        description = description,
                        steps = steps,
                        agentStrategy = process.agentStrategy.copy(systemPrompt = systemPrompt, stepPromptTemplate = stepPrompt),
                        diagrams = parseDiagramList(diagramKinds, process.diagrams)
                    )
                )
            }) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

/** "Name=kind,kind; Other=kind" -> diagram definitions, keeping the configuration of existing ones */
private fun parseDiagramList(text: String, existing: List<DiagramDefinition>): List<DiagramDefinition> =
    text.split(';').map { it.trim() }.filter { it.contains('=') }.map { entry ->
        val name = entry.substringBefore('=').trim()
        val kinds = entry.substringAfter('=').split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val old = existing.firstOrNull { it.name == name }
        old?.copy(selection = old.selection.copy(elementKinds = kinds))
            ?: DiagramDefinition(newId("diagram"), name, ElementSelection(elementKinds = kinds))
    }

@Composable
private fun ProjectTemplatesAdmin(services: GuiServices, session: Session) {
    var templates by remember { mutableStateOf(services.admin.projectTemplates()) }
    var selected by remember { mutableStateOf(templates.firstOrNull()) }
    var error by remember { mutableStateOf<String?>(null) }
    MasterDetail(templates, selected, { it.name }, { selected = it }, actions = {
        Button(onClick = {
            selected = ProjectTemplate(newId("template"), "New template", "", services.admin.processes().firstOrNull()?.id ?: "")
        }) { Text("New template") }
    }) {
        val t = selected ?: return@MasterDetail
        key(t.id) {
            var name by remember { mutableStateOf(t.name) }
            var description by remember { mutableStateOf(t.description) }
            var processId by remember { mutableStateOf(t.processId) }
            var files by remember { mutableStateOf(t.initialFiles.entries.joinToString("\n") { (p, c) -> "=== $p\n$c" }) }
            var diagrams by remember { mutableStateOf(t.diagrams.joinToString("; ") { d -> d.name + "=" + d.selection.elementKinds.joinToString(",") }) }
            val docTemplates = services.admin.documentTemplates()
            var docIds by remember { mutableStateOf(t.documentTemplateIds.toSet()) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field("Name", name, { name = it })
                Field("Description", description, { description = it }, singleLine = false)
                Text("Engineering process")
                ChoiceList(services.admin.processes(), services.admin.process(processId), { it.name }) { processId = it.id }
                Text("Initial SysML files: each file starts with a line '=== path'")
                CodeField("Initial files", files, { files = it }, minLines = 8)
                SectionTitle("Pre-selected diagrams")
                Field("name=kind,kind; name=kind …", diagrams, { diagrams = it })
                SectionTitle("Document templates")
                docTemplates.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(d.id in docIds, { on -> docIds = if (on) docIds + d.id else docIds - d.id })
                        Text(d.name)
                    }
                }
                Row {
                    Button(onClick = {
                        val changed = t.copy(
                            name = name, description = description, processId = processId,
                            initialFiles = parseFiles(files), diagrams = parseDiagramList(diagrams, t.diagrams),
                            documentTemplateIds = docIds.toList()
                        )
                        try {
                            services.admin.saveProjectTemplate(session, changed)
                            templates = services.admin.projectTemplates()
                            selected = changed
                            error = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }) { Text("Save") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        services.admin.deleteProjectTemplate(session, t.id)
                        templates = services.admin.projectTemplates()
                        selected = templates.firstOrNull()
                    }) { Text("Delete") }
                }
                ErrorText(error)
            }
        }
    }
}

private fun parseFiles(text: String): Map<String, String> {
    val result = linkedMapOf<String, String>()
    var path: String? = null
    val content = StringBuilder()
    fun flush() {
        path?.let { result[it] = content.toString().trimEnd() + "\n" }
        content.clear()
    }
    text.lines().forEach { line ->
        if (line.startsWith("=== ")) {
            flush(); path = line.removePrefix("=== ").trim()
        } else {
            content.appendLine(line)
        }
    }
    flush()
    return result
}

@Composable
private fun DocumentTemplatesAdmin(services: GuiServices, session: Session) {
    var templates by remember { mutableStateOf(services.admin.documentTemplates()) }
    var selected by remember { mutableStateOf(templates.firstOrNull()) }
    var error by remember { mutableStateOf<String?>(null) }
    MasterDetail(templates, selected, { it.name }, { selected = it }, actions = {
        Button(onClick = { selected = DocumentTemplate(newId("doc"), "New document template", "# {{project.name}}\n") }) { Text("New template") }
    }) {
        val t = selected ?: return@MasterDetail
        key(t.id) {
            var name by remember { mutableStateOf(t.name) }
            var body by remember { mutableStateOf(t.body) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field("Name", name, { name = it })
                Text(
                    "Placeholders: {{project.name}} {{project.description}} {{process.name}} {{process.description}} {{process.steps}} {{date}} {{elements:<kind>}} {{diagram:<name>}} {{model}}",
                    style = MaterialTheme.typography.bodySmall
                )
                CodeField("Template (Markdown)", body, { body = it }, minLines = 20)
                Row {
                    Button(onClick = {
                        val changed = t.copy(name = name, body = body)
                        try {
                            services.admin.saveDocumentTemplate(session, changed)
                            templates = services.admin.documentTemplates()
                            selected = changed
                            error = null
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }) { Text("Save") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        services.admin.deleteDocumentTemplate(session, t.id)
                        templates = services.admin.documentTemplates()
                        selected = templates.firstOrNull()
                    }) { Text("Delete") }
                }
                ErrorText(error)
            }
        }
    }
}

@Composable
private fun UsersAdmin(services: GuiServices, session: Session) {
    var users by remember { mutableStateOf(services.admin.users(session)) }
    var userId by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SectionTitle("Users")
        users.forEach { u ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${u.name} (${u.id}, ${u.email})", Modifier.width(420.dp))
                Checkbox(u.isAdmin, { on ->
                    val changed = u.copy(roles = if (on) u.roles + Role.ADMIN else u.roles - Role.ADMIN)
                    services.admin.saveUser(session, changed)
                    users = services.admin.users(session)
                }, enabled = u.id != session.user.id)
                Text("Admin")
            }
        }
        SectionTitle("Add a user (logs in with single sign on, by e-mail)")
        Field("Id", userId, { userId = it })
        Field("Name", newName, { newName = it })
        Field("E-mail", newEmail, { newEmail = it })
        Button(enabled = userId.isNotBlank() && newEmail.isNotBlank(), onClick = {
            services.admin.saveUser(session, User(userId, newName.ifBlank { userId }, newEmail))
            users = services.admin.users(session)
            userId = ""; newName = ""; newEmail = ""
        }) { Text("Add user") }
    }
}
