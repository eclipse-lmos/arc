// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agent.client.ws.Session
import org.eclipse.lmos.arc.agent.client.ws.toJsonSchema
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.Result
import org.junit.jupiter.api.Test

class FunctionToSchemaTest {

    private val testFunction = object : LLMFunction {
        override val name: String = "test-name"
        override val version: String? = null
        override val parameters = ParametersSchema(
            properties = mapOf(
                "country" to ParameterSchema(
                    name = "country",
                    description = "The country to get the capital of",
                    type = "string",
                    enum = emptyList(),
                ),
            ),
            required = listOf("country"),
        )
        override val description: String = "test-description"
        override val group: String? = null
        override val isSensitive: Boolean = false
        override val outputDescription: String? = null

        override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
            error("not implemented")
        }
    }

    @Test
    fun `test converting a simple function`() {
        val result = testFunction.toJsonSchema()
        assertThat(result.toString()).isEqualTo(
            """
            {"type":"function","name":"test-name","description":"test-description","parameters":{"type":"object","properties":{"country":{"type":"string","description":"The country to get the capital of","enum":[]}},"required":["country"]}}
            """.trimIndent(),
        )
    }

    @Test
    fun `test converting a function within a Session`() {
        val session = Session(tools = listOf(testFunction.toJsonSchema()))
        assertThat(Json.encodeToString(session)).isEqualTo(
            """
            {"tools":[{"type":"function","name":"test-name","description":"test-description","parameters":{"type":"object","properties":{"country":{"type":"string","description":"The country to get the capital of","enum":[]}},"required":["country"]}}]}
            """.trimIndent(),
        )
    }
}
