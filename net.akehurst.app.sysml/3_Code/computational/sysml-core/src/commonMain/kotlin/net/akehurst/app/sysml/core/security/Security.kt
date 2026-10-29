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

package net.akehurst.app.sysml.core.security

import net.akehurst.app.sysml.api.AuthenticationService
import net.akehurst.app.sysml.api.AuthorisationService
import net.akehurst.app.sysml.api.Credentials
import net.akehurst.app.sysml.core.newId
import net.akehurst.app.sysml.info.*

/** Holds the users known to the application. */
class UserDirectory {
    private val users = mutableMapOf<String, User>()
    private val passwords = mutableMapOf<String, String>()

    fun all(): List<User> = users.values.toList()
    fun get(id: String): User? = users[id]
    fun byEmail(email: String): User? = users.values.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun save(user: User, password: String? = null) {
        users[user.id] = user
        if (null != password) passwords[user.id] = password
    }

    fun checkPassword(userId: String, password: String): Boolean = passwords[userId]?.let { it == password } ?: false
}

/**
 * Validates a single sign on token and returns the e-mail of the user it identifies, or null.
 * A real implementation checks an OpenID Connect ID token against the provider (signature, issuer, audience, expiry).
 */
fun interface SsoTokenValidator {
    suspend fun validate(provider: SsoProvider, token: String): String?
}

/**
 * FOR DEVELOPMENT ONLY: accepts any token and treats it as the user's e-mail.
 * To be replaced by an OpenID Connect validator.
 */
object DevelopmentSsoTokenValidator : SsoTokenValidator {
    override suspend fun validate(provider: SsoProvider, token: String): String? = token.takeIf { it.contains('@') }
}

/** Login with password or single sign on. [REQ-2] */
class DefaultAuthenticationService(
    private val users: UserDirectory,
    private val providers: List<SsoProvider>,
    private val ssoValidator: SsoTokenValidator
) : AuthenticationService {

    private val sessions = mutableMapOf<String, Session>()

    override fun ssoProviders(): List<SsoProvider> = providers

    override suspend fun login(credentials: Credentials): Session? {
        val (user, providerId) = when (credentials) {
            is Credentials.Password -> {
                val u = users.get(credentials.userId) ?: users.byEmail(credentials.userId)
                if (null != u && users.checkPassword(u.id, credentials.password)) u to null else null to null
            }

            is Credentials.SsoToken -> {
                val provider = providers.firstOrNull { it.id == credentials.providerId }
                val email = provider?.let { ssoValidator.validate(it, credentials.token) }
                (email?.let { users.byEmail(it) }) to provider?.id
            }
        }
        return user?.let { Session(newId(), it, providerId).also { s -> sessions[s.id] = s } }
    }

    override fun logout(session: Session) {
        sessions.remove(session.id)
    }

    override fun isValid(session: Session): Boolean = sessions.containsKey(session.id)
}

/**
 * Role based authorisation. [REQ-3]
 * - ADMIN may do everything, and only ADMIN may administer the application. [REQ-3.3]
 * - any logged in user may create a project
 * - on a project, the user's project role decides.
 */
class RoleBasedAuthorisationService : AuthorisationService {

    companion object {
        val PROJECT_PERMISSIONS: Map<Role, Set<Permission>> = mapOf(
            Role.PROJECT_OWNER to setOf(
                Permission.VIEW_PROJECT, Permission.EDIT_MODEL, Permission.ADVANCE_PROCESS, Permission.SET_BASELINE,
                Permission.GENERATE_DOCUMENT, Permission.MANAGE_PROJECT_MEMBERS
            ),
            Role.ENGINEER to setOf(Permission.VIEW_PROJECT, Permission.EDIT_MODEL, Permission.ADVANCE_PROCESS, Permission.GENERATE_DOCUMENT),
            Role.VIEWER to setOf(Permission.VIEW_PROJECT, Permission.GENERATE_DOCUMENT),
        )
    }

    override fun isAllowed(user: User, permission: Permission, project: Project?): Boolean = when {
        user.isAdmin -> true
        permission == Permission.ADMINISTER_APPLICATION -> false
        permission == Permission.CREATE_PROJECT -> true
        null == project -> false
        else -> project.roleOf(user.id)?.let { PROJECT_PERMISSIONS[it]?.contains(permission) } ?: false
    }
}
