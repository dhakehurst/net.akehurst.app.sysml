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

/**
 * A light weight view of the elements in a project's SysML v2 text.
 * First cut: produced by a simple text scanner. To be replaced by the omg sysml model.
 */
data class SysmlModel(
    val elements: List<ModelElement>,
    val relationships: List<ModelRelationship>
) {
    private val byQualifiedName = elements.associateBy { it.qualifiedName }

    fun element(qualifiedName: String): ModelElement? = byQualifiedName[qualifiedName]
    fun childrenOf(qualifiedName: String): List<ModelElement> = elements.filter { it.ownerQualifiedName == qualifiedName }

    /** find an element by qualified name, or by simple name if that is unique */
    fun resolve(name: String): ModelElement? =
        byQualifiedName[name] ?: elements.filter { it.name == name }.singleOrNull()

    companion object {
        val EMPTY = SysmlModel(emptyList(), emptyList())
    }
}

data class ModelElement(
    val qualifiedName: String,
    val name: String,
    /** the SysML keyword(s), e.g. "package", "part def", "part", "attribute", "requirement def" */
    val kind: String,
    /** the declared type, e.g. `Engine` in `part engine : Engine;` */
    val typeName: String? = null,
    val ownerQualifiedName: String? = null,
    /** the file the element is declared in */
    val file: String = ""
) {
    val isDefinition: Boolean get() = kind.endsWith(" def")
}

enum class RelationshipKind {
    OWNERSHIP,
    TYPING,
    SPECIALISATION,
    CONNECTION,
    SATISFY
}

data class ModelRelationship(
    val sourceQualifiedName: String,
    val targetQualifiedName: String,
    val kind: RelationshipKind
)
