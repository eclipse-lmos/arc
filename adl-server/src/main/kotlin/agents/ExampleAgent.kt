package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents

/**
 * Creates the example agent that generates examples for ADL use cases.
 */
fun createExampleAgent(): ConversationAgent = agents {
    agent {
        name = "example_agent"
        prompt {
            """
              Generate 20 examples that match the following use case description.
                """
        }
    }
}.getAgents().first() as ConversationAgent
