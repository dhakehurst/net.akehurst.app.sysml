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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.akehurst.app.sysml.api.Credentials
import net.akehurst.app.sysml.info.Session

/** Login with user and password, or with single sign on. [REQ-2] */
@Composable
fun LoginScreen(services: GuiServices, title: String, onLogin: (Session) -> Unit) {
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ssoToken by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun attempt(credentials: Credentials) {
        scope.launch {
            val s = services.authentication.login(credentials)
            if (null == s) error = "Login failed" else onLogin(s)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.width(420.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                SectionTitle("Log in")
                Field("User or e-mail", userId, { userId = it })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { attempt(Credentials.Password(userId, password)) }, modifier = Modifier.fillMaxWidth()) { Text("Log in") }

                val providers = services.authentication.ssoProviders()
                if (providers.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    SectionTitle("Single sign on")
                    // first cut: the token is entered by hand; a real flow redirects to the provider and returns the token
                    Field("Token from the identity provider", ssoToken, { ssoToken = it })
                    providers.forEach { p ->
                        OutlinedButton(onClick = { attempt(Credentials.SsoToken(p.id, ssoToken)) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Sign in with ${p.name}")
                        }
                    }
                }
                ErrorText(error)
            }
        }
    }
}
