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
            assertThat(parsedUseCases.trim()).isEqualTo(("#" + expectedResult.substringAfter("#")).trim())
        }
    }

    @Test
    fun `test use case conditional lines work`(): Unit = runBlocking {
        withDSLContext {
            val useCases = local("use_cases.md")!!.toUseCases()
            val expectedResult = local("use_cases_mobile.md")!!
            val parsedUseCases = useCases.formatToString(conditions = setOf("mobile"))
            assertThat(parsedUseCases.trim()).isEqualTo(("#" + expectedResult.substringAfter("#")).trim())
        }
    }

    @Test
    fun `test use case comments`(): Unit = runBlocking {
        val useCases = """
                ### UseCase: usecase
                #### Description
                // this is a comment
                The description of the use case 2.

                #### Solution
                Primary Solution
                ----
            """.toUseCases()
        assertThat(useCases).hasSize(1)
        assertThat(useCases.toString()).doesNotContain("this is a comment")
    }

    @Test
    fun `test conditional use case`(): Unit = runBlocking {
        val useCases = """
                ### UseCase: usecase <myCondition>
                #### Description
                The description of the use case 2.

                #### Solution
                Primary Solution
                ----
            """.toUseCases()
        assertThat(useCases).hasSize(1)
        assertThat(useCases.first().id).isEqualTo("usecase")
        val parsedUseCases = useCases.formatToString(conditions = setOf("myCondition"))
        assertThat(parsedUseCases.replace("\\W".toRegex(), "")).isEqualTo(
            """
             ### UseCase: usecase
                #### Description
                The description of the use case 2.

                #### Solution
                Primary Solution
                ----
        """.replace("\\W".toRegex(), ""),
        )
    }

    @Test
    fun `test conditional use case filtered`(): Unit = runBlocking {
        val useCases = """
                ### UseCase: usecase <myCondition>
                #### Description
                The description of the use case 2.

                #### Solution
                Primary Solution
                ----
            """.toUseCases()
        assertThat(useCases).hasSize(1)
        assertThat(useCases.first().id).isEqualTo("usecase")
        val parsedUseCases = useCases.formatToString(conditions = setOf(""))
        assertThat(parsedUseCases.trim()).isEqualTo("""""".trim())
    }

    @Test
    fun `test negative conditional use case`(): Unit = runBlocking {
        val useCases = """
                ### UseCase: usecase <!myCondition>
                #### Description
                The description of the use case 2.

                #### Solution
                Primary Solution
                ----
            """.toUseCases()
        assertThat(useCases).hasSize(1)
        assertThat(useCases.first().id).isEqualTo("usecase")
        val parsedUseCases = useCases.formatToString(conditions = setOf(""))
        assertThat(parsedUseCases.trim()).isEqualTo("""""".trim())
    }
}
