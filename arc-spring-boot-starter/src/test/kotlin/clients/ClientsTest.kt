// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.clients

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ClientsTest {

    @Autowired
    lateinit var chatCompleterProvider: ChatCompleterProvider

    @Test
    fun `test chatCompleterProvider has loaded all expected clients`(): Unit = runBlocking {
        assertThat(chatCompleterProvider.provideByModel("GPT-4o")).isNotNull
        assertThat(chatCompleterProvider.provideByModel("llama3.3")).isNotNull
        assertThat(chatCompleterProvider.provideByModel("GPT-4o-mini")).isNotNull
    }
}
