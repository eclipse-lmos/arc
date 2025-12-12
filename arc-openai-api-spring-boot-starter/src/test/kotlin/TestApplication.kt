// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.result
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class TestApplication {

    @Bean
    open fun chatCompleterProvider() = ChatCompleterProvider {
        object : ChatCompleter {
            override suspend fun complete(
                messages: List<ConversationMessage>,
                functions: List<LLMFunction>?,
                settings: ChatCompletionSettings?,
                eventPublisher: EventPublisher?,
            ) = result<AssistantMessage, ArcException> {
                functions?.forEach { function ->
                    function.execute(mapOf("param" to "test"))
                }
                AssistantMessage("Hello from Bot!")
            }
        }
    }

    @Bean
    open fun myAgent(agent: Agents) = agent {
        name = "agentBean"
        systemPrompt = { "you are a helpful agent that tell funny jokes." }
        description = "Test"
    }
}
