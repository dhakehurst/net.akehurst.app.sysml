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

import net.akehurst.app.sysml.api.SysmlModelReader
import net.akehurst.app.sysml.info.*
import net.akehurst.omg.sysml.v2_0.ram.SysML_ModelFactoryRam
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

/**
 * Reads SysML v2 text with the agl grammar [SysmlGrammar], [REQ-6.3]
 * compiles it into the omg SysML v2.0 metamodel, [REQ-6.4]
 * and derives the light weight [SysmlModel] view from that, for diagrams, documents and the tree view.
 */
class AglSysmlModelReader : SysmlModelReader {

    companion object {
        /** the SysML keyword(s) of each metaclass the [SysmlCompiler] creates */
        val keywordOf = mapOf(
            "Namespace" to "namespace", "Package" to "package", "LibraryPackage" to "library package",
            "Comment" to "comment", "Documentation" to "doc",
            "Definition" to "def", "Usage" to "usage", "ReferenceUsage" to "ref",
            "AttributeDefinition" to "attribute def", "AttributeUsage" to "attribute",
            "EnumerationDefinition" to "enum def", "EnumerationUsage" to "enum",
            "OccurrenceDefinition" to "occurrence def", "OccurrenceUsage" to "occurrence", "EventOccurrenceUsage" to "event occurrence",
            "ItemDefinition" to "item def", "ItemUsage" to "item",
            "PartDefinition" to "part def", "PartUsage" to "part",
            "PortDefinition" to "port def", "PortUsage" to "port",
            "ConnectionDefinition" to "connection def", "ConnectionUsage" to "connection",
            "BindingConnectorAsUsage" to "bind", "SuccessionAsUsage" to "succession",
            "InterfaceDefinition" to "interface def", "InterfaceUsage" to "interface",
            "AllocationDefinition" to "allocation def", "AllocationUsage" to "allocation",
            "FlowDefinition" to "flow def", "FlowUsage" to "flow", "SuccessionFlowUsage" to "succession flow",
            "ActionDefinition" to "action def", "ActionUsage" to "action", "PerformActionUsage" to "perform",
            "CalculationDefinition" to "calc def", "CalculationUsage" to "calc",
            "StateDefinition" to "state def", "StateUsage" to "state", "ExhibitStateUsage" to "exhibit state",
            "ConstraintDefinition" to "constraint def", "ConstraintUsage" to "constraint", "AssertConstraintUsage" to "assert constraint",
            "RequirementDefinition" to "requirement def", "RequirementUsage" to "requirement",
            "ConcernDefinition" to "concern def", "ConcernUsage" to "concern", "SatisfyRequirementUsage" to "satisfy",
            "CaseDefinition" to "case def", "CaseUsage" to "case",
            "AnalysisCaseDefinition" to "analysis def", "AnalysisCaseUsage" to "analysis",
            "VerificationCaseDefinition" to "verification def", "VerificationCaseUsage" to "verification",
            "UseCaseDefinition" to "use case def", "UseCaseUsage" to "use case", "IncludeUseCaseUsage" to "include use case",
            "ViewDefinition" to "view def", "ViewUsage" to "view", "ViewpointDefinition" to "viewpoint def", "ViewpointUsage" to "viewpoint",
            "RenderingDefinition" to "rendering def", "RenderingUsage" to "rendering",
            "MetadataDefinition" to "metadata def",
        )
    }

    override fun read(files: Map<String, String>): SysmlModel {
        val factory = SysML_ModelFactoryRam("SysMLModel")
        val compiler = SysmlCompiler(factory)
        val issues = mutableListOf<ModelIssue>()
        for ((file, text) in files.entries.sortedBy { it.key }) {
            val outcome = SysmlParser.parse(file, text)
            issues += outcome.issues.map { ModelIssue(it.file, it.line, it.column, it.message) }
            outcome.root?.let { compiler.compile(file, it) }
        }
        // names of the SysML standard library (e.g. ScalarValues::Real) are not resolved, as the library is not loaded, so these are not reported
        compiler.resolveReferences()
        return view(compiler, issues)
    }

    private fun view(compiler: SysmlCompiler, issues: List<ModelIssue>): SysmlModel {
        val elements = mutableListOf<ModelElement>()
        val relationships = mutableListOf<ModelRelationship>()
        val qualifiedNameOf = mutableMapOf<Element, String>()

        fun walk(el: Element, ownerQn: String?) {
            for (m in OmgModel.ownedMembers(el)) {
                val name = m.name
                val metaclass = compiler.metaclassOf[m] ?: "Element"
                val qn = when {
                    null == name -> null
                    null == ownerQn -> name
                    else -> "$ownerQn::$name"
                }
                if (null != qn && m !is Comment) {
                    qualifiedNameOf[m] = qn
                    elements += ModelElement(
                        qualifiedName = qn,
                        name = name!!,
                        kind = keywordOf[metaclass] ?: metaclass,
                        typeName = OmgModel.typeNames(m).firstOrNull(),
                        ownerQualifiedName = ownerQn,
                        file = compiler.fileOf[m] ?: ""
                    )
                }
                // unnamed elements (e.g. 'satisfy R by x;') are not in the view, their members belong to the named owner
                walk(m, qn ?: ownerQn)
            }
        }
        compiler.roots.values.forEach { walk(it, null) }

        fun qn(el: Element?): String? = el?.let { qualifiedNameOf[it] }
        fun target(r: Element): String? = qn(compiler.targetOf[r])
        fun endTargets(connector: Element): List<String> =
            connector.ownedRelationshipOrderedSet.filterIsInstance<EndFeatureMembership>()
                .flatMap { it.ownedRelatedElementOrderedSet }
                .mapNotNull { end -> end.ownedRelationshipOrderedSet.filterIsInstance<ReferenceSubsetting>().firstOrNull()?.let { target(it) } }

        for ((el, metaclass) in compiler.metaclassOf) {
            val source = qn(el)
            if (null != source) {
                elements.firstOrNull { it.qualifiedName == source }?.ownerQualifiedName?.let {
                    relationships += ModelRelationship(it, source, RelationshipKind.OWNERSHIP)
                }
            }
            for (r in el.ownedRelationshipOrderedSet) {
                val t = target(r) ?: continue
                if (null == source) continue
                when (r) {
                    is FeatureTyping -> relationships += ModelRelationship(source, t, RelationshipKind.TYPING)
                    is Subclassification, is Redefinition -> relationships += ModelRelationship(source, t, RelationshipKind.SPECIALISATION)
                    is ReferenceSubsetting -> {}
                    is Subsetting -> relationships += ModelRelationship(source, t, RelationshipKind.SPECIALISATION)
                    else -> {}
                }
            }
            when (metaclass) {
                "ConnectionUsage", "InterfaceUsage", "FlowUsage", "SuccessionFlowUsage", "BindingConnectorAsUsage", "AllocationUsage" -> {
                    val ends = endTargets(el)
                    if (ends.size >= 2) relationships += ModelRelationship(ends[0], ends[1], RelationshipKind.CONNECTION)
                }

                "SatisfyRequirementUsage" -> {
                    val requirement = el.ownedRelationshipOrderedSet.filterIsInstance<ReferenceSubsetting>().firstOrNull()?.let { target(it) }
                    val subject = el.ownedRelationshipOrderedSet.filterIsInstance<OwningMembership>()
                        .flatMap { it.ownedRelatedElementOrderedSet }
                        .firstNotNullOfOrNull { s -> s.ownedRelationshipOrderedSet.filterIsInstance<ReferenceSubsetting>().firstOrNull()?.let { target(it) } }
                    if (null != requirement && null != subject) relationships += ModelRelationship(subject, requirement, RelationshipKind.SATISFY)
                }
            }
        }
        return SysmlModel(elements, relationships.distinct(), issues, compiler.roots)
    }
}
