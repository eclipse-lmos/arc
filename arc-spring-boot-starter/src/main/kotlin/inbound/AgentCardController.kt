// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.inbound

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.AgentProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AgentCardController(private val agentProvider: AgentProvider) {

    @GetMapping("/.well-known/agent.json")
    suspend fun getAgentCard(): AgentCard {
        val agents = agentProvider.getAgents()
        val agent = agents.firstOrNull { it.skills()?.isNotEmpty() == true }

        return AgentCard(
            name = if (agents.size > 1) "multi-agent" else agent?.name ?: "arc-agent",
            description = if (agents.size > 1) {
                "Multiple Agents: ${agents.joinToString { it.name }}"
            } else {
                agent?.description
                    ?: "undefined"
            },
            url = "",
            version = if (agents.size > 1) "1.0.0" else agent?.version ?: "1.0.0",
            defaultInputModes = listOf("text"),
            defaultOutputModes = listOf("text"),
            capabilities = Capabilities(
                streaming = false,
                pushNotifications = false,
                stateTransitionHistory = false,
            ),
            skills = agents.flatMap {
                it.skills()?.map { skill ->
                    Skill(
                        id = skill.id,
                        name = skill.name,
                        description = skill.description,
                        tags = skill.tags,
                        examples = skill.examples,
                        inputModes = skill.inputModes,
                        outputModes = skill.outputModes,
                    )
                } ?: emptyList()
            },
        )
    }
}

@Serializable
data class AgentCard(
    val name: String,
    val description: String,
    val url: String,
    val provider: Provider? = null,
    val version: String,
    val documentationUrl: String? = null,
    val capabilities: Capabilities,
    val authentication: Authentication? = null,
    val defaultInputModes: List<String>,
    val defaultOutputModes: List<String>,
    val skills: List<Skill>,
)

@Serializable
data class Authentication(
    val schemes: List<String>,
    val credentials: String? = null,
)

@Serializable
data class Capabilities(
    val streaming: Boolean = false,
    val pushNotifications: Boolean = false,
    val stateTransitionHistory: Boolean = false,
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val examples: List<String>? = null,
    val inputModes: List<String>? = null,
    val outputModes: List<String>? = null,
)

@Serializable
data class Provider(
    val organization: String,
    val url: String,
)
