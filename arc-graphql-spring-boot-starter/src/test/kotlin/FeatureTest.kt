// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.Metadata
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.Value
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.agents.dsl.extensions.getFeature
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.ConversationContext
import org.eclipse.lmos.arc.api.UserContext
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.graphql.features.OpenFeatureFlags
import org.eclipse.lmos.arc.spring.Agents
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(FeatureTestConfig::class)
class FeatureTest {

    @Autowired
    lateinit var openFeatureFlags: OpenFeatureFlags

    @Autowired
    lateinit var agentResolver: AgentResolver

    @Test
    fun `test openFeatureFlags is defined`(): Unit = runBlocking {
        assertThat(openFeatureFlags).isNotNull
    }

    @Test
    fun `test agent with beta feature`(): Unit = runBlocking {
        val agent = agentResolver.resolveAgent(
            request = AgentRequest(
                messages = emptyList(),
                conversationContext = ConversationContext("cid"),
                systemContext = emptyList(),
                userContext = UserContext(profile = emptyList()),
            ),
        ) as ConversationAgent
        assertThat(agent.name).isEqualTo("beta-agent")

        val answer = agent.ask("Test").getOrThrow()
        assertThat(answer).isEqualTo("test")
    }
}

open class FeatureTestConfig {

    @Bean
    open fun betaAgent(agent: Agents) = agent {
        name = "beta-agent"
        activateOnFeatures = setOf("beta")
        filterInput { breakWith(getFeature("beta", "")) }
    }

    @Bean
    open fun betaAgent2(agent: Agents) = agent {
        name = "beta-agent"
        activateOnFeatures = setOf("beta2")
    }

    @Bean
    open fun featureProvider(): FeatureProvider = object : FeatureProvider {
        override fun getMetadata(): Metadata = Metadata { "TestFeatureProvider" }

        override fun getBooleanEvaluation(
            key: String,
            defaultValue: Boolean?,
            ctx: EvaluationContext?,
        ): ProviderEvaluation<Boolean> {
            return ProviderEvaluation.builder<Boolean>().value(key == "beta").build()
        }

        override fun getStringEvaluation(
            key: String,
            defaultValue: String?,
            ctx: EvaluationContext?,
        ): ProviderEvaluation<String> {
            return ProviderEvaluation.builder<String>().value("test").build()
        }

        override fun getIntegerEvaluation(
            key: String,
            defaultValue: Int?,
            ctx: EvaluationContext?,
        ): ProviderEvaluation<Int> {
            return ProviderEvaluation.builder<Int>().value(1).build()
        }

        override fun getDoubleEvaluation(
            key: String,
            defaultValue: Double?,
            ctx: EvaluationContext?,
        ): ProviderEvaluation<Double> {
            return ProviderEvaluation.builder<Double>().value(1.0).build()
        }

        override fun getObjectEvaluation(
            key: String,
            defaultValue: Value?,
            ctx: EvaluationContext?,
        ): ProviderEvaluation<Value> {
            return ProviderEvaluation.builder<Value>().value(Value("HI")).build()
        }
    }
}
