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

package net.akehurst.app.sysml.core.project

import net.akehurst.app.sysml.api.*
import net.akehurst.app.sysml.core.newId
import net.akehurst.app.sysml.core.nowMillis
import net.akehurst.app.sysml.info.*

/** [REQ-5] */
class DefaultProjectService(
    private val authorisation: AuthorisationService,
    private val admin: AdminService,
    private val repositoryFactory: ModelRepositoryFactory
) : ProjectService {

    private val projects = linkedMapOf<String, Project>()
    private val repositories = mutableMapOf<String, ModelRepository>()

    override fun projects(session: Session): List<Project> =
        projects.values.filter { authorisation.isAllowed(session.user, Permission.VIEW_PROJECT, it) }

    override fun project(session: Session, id: String): Project? =
        projects[id]?.takeIf { authorisation.isAllowed(session.user, Permission.VIEW_PROJECT, it) }

    override suspend fun createProject(
        session: Session,
        name: String,
        description: String,
        storage: ModelStorage,
        projectTemplateId: String?,
        processId: String?
    ): Project {
        authorisation.require(session.user, Permission.CREATE_PROJECT)
        val template = projectTemplateId?.let { admin.projectTemplate(it) }
        val pid = processId ?: template?.processId ?: admin.processes().firstOrNull()?.id
        ?: error("No engineering process is defined, an admin must define one first")
        val process = admin.process(pid) ?: error("Unknown process '$pid'")
        val project = Project(
            id = newId(),
            name = name,
            description = description,
            storage = storage,
            processId = pid,
            members = listOf(ProjectMember(session.user.id, Role.PROJECT_OWNER)),
            projectTemplateId = template?.id,
            // pre-selected diagrams from the process and project templates [REQ-9.4], [REQ-9.5]
            diagrams = process.diagrams + (template?.diagrams ?: emptyList()),
            documentTemplateIds = template?.documentTemplateIds ?: admin.documentTemplates().map { it.id }
        )
        val repo = repositoryFactory.create(storage)
        repo.open()
        template?.initialFiles?.forEach { (path, text) ->
            if (null == repo.read(path)) repo.write(path, text, "Initial content from template '${template.name}'")
        }
        projects[project.id] = project
        repositories[project.id] = repo
        return project
    }

    override fun saveProject(session: Session, project: Project) {
        val existing = projects[project.id]
        if (null != existing) authorisation.require(session.user, Permission.MANAGE_PROJECT_MEMBERS, existing)
        projects[project.id] = project
    }

    override fun repository(session: Session, project: Project): ModelRepository {
        authorisation.require(session.user, Permission.VIEW_PROJECT, project)
        return repositories.getOrPut(project.id) { repositoryFactory.create(project.storage) }
    }
}

/** [REQ-7.2] */
class DefaultProcessService(
    private val authorisation: AuthorisationService,
    private val admin: AdminService
) : ProcessService {

    private val states = mutableMapOf<String, ProcessState>()

    override fun state(project: Project): ProcessState = states.getOrPut(project.id) { ProcessState(project.id, project.processId) }

    override fun process(project: Project): ProcessDefinition =
        admin.process(project.processId) ?: error("Unknown process '${project.processId}' for project '${project.name}'")

    override fun currentStep(project: Project): ProcessStep? {
        val done = state(project).completedStepIds
        return process(project).steps.firstOrNull { it.id !in done }
    }

    override fun completeStep(session: Session, project: Project, stepId: String): ProcessState {
        authorisation.require(session.user, Permission.ADVANCE_PROCESS, project)
        val s = state(project)
        return s.copy(completedStepIds = s.completedStepIds + stepId).also { states[project.id] = it }
    }

    override fun reopenStep(session: Session, project: Project, stepId: String): ProcessState {
        authorisation.require(session.user, Permission.ADVANCE_PROCESS, project)
        val s = state(project)
        return s.copy(completedStepIds = s.completedStepIds - stepId).also { states[project.id] = it }
    }
}

/** [REQ-11] */
class DefaultBaselineService(
    private val authorisation: AuthorisationService,
    private val projects: ProjectService
) : BaselineService {

    private val baselines = linkedMapOf<String, Baseline>()

    override suspend fun setBaseline(session: Session, project: Project, name: String): Baseline {
        authorisation.require(session.user, Permission.SET_BASELINE, project)
        val repo = projects.repository(session, project)
        val files = repo.readAll()
        val ref = repo.markBaseline(name)
        val baseline = Baseline(newId(), project.id, name, nowMillis(), session.user.id, files, ref)
        baselines[baseline.id] = baseline
        return baseline
    }

    override fun baselines(project: Project): List<Baseline> = baselines.values.filter { it.projectId == project.id }

    override fun baseline(id: String): Baseline? = baselines[id]
}
