// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.ws

import org.eclipse.lmos.arc.agent.client.ws.OpenAIRealtimeClient
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.spring.Agents
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import java.io.File
import java.io.StringReader
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@SpringBootApplication
open class TestApplication {

    val key = File(System.getProperty("user.home"), ".arc/arc.properties").readText().let {
        Properties().apply { load(StringReader(it)) }.getProperty("ARC_AI_KEY")
    }

    @Bean
    open fun myAgent(agent: Agents) = agent {
        name = "agent"
        filterInput {
            lastInputMessage.set(inputMessage)
        }
        prompt { "you are a helpful agent that tell funny jokes." }
    }

    @Bean
    open fun chatCompleterProvider() = ChatCompleterProvider {
        OpenAIRealtimeClient("wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01", key)
    }
}

val lastInputMessage = AtomicReference<ConversationMessage>()
