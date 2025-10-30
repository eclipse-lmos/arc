// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.junit.jupiter.api.Test

class UseCaseExtractorTest : TestBase() {

    @Test
    fun `extracts Use Case ID and cleans message`() {
        val input = "<ID:UC123> This is a message"
        val (id, cleaned) = input.extractId()
        assertThat(id).isEqualTo("UC123")
        assertThat(cleaned).isEqualTo("This is a message")
    }

    @Test
    fun `no Use Case ID present`() {
        val input = "This is a message without ID"
        val (id, cleaned) = input.extractId()
        assertThat(id).isNull()
        assertThat(cleaned).isEqualTo("This is a message without ID")
    }

    @Test
    fun `extracts Use Case ID with spaces`() {
        val input = "<ID:  UC456  > Another message"
        val (id, cleaned) = input.extractId()
        assertThat(id).isEqualTo("UC456")
        assertThat(cleaned).isEqualTo("Another message")
    }

    @Test
    fun `extracts Use Case ID case-insensitive`() {
        val input = "<id:uc789> Message"
        val (id, cleaned) = input.extractId()
        assertThat(id).isEqualTo("uc789")
        assertThat(cleaned).isEqualTo("Message")
    }

    @Test
    fun `multiple IDs only extract the first`() {
        val input = "<ID:UC1> Text <ID:UC2> More text"
        val (id, cleaned) = input.extractId()
        assertThat(id).isEqualTo("UC1")
        assertThat(cleaned).isEqualTo("Text  More text")
    }
}
