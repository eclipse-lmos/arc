// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenAiChatParametersTest {

    @Test
    fun `gpt-5 family requires max_completion_tokens`() {
        assertThat(requiresMaxCompletionTokens("GPT-5.4")).isTrue()
        assertThat(requiresMaxCompletionTokens("gpt-5.4-mini")).isTrue()
        assertThat(requiresMaxCompletionTokens("GPT-5")).isTrue()
        assertThat(requiresMaxCompletionTokens("o1-mini")).isTrue()
        assertThat(requiresMaxCompletionTokens("o3")).isTrue()
        assertThat(requiresMaxCompletionTokens("o4-mini")).isTrue()
    }

    @Test
    fun `gpt-4 family keeps max_tokens`() {
        assertThat(requiresMaxCompletionTokens("GPT-4o")).isFalse()
        assertThat(requiresMaxCompletionTokens("gpt-4.1")).isFalse()
        assertThat(requiresMaxCompletionTokens("GPT-4o-mini")).isFalse()
        assertThat(requiresMaxCompletionTokens(null)).isFalse()
    }
}
