package org.eclipse.lmos.arc.server.ktor

import kotlinx.serialization.Serializable

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
