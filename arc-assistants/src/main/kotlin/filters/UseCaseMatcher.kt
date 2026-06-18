// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.assistants.support.extensions.UseCasePromptProvider
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure
import org.slf4j.LoggerFactory

class UseCaseMatcher(
    private val model: String? = null,
    private val maxMessages: Int = DEFAULT_MAX_MESSAGES,
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun matchUseCase(
        transcript: List<ConversationMessage>,
        useCases: String,
        context: DSLContext,
    ): String? {
        val systemPrompt = context.getOptional<UseCasePromptProvider>()
            ?.buildSystemPrompt(useCases, context)
            ?: UseCaseMatcherPrompts.build(useCases)
        val userPrompt = buildUserPrompt(transcript)
        val result = context.llm(
            system = systemPrompt,
            user = userPrompt,
            model = model,
        ).onFailure {
            log.warn("Failure while matching UseCase: $it")
        }.getOrNull() ?: return null
        log.debug("Matched UseCase: $result")
        val useCase = result.content.substringAfter("<", missingDelimiterValue = "")
            .substringBefore(">", missingDelimiterValue = "")
        if (useCase.isBlank()) return null
        if (!useCases.contains(useCase)) return null
        log.info("Matched UseCase found: $result")
        return useCase
    }

    private fun buildUserPrompt(transcript: List<ConversationMessage>): String =
        Companion.buildUserPrompt(transcript, maxMessages)

    companion object {
        const val DEFAULT_MAX_MESSAGES = 10

        internal fun buildUserPrompt(
            transcript: List<ConversationMessage>,
            maxMessages: Int,
        ): String {
            val relevantMessageCount = transcript.count { it is UserMessage || it is AssistantMessage }
            val conversationHeader = if (relevantMessageCount > maxMessages) {
                "Conversation (last $maxMessages user/assistant messages; earlier turns omitted):"
            } else {
                "Conversation:"
            }
            val history = formatConversationHistory(transcript, maxMessages)
            return buildString {
                appendLine(conversationHeader)
                appendLine(history)
                appendLine()
                append("Classify the latest assistant message based on the conversation above.")
            }
        }

        internal fun formatConversationHistory(
            transcript: List<ConversationMessage>,
            maxMessages: Int,
        ): String = transcript
            .filter { it is UserMessage || it is AssistantMessage }
            .takeLast(maxMessages)
            .joinToString("\n") { message ->
                when (message) {
                    is UserMessage -> "User: ${message.content}"
                    is AssistantMessage -> "Assistant: ${message.content}"
                    else -> ""
                }
            }
    }
}
