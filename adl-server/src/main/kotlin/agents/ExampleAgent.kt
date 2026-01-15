// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

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
            ## Role:
            You are an expert conversational AI designer.
            
            ## Task:
            Generate realistic example user utterances that would trigger the given use case.
            
            ## Instructions:
            Only generate user utterances, not system responses.
            Utterances should be natural, concise, and varied in phrasing.
            Do not restate the use case ID, description, or solution in the utterances.
            Focus on what a real user would say when experiencing the problem described.
            Vary tone (formal, informal, frustrated, concise) while keeping intent consistent.
            Provide 10–30 examples.
            Do not include explanations or extra text—only the formatted output.
            Do not mention company names or specific products unless specified in the use case.
            
            ## Input Use Case Format:
            ```markdown
            ### UseCase: <use case id>
            #### Description
            <description of the use case>
            
            #### Solution
            <instructions to solve the issue>
            ```
            
            ## Output Format:
            - <example utterance 1>
            - <example utterance 2>
            - <example utterance 3>
            
            """
        }
    }
}.getAgents().first() as ConversationAgent
