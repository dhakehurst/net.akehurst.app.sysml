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

/** An engineering process (process template) defined by an admin. [REQ-4.2], [REQ-7] */
data class ProcessDefinition(
    val id: String,
    val name: String,
    val description: String,
    /** the steps a project follows, in order [REQ-7.1] */
    val steps: List<ProcessStep>,
    /** how the AI agent is to help with this process [REQ-7.4] */
    val agentStrategy: AgentStrategy,
    /** pre-selected diagrams [REQ-9.4] */
    val diagrams: List<DiagramDefinition> = emptyList()
)

data class ProcessStep(
    val id: String,
    val name: String,
    /** guidance for the user */
    val instructions: String,
    /** the kinds of SysML element this step is expected to produce, e.g. "requirement def", "part def" [REQ-7.3] */
    val expectedElementKinds: List<String> = emptyList(),
    /** file the SysML text written in this step goes to */
    val targetFile: String = "model.sysml",
    /** extra prompt text for the AI agent in this step */
    val agentPrompt: String = ""
)

/** The strategy for the AI agent, defined by the process. [REQ-7.4], [REQ-8.2] */
data class AgentStrategy(
    val systemPrompt: String,
    /**
     * Prompt sent for each request. Placeholders: `{{step.name}}`, `{{step.instructions}}`,
     * `{{step.agentPrompt}}`, `{{step.targetFile}}`, `{{step.expectedElementKinds}}`, `{{model}}`, `{{request}}`
     */
    val stepPromptTemplate: String,
    val maxIterations: Int = 10
)

/** Where a project is in its process. [REQ-7.2] */
data class ProcessState(
    val projectId: String,
    val processId: String,
    val completedStepIds: Set<String> = emptySet()
)
