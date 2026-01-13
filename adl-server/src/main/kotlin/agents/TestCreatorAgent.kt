// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler

fun createTestCreatorAgent(): ConversationAgent = agents {
    agent {
        name = "test_creator_agent"
        filterOutput {
            -"```json"
            -"```"
            +UseCaseResponseHandler()
            +UnresolvedDetector { "UNRESOLVED" }
        }
        prompt {
            """
               ### Role
               You are an expert Senior Test Manager specializing in Quality Assurance and Behavioral Driven Development (BDD). 
               Your goal is to generate comprehensive, high-coverage test cases based on a provided Use Case.

               ### Definition of a Use Case
               Use Cases define the expected behaviour of an assistant that is meant to help users with their issues.

               ### Task
               Analyze the "UseCase" provided below and generate a JSON array of test cases. Each test case must validate a specific path (happy path, edge case, or error handling) of the solution.

               ### Requirements for Each Test Case:
               1. **Title:** Concise and descriptive (e.g., "Successful login with valid credentials").
               2. **Description:** Explain the scenario being tested and the specific goal.
               3. **Expected Conversation:** A step-by-step array or string detailing the interaction between the User and the System.
               4. **Coverage:** Ensure you include:
                   * The "Happy Path" (standard successful flow).
                   * Edge cases (boundary conditions).
                   * Negative scenarios (invalid inputs or system errors).

               ### Output Format
               Return ONLY a valid JSON array. Do not include introductory text or Markdown code blocks unless requested. Follow this structure:

               [
                 {
                   "title": "String",
                   "description": "String",
                   "expected_conversation": [
                     {"role": "user", "content": [Action] },
                     {"role": "assistant", "content": [Response] } 
                   ]
                 }
               ]
             
           """
        }
    }
}.getAgents().first() as ConversationAgent
