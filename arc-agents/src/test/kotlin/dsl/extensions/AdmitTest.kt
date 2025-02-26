// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.beans
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AdmitTest : TestBase() {

    private val message = "message".toConversation(User("user")).transcript.last()

    @Test
    fun `test first conversation is blocked`(): Unit = runBlocking {
        with(BasicDSLContext(beans(Conversation(conversationId = UUID.randomUUID().toString())))) {
            val filter = AdmitFilter(50, "rejected")
            val success = filter.runTest()
            assertThat(success).isEqualTo(0)
        }
        with(BasicDSLContext(beans(Conversation(conversationId = UUID.randomUUID().toString())))) {
            val filter = AdmitFilter(50, "rejected")
            val success = filter.runTest()
            assertThat(success).isEqualTo(100)
        }
    }

    @Test
    fun `test with multiple conversations`() {
        val success = AtomicInteger(0)
        runBlocking {
            repeat(100) {
                launch {
                    with(BasicDSLContext(beans(Conversation(conversationId = UUID.randomUUID().toString())))) {
                        val filter = AdmitFilter(10, "rejected")
                        if (filter.runTest(times = 1) > 0) {
                            success.incrementAndGet()
                        }
                    }
                }
            }
        }
        assertThat(success.get()).isEqualTo(10)
    }

    private suspend fun AdmitFilter.runTest(times: Int = 100): Int {
        var success = 0
        repeat(times) {
            try {
                filter(message)
                success++
            } catch (e: InterruptProcessingException) {
                assertThat(e.conversation.transcript.last().content).isEqualTo("rejected")
            }
        }
        return success
    }
}
