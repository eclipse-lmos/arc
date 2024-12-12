// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class FunctionsTest : TestBase() {

    @Test
    fun `test simple function`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        with(context) {
            function(
                name = "get_weather",
                description = "the weather service",
                params = types(string("location", "the location")),
            ) {
                "result"
            }
            val result = context.functions.firstOrNull()
            assertThat(result).isNotNull
            assertThat(result!!.name).isEqualTo("get_weather")
            assertThat(result.description).isEqualTo("the weather service")
            assertThat(result.parameters.parameters).hasSize(1)
            with(result.parameters.parameters.first()) {
                assertThat(name).isEqualTo("location")
                assertThat(type.schemaType).isEqualTo("string")
                assertThat(description).isEqualTo("the location")
            }
            assertThat(result.execute(emptyMap()).getOrThrow()).isEqualTo("result")
        }
    }

    @Test
    fun `test function output builder`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        with(context) {
            function(name = "name", description = "description") {
                repeat(2) {
                    +"result"
                }
                "result"
            }
            val result = context.functions.first().execute(emptyMap()).getOrThrow()
            assertThat(result).isEqualTo("resultresultresult")
        }
    }

    @Test
    fun `test function parameters are passed correctly`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        with(context) {
            function(
                name = "name",
                description = "description",
                params = types(
                    string("p1", description = "p1"),
                    string("p2", description = "p2"),
                ),
            ) { (p1, p2) -> "$p1-$p2" }
            val result = context.functions.first().execute(mapOf("p1" to "p1", "p2" to "p2")).getOrThrow()
            assertThat(result).isEqualTo("p1-p2")
        }
    }

    @Test
    fun `test function exceptions are caught`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        with(context) {
            function(
                name = "name",
                description = "description",
            ) {
                throw ArcException("test")
            }
            val result = context.functions.first().execute(mapOf("p1" to "p1", "p2" to "p2"))
            assertThat(result is Failure).isTrue()
            if (result is Failure) {
                assertThat(result.reason.cause is ArcException).isTrue()
            }
        }
    }
}
