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

import kotlinx.coroutines.test.runTest
import net.akehurst.app.sysml.api.Credentials
import net.akehurst.app.sysml.api.NotAuthorisedException
import net.akehurst.app.sysml.core.ai.PromptBuilder
import net.akehurst.app.sysml.core.diagram.DiagramGenerator
import net.akehurst.app.sysml.core.language.AglSysmlModelReader
import net.akehurst.app.sysml.info.*
import kotlin.test.*

class CoreTest {

    private val vehicle = """
        package Vehicle {
            // a comment
            part def Engine {
                attribute power : Real;
                port fuelIn : FuelPort;
            }
            port def FuelPort;
            part def Car :> Vehicle::Base {
                part engine : Engine;
                part tank : Tank;
                connect tank.fuelOut to engine.fuelIn;
            }
            abstract part def Base;
            part def Tank { port fuelOut : FuelPort; }
            requirement def MaxSpeed {
                doc /* The car shall go fast */
            }
            satisfy MaxSpeed by Car;
        }
    """.trimIndent()

    @Test
    fun reader_finds_elements_types_and_relationships() {
        val model = AglSysmlModelReader().read(mapOf("vehicle.sysml" to vehicle))
        val engine = model.element("Vehicle::Engine")
        assertNotNull(engine)
        assertEquals("part def", engine.kind)
        assertEquals("Real", model.element("Vehicle::Engine::power")?.typeName)
        assertEquals("abstract part def".removePrefix("abstract "), model.element("Vehicle::Base")?.kind)
        assertEquals("requirement def", model.element("Vehicle::MaxSpeed")?.kind)
        val rels = model.relationships
        assertTrue(rels.contains(ModelRelationship("Vehicle::Car", "Vehicle::Base", RelationshipKind.SPECIALISATION)))
        assertTrue(rels.contains(ModelRelationship("Vehicle::Car::engine", "Vehicle::Engine", RelationshipKind.TYPING)))
        assertTrue(rels.contains(ModelRelationship("Vehicle::Car", "Vehicle::MaxSpeed", RelationshipKind.SATISFY)))
        assertTrue(rels.any { it.kind == RelationshipKind.CONNECTION })
    }

    @Test
    fun text_is_compiled_into_the_omg_metamodel() {
        val model = AglSysmlModelReader().read(mapOf("vehicle.sysml" to vehicle))
        assertEquals(emptyList(), model.issues)
        val root = model.namespaces["vehicle.sysml"] as net.akehurst.omg.sysml.v2_0.api.KerML.Root.Namespaces.Namespace
        val pkg = net.akehurst.app.sysml.core.language.OmgModel.ownedMembers(root).single()
        assertTrue(pkg is net.akehurst.omg.sysml.v2_0.api.KerML.Kernel.Packages.Package)
        assertEquals("Vehicle", pkg.name)
        val engine = net.akehurst.app.sysml.core.language.OmgModel.ownedMembers(pkg).first { it.name == "Engine" }
        assertTrue(engine is net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Parts.PartDefinition)
        val power = net.akehurst.app.sysml.core.language.OmgModel.ownedMembers(engine).first { it.name == "power" }
        assertTrue(power is net.akehurst.omg.sysml.v2_0.api.SysML.Systems.Attributes.AttributeUsage)
        assertEquals(listOf("Real"), net.akehurst.app.sysml.core.language.OmgModel.typeNames(power))
    }

    @Test
    fun parse_problems_are_reported_with_their_position() {
        val model = AglSysmlModelReader().read(mapOf("bad.sysml" to "package P {\n  part def ;\n  part x : ;\n}"))
        assertTrue(model.issues.isNotEmpty())
        assertEquals("bad.sysml", model.issues.first().file)
        assertEquals(3, model.issues.first().line)
    }

    @Test
    fun diagram_is_generated_and_laid_out_without_overlaps() {
        val model = AglSysmlModelReader().read(mapOf("vehicle.sysml" to vehicle))
        val def = DiagramDefinition("d", "Structure", ElementSelection(elementKinds = setOf("part def", "port def")))
        val diagram = DiagramGenerator().generate(def, model)
        assertEquals(setOf("Engine", "FuelPort", "Car", "Base", "Tank"), diagram.nodes.map { it.title }.toSet())
        // Car is typed by Engine (via its part) and specialises Base
        assertTrue(diagram.edges.any { it.sourceQualifiedName == "Vehicle::Car" && it.targetQualifiedName == "Vehicle::Engine" })
        assertTrue(diagram.edges.any { it.sourceQualifiedName == "Vehicle::Car" && it.targetQualifiedName == "Vehicle::Base" && it.kind == RelationshipKind.SPECIALISATION })
        // the general element is above the specific one
        val base = diagram.nodes.first { it.title == "Base" }
        val car = diagram.nodes.first { it.title == "Car" }
        assertTrue(base.y < car.y)
        for (a in diagram.nodes) for (b in diagram.nodes) if (a !== b) {
            val overlap = a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
            assertFalse(overlap, "${a.title} overlaps ${b.title}")
        }
        val svg = DiagramGenerator().toSvg(diagram)
        assertTrue(svg.startsWith("<svg"))
    }

    @Test
    fun authorisation_and_project_flow() = runTest {
        val services = SysmlServices(repositoryFactory = { InMemoryRepo(it) })
        services.seedDefaults()
        val auth = services.authentication
        assertNull(auth.login(Credentials.Password("admin", "wrong")))
        val admin = auth.login(Credentials.Password("admin", "admin"))!!
        val engineer = auth.login(Credentials.SsoToken("dev-sso", "engineer@example.com"))!!
        assertEquals("engineer", engineer.user.id)

        // only admins administer
        assertFailsWith<NotAuthorisedException> { services.admin.saveDocumentTemplate(engineer, DocumentTemplate("x", "x", "x")) }
        services.admin.saveDocumentTemplate(admin, DocumentTemplate("x", "x", "x"))

        val project = services.projects.createProject(engineer, "Car", "A car", ModelStorage.InMemory("car"), Defaults.projectTemplate.id, null)
        assertEquals(listOf("architecture.sysml", "context.sysml", "requirements.sysml", "verification.sysml"), services.projects.repository(engineer, project).listFiles())
        assertEquals(listOf("Requirements", "Structure"), project.diagrams.map { it.name })
        assertTrue(services.projects.projects(admin).contains(project))

        assertEquals("needs", services.process.currentStep(project)?.id)
        services.process.completeStep(engineer, project, "needs")
        assertEquals("context", services.process.currentStep(project)?.id)

        val baseline = services.baselines.setBaseline(engineer, project, "B1")
        assertEquals(4, baseline.files.size)

        val doc = services.documents.generate(Defaults.documentTemplate, project, services.process.process(project), SysmlModel.EMPTY, baseline.files)
        assertTrue(doc.startsWith("# Car"))
        assertFalse(doc.contains("{{"))
    }

    @Test
    fun prompt_is_built_from_the_strategy_and_sysml_extracted() {
        val step = Defaults.process.steps.first()
        val prompt = PromptBuilder.stepPrompt(Defaults.agentStrategy, step, "package A {}", "Add a speed requirement")
        assertTrue(prompt.contains("Stakeholder needs"))
        assertTrue(prompt.contains("requirements.sysml"))
        assertTrue(prompt.contains("Add a speed requirement"))
        assertEquals("package A {\n}", PromptBuilder.extractSysml("Here:\n```sysml\npackage A {\n}\n```\nDone."))
    }

    private class InMemoryRepo(storage: ModelStorage) : net.akehurst.app.sysml.api.ModelRepository by net.akehurst.app.sysml.core.storage.InMemoryModelRepository(storage)
}
