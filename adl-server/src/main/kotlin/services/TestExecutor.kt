// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.services

import org.eclipse.lmos.adl.server.models.SimpleMessage
import org.eclipse.lmos.adl.server.models.ConversationTurn
import org.eclipse.lmos.adl.server.models.TestCase
import org.eclipse.lmos.adl.server.models.TestExecutionResult
import org.eclipse.lmos.adl.server.models.TestRunResult
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.OutputContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getUseCase
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Service for executing tests.
 */
class TestExecutor(
    private val assistantAgent: ConversationAgent,
    private val adlStorage: AdlRepository,
    private val testCaseRepository: TestCaseRepository,
    private val conversationEvaluator: ConversationEvaluator,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun executeTests(adlId: String, testCaseId: String? = null): TestRunResult {
        val adl = adlStorage.get(adlId) ?: throw IllegalArgumentException("ADL not found: $adlId")
        val useCases = adl.content.toUseCases()

        val testCases = if (testCaseId != null) {
            val testCase = testCaseRepository.findById(testCaseId)
                ?: throw IllegalArgumentException("Test Case not found: $testCaseId")
            listOf(testCase)
        } else {
            testCaseRepository.findByADLId(adlId)
        }

        val results = testCases.map { testCase ->
            executeTestCase(testCase, useCases)
        }

        val overallScore = if (results.isNotEmpty()) results.map { it.score }.average() else 0.0

        return TestRunResult(
            overallScore = overallScore,
            results = results,
        )
    }

    private suspend fun executeTestCase(testCase: TestCase, useCases: Any): TestExecutionResult {
        val results = testCase.variants.map {
            runSingleTestCase(it, testCase, useCases)
        }
        return results.minByOrNull { it.score } ?: results.first()
    }

    private suspend fun runSingleTestCase(input: List<ConversationTurn>, testCase: TestCase, useCases: Any): TestExecutionResult {
        val transcript = mutableListOf<ConversationMessage>()
        val actualConversation = mutableListOf<SimpleMessage>()
        var failureReason: String? = null
        val conversationId = "test-${testCase.id}-${UUID.randomUUID()}"
        val useCasesHistory = mutableListOf<String>()

        try {
            for (turn in input) {
                if (turn.role == "user") {
                    val userMsg = UserMessage(turn.content)
                    transcript.add(userMsg)
                    actualConversation.add(SimpleMessage("user", turn.content))

                    val conv = Conversation(transcript = transcript, conversationId = conversationId)
                    val outputContext = OutputContext()
                    val result = assistantAgent.execute(conv, setOf(useCases, outputContext))
                    outputContext.getUseCase()?.let { useCasesHistory.add(it) }

                    when (result) {
                        is Success -> {
                            val assistantMsg = result.value.latest<AssistantMessage>() ?: AssistantMessage("")
                            transcript.add(assistantMsg)
                            actualConversation.add(SimpleMessage("assistant", assistantMsg.content))
                        }

                        is Failure -> {
                            failureReason = "Agent execution failed: ${result.reason.message}"
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            failureReason = "Exception: ${e.message}"
        }

        val evalOutput = conversationEvaluator.evaluate(
            actualConversation,
            testCase.expectedConversation.map { SimpleMessage(it.role, it.content) },
        )

        val finalVerdict = if (failureReason != null) "fail" else evalOutput.verdict
        val finalReasons = if (failureReason != null) (evalOutput.reasons + failureReason) else evalOutput.reasons

        return TestExecutionResult(
            testCaseId = testCase.id,
            testCaseName = testCase.name,
            status = if (finalVerdict == "fail") "FAIL" else "PASS",
            score = evalOutput.score,
            actualConversation = actualConversation.map { ConversationTurn(it.role, it.content) },
            details = evalOutput.copy(verdict = finalVerdict, reasons = finalReasons as MutableList<String>),
            useCases = useCasesHistory
        )
    }
}
