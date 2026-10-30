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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.akehurst.app.sysml.api.ModelRepository
import net.akehurst.app.sysml.info.*

private enum class ProjectTab(val label: String) {
    PROCESS("Process"), MODEL("Model"), DIAGRAMS("Diagrams"), DOCUMENTS("Documents"), BASELINES("Baselines")
}

/** Everything to do with one project. */
@Composable
fun ProjectScreen(services: GuiServices, session: Session, project: Project, onProjectChanged: (Project) -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember(project.id) { services.projects.repository(session, project) }
    var files by remember(project.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(ProjectTab.PROCESS) }

    suspend fun reload() {
        try {
            files = repository.readAll()
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(project.id) {
        try {
            repository.open()
        } catch (e: Exception) {
            error = e.message
        }
        reload()
    }
    val model = remember(files) { services.modelReader.read(files) }
    val canEdit = services.authorisation.isAllowed(session.user, Permission.EDIT_MODEL, project)

    fun save(path: String, text: String, message: String) {
        scope.launch {
            try {
                repository.write(path, text, message)
                reload()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(project.name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(16.dp))
            Text(storageLabel(project.storage), style = MaterialTheme.typography.bodySmall)
        }
        ErrorText(error)
        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            ProjectTab.entries.forEach { t ->
                Tab(selected = t == tab, onClick = { tab = t }, text = { Text(t.label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            when (tab) {
                ProjectTab.PROCESS -> ProcessTab(services, session, project, files, canEdit, ::save)
                ProjectTab.MODEL -> ModelTab(files, model, canEdit, ::save)
                ProjectTab.DIAGRAMS -> DiagramsTab(services, session, project, model, onProjectChanged)
                ProjectTab.DOCUMENTS -> DocumentsTab(services, project, model, files)
                ProjectTab.BASELINES -> BaselinesTab(services, session, project)
            }
        }
    }
}

/** Guides the user through the process, with the AI agent writing SysML. [REQ-7], [REQ-8] */
@Composable
private fun ProcessTab(
    services: GuiServices,
    session: Session,
    project: Project,
    files: Map<String, String>,
    canEdit: Boolean,
    save: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val process = remember(project.processId) { services.process.process(project) }
    var state by remember(project.id) { mutableStateOf(services.process.state(project)) }
    var selectedStep by remember(project.id) { mutableStateOf(services.process.currentStep(project) ?: process.steps.firstOrNull()) }
    var request by remember { mutableStateOf("") }
    var proposal by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var apiKey by remember { mutableStateOf("") }
    val canAdvance = services.authorisation.isAllowed(session.user, Permission.ADVANCE_PROCESS, project)

    MasterDetail(
        items = process.steps,
        selected = selectedStep,
        label = { s -> (if (s.id in state.completedStepIds) "✓ " else "○ ") + s.name },
        onSelect = { selectedStep = it; proposal = ""; error = null },
        actions = {
            Spacer(Modifier.height(16.dp))
            Text(process.name, fontWeight = FontWeight.Bold)
            Text(process.description, style = MaterialTheme.typography.bodySmall)
        }
    ) {
        val step = selectedStep
        if (null == step) {
            Text("This process has no steps.")
            return@MasterDetail
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SectionTitle(step.name)
            Text(step.instructions)
            if (step.expectedElementKinds.isNotEmpty()) Text("Expected: " + step.expectedElementKinds.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            Text("Writes to: ${step.targetFile}", style = MaterialTheme.typography.bodySmall)
            Row {
                if (step.id in state.completedStepIds) {
                    OutlinedButton(enabled = canAdvance, onClick = { state = services.process.reopenStep(session, project, step.id) }) { Text("Reopen step") }
                } else {
                    Button(enabled = canAdvance, onClick = {
                        state = services.process.completeStep(session, project, step.id)
                        selectedStep = services.process.currentStep(project) ?: step
                    }) { Text("Mark step complete") }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("AI assistant")
            if (!services.ai.isAvailable) {
                Text("The AI agent needs an Anthropic API key.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field("API key", apiKey, { apiKey = it }, Modifier.weight(1f))
                    Button(onClick = { services.ai.configure(apiKey) }) { Text("Use key") }
                }
            }
            Field("What should the AI write?", request, { request = it }, singleLine = false, minLines = 3)
            Button(enabled = canEdit && !busy && services.ai.isAvailable && request.isNotBlank(), onClick = {
                scope.launch {
                    busy = true
                    error = null
                    try {
                        val modelText = files.entries.joinToString("\n\n") { (p, t) -> "// file: $p\n$t" }
                        proposal = services.ai.proposeSysml(process, step, modelText, request)
                    } catch (e: Exception) {
                        error = e.message ?: e.toString()
                    } finally {
                        busy = false
                    }
                }
            }) { Text(if (busy) "Working…" else "Ask the AI") }
            ErrorText(error)
            if (proposal.isNotEmpty()) {
                CodeField("Proposed content of ${step.targetFile}", proposal, { proposal = it })
                Row {
                    Button(enabled = canEdit, onClick = {
                        save(step.targetFile, proposal, "${step.name}: $request")
                        proposal = ""
                    }) { Text("Accept and save") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { proposal = "" }) { Text("Discard") }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Current ${step.targetFile}")
            SelectionContainer { Text(files[step.targetFile] ?: "(empty)", style = monospace) }
        }
    }
}

/** Edit the SysML v2 text directly. */
@Composable
private fun ModelTab(files: Map<String, String>, model: SysmlModel, canEdit: Boolean, save: (String, String, String) -> Unit) {
    var selected by remember { mutableStateOf(files.keys.firstOrNull()) }
    var text by remember(selected, files) { mutableStateOf(selected?.let { files[it] } ?: "") }
    var newFile by remember { mutableStateOf("") }
    MasterDetail(
        items = files.keys.toList(),
        selected = selected,
        label = { it },
        onSelect = { selected = it },
        actions = {
            Spacer(Modifier.height(16.dp))
            Field("New file", newFile, { newFile = it })
            Button(enabled = canEdit && newFile.isNotBlank(), onClick = {
                val path = if (newFile.endsWith(".sysml")) newFile else "$newFile.sysml"
                save(path, "package ${path.substringAfterLast('/').removeSuffix(".sysml")} {\n}\n", "Add $path")
                selected = path
                newFile = ""
            }) { Text("Add file") }
            Spacer(Modifier.height(16.dp))
            Text("Composition", fontWeight = FontWeight.Bold)
            ModelTreeView(model, Modifier.fillMaxWidth().weight(1f)) { el ->
                if (el.file.isNotEmpty()) selected = el.file
            }
        }
    ) {
        val path = selected
        if (null == path) {
            Text("No file selected.")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(path, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(enabled = canEdit && text != files[path], onClick = { save(path, text, "Edit $path") }) { Text("Save") }
            }
            val issues = model.issues.filter { it.file == path }
            issues.forEach { ErrorText("${it.line}:${it.column} ${it.message.lines().first()}") }
            CodeField("SysML v2", text, { if (canEdit) text = it }, Modifier.fillMaxSize(), minLines = 20)
        }
    }
}

/** Generated diagrams, and definitions of new ones. [REQ-9] */
@Composable
private fun DiagramsTab(services: GuiServices, session: Session, project: Project, model: SysmlModel, onProjectChanged: (Project) -> Unit) {
    var selected by remember(project.id) { mutableStateOf(project.diagrams.firstOrNull()) }
    var newName by remember { mutableStateOf("") }
    var newKinds by remember { mutableStateOf("part def") }
    var newElements by remember { mutableStateOf("") }
    var newDepth by remember { mutableStateOf("0") }
    var showFeatures by remember { mutableStateOf(true) }
    var relKinds by remember { mutableStateOf(setOf(RelationshipKind.SPECIALISATION, RelationshipKind.TYPING, RelationshipKind.CONNECTION)) }
    var direction by remember { mutableStateOf(LayoutDirection.TOP_TO_BOTTOM) }
    var error by remember { mutableStateOf<String?>(null) }

    MasterDetail(
        items = project.diagrams,
        selected = selected,
        label = { it.name },
        onSelect = { selected = it }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            val def = selected
            if (null != def) {
                val diagram = remember(def, model) { services.diagrams.generate(def, model) }
                SectionTitle(def.name)
                if (diagram.nodes.isEmpty()) Text("No model elements match this diagram's selection.")
                DiagramView(diagram)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            SectionTitle("New diagram")
            Field("Name", newName, { newName = it })
            Field("Element kinds (comma separated, e.g. part def, port def)", newKinds, { newKinds = it })
            Field("Elements (comma separated qualified names)", newElements, { newElements = it })
            Field("Include owned elements to depth", newDepth, { newDepth = it.filter { c -> c.isDigit() } })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(showFeatures, { showFeatures = it }); Text("Show features")
            }
            Text("Relationships to show")
            Row {
                RelationshipKind.entries.forEach { k ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(k in relKinds, { on -> relKinds = if (on) relKinds + k else relKinds - k })
                        Text(k.name.lowercase())
                    }
                }
            }
            ChoiceList(LayoutDirection.entries, direction, { it.name.lowercase().replace('_', ' ') }) { direction = it }
            Button(enabled = newName.isNotBlank(), onClick = {
                val d = DiagramDefinition(
                    id = "diagram-" + newName.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
                    name = newName,
                    selection = ElementSelection(
                        elementNames = newElements.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        elementKinds = newKinds.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                        nestingDepth = newDepth.toIntOrNull() ?: 0
                    ),
                    configuration = ViewConfiguration(relationshipKinds = relKinds, showFeatures = showFeatures, direction = direction)
                )
                try {
                    val changed = project.copy(diagrams = project.diagrams.filter { it.name != d.name } + d)
                    services.projects.saveProject(session, changed)
                    onProjectChanged(changed)
                    selected = d
                    newName = ""
                    error = null
                } catch (e: Exception) {
                    error = e.message
                }
            }) { Text("Add diagram") }
            ErrorText(error)
        }
    }
}

/** Generate documents from document templates. [REQ-10] */
@Composable
private fun DocumentsTab(services: GuiServices, project: Project, model: SysmlModel, files: Map<String, String>) {
    val templates = services.admin.documentTemplates().filter { project.documentTemplateIds.isEmpty() || it.id in project.documentTemplateIds }
    var selected by remember { mutableStateOf(templates.firstOrNull()) }
    var output by remember { mutableStateOf("") }
    MasterDetail(templates, selected, { it.name }, { selected = it; output = "" }) {
        val t = selected
        if (null == t) {
            Text("No document templates.")
        } else {
            Button(onClick = { output = services.documents.generate(t, project, services.process.process(project), model, files) }) { Text("Generate ${t.name}") }
            if (output.isNotEmpty()) {
                Text("Markdown (diagrams are embedded as SVG)", style = MaterialTheme.typography.bodySmall)
                CodeField(t.name, output, { output = it }, Modifier.fillMaxSize(), minLines = 20)
            }
        }
    }
}

/** Baselines set at points in time. [REQ-11] */
@Composable
private fun BaselinesTab(services: GuiServices, session: Session, project: Project) {
    val scope = rememberCoroutineScope()
    var baselines by remember(project.id) { mutableStateOf(services.baselines.baselines(project)) }
    var selected by remember { mutableStateOf<Baseline?>(null) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val canSet = services.authorisation.isAllowed(session.user, Permission.SET_BASELINE, project)
    MasterDetail(
        items = baselines,
        selected = selected,
        label = { it.name },
        onSelect = { selected = it },
        actions = {
            Spacer(Modifier.height(16.dp))
            Field("Baseline name", name, { name = it })
            Button(enabled = canSet && name.isNotBlank(), onClick = {
                scope.launch {
                    try {
                        selected = services.baselines.setBaseline(session, project, name)
                        baselines = services.baselines.baselines(project)
                        name = ""
                        error = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            }) { Text("Set baseline") }
            ErrorText(error)
        }
    ) {
        val b = selected
        if (null == b) {
            Text("Select a baseline.")
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SectionTitle(b.name)
                Text("Set by ${b.createdBy} at ${b.timestamp}" + (b.storageReference?.let { ", stored as $it" } ?: ""))
                b.files.forEach { (path, text) ->
                    Text(path, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    SelectionContainer { Text(text, style = monospace) }
                }
            }
        }
    }
}
