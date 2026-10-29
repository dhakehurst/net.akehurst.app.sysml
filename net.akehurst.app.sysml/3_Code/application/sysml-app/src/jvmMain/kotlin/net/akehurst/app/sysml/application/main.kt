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

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val app = SysmlApplication(
        aiApiKey = System.getenv("ANTHROPIC_API_KEY"),
        supportsFileSystemStorage = true
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = SysmlApplication.TITLE,
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            app.content()
        }
    }
}
