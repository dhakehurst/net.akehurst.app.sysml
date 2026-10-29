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

/** A person who uses the application. [REQ-2], [REQ-3] */
data class User(
    val id: String,
    val name: String,
    val email: String,
    /** application wide roles, e.g. [Role.ADMIN] */
    val roles: Set<Role> = emptySet()
) {
    val isAdmin: Boolean get() = Role.ADMIN in roles
}

/**
 * Roles are the basis of the authorisation concept. [REQ-3.1]
 * ADMIN is an application wide role, the others are given per project via [ProjectMember].
 */
enum class Role {
    ADMIN,
    PROJECT_OWNER,
    ENGINEER,
    VIEWER
}

/** Things a user may be allowed to do. */
enum class Permission {
    ADMINISTER_APPLICATION,
    CREATE_PROJECT,
    VIEW_PROJECT,
    EDIT_MODEL,
    ADVANCE_PROCESS,
    SET_BASELINE,
    GENERATE_DOCUMENT,
    MANAGE_PROJECT_MEMBERS
}

/** An identity provider that can be used for single sign on. [REQ-2.1] */
data class SsoProvider(
    val id: String,
    val name: String,
    /** e.g. the OpenID Connect issuer URL */
    val issuerUrl: String,
    val clientId: String
)

/** A logged in user. [REQ-2.2] */
data class Session(
    val id: String,
    val user: User,
    /** id of the [SsoProvider] used to log in, null for a local login */
    val ssoProviderId: String?
)
