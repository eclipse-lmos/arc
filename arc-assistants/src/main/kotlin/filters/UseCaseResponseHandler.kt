// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.AGENT_TAGS_LOCAL_CONTEXT_KEY
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.Data
import org.eclipse.lmos.arc.agents.dsl.addData
import org.eclipse.lmos.arc.agents.dsl.extensions.emit
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.memory
import org.eclipse.lmos.arc.agents.dsl.extensions.outputContext
import org.eclipse.lmos.arc.agents.dsl.extensions.setCurrentUseCases
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.assistants.support.events.UseCaseEvent
import org.eclipse.lmos.arc.assistants.support.usecases.extractUseCaseId
import org.eclipse.lmos.arc.assistants.support.usecases.extractUseCaseStepId
import org.slf4j.LoggerFactory

/**
 * An output filter that handles responses from the LLM that contain a use case id.
 * For example, "<ID:useCaseId>"
 */
context(DSLContext)
class UseCaseResponseHandler : AgentFilter {

    private val log = LoggerFactory.getLogger(this.javaClass)

    override suspend fun filter(message: ConversationMessage): ConversationMessage {
        val (messageWithoutStep, stepId) = extractUseCaseStepId(message.content)
        val (cleanMessage, useCaseId) = extractUseCaseId(messageWithoutStep)

        // Store the current step in memory
        setUseCaseStep(stepId)

        // Store the identified use case.
        if (useCaseId != null) {
            log.info("Use case: $useCaseId used. Step: $stepId")
            var updatedUseCases: List<String>? = null

            if (stepId == null) {
                val usedUseCases = memory<List<String>>("usedUseCases") ?: emptyList()
                log.info("All Use cases used: $usedUseCases")
                updatedUseCases = usedUseCases + useCaseId
                memory("usedUseCases", updatedUseCases)
                getLocal(AGENT_TAGS_LOCAL_CONTEXT_KEY)?.takeIf { it is Tags }?.let {
                    val tags = it as Tags
                    usedUseCases.forEachIndexed { i, uc -> tags.tag("tag.tags.$i", uc) }
                }
            }

            val loadedUseCases = getCurrentUseCases()
            val useCase = loadedUseCases?.useCases?.find { it.id == useCaseId }
            emit(UseCaseEvent(useCaseId, stepId, version = useCase?.version, description = useCase?.description))
            loadedUseCases?.let {
                setCurrentUseCases(
                    it.copy(
                        currentUseCaseId = useCaseId,
                        currentStep = stepId,
                        usedUseCases = updatedUseCases ?: it.usedUseCases,
                    ),
                )
                addData(Data(name = it.name, data = it.processedUseCases))
                outputContext("useCase", useCaseId)
            }
        }
        return message.update(cleanMessage)
    }
}

/**
 * Gets and sets the current step from the memory.
 */
suspend fun DSLContext.getUseCaseStep(): String? = memory<String>("current_step")
suspend fun DSLContext.setUseCaseStep(step: String?) {
    memory("current_step", step)
}
