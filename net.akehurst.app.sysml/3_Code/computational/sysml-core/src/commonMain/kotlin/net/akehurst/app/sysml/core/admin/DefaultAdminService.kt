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

package net.akehurst.app.sysml.core.admin

import net.akehurst.app.sysml.api.AdminService
import net.akehurst.app.sysml.api.AuthorisationService
import net.akehurst.app.sysml.core.security.UserDirectory
import net.akehurst.app.sysml.info.*

/**
 * Admin defined templates and processes, held in memory. [REQ-4]
 * Reading is open to all users (they are needed to create and run projects), changing needs [Permission.ADMINISTER_APPLICATION].
 */
class DefaultAdminService(
    private val authorisation: AuthorisationService,
    private val users: UserDirectory
) : AdminService {

    private val processes = linkedMapOf<String, ProcessDefinition>()
    private val projectTemplates = linkedMapOf<String, ProjectTemplate>()
    private val documentTemplates = linkedMapOf<String, DocumentTemplate>()

    private fun requireAdmin(session: Session) = authorisation.require(session.user, Permission.ADMINISTER_APPLICATION)

    override fun processes(): List<ProcessDefinition> = processes.values.toList()
    override fun process(id: String): ProcessDefinition? = processes[id]
    override fun saveProcess(session: Session, process: ProcessDefinition) {
        requireAdmin(session); processes[process.id] = process
    }

    override fun deleteProcess(session: Session, id: String) {
        requireAdmin(session); processes.remove(id)
    }

    override fun projectTemplates(): List<ProjectTemplate> = projectTemplates.values.toList()
    override fun projectTemplate(id: String): ProjectTemplate? = projectTemplates[id]
    override fun saveProjectTemplate(session: Session, template: ProjectTemplate) {
        requireAdmin(session); projectTemplates[template.id] = template
    }

    override fun deleteProjectTemplate(session: Session, id: String) {
        requireAdmin(session); projectTemplates.remove(id)
    }

    override fun documentTemplates(): List<DocumentTemplate> = documentTemplates.values.toList()
    override fun documentTemplate(id: String): DocumentTemplate? = documentTemplates[id]
    override fun saveDocumentTemplate(session: Session, template: DocumentTemplate) {
        requireAdmin(session); documentTemplates[template.id] = template
    }

    override fun deleteDocumentTemplate(session: Session, id: String) {
        requireAdmin(session); documentTemplates.remove(id)
    }

    override fun users(session: Session): List<User> {
        requireAdmin(session); return users.all()
    }

    override fun saveUser(session: Session, user: User) {
        requireAdmin(session); users.save(user)
    }

    /** used to load the initial definitions, without a session */
    fun seed(processes: List<ProcessDefinition>, projectTemplates: List<ProjectTemplate>, documentTemplates: List<DocumentTemplate>) {
        processes.forEach { this.processes[it.id] = it }
        projectTemplates.forEach { this.projectTemplates[it.id] = it }
        documentTemplates.forEach { this.documentTemplates[it.id] = it }
    }
}
