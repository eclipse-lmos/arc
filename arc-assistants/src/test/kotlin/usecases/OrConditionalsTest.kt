// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class OrConditionalsTest {

    @Test
    fun `test or conditional in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>FooBar
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |FooBar
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test negative or conditional in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or !bar>FooBar
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi").trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |FooBar
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with end tag in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>
               FooBar
            </>
            <else>
              else
            </>  
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |
           |FooBar
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional in solution - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>FooBar
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("nope")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional in solution with multiple conditions - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <tag>tag
            <foo or bar>foo
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("tag")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |tag
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional in solution with negative conditions - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <bar>Bar
            <!foo>foo
            <else>else
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi").trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |foo
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with more than two conditions - match first`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar or baz>Multiple conditions matched
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Multiple conditions matched
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with more than two conditions - match last`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar or baz>Multiple conditions matched
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("baz")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Multiple conditions matched
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with more than two conditions - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar or baz>Multiple conditions matched
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("qux")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional in steps - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Steps
            <foo or bar>Step with condition
            Regular step
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Steps
           |Step with condition
           |Regular step
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional in steps - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Steps
            <foo or bar>Step with condition
            Regular step
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("other")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Steps
           |Regular step
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with else block - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>
               Matched content
            </>
            <else>
              Else content
            </>  
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("other")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |
           |Else content
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional combined with negative condition - match or`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>OR matched
            <!baz>Not baz
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |OR matched
           |Not baz
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test multiple or conditionals in solution - mixed matches`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>First OR
            <baz or qux>Second OR
            End
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar", "qux")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |First OR
           |Second OR
           |End
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test multiple or conditionals in solution - one matches`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>First OR
            <baz or qux>Second OR
            End
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |First OR
           |End
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with more than two conditions - match middle`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar or baz or qux>Multiple conditions matched
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Multiple conditions matched
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with negative condition inside - match positive`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or !bar>Content
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo", "bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Content
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with negative condition inside - match negative`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or !bar>Content
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("baz")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Content
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with negative condition inside - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or !bar>Content
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("bar")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test nested or conditionals with end tags - both match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>
            Outer content
            <baz or qux>Inner content
            </>
            End
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo", "baz")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |
           |Outer content
           |Inner content
           |End
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test nested or conditionals with end tags - outer match only`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>
            Outer content
            <baz or qux>Inner content
            </>
            End
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |
           |Outer content
           |End
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with empty conditions set`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>Should not appear
            Default content
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = emptySet()).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Default content
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with multiple matches in conditions`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>Content
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo", "bar", "baz")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Content
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test multiple or conditionals - none match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>First
            <baz or qux>Second
            End
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("other")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |End
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test or conditional with whitespace in condition names`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo or bar>Content
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Content
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }
}
