// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents.extensions

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.RetrySignal
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.AgentDefinition
import org.eclipse.lmos.arc.agents.dsl.AgentInputFilter
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.InputFilterContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentAgent
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory

/**
 * Key for storing retry reason in the RetrySignal.
 */
const val RESPONSE_GUIDE_RETRY_REASON = "RESPONSE_GUIDE_RETRY_REASON"

/**
 * Filter to prevent the agent from asking questions that can be answered from the conversation history.
 */
class ConversationGuider(private val retryMax: Int = 4, private val model: String? = null) : AgentOutputFilter {
    private val log = LoggerFactory.getLogger(ConversationGuider::class.java)
    private val noAnswerReturn = "NO_ANSWER"
    private val outputDivider = "Final Answer >>"

    private val symbolRegex = "<(.*?)>".toRegex(RegexOption.IGNORE_CASE)

    private fun system() =
        """
    ## Goal
    You are an AI assistant designed to answer questions by referencing information previously shared in this conversation.
    
    ## Instructions 
    - You *must* only use information explicitly stated in the conversation history to formulate your answer.
    - Do not introduce new external information or make assumptions.
    - If the answer cannot be derived from the conversation history, return "$noAnswerReturn".
    - Formulate your response so that it is clear what information you are using from the conversation history.
        
      Use the following format in your response:
    
      Question: the input question you must answer.
      Thought: you should always think about what to do.
      Action: the action to take, examine the conversation so far and extract the answer or conclude that the answer cannot be derived from the conversation.
      Observation: the result of the action.
      (Note: this Thought/Action/Observation can repeat N times)
      Thought: I now know the final answer.
      $outputDivider the final answer to the original input question.

  """

    override suspend fun filter(message: ConversationMessage, context: OutputFilterContext): ConversationMessage? {
        if (!message.content.contains("?")) return message

        log.debug("Checking Agent response: ${message.content}")
        val conversation = context.get<Conversation>()
        val cleanOutputMessage = message.content.clean()
        val result =
            context.callLLM(
                buildList {
                    add(SystemMessage(system()))
                    conversation.transcript.forEach { add(it) }
                    add(UserMessage(cleanOutputMessage))
                },
            )

        if (result is Failure) {
            log.warn("ResponseHelper failed!", result.reason)
            return message
        }

        val output = result.getOrNull()?.content?.substringAfter(outputDivider) ?: return message
        val agentName = context.getCurrentAgent()?.name ?: return message
        if (!output.contains(noAnswerReturn, ignoreCase = true)) {
            val previousHistory = context.getOptional<RetrySignal>()?.details ?: emptyMap()
            val newHistory = mapOf(cleanOutputMessage to output)
            log.info("Retrying $agentName...")
            context.retry(max = retryMax, details = previousHistory + newHistory, reason = RESPONSE_GUIDE_RETRY_REASON)
        }
        return message
    }

    suspend fun DSLContext.callLLM(messages: List<ConversationMessage>) = result<AssistantMessage, ArcException> {
        val chatCompleterProvider = get<ChatCompleterProvider>()
        val chatCompleter = chatCompleterProvider.provideByModel(model = model)
        return chatCompleter.complete(
            messages,
            null,
            settings = ChatCompletionSettings(temperature = 0.0, seed = 42),
        )
    }

    private fun String.clean(): String = replace(symbolRegex, "").trim()
}

/**
 * Filter to provide input hints based on the response helper's history.
 */
class InputHintProvider : AgentInputFilter {
    private val log = LoggerFactory.getLogger(ConversationGuider::class.java)

    override suspend fun filter(message: ConversationMessage, context: InputFilterContext): ConversationMessage? {
        val retry = context.getOptional<RetrySignal>()
        val history = retry?.details
        return if (RESPONSE_GUIDE_RETRY_REASON == retry?.reason && history != null) {
            log.info("Updating input message hints: $history")
            message.update(
                """
                {
                   Customer: { message: "${message.content}" }
                   Notes: { Hint: "$history" }
                }
                """.trimIndent(),
            )
        } else {
            message
        }
    }
}
