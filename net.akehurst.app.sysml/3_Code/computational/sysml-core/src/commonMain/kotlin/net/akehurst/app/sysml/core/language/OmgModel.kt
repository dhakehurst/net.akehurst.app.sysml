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

package net.akehurst.app.sysml.core.language

import net.akehurst.omg.sysml.v2_0.api.KerML.Core.Classifiers.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Core.Features.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Core.Types.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Associations.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Behaviors.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Classes.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Connectors.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.DataTypes.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Expressions.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.FeatureValues.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Functions.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Interactions.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Metadata.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Multiplicities.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Packages.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Structures.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Root.Annotations.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Root.Dependencies.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Root.Elements.*
import net.akehurst.omg.sysml.v2_0.api.KerML.Root.Namespaces.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Actions.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Allocations.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.AnalysisCases.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Attributes.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Calculations.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Cases.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Connections.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Constraints.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.DefinitionAndUsage.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Enumerations.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Flows.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Interfaces.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Items.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Metadata.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Occurrences.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Parts.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Ports.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Requirements.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.States.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.UseCases.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.VerificationCases.*
import net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Views.*

/** navigation of the compiled omg model, through the relationships that the [SysmlCompiler] creates */
object OmgModel {

    /** the elements owned through memberships, in order */
    fun ownedMembers(el: Element): List<Element> =
        el.ownedRelationshipOrderedSet.filterIsInstance<OwningMembership>().flatMap { it.ownedRelatedElementOrderedSet }

    /** the (possibly unresolved) names of the types of a feature */
    fun typeNames(f: Element): List<String> =
        f.ownedRelationshipOrderedSet.filterIsInstance<FeatureTyping>().mapNotNull { it.typeReference.reference as? String }

    fun types(f: Element): List<Element> =
        f.ownedRelationshipOrderedSet.filterIsInstance<FeatureTyping>().mapNotNull { it.typeReference.resolved }

    /** the general elements: superclassifiers, subsetted and redefined features */
    fun generals(el: Element): List<Element> = el.ownedRelationshipOrderedSet.mapNotNull {
        when (it) {
            is Subclassification -> it.superclassifierReference.resolved
            is Redefinition -> it.redefinedFeatureReference.resolved
            is ReferenceSubsetting -> null
            is Subsetting -> it.subsettedFeatureReference.resolved
            else -> null
        }
    }

    /** features referenced by reference subsetting, e.g. the ends of a connection */
    fun referencedFeatures(el: Element): List<Element> =
        el.ownedRelationshipOrderedSet.filterIsInstance<ReferenceSubsetting>().mapNotNull { it.referencedFeatureReference.resolved }
}

/**
 * Resolves names in the compiled omg model.
 * A simple name is looked up in the scope element, its owners outwards, and then all files (which share one global namespace);
 * at each level members inherited from general elements, and for features from their types, are included.
 * As imports are not yet compiled, a name not found that way is resolved if it is the name of exactly one element.
 * A qualified name (A::B) resolves its first part as a simple name, and the rest as members.
 * A feature chain (a.b) resolves each part in the type of the previous one.
 */
class NameResolver(private val roots: List<Namespace>) {
    private val ownerOf = mutableMapOf<Element, Element>()
    private val allElements = mutableListOf<Element>()

    init {
        fun walk(el: Element) {
            for (m in OmgModel.ownedMembers(el)) {
                ownerOf[m] = el
                allElements += m
                walk(m)
            }
        }
        roots.forEach { walk(it) }
    }

    fun owner(el: Element): Element? = ownerOf[el]

    /** members visible inside [el]: owned, inherited, and of its types */
    private fun visibleMembers(el: Element, seen: MutableSet<Element> = mutableSetOf()): List<Element> {
        if (!seen.add(el)) return emptyList()
        return OmgModel.ownedMembers(el) +
                OmgModel.generals(el).flatMap { visibleMembers(it, seen) } +
                OmgModel.types(el).flatMap { visibleMembers(it, seen) }
    }

    private fun member(el: Element, name: String): Element? = visibleMembers(el).firstOrNull { it.name == name || it.shortName == name || it.declaredShortName == name }

    private fun lookup(scope: Element, name: String): Element? {
        var s: Element? = scope
        while (null != s) {
            member(s, name)?.let { return it }
            s = ownerOf[s]
        }
        roots.forEach { r -> member(r, name)?.let { return it } }
        return allElements.filter { it.name == name }.singleOrNull()
    }

    fun resolve(scope: Element, text: String): Element? {
        val chain = text.split('.')
        var cur = resolveQualified(scope, chain.first()) ?: return null
        for (part in chain.drop(1)) {
            cur = member(cur, unquote(part)) ?: return null
        }
        return cur
    }

    private fun resolveQualified(scope: Element, text: String): Element? {
        val global = text.startsWith("$::")
        val parts = text.removePrefix("$::").split("::").map { unquote(it) }
        var cur = if (global) roots.firstNotNullOfOrNull { member(it, parts.first()) } else lookup(scope, parts.first())
        for (p in parts.drop(1)) {
            cur = cur?.let { member(it, p) } ?: return null
        }
        return cur
    }

    private fun unquote(s: String) = if (s.length >= 2 && s.startsWith("'") && s.endsWith("'")) s.substring(1, s.length - 1) else s
}
