// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.conversation

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.User
import org.junit.jupiter.api.Test

class ConversationTest {

    private val testUser = User("testUser")

    @Test
    fun `test conversation isEmpty`() {
        val conversation = Conversation(user = testUser)
        val newConversation = conversation.add(UserMessage("message"))
        assertThat(newConversation.isEmpty()).isFalse
        assertThat(conversation.isEmpty()).isTrue
    }

    @Test
    fun `test conversation getAssistantMessage`() {
        val message = AssistantMessage("message", turnId = "1")
        val conversation = Conversation(user = testUser, currentTurnId = "1").add(message)
        assertThat(conversation.getAssistantMessage("1")).isEqualTo(message)
    }

    @Test
    fun `test conversation add`() {
        val conversation = Conversation(user = testUser, currentTurnId = "1")
        val message = UserMessage("message", turnId = "1")
        val newConversation = conversation.add(message)
        assertThat(newConversation.transcript).containsExactly(message)
    }

    @Test
    fun `test conversation addFirst`() {
        val assistantMessage = AssistantMessage("assistant", turnId = "1")
        val conversation =
            Conversation(user = testUser, currentTurnId = "1", transcript = listOf(UserMessage("1"), UserMessage("2")))
        val newConversation = conversation.addFirst(assistantMessage)
        assertThat(newConversation.transcript).hasSize(3)
        assertThat(newConversation.transcript.first()).isEqualTo(assistantMessage)
    }

    @Test
    fun `test conversation hasSensitiveMessages`() {
        val conversation = Conversation(user = testUser, transcript = listOf(UserMessage("1"), UserMessage("2")))
        val newConversation = conversation.add(UserMessage("3", sensitive = true))
        assertThat(conversation.hasSensitiveMessages()).isFalse()
        assertThat(newConversation.hasSensitiveMessages()).isTrue
    }

    @Test
    fun `test conversation removeLast`() {
        val firstMessage = UserMessage("first", turnId = "1")
        val conversation = Conversation(user = testUser, transcript = listOf(firstMessage, UserMessage("content")))
        val newConversation = conversation.removeLast()
        assertThat(conversation.transcript).hasSize(2)
        assertThat(newConversation.transcript).hasSize(1)
        assertThat(newConversation.transcript.first()).isEqualTo(firstMessage)
    }

    @Test
    fun `test conversation mapTranscript filtering`(): Unit = runBlocking {
        val firstMessage = UserMessage("content", turnId = "1")
        val conversation = Conversation(
            user = testUser,
            currentTurnId = "1",
            transcript = listOf(
                UserMessage("remove"),
                firstMessage,
            ),
        )
        val newConversation = conversation.map { if (it.content == "remove") null else it }
        assertThat(conversation.transcript).hasSize(2)
        assertThat(newConversation.transcript).hasSize(1)
        assertThat(newConversation.transcript.first()).isEqualTo(firstMessage)
    }

    @Test
    fun `test conversation turnId is applied to messages`() {
        val conversation = Conversation(user = testUser, currentTurnId = "23")
        val userMessage = UserMessage("content")
        val assistantMessage = AssistantMessage("content")
        val systemMessage = SystemMessage("content")

        val newConversation = conversation.add(systemMessage).add(userMessage).add(assistantMessage)

        assertThat(newConversation.transcript[0]).isEqualTo(SystemMessage("content", turnId = "23"))
        assertThat(newConversation.transcript[1]).isEqualTo(UserMessage("content", turnId = "23"))
        assertThat(newConversation.transcript[2]).isEqualTo(AssistantMessage("content", turnId = "23"))
    }
}
