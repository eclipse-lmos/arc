// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.ConversationContext
import org.eclipse.lmos.arc.api.UserContext
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.junit.jupiter.api.Test

class ContextHandlerTest {

    private val testRequest = AgentRequest(
        emptyList(),
        ConversationContext("1"),
        emptyList(),
        UserContext(profile = emptyList()),
    )

    @Test
    fun `test CombineContextHandler`(): Unit = runBlocking {
        val combineContextHandler =
            CombineContextHandler(TestContextHandler(setOf("Test")), TestContextHandler(setOf(32)))
        val result = combineContextHandler.inject(testRequest) { context ->
            assertThat(context).contains("Test", 32)
            Success(Conversation())
        }
        assertThat(result is Success).isTrue()
    }

    @Test
    fun `test combine function`(): Unit = runBlocking {
        val handlers =
            listOf(TestContextHandler(setOf("Test")), TestContextHandler(setOf(32)), TestContextHandler(setOf(true)))
        val result = handlers.combine().inject(testRequest) { context ->
            assertThat(context).contains("Test", 32, true)
            Success(Conversation())
        }
        assertThat(result is Success).isTrue()
    }
}

class TestContextHandler(private val context: Set<Any>) : ContextHandler {
    override suspend fun inject(
        request: AgentRequest,
        block: suspend (Set<Any>) -> Result<Conversation, AgentFailedException>,
    ): Result<Conversation, AgentFailedException> {
        return block(context)
    }
}
