// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.adl.server.agents.extensions.RESPONSE_GUIDE_RETRY_REASON
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory

/**
 * An [AgentOutputFilter] that extracts "MUST" instructions from processed use cases and verifies compliance.
 */
class MustFeature(private val keyword: String = "MUST", private val retryMax: Int = 3) : AgentOutputFilter {

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun filter(
        message: ConversationMessage,
        context: OutputFilterContext
    ): ConversationMessage {
        val useCases = context.getCurrentUseCases() ?: return message
        val currentUseCaseId = useCases.currentUseCaseId ?: return message
        val processedUseCasesText = useCases.processedUseCaseMap[currentUseCaseId] ?: return message

        val mustInstructions = processedUseCasesText
            .substringAfter("## Solution", "") // Get text after "## Solution"
            .split(Regex("(?<=[.!?])\\s+")) // Split into sentences
            .filter { it.contains(Regex("\\b$keyword\\b")) } // Match whole word "MUST"
            .map { it.trim() }

        if (mustInstructions.isEmpty()) {
            return message
        }

        val instructionsText = mustInstructions.joinToString("\n- ")
        log.info("Verifying MUST instructions:\n- $instructionsText")

        val verificationResult = context.llm(
            system = """
                You are a Quality Assurance Evaluator. 
                Your role is to rigorously assess whether an Assistant’s responses comply with all required "MUST" instructions.
                Use the full conversation history to determine compliance.
                
                ----
                
                ## Evaluation Instructions
                You must evaluate the Agent’s response against the following mandatory requirements:

                MUST Instructions:
                ```
                $instructionsText
                ```
      
                 Conversation History:
                 ```
                 ${context.get<Conversation>().transcript.joinToString("\n") { "${it.javaClass.simpleName}: ${it.content}" }}
                 AssistantMessage: ${message.content}
                 ```
                 
                 Evaluate whether the Assistant Responses complies with the MUST instructions.
                 
                ----
                
                ## Evaluation Process

                1. Instruction Decomposition
                Break down $instructionsText into distinct, testable requirements.
                Treat each "MUST" instruction as independently mandatory.

                2. Contextual Validation
                Use the full conversation history to determine:
                - Whether prior constraints apply.
                - Whether the response contradicts earlier instructions.
                - Whether required context-dependent behavior was followed.
                
                3. Strict Compliance Check
                Every MUST instruction must be fully satisfied.
                Partial compliance = failure.
                Implicit or assumed compliance is not acceptable.
                If any instruction is ambiguous, interpret it conservatively.
                
                4. Failure Detection Rules
                Missing required elements = failure.
                Format violations = failure.
                Tone/style violations (if specified as MUST) = failure.
                Logical contradictions = failure.
                Ignoring conversation context = failure.
                
                ----
                
                ## Output Rules (Critical)

                If all MUST instructions are fully satisfied, output exactly:
                ```
                PASS
                ```

                If any MUST instruction is violated, output:
                A concise but specific explanation of:
                - Which instruction was violated
                - Why it was violated
                - The final answer from the Assistant rephrased to comply with the MUST instructions so that it can be to sent to the user.
                - Do NOT output "PASS" in this case.
                - Do NOT include praise, soft language, or meta commentary.

                ----

                ## Output Format

                If PASS:
                ```
                PASS
                ```
                
                If FAIL:
                ```
                FAIL
                 
                Issue:
                - [Instruction violated]
                - Explanation: [Why it fails]
                - Fixed Response: [The final answer from the Assistant rephrased to comply with the MUST instructions so that it can be to sent to the user.]
                ```
                
                **Important*: The Fixed Response MUST be ready to be sent directly to the user. 
                - It MUST match the tone, style, and format requirements specified in the MUST instructions.
                - The Fixed Response should not include any explanations, apologies, or meta commentary. 
                
            """.trimIndent(),
            user = "Verify the response."
        ).getOrThrow().content

        if (verificationResult.replace("```", "").replace(""""""", "").trim().uppercase() == "PASS") {
            log.info("MustFeature verification passed.")
            return message
        }

        log.warn("MustFeature verification failed: $verificationResult")
        val fixedResponse = verificationResult.substringAfter("Fixed Response:")
            .trim()
            .replace("```", "")
            .replace(""""""", "")

        if (fixedResponse.isNotEmpty()) {
            log.info("Updating response with fixed version from verification: $fixedResponse")
            return message.update(fixedResponse)
        }

        context.retry(
            max = retryMax,
            details = mapOf("error" to "The following instructions must be followed: $instructionsText"),
            reason = RESPONSE_GUIDE_RETRY_REASON
        )

        return message
    }
}
