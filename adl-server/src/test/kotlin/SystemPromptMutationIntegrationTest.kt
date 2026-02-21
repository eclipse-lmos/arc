// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.ktor.client.engine.cio.CIO as ClientCIO

class SystemPromptMutationIntegrationTest {

    companion object {
    }

    private val testPort = 18080
    private val baseUrl = "http://localhost:$testPort"
    private lateinit var client: HttpClient
    private lateinit var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @BeforeEach
    fun setUp() {
        val qdrantConfig = QdrantConfig(
            host = "localhost",
            port = 6333,
        )
        server = startServer(wait = false, port = testPort, qdrantConfig = qdrantConfig)
        client = HttpClient(ClientCIO)
    }

    @AfterEach
    fun tearDown() {
        client.close()
        server.stop(1000, 2000)
    }

    @Test
    fun `systemPrompt generates prompt with session and increments turn`() {
        runBlocking {
            val adl = """
                ### UseCase: test_case
                #### Description
                Test description.
                
                #### Solution
                Test solution.
                ----
            """.trimIndent()
            val sessionId = "integration-test-session-123"

            val result1 = executeSystemPromptMutation(adl, sessionId = sessionId)
            val result2 = executeSystemPromptMutation(adl, sessionId = sessionId)
            val result3 = executeSystemPromptMutation(adl, sessionId = sessionId)

            assertThat(result1.turn).isEqualTo(1)
            assertThat(result2.turn).isEqualTo(2)
            assertThat(result3.turn).isEqualTo(3)
            assertThat(result1.useCaseCount).isEqualTo(1)
        }
    }

    @Test
    fun `systemPrompt generates prompt without session`() {
        runBlocking {
            val adl = """
                ### UseCase: greeting
                #### Description
                Customer wants to greet the assistant.
                
                #### Solution
                Say hello back to the customer.
                ----
            """.trimIndent()

            val result = executeSystemPromptMutation(adl)

            assertThat(result.useCaseCount).isEqualTo(1)
            assertThat(result.turn).isNull()
            assertThat(result.systemPrompt).contains("greeting")
            assertThat(result.systemPrompt).contains("Customer wants to greet the assistant")
        }
    }

    @Test
    fun `systemPrompt filters use cases with conditionals`() {
        runBlocking {
            val adl = """
                ### UseCase: mobile_case <mobile>
                #### Description
                Mobile-specific use case.
                
                #### Solution
                Handle mobile request.
                ----
                
                ### UseCase: general_case
                #### Description
                General use case.
                
                #### Solution
                Handle general request.
                ----
            """.trimIndent()

            val resultWithMobile = executeSystemPromptMutation(adl, conditionals = listOf("mobile"))
            val resultWithoutMobile = executeSystemPromptMutation(adl, conditionals = emptyList())

            // Both use cases are parsed
            assertThat(resultWithMobile.useCaseCount).isEqualTo(2)
            assertThat(resultWithoutMobile.useCaseCount).isEqualTo(2)
            // System prompt should contain use case content
            assertThat(resultWithMobile.systemPrompt).contains("mobile_case")
            assertThat(resultWithMobile.systemPrompt).contains("general_case")
        }
    }

    @Test
    fun `systemPrompt returns correct prompt structure`() {
        runBlocking {
            val adl = """
                ### UseCase: structure_test
                #### Description
                Test structure.
                
                #### Solution
                Handle structure test.
                ----
            """.trimIndent()

            val result = executeSystemPromptMutation(adl)

            // The prompt should contain the use case information
            assertThat(result.systemPrompt).isNotBlank()
            assertThat(result.systemPrompt).contains("structure_test")
            assertThat(result.useCaseCount).isEqualTo(1)
        }
    }

    private suspend fun executeSystemPromptMutation(
        adl: String,
        conditionals: List<String> = emptyList(),
        sessionId: String? = null,
    ): SystemPromptResult {
        val conditionalsJson = conditionals.joinToString(", ") { "\"$it\"" }
        val sessionIdPart = sessionId?.let { ", sessionId: \"$it\"" } ?: ""

        val query = """
            mutation {
                systemPrompt(adl: ${adl.toGraphQLString()}, conditionals: [$conditionalsJson]$sessionIdPart) {
                    systemPrompt
                    useCaseCount
                    turn
                }
            }
        """.trimIndent()

        val response = client.post("$baseUrl/graphql") {
            contentType(ContentType.Application.Json)
            setBody("""{"query": ${json.encodeToString(kotlinx.serialization.serializer<String>(), query)}}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val responseBody = response.bodyAsText()
        val graphqlResponse = json.decodeFromString<GraphQLResponse>(responseBody)

        assertThat(graphqlResponse.errors).isNull()
        assertThat(graphqlResponse.data).isNotNull()

        return graphqlResponse.data!!.systemPrompt
    }

    private fun String.toGraphQLString(): String {
        return "\"\"\"${this.replace("\"\"\"", "\\\"\\\"\\\"")}\"\"\""
    }

    @Serializable
    data class GraphQLResponse(
        val data: GraphQLData? = null,
        val errors: List<GraphQLError>? = null,
    )

    @Serializable
    data class GraphQLData(
        val systemPrompt: SystemPromptResult,
    )

    @Serializable
    data class SystemPromptResult(
        val systemPrompt: String,
        val useCaseCount: Int,
        val turn: Int? = null,
    )

    @Serializable
    data class GraphQLError(
        val message: String,
    )
}
