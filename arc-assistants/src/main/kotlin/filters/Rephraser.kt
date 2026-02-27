// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.AgentInputFilter
import org.eclipse.lmos.arc.agents.dsl.InputFilterContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.LoggerFactory


class Rephraser(
    private val model: String? = null,
    private val settings: ChatCompletionSettings? = null,
) : AgentInputFilter {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun systemPrompt(history: String) = """
    ### Role
    You are a Contextual Query Optimizer. 
    Your task is to rewrite the latest user utterance into a standalone,
     self-contained sentence by incorporating all necessary context from the previous conversation history.

    ### Rules
    1. **Preserve Intent:** Do not add new goals or change the user's underlying request.
    2. **Be Concise:** The rephrased version should be clear but not overly wordy.
    3. **No Meta-Talk:** Output ONLY the rephrased text. Do not provide explanations or introductions.
    4. **Standalone:** The output must be understandable even if the conversation history is removed.

    ### Examples
    1. 
    **History:** Assistant: "I see you have a checking and a savings account."
    **User:** "the first one."
    **Rephrased:** "I would like to access my checking account."

    2.
    **History:** Assistant: "Would you like to book the flight to New York or London?"
    **User:** "London, please."
    **Rephrased:** "I would like to book the flight to London."

    3.
    **History:** Assistant: "How may I help you with your account?"
    **User:** "balance."
    **Rephrased:** "I would like to know the balance of my account."

    ### Current Conversation History
    $history

""".trimIndent()


    override suspend fun filter(message: ConversationMessage, context: InputFilterContext): ConversationMessage {
        if (message !is UserMessage) return message

        val conversation = context.input
        // If there is no history (just the current message), no need to rewrite
        if (conversation.transcript.size <= 1) return message

        val chatCompleterProvider = context.get<ChatCompleterProvider>()
        val chatCompleter = chatCompleterProvider.provideByModel(model)

        val history = conversation.transcript.dropLast(1).joinToString("\n") {
            val role = when (it) {
                is UserMessage -> "User"
                is SystemMessage -> "System"
                is AssistantMessage -> "Assistant"
                else -> "Unknown"
            }
            "${role}: ${it.content}"
        }
        val currentInput = message.content
        val prompt = systemPrompt(history)
        val messages = listOf(SystemMessage(prompt), UserMessage(currentInput))

        return try {
            val eventPublisher = context.getOptional<EventPublisher>()
            val result = chatCompleter.complete(messages, settings = settings, eventPublisher = eventPublisher)
            val rewrittenContent = result.getOrNull()?.content ?: currentInput
            log.info("Rewrote input: '$currentInput' to '$rewrittenContent'")
            UserMessage(rewrittenContent)
        } catch (e: Exception) {
            log.warn("Failed to rewrite input", e)
            message
        }
    }
}

