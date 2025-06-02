// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.openai.api

import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.events.*
import org.eclipse.lmos.arc.openai.api.inbound.OpenAIController
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration
open class ArcOpenAIApiAutoConfiguration {

    @Bean
    fun openAIController(
        @Value("\${arc.openai.endpoint.key:}") key: String?,
        agentProvider: AgentProvider,
    ): OpenAIController = OpenAIController(agentProvider, key.takeIf { it?.isNotEmpty() == true })
}
