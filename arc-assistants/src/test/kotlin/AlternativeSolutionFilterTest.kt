// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.junit.jupiter.api.Test

class AlternativeSolutionFilterTest : TestBase() {

    @Test
    fun `test alternative solution filter`(): Unit = runBlocking {
        withDSLContext {
            val useCases = local("use_cases.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString()
            assertThat(parsedUseCases).doesNotContain("alternative")
            assertThat(parsedUseCases).contains("usecase1", "usecase2", "usecase3")
        }
    }

    @Test
    fun `test alternative solution filter with alternatives`(): Unit = runBlocking {
        withDSLContext {
            val useCases = local("use_cases.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString(setOf("usecase2"))
            assertThat(parsedUseCases).doesNotContain("Primary Solution")
            assertThat(parsedUseCases).contains("Alternative solution")
        }
    }
}
