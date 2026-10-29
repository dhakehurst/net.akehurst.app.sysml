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

/** Where a project's SysML v2 text files are stored. [REQ-6.2] */
sealed class ModelStorage {
    /** [REQ-6.2.1] */
    data class LocalFileSystem(val rootPath: String) : ModelStorage()

    /** [REQ-6.2.2] the repository is cloned to [localPath] */
    data class GitRepository(
        val url: String,
        val branch: String = "main",
        val localPath: String
    ) : ModelStorage()

    /** kept in memory only, used where no file system is available (e.g. the browser) and for tests */
    data class InMemory(val name: String) : ModelStorage()
}

data class ProjectMember(
    val userId: String,
    val role: Role
)

/** A body of engineering work, engineered according to a process. [REQ-5] */
data class Project(
    val id: String,
    val name: String,
    val description: String,
    val storage: ModelStorage,
    /** the engineering process the project follows [REQ-5.3] */
    val processId: String,
    val members: List<ProjectMember>,
    val projectTemplateId: String? = null,
    /** diagrams of this project, initially copied from the templates [REQ-9.4], [REQ-9.5] */
    val diagrams: List<DiagramDefinition> = emptyList(),
    val documentTemplateIds: List<String> = emptyList()
) {
    fun roleOf(userId: String): Role? = members.firstOrNull { it.userId == userId }?.role
}

/** Reusable definition from which projects are created. [REQ-4.1], [REQ-5.1.1] */
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val processId: String,
    /** SysML v2 files (path -> text) put into a new project */
    val initialFiles: Map<String, String> = emptyMap(),
    /** pre-selected diagrams [REQ-9.5] */
    val diagrams: List<DiagramDefinition> = emptyList(),
    val documentTemplateIds: List<String> = emptyList()
)

/** A template for generating documents from the model. [REQ-4.3], [REQ-10.1] */
data class DocumentTemplate(
    val id: String,
    val name: String,
    /**
     * Markdown text containing placeholders, see the document generator in sysml-core:
     * `{{project.name}}`, `{{project.description}}`, `{{process.name}}`, `{{date}}`,
     * `{{elements:<kind>}}`, `{{diagram:<name>}}`, `{{model}}`
     */
    val body: String
)

/** A recorded state of a project's model at a point in time. [REQ-11] */
data class Baseline(
    val id: String,
    val projectId: String,
    val name: String,
    /** milliseconds since the epoch */
    val timestamp: Long,
    val createdBy: String,
    /** the SysML v2 files at the time the baseline was set (path -> text) */
    val files: Map<String, String>,
    /** reference in the storage, e.g. a git tag, if the storage supports one */
    val storageReference: String? = null
)
