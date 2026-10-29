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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.akehurst.app.sysml.info.*

private enum class StorageKind(val label: String) {
    LOCAL("Local file system"),
    GIT("Git repository"),
    MEMORY("In memory (not saved)")
}

/** List and create projects. [REQ-5] */
@Composable
fun ProjectsScreen(services: GuiServices, session: Session, onOpen: (Project) -> Unit) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf(services.projects.projects(session)) }
    var error by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var storageKind by remember { mutableStateOf(if (services.supportsFileSystemStorage) StorageKind.LOCAL else StorageKind.MEMORY) }
    var path by remember { mutableStateOf("") }
    var gitUrl by remember { mutableStateOf("") }
    var gitBranch by remember { mutableStateOf("main") }
    val templates = services.admin.projectTemplates()
    var template by remember { mutableStateOf(templates.firstOrNull()) }
    val processes = services.admin.processes()
    var process by remember { mutableStateOf(template?.let { t -> processes.firstOrNull { it.id == t.processId } } ?: processes.firstOrNull()) }

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            SectionTitle("Projects")
            if (projects.isEmpty()) Text("No projects yet.")
            projects.forEach { p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.padding(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text(p.description)
                            Text(storageLabel(p.storage), style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { onOpen(p) }) { Text("Open") }
                    }
                }
            }
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(start = 16.dp).verticalScroll(rememberScrollState())) {
            SectionTitle("New project")
            Field("Name", name, { name = it })
            Field("Description", description, { description = it })
            Text("Project template")
            ChoiceList(listOf<ProjectTemplate?>(null) + templates, template, { it?.name ?: "None" }) { t ->
                template = t
                t?.let { tt -> processes.firstOrNull { it.id == tt.processId }?.let { process = it } }
            }
            Text("Engineering process")
            ChoiceList(processes, process, { it.name }) { process = it }
            Text("Where the SysML models are stored")
            val kinds = if (services.supportsFileSystemStorage) StorageKind.entries else listOf(StorageKind.MEMORY)
            ChoiceList(kinds, storageKind, { it.label }) { storageKind = it }
            when (storageKind) {
                StorageKind.LOCAL -> Field("Folder", path, { path = it })
                StorageKind.GIT -> {
                    Field("Repository URL", gitUrl, { gitUrl = it })
                    Field("Branch", gitBranch, { gitBranch = it })
                    Field("Local folder for the clone", path, { path = it })
                }

                StorageKind.MEMORY -> {}
            }
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val storage = when (storageKind) {
                        StorageKind.LOCAL -> ModelStorage.LocalFileSystem(path)
                        StorageKind.GIT -> ModelStorage.GitRepository(gitUrl, gitBranch, path)
                        StorageKind.MEMORY -> ModelStorage.InMemory(name)
                    }
                    scope.launch {
                        error = null
                        try {
                            val p = services.projects.createProject(session, name, description, storage, template?.id, process?.id)
                            projects = services.projects.projects(session)
                            onOpen(p)
                        } catch (e: Exception) {
                            error = e.message ?: e.toString()
                        }
                    }
                }
            ) { Text("Create project") }
            ErrorText(error)
        }
    }
}

fun storageLabel(storage: ModelStorage): String = when (storage) {
    is ModelStorage.LocalFileSystem -> "Folder ${storage.rootPath}"
    is ModelStorage.GitRepository -> "Git ${storage.url} (${storage.branch})"
    is ModelStorage.InMemory -> "In memory"
}
