// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.graphql

import ai.ancf.lmos.arc.agents.AgentProvider
import ai.ancf.lmos.arc.graphql.inbound.AgentQuery
import ai.ancf.lmos.arc.graphql.inbound.AgentSubscription
import com.expediagroup.graphql.server.spring.GraphQLAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource

@AutoConfiguration
@AutoConfigureBefore(
    GraphQLAutoConfiguration::class,
)
@PropertySource("classpath:arc.properties")
open class AgentGraphQLAutoConfiguration {

    @Bean
    fun agentQuery(agentProvider: AgentProvider) = AgentQuery(agentProvider)

    @Bean
    fun agentSubscription(agentProvider: AgentProvider) = AgentSubscription(agentProvider)
}
