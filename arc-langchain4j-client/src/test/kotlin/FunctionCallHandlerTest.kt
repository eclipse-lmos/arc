// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FunctionCallHandlerTest {

    private lateinit var functionCallHandler: FunctionCallHandler
    private lateinit var testFunction: TestFunction
    private lateinit var sensitiveFunction: TestFunction
    private lateinit var eventPublisher: TestEventPublisher

    @BeforeEach
    fun setUp() {
        testFunction = TestFunction("testFunction", false)
        sensitiveFunction = TestFunction("sensitiveFunction", true)
        eventPublisher = TestEventPublisher()
        functionCallHandler = FunctionCallHandler(
            functions = listOf(testFunction, sensitiveFunction),
            eventHandler = eventPublisher,
            tracer = null,
            functionCallLimit = 3,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `calledSensitiveFunction should return false when no functions were called`() {
        // Assert
        assertThat(functionCallHandler.calledSensitiveFunction()).isFalse()
    }

    @Test
    fun `calledSensitiveFunction should return true when sensitive function was called`() {
        runBlocking {
            // Arrange - We'll manually add the sensitive function to the called functions map
            val functionCallHandlerWithSensitiveFunction = FunctionCallHandler(
                functions = listOf(sensitiveFunction),
                eventHandler = eventPublisher,
                tracer = null,
            )

            // Use reflection to access the private _calledFunctions field
            val field = FunctionCallHandler::class.java.getDeclaredField("_calledFunctions")
            field.isAccessible = true
            val calledFunctions = field.get(functionCallHandlerWithSensitiveFunction) as MutableMap<String, LLMFunction>

            // Add the sensitive function to the called functions map
            calledFunctions["sensitiveFunction"] = sensitiveFunction

            // Assert
            assertThat(functionCallHandlerWithSensitiveFunction.calledSensitiveFunction()).isTrue()
        }
    }

    @Test
    fun `calledSensitiveFunction should return false when only non-sensitive function was called`() {
        runBlocking {
            // Arrange - We'll manually add the non-sensitive function to the called functions map
            val functionCallHandlerWithNonSensitiveFunction = FunctionCallHandler(
                functions = listOf(testFunction),
                eventHandler = eventPublisher,
                tracer = null,
            )

            // Use reflection to access the private _calledFunctions field
            val field = FunctionCallHandler::class.java.getDeclaredField("_calledFunctions")
            field.isAccessible = true
            val calledFunctions =
                field.get(functionCallHandlerWithNonSensitiveFunction) as MutableMap<String, LLMFunction>

            // Add the non-sensitive function to the called functions map
            calledFunctions["testFunction"] = testFunction

            // Assert
            assertThat(functionCallHandlerWithNonSensitiveFunction.calledSensitiveFunction()).isFalse()
        }
    }

    @Test
    fun `handle should fail when function call limit is exceeded`() {
        runBlocking {
            // Arrange - Create a handler with a limit of 1
            val handlerWithLimit = FunctionCallHandler(
                functions = listOf(testFunction),
                eventHandler = eventPublisher,
                functionCallLimit = 1,
                tracer = null,
            )

            // Use reflection to set the functionCallCount to the limit
            val field = FunctionCallHandler::class.java.getDeclaredField("functionCallCount")
            field.isAccessible = true
            val functionCallCount = field.get(handlerWithLimit)
            functionCallCount.javaClass.getMethod("set", Int::class.java).invoke(functionCallCount, 1)

            // Create a mock AiMessage that has tool execution requests
            val aiMessage = mockk<AiMessage>()
            every { aiMessage.hasToolExecutionRequests() } returns true

            // Act
            val result = handlerWithLimit.handle(aiMessage)

            // Assert
            assertThat(result).isInstanceOf(Failure::class.java)
            val exception = (result as Failure).reason
            assertThat(exception).isInstanceOf(ArcException::class.java)
            assertThat(exception.message).contains("Function call limit exceeded")
        }
    }

    @Test
    fun `handle should return empty list when no tool execution requests`() {
        runBlocking {
            // Arrange
            val aiMessage = mockk<AiMessage>()
            every { aiMessage.hasToolExecutionRequests() } returns false

            // Act
            val result = functionCallHandler.handle(aiMessage)

            // Assert
            assertThat(result).isInstanceOf(Success::class.java)
            val messages = (result as Success).value
            assertThat(messages).isEmpty()
        }
    }

    private class TestFunction(
        override val name: String,
        override val isSensitive: Boolean,
    ) : LLMFunction {
        override val version: String? = null
        override val parameters = ParametersSchema()
        override val description: String = "Test function for unit tests"
        override val group: String? = null
        override val outputDescription: String? = null

        override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
            return Success("Function executed successfully with input: $input")
        }
    }

    private class TestEventPublisher : EventPublisher {
        val events = mutableListOf<Event>()

        override fun publish(event: Event) {
            events.add(event)
        }
    }
}
