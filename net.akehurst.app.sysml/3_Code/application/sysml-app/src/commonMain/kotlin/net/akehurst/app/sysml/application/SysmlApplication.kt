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

package net.akehurst.app.sysml.application

import androidx.compose.runtime.Composable
import net.akehurst.app.sysml.core.SysmlServices
import net.akehurst.app.sysml.gui.GuiServices
import net.akehurst.app.sysml.gui.SysmlAppView

/**
 * The SysML application: wires the core services to the user interface.
 * The same class is used by the JVM and the web version. [REQ-1.2]
 */
class SysmlApplication(
    aiApiKey: String? = null,
    supportsFileSystemStorage: Boolean
) {
    val services = SysmlServices(aiApiKey = aiApiKey).also { it.seedDefaults() }

    private val gui = GuiServices(
        authentication = services.authentication,
        authorisation = services.authorisation,
        admin = services.admin,
        projects = services.projects,
        process = services.process,
        baselines = services.baselines,
        modelReader = services.modelReader,
        diagrams = services.diagrams,
        documents = services.documents,
        ai = services.ai,
        supportsFileSystemStorage = supportsFileSystemStorage
    )

    @Composable
    fun content() {
        SysmlAppView(gui, TITLE)
    }

    companion object {
        const val TITLE = "SysML Application"
    }
}
