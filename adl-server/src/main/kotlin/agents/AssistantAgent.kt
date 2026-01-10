package org.eclipse.lmos.adl.server.agents

import com.fasterxml.jackson.databind.jsonschema.JsonSerializableSchema
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.info
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.extensions.useCases
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString


fun createAssistantAgent(): ConversationAgent = agents {
    agent {
        name = "assistant_agent"
        filterOutput {
            -"```json"
            -"```"
            +UseCaseResponseHandler()
            +UnresolvedDetector { "UNRESOLVED" }
        }
        prompt {
            val role = local("role.md")!!
            val useCases = processUseCases(useCases = get<List<UseCase>>())
            val prompt = local("assistant.md")!!
                .replace($$$"$$ROLE$$", role)
                .replace($$$"$$USE_CASES$$", useCases)
                .replace($$$"$$TIME$$", time())
            prompt
        }
    }
}.getAgents().first() as ConversationAgent




