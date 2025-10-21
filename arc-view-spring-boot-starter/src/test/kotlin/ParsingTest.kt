// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.view

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ParsingTest {
    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `test valid use cases are parsed`(): Unit = runBlocking {
        val data = """
        ### UseCase: usecase
        #### Description
        // this is a comment
        The description of the use case 2.
     
        #### Solution
        Primary Solution
        <Conditional> Some condition
        ----
        """.trimIndent()

        val response = restTemplate.postForEntity("/arc/usecases", data, String::class.java).body
        assertThat(response).isEqualTo(
            """{"useCases":[{"id":"usecase","description":"The description of the use case 2.\n\n","steps":[],"solution":[{"text":"Primary Solution","conditions":[],"functions":[],"useCaseRefs":[],"endConditional":false},{"text":"Some condition","conditions":["Conditional"],"functions":[],"useCaseRefs":[],"endConditional":false}],"alternativeSolution":[],"fallbackSolution":[],"examples":"","conditions":[],"goal":[],"subUseCase":false}],"useCaseCount":1,"errors":[]}""",
        )
    }
}
