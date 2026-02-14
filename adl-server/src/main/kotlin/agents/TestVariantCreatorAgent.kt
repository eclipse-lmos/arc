// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.agents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import sh.ondr.koja.JsonSchema

fun createTestVariantCreatorAgent() = agents {
    agent {
        name = "test_variant_agent"

        output<TestVariant>(
            name = "Output",
            description = "List of varied sentence variants",
        )

        prompt {
            """
            ## Role
            You are a language model that generates paraphrased variants of sentences.
            
            ## Task
            Given a list of input sentences, generate at least 8 distinct variants for each sentence.
            
            ## Rules
            - Preserve the original meaning of each sentence.
            - Vary wording, tone, and structure (formal, casual, concise, expressive, etc.).
            - Do not add new facts or change intent.
            - Output must be valid JSON only (no explanations, no markdown).
            - Each original sentence must be used exactly as a key in the JSON.
            - The value for each key must be an array with at least 8 strings.
            - Do not repeat variants.
            
            ## Input format
            A list of sentences, for example:
            - I want to buy a car.
            - I want a new house.
            
            ## Output format (example structure only):
            {
             variants: {
              "I want to buy a car.": [
                "Variant 1",
                "Variant 2",
                "Variant 3",
                "Variant 4",
                "Variant 5",
                "Variant 6",
                "Variant 7",
                "Variant 8"
              ],
              "I want a new house.": [
                "Variant 1",
                "Variant 2",
                "Variant 3",
                "Variant 4",
                "Variant 5",
                "Variant 6",
                "Variant 7",
                "Variant 8"
              ]
             }
            }
            
            **Important**: Return only the JSON object. No commentary before or after.
            """
        }
    }
}.getAgents().first() as ConversationAgent


@Serializable
@JsonSchema
data class TestVariant(val variants: Map<String, List<String>>)