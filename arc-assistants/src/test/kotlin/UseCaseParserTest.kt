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

class UseCaseParserTest : TestBase() {

    @Test
    fun `test use case parsing works`(): Unit = runBlocking {
        withDSLContext {
            val useCases = local("use_cases.md")!!.toUseCases()
            val expectedResult = local("use_cases_out.md")!!
            val parsedUseCases = useCases.formatToString()
            assertThat(parsedUseCases.trim()).isEqualTo(("#" + expectedResult.substringAfter("#")).trim()) // remove copyright
        }
    }

    @Test
    fun `test use case conditional lines work`(): Unit = runBlocking {
        withDSLContext {
            val useCases = local("use_cases.md")!!.toUseCases()
            val expectedResult = local("use_cases_mobile.md")!!
            val parsedUseCases = useCases.formatToString(conditions = setOf("mobile"))
            assertThat(parsedUseCases.trim()).isEqualTo(("#" + expectedResult.substringAfter("#")).trim()) // remove copyright
        }
    }
}
