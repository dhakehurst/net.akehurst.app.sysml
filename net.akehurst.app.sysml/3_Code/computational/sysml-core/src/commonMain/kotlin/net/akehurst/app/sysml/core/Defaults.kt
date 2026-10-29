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

import net.akehurst.app.sysml.info.*

/** Initial content so that a new installation can be used straight away. Admins can change all of it. */
object Defaults {

    val agentStrategy = AgentStrategy(
        systemPrompt = """
            You are an experienced systems engineer who writes models in the SysML v2 textual notation.
            You help the user carry out one step of an engineering process.
            Write only valid SysML v2 textual notation. Keep names short and meaningful.
            Answer with exactly one ```sysml code block containing the complete new content of the file you are asked to write,
            followed by at most three sentences explaining what you changed.
        """.trimIndent(),
        stepPromptTemplate = """
            Process step: {{step.name}}
            Instructions for the step: {{step.instructions}}
            {{step.agentPrompt}}
            The step is expected to produce elements of these kinds: {{step.expectedElementKinds}}
            Write the complete new content of the file '{{step.targetFile}}'.

            The current model of the project is:
            {{model}}

            The user's request:
            {{request}}
        """.trimIndent(),
        maxIterations = 10
    )

    val requirementsDiagram = DiagramDefinition(
        id = "diagram-requirements",
        name = "Requirements",
        selection = ElementSelection(elementKinds = setOf("requirement def", "requirement")),
        configuration = ViewConfiguration(
            relationshipKinds = setOf(RelationshipKind.SPECIALISATION, RelationshipKind.SATISFY),
            featureKinds = setOf("attribute", "subject")
        )
    )

    val structureDiagram = DiagramDefinition(
        id = "diagram-structure",
        name = "Structure",
        selection = ElementSelection(elementKinds = setOf("part def", "port def")),
        configuration = ViewConfiguration(
            relationshipKinds = setOf(RelationshipKind.SPECIALISATION, RelationshipKind.TYPING),
            featureKinds = setOf("attribute", "port", "part")
        )
    )

    val process = ProcessDefinition(
        id = "process-basic-se",
        name = "Basic systems engineering",
        description = "From stakeholder needs, via the system context and the logical architecture, to verification.",
        steps = listOf(
            ProcessStep(
                id = "needs",
                name = "Stakeholder needs",
                instructions = "Capture what the stakeholders need from the system as requirements.",
                expectedElementKinds = listOf("requirement def", "requirement"),
                targetFile = "requirements.sysml",
                agentPrompt = "Each requirement should have a doc comment with a single 'shall' statement."
            ),
            ProcessStep(
                id = "context",
                name = "System context",
                instructions = "Define the system, the actors and external systems it interacts with, and the interfaces between them.",
                expectedElementKinds = listOf("part def", "part", "port def"),
                targetFile = "context.sysml"
            ),
            ProcessStep(
                id = "logical",
                name = "Logical architecture",
                instructions = "Break the system down into logical parts, connect them, and say which parts satisfy which requirements.",
                expectedElementKinds = listOf("part def", "part", "port def", "connection"),
                targetFile = "architecture.sysml"
            ),
            ProcessStep(
                id = "verification",
                name = "Verification",
                instructions = "Define how each requirement will be verified.",
                expectedElementKinds = listOf("verification def"),
                targetFile = "verification.sysml"
            )
        ),
        agentStrategy = agentStrategy,
        diagrams = listOf(requirementsDiagram)
    )

    val documentTemplate = DocumentTemplate(
        id = "doc-system-specification",
        name = "System specification",
        body = """
            # {{project.name}} – System specification

            Generated on {{date}}. {{project.description}}

            The project follows the process '{{process.name}}':

            {{process.steps}}

            ## Requirements

            {{elements:requirement def}}

            {{diagram:Requirements}}

            ## Structure

            {{elements:part def}}

            {{diagram:Structure}}

            ## Appendix: model

            {{model}}
        """.trimIndent()
    )

    val projectTemplate = ProjectTemplate(
        id = "template-basic-system",
        name = "Basic system",
        description = "One package per process step.",
        processId = process.id,
        initialFiles = mapOf(
            "requirements.sysml" to "package Requirements {\n}\n",
            "context.sysml" to "package Context {\n}\n",
            "architecture.sysml" to "package Architecture {\n}\n",
            "verification.sysml" to "package Verification {\n}\n"
        ),
        diagrams = listOf(structureDiagram),
        documentTemplateIds = listOf(documentTemplate.id)
    )

    /** FOR DEVELOPMENT ONLY: users with well known passwords */
    val developmentUsers: List<Pair<User, String>> = listOf(
        User("admin", "Administrator", "admin@example.com", setOf(Role.ADMIN)) to "admin",
        User("engineer", "Engineer", "engineer@example.com") to "engineer"
    )

    /** FOR DEVELOPMENT ONLY: used with the development token validator */
    val developmentSsoProvider = SsoProvider("dev-sso", "Development SSO", "https://sso.example.com", "sysml-app")
}
