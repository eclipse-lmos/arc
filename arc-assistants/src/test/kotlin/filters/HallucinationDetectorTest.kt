// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.lmos.arc.agents.HallucinationDetectedException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.Data
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.addData
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.assistants.support.TestBase
import org.junit.jupiter.api.Test

class HallucinationDetectorTest : TestBase() {

    @Test
    fun `implements AgentOutputFilter`() = runBlocking {
        withDSLContext {
            assertThat(HallucinationDetector::class.java).isAssignableTo(AgentOutputFilter::class.java)
        }
    }

    @Test
    fun `returns message when URL is in context data`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Visit https://example.com for more info"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Check out https://example.com")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `throws when URL is not in context data`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Some other content without url"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Check out https://fabricated-site.com")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            assertThatThrownBy {
                runBlocking { HallucinationDetector().filter(message, context) }
            }.isInstanceOf(HallucinationDetectedException::class.java)
                .hasMessageContaining("Fabricated link detected")
                .hasMessageContaining("https://fabricated-site.com")
        }
    }

    @Test
    fun `returns message when URL was in earlier assistant response`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Unrelated content"))
            val earlierResponse = AssistantMessage("Here is the link: https://agent-provided.org/page")
            val input = Conversation(transcript = listOf(earlierResponse))
            val message = AssistantMessage("As I mentioned, visit https://agent-provided.org/page for details")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `returns message when email is in context data`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Contact support@example.com"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Email us at support@example.com")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `throws when email is not in context`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Some content"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Contact fake@spam-domain.xyz")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            assertThatThrownBy {
                runBlocking { HallucinationDetector().filter(message, context) }
            }.isInstanceOf(HallucinationDetectedException::class.java)
                .hasMessageContaining("Fabricated email detected")
                .hasMessageContaining("fake@spam-domain.xyz")
        }
    }

    @Test
    fun `returns message when email was in earlier assistant response`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Other data"))
            val earlierResponse = AssistantMessage("Your contact is agent@company.de")
            val input = Conversation(transcript = listOf(earlierResponse))
            val message = AssistantMessage("As stated, reach agent@company.de for help")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `returns message when IBAN is in context data`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "IBAN: DE89370400440532013000"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Transfer to DE89370400440532013000")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `throws when IBAN is not in context`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Some content"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Pay to DE89370400440532013000")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            assertThatThrownBy {
                runBlocking { HallucinationDetector().filter(message, context) }
            }.isInstanceOf(HallucinationDetectedException::class.java)
                .hasMessageContaining("Fabricated Iban detected")
                .hasMessageContaining("DE89370400440532013000")
        }
    }

    @Test
    fun `returns message when getData is null without running checks`() = runBlocking {
        withDSLContext {
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Check https://any-url.com and contact any@email.com")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }

    @Test
    fun `returns message when content has no URLs emails or IBANs`() = runBlocking {
        withDSLContext {
            addData(Data("tool", "Some reference data"))
            val input = Conversation(transcript = emptyList())
            val message = AssistantMessage("Hello, how can I help you today?")
            val output = input + message
            val context = OutputFilterContext(this, input, output, "")

            val result = HallucinationDetector().filter(message, context)

            assertThat(result).isEqualTo(message)
        }
    }
}
