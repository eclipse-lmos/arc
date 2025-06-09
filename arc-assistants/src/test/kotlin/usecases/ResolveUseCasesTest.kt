// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.extensions.resolveReferences
import org.junit.jupiter.api.Test
import java.io.File

class ResolveUseCasesTest {

    @Test
    fun `test use case reference`() {
        val useCases = """
            ### UseCase: usecase
            #### Description
            The description.
            #### Solution
            Primary Solution is #use_cases/usecase1.
            ----
        """.toUseCases().resolveReferences(File("src/test/resources/"))

        assertThat(useCases).hasSize(2)
        assertThat(useCases[0].id).isEqualTo("usecase")
        assertThat(useCases[1].id).isEqualTo("usecase1")
    }

    @Test
    fun `test use case with 2 references`() {
        val useCases = """
            ### UseCase: usecase
            #### Description
            The description.
            #### Steps
            - check out #use_cases/usecase2
            #### Solution
            Primary Solution is #use_cases/usecase1.
            ----
        """.toUseCases().resolveReferences(File("src/test/resources/"))

        assertThat(useCases).hasSize(3)
        assertThat(useCases[0].id).isEqualTo("usecase")
        assertThat(useCases[1].id).isEqualTo("usecase1")
        assertThat(useCases[2].id).isEqualTo("usecase2")
    }

    @Test
    fun `test use case without reference`() {
        val useCases = """
            ### UseCase: usecase
            #### Description
            The description.
            #### Solution
            Primary Solution.
            ----
        """.toUseCases().resolveReferences(File("src/test/resources/"))

        assertThat(useCases).hasSize(1)
        assertThat(useCases[0].id).isEqualTo("usecase")
    }

    @Test
    fun `test use case with internal reference does not access file system`() {
        val file = mockk<File>()

        val useCases = """
            ### UseCase: usecase
            #### Description
            The description.
            #### Solution
            Primary Solution is #usecase2.
            ----
            ### UseCase: usecase2
            #### Description
            The description.
            #### Solution
            Primary Solution.
            ----
        """.toUseCases().resolveReferences(file)

        verify(exactly = 0) { file.listFiles() }
        assertThat(useCases).hasSize(2)
        assertThat(useCases[0].id).isEqualTo("usecase")
        assertThat(useCases[1].id).isEqualTo("usecase2")
    }
}
