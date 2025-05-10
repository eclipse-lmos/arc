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
        val agent = agentProvider.getAgents().firstOrNull { it.skills()?.isNotEmpty() == true }
        return AgentCard(
            name = agent?.name ?: "undefined",
            description = agent?.description ?: "",
            url = "",
            version = agent?.version ?: "1.0.0",
            defaultInputModes = emptyList(),
            defaultOutputModes = emptyList(),
            capabilities = Capabilities(
                streaming = false,
                pushNotifications = false,
                stateTransitionHistory = false,
            ),
            skills = agent?.skills()?.map { skill ->
                Skill(
                    id = skill.id,
                    name = skill.name,
                    description = skill.description,
                    tags = skill.tags,
                    examples = skill.examples,
                    inputModes = skill.inputModes,
                    outputModes = skill.outputModes,
                )
            } ?: emptyList(),
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
