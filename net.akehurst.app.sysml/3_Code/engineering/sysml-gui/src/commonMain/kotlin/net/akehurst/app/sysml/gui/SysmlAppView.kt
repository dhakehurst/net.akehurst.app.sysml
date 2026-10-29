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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.akehurst.app.sysml.info.Permission
import net.akehurst.app.sysml.info.Project
import net.akehurst.app.sysml.info.Session

private sealed class Page {
    data object Projects : Page()
    data class OpenProject(val project: Project) : Page()
    data object Administration : Page()
}

/** The whole user interface. The same composables are used by the JVM and the web version. [REQ-1.2] */
@Composable
fun SysmlAppView(services: GuiServices, title: String = "SysML Application") {
    var session by remember { mutableStateOf<Session?>(null) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            val s = session
            if (null == s) {
                LoginScreen(services, title) { session = it }
            } else {
                MainScreen(services, s, title) {
                    services.authentication.logout(s)
                    session = null
                }
            }
        }
    }
}

@Composable
private fun MainScreen(services: GuiServices, session: Session, title: String, onLogout: () -> Unit) {
    var page by remember { mutableStateOf<Page>(Page.Projects) }
    val isAdmin = services.authorisation.isAllowed(session.user, Permission.ADMINISTER_APPLICATION)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(24.dp))
            TextButton(onClick = { page = Page.Projects }) { Text("Projects") }
            if (isAdmin) {
                TextButton(onClick = { page = Page.Administration }) { Text("Administration") }
            }
            Spacer(Modifier.weight(1f))
            Text(session.user.name + (session.ssoProviderId?.let { " (SSO)" } ?: ""))
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onLogout) { Text("Log out") }
        }
        HorizontalDivider()
        when (val p = page) {
            is Page.Projects -> ProjectsScreen(services, session) { page = Page.OpenProject(it) }
            is Page.OpenProject -> ProjectScreen(services, session, p.project) { page = Page.OpenProject(it) }
            is Page.Administration -> AdminScreen(services, session)
        }
    }
}
