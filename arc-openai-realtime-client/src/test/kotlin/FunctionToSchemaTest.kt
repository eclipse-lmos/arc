package org.eclipse.lmos.arc.agent.client

import org.eclipse.lmos.arc.agent.client.ws.Session
import org.eclipse.lmos.arc.agent.client.ws.toJsonSchema
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParameterType
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionToSchemaTest {

    private val testFunction = object : LLMFunction {
        override val name: String = "test-name"
        override val parameters = ParametersSchema(
            parameters = listOf(
                ParameterSchema(
                    name = "country",
                    description = "The country to get the capital of",
                    type = ParameterType(schemaType = "string"),
                    enum = emptyList(),
                ),
            ),
            required = listOf("country"),
        )
        override val description: String = "test-description"
        override val group: String? = null
        override val isSensitive: Boolean = false

        override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
            error("not implemented")
        }
    }

    @Test
    fun `test converting a simple function`() {
        val result = testFunction.toJsonSchema()
        assertThat(result.toString()).isEqualTo(
            """
            {"type":"function","name":"test-name","description":"test-description","parameters":{"type":"object","required":["country"],"properties":{"country":{"type":"string","description":"The country to get the capital of"}}}}
            """.trimIndent(),
        )
    }

    @Test
    fun `test converting a function within a Session`() {
        val session = Session(tools = listOf(testFunction.toJsonSchema()))
        assertThat(Json.encodeToString(session)).isEqualTo(
            """
            {"tools":[{"type":"function","name":"test-name","description":"test-description","parameters":{"type":"object","required":["country"],"properties":{"country":{"type":"string","description":"The country to get the capital of"}}}}]}
            """.trimIndent(),
        )
    }
}
