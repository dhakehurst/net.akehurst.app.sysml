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

package net.akehurst.app.sysml.api

import net.akehurst.app.sysml.info.*

/** Thrown when a user is not allowed to do something. [REQ-3] */
class NotAuthorisedException(message: String) : RuntimeException(message)

sealed class Credentials {
    /** local login [REQ-2.2] */
    data class Password(val userId: String, val password: String) : Credentials()

    /** single sign on: a token issued by an [SsoProvider] [REQ-2.1] */
    data class SsoToken(val providerId: String, val token: String) : Credentials()
}

/** [REQ-2] */
interface AuthenticationService {
    fun ssoProviders(): List<SsoProvider>
    suspend fun login(credentials: Credentials): Session?
    fun logout(session: Session)
    fun isValid(session: Session): Boolean
}

/** [REQ-3] */
interface AuthorisationService {
    fun isAllowed(user: User, permission: Permission, project: Project? = null): Boolean

    fun require(user: User, permission: Permission, project: Project? = null) {
        if (!isAllowed(user, permission, project)) {
            val on = project?.let { " on project '${it.name}'" } ?: ""
            throw NotAuthorisedException("${user.name} is not allowed to $permission$on")
        }
    }
}

/** Administration of templates and processes. [REQ-4] */
interface AdminService {
    fun processes(): List<ProcessDefinition>
    fun process(id: String): ProcessDefinition?
    fun saveProcess(session: Session, process: ProcessDefinition)
    fun deleteProcess(session: Session, id: String)

    fun projectTemplates(): List<ProjectTemplate>
    fun projectTemplate(id: String): ProjectTemplate?
    fun saveProjectTemplate(session: Session, template: ProjectTemplate)
    fun deleteProjectTemplate(session: Session, id: String)

    fun documentTemplates(): List<DocumentTemplate>
    fun documentTemplate(id: String): DocumentTemplate?
    fun saveDocumentTemplate(session: Session, template: DocumentTemplate)
    fun deleteDocumentTemplate(session: Session, id: String)

    fun users(session: Session): List<User>
    fun saveUser(session: Session, user: User)
}

/** Read and write the SysML v2 text files of a project. [REQ-6] */
interface ModelRepository {
    val storage: ModelStorage
    suspend fun open()
    suspend fun listFiles(): List<String>
    suspend fun read(path: String): String?
    suspend fun write(path: String, text: String, message: String)
    suspend fun delete(path: String, message: String)

    /** record a baseline in the storage if it supports it (e.g. a git tag); returns the reference or null */
    suspend fun markBaseline(name: String): String?

    suspend fun readAll(): Map<String, String> = listFiles().associateWith { read(it) ?: "" }
}

fun interface ModelRepositoryFactory {
    fun create(storage: ModelStorage): ModelRepository
}

/** [REQ-5] */
interface ProjectService {
    fun projects(session: Session): List<Project>
    fun project(session: Session, id: String): Project?
    suspend fun createProject(
        session: Session,
        name: String,
        description: String,
        storage: ModelStorage,
        projectTemplateId: String?,
        processId: String?
    ): Project
    fun saveProject(session: Session, project: Project)
    fun repository(session: Session, project: Project): ModelRepository
}

/** Helps users follow a project's process. [REQ-7] */
interface ProcessService {
    fun state(project: Project): ProcessState
    fun process(project: Project): ProcessDefinition
    fun currentStep(project: Project): ProcessStep?
    fun completeStep(session: Session, project: Project, stepId: String): ProcessState
    fun reopenStep(session: Session, project: Project, stepId: String): ProcessState
}

/** The AI agent that helps write SysML v2 text. [REQ-8] */
interface AiAssistant {
    val isAvailable: Boolean

    /** set the key used to access the AI model provider */
    fun configure(apiKey: String?)

    /**
     * Ask the agent for SysML v2 text for the given step, following the process' [AgentStrategy].
     * @return the proposed SysML v2 text
     */
    suspend fun proposeSysml(process: ProcessDefinition, step: ProcessStep, currentModelText: String, request: String): String
}

/** Parses SysML v2 text into the light weight [SysmlModel]. */
interface SysmlModelReader {
    fun read(files: Map<String, String>): SysmlModel
}

/** [REQ-9] */
interface DiagramService {
    fun generate(definition: DiagramDefinition, model: SysmlModel): Diagram
    fun toSvg(diagram: Diagram): String
}

/** [REQ-10] */
interface DocumentService {
    fun generate(template: DocumentTemplate, project: Project, process: ProcessDefinition, model: SysmlModel, modelText: Map<String, String>): String
}

/** [REQ-11] */
interface BaselineService {
    suspend fun setBaseline(session: Session, project: Project, name: String): Baseline
    fun baselines(project: Project): List<Baseline>
    fun baseline(id: String): Baseline?
}
