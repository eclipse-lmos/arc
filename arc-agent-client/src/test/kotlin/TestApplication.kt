// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.spring.Agents
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class TestApplication {

    @Bean
    open fun agent02(agent: Agents) = agent {
        name = "Agent02"
        filterInput { breakWith("2") }
        prompt { "" }
    }

    @Bean
    open fun agent01(agent: Agents) = agent {
        name = "Agent01"
        filterInput { breakWith("1") }
        prompt { "" }
    }

    @Bean
    open fun chatCompleterProvider() = ChatCompleterProvider {
        object : ChatCompleter {
            override suspend fun complete(
                messages: List<ConversationMessage>,
                functions: List<LLMFunction>?,
                settings: ChatCompletionSettings?,
            ) = result<AssistantMessage, ArcException> {
                functions?.forEach { function ->
                    function.execute(mapOf("param" to "test"))
                }
                AssistantMessage("Hello!")
            }
        }
    }
}
