// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SubUseCaseTest {

    val testCases = """
            ### UseCase: usecase
            #### Description
            The description.
            #### Solution
            Primary Solution.
            ----
            ### Case: sub_usecase
            Sub use case.
            Sub use case, line 2.
            ----
    """.trimIndent().toUseCases()

    @Test
    fun `test sub use case parsing`() {
        assertThat(testCases).hasSize(2)
        assertThat(testCases[0].id).isEqualTo("usecase")
        assertThat(testCases[0].subUseCase).isEqualTo(false)
        assertThat(testCases[1].id).isEqualTo("sub_usecase")
        assertThat(testCases[1].subUseCase).isEqualTo(true)
        assertThat(testCases[1].solution.joinToString { it.text }).isEqualTo("""Sub use case., Sub use case, line 2.""")
    }

    @Test
    fun `test sub use case formatting`(): Unit = runBlocking {
        val result = testCases.formatToString()
        assertThat(result).isEqualTo(
            """
            ### UseCase: usecase
            #### Description
            The description.
            
            #### Solution
            Primary Solution.
            
            ----
            
            
            """.trimIndent(),
        )
    }
}
