
// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

fun createImprovementAgent(): ConversationAgent = agents {
    agent {
        name = "improvement_agent"
        filterOutput {
            -"```json"
            -"```"
        }
        prompt {
           """
                ### Role
                You are an expert AI assistant specialized in analyzing and improving Use Cases for conversational agents.
    
                ### Definition of a Use Case
                Use Cases define the expected behaviour of an assistant that is meant to help users with their issues.
    
                ### Task
                Analyze the provided Use Case and suggest specific improvements to enhance its clarity, completeness, and effectiveness. Focus on identifying any gaps, ambiguities, or areas where additional detail could improve the Use Case.
    
                ### Output Format
                Return ONLY a valid JSON object with the following structure:
    
                {
                  "improvements": [
                     {
                        "issue": "String describing the identified issue",
                        "suggestion": "String detailing the suggested improvement",
                        "improved_use_case": "String with the revised section of the Use Case reflecting the improvement"
                     }
                  ]
                }
    
                Ensure that your suggestions are actionable and directly address the identified issues.
                """
        }
    }
}.getAgents().first() as ConversationAgent
