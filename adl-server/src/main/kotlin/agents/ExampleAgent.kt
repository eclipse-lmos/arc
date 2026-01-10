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
              Do not mention company names or products in the examples unless they are explicitly part of the use case description.
              
              ## OUTPUT FORMAT
              Output the examples as a markdown list. Example:
                - Example 1
                - Example 2
                - Example 3
            """
        }
    }
}.getAgents().first() as ConversationAgent
