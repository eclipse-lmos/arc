// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.agents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort

fun createEvalAgent(): ConversationAgent = agents {
    agent {
        name = "eval_agent"

        // output<EvalOutput>(
        //     name = "Eval Output",
        //     description = "Evaluation output for use case compliance",
        // )

        prompt {
            """
        You are an evaluator that determines whether an assistant response correctly fulfills a specific use case.
        
        INPUTS:
        1. Use case definition (description, steps, solution)
        2. A conversation between the user utterance and assistant response:
           - User: User input
           - Assistant: Assistant response
        
        TASK:
        Evaluate the assistant response strictly against the use case.
        
        EVALUATION LOGIC:
        - The assistant MUST NOT invent alternative solutions not described in the use case.
        - Tone and verbosity are not important unless they interfere with correctness.
        - The assistant MUST follow the instructions in solution and not return the instructions to the user.
        - The assistant MUST NOT refer to the user as "they".
        
        HANDLING AMBIGUITY:
        - If the use case does not define the correct behavior for the situation presented, return "unknown".
        
        OUTPUT:
        Return ONLY a valid JSON object in the following format:
        
        {
          "verdict": "pass | fail | partial | unknown",
          "score": 0-100,
          "reasons": [string],
          "missing_requirements": [string],
          "violations": [string],
          "evidence": [
            {
              "quote": "<short quote from assistant response or user message>",
              "maps_to": "<specific use case requirement>"
            }
          ]
        }
        
        SCORING GUIDANCE:
        - 90–100: Fully correct, all required steps followed
        - 60–89: Mostly correct, minor omission or clarity issue
        - 1–59: Major requirement missing or incorrect action taken
        - 0: Completely incorrect or misleading
        
        Do not include explanations outside the JSON.
        Do not reveal internal reasoning.

            """
        }
    }
}.getAgents().first() as ConversationAgent

@Serializable
// @JsonSchema
data class EvalOutput(
    val verdict: String,
    val score: Int,
    val reasons: List<String>,
    @SerialName("missing_requirements")
    val missingRequirements: List<String>,
    val violations: List<String>,
    val evidence: List<EvalEvidence>,
)

@Serializable
data class EvalEvidence(
    val quote: String,
    @SerialName("maps_to")
    val mapsTo: String,
)
