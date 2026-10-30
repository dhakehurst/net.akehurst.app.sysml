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

package net.akehurst.app.sysml.core

import net.akehurst.app.sysml.api.*
import net.akehurst.app.sysml.core.admin.DefaultAdminService
import net.akehurst.app.sysml.core.ai.KoogAiAssistant
import net.akehurst.app.sysml.core.diagram.DiagramGenerator
import net.akehurst.app.sysml.core.document.DocumentGenerator
import net.akehurst.app.sysml.core.language.AglSysmlModelReader
import net.akehurst.app.sysml.core.project.DefaultBaselineService
import net.akehurst.app.sysml.core.project.DefaultProcessService
import net.akehurst.app.sysml.core.project.DefaultProjectService
import net.akehurst.app.sysml.core.security.*
import net.akehurst.app.sysml.core.storage.platformModelRepositoryFactory
import net.akehurst.app.sysml.info.SsoProvider
import kotlin.time.Clock

/**
 * All services of the application, wired together.
 * The same code is used by the JVM and the web version. [REQ-1.2]
 */
class SysmlServices(
    repositoryFactory: ModelRepositoryFactory = platformModelRepositoryFactory(),
    ssoProviders: List<SsoProvider> = listOf(Defaults.developmentSsoProvider),
    ssoValidator: SsoTokenValidator = DevelopmentSsoTokenValidator,
    aiApiKey: String? = null
) {
    val users = UserDirectory()
    val authorisation: AuthorisationService = RoleBasedAuthorisationService()
    val authentication: AuthenticationService = DefaultAuthenticationService(users, ssoProviders, ssoValidator)
    val admin = DefaultAdminService(authorisation, users)
    val projects: ProjectService = DefaultProjectService(authorisation, admin, repositoryFactory)
    val process: ProcessService = DefaultProcessService(authorisation, admin)
    val baselines: BaselineService = DefaultBaselineService(authorisation, projects)
    val modelReader: SysmlModelReader = AglSysmlModelReader()
    val diagrams: DiagramService = DiagramGenerator()
    val documents: DocumentService = DocumentGenerator(diagrams) { Clock.System.now().toString().substringBefore('T') }
    val ai: AiAssistant = KoogAiAssistant(aiApiKey)

    /** load the default process, templates and development users */
    fun seedDefaults() {
        admin.seed(listOf(Defaults.process), listOf(Defaults.projectTemplate), listOf(Defaults.documentTemplate))
        Defaults.developmentUsers.forEach { (u, pw) -> users.save(u, pw) }
    }
}
