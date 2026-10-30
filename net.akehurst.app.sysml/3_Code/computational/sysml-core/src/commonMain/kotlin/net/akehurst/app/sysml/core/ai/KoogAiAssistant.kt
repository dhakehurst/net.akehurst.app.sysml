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

package net.akehurst.app.sysml.core.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.AnthropicLLMProvider
import ai.koog.prompt.llm.LLModel
import net.akehurst.app.sysml.api.AiAssistant
import net.akehurst.app.sysml.info.AgentStrategy
import net.akehurst.app.sysml.info.ProcessDefinition
import net.akehurst.app.sysml.info.ProcessStep

/**
 * The AI agent, implemented with Koog. [REQ-8.3]
 * The system prompt and the prompt for each request come from the process' [AgentStrategy]. [REQ-8.2]
 */
class KoogAiAssistant(
    private var apiKey: String? = null,
    private val model: LLModel = AnthropicModels.Sonnet_4_5
) : AiAssistant {

    override val isAvailable: Boolean get() = !apiKey.isNullOrBlank()

    override fun configure(apiKey: String?) {
        this.apiKey = apiKey
    }

    override suspend fun proposeSysml(process: ProcessDefinition, step: ProcessStep, currentModelText: String, request: String): String {
        val key = apiKey?.takeIf { it.isNotBlank() } ?: error("The AI agent is not configured: no API key")
        val strategy = process.agentStrategy
        val client = AnthropicLLMClient(apiKey = key, httpClientFactory = KtorKoogHttpClient.Factory())
        val executor = MultiLLMPromptExecutor(mapOf(AnthropicLLMProvider to client))
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            systemPrompt = strategy.systemPrompt,
            maxIterations = strategy.maxIterations
        )
        val answer = agent.run(PromptBuilder.stepPrompt(strategy, step, currentModelText, request))
        return PromptBuilder.extractSysml(answer)
    }
}

object PromptBuilder {

    fun stepPrompt(strategy: AgentStrategy, step: ProcessStep, currentModelText: String, request: String): String =
        strategy.stepPromptTemplate
            .replace("{{step.name}}", step.name)
            .replace("{{step.instructions}}", step.instructions)
            .replace("{{step.agentPrompt}}", step.agentPrompt)
            .replace("{{step.targetFile}}", step.targetFile)
            .replace("{{step.expectedElementKinds}}", step.expectedElementKinds.joinToString(", "))
            .replace("{{model}}", currentModelText)
            .replace("{{request}}", request)

    /** take the text of the first ```sysml (or plain ```) block, or the whole answer if there is none */
    fun extractSysml(answer: String): String {
        val fence = Regex("```(?:sysml|SysML)?\\s*\\n([\\s\\S]*?)```")
        return fence.find(answer)?.groupValues?.get(1)?.trimEnd() ?: answer.trim()
    }
}
