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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.ktor.client.engine.cio.CIO as ClientCIO
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases

class AdlStorageMutationIntegrationTest {

    companion object {
    }

    private val testPort = 18081
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
    fun `store stores a single use case with examples`() {
         runBlocking {
            val adl = """
                ### UseCase: password_reset
                #### Description
                Password reset use case.
                
                #### Examples
                - How do I reset my password?
                - I forgot my login credentials
                - Help me recover my account
                
                #### Solution
                Guide the user through the password reset process.
                ----
            """.trimIndent()

            val result = executeStoreMutation(adl)

            assertThat(result.message).describedAs("Storage should succeed").doesNotContain("Failed")
            assertThat(result.storedExamplesCount).isEqualTo(3)
            assertThat(result.message).contains("successfully stored")
        }
    }

    @Test
    fun `store stores use case with conditions`() {
        runBlocking {
            val adl = """
                ### UseCase: mobile_settings <mobile>
                #### Description
                Mobile app settings use case.
                
                #### Examples
                - Show me the mobile app
                - Open the app settings
                
                #### Solution
                Open the mobile app settings screen.
                ----
            """.trimIndent()

            val result = executeStoreMutation(adl)

            assertThat(result.storedExamplesCount).isEqualTo(2)
        }
    }

    @Test
    fun `store stores multiple use cases from adl`() {
        runBlocking {
            val adl = """
                ### UseCase: batch_case_1
                #### Description
                First use case.
                
                #### Examples
                - Example 1
                - Example 2
                
                #### Solution
                Handle first case.
                ----
                
                ### UseCase: batch_case_2
                #### Description
                Second use case.
                
                #### Examples
                - Example A
                - Example B
                - Example C
                
                #### Solution
                Handle second case.
                ----
            """.trimIndent()

            val result = executeStoreMutation(adl)

            assertThat(result.storedExamplesCount).isEqualTo(5)
            assertThat(result.message).contains("successfully stored")
        }
    }

    @Test
    fun `store overwrites existing use case with same id`() {
        runBlocking {
            val useCaseId = "overwrite_test"

            // First store a use case with 3 examples
            val adl1 = """
                ### UseCase: $useCaseId
                #### Description
                Original use case.
                
                #### Examples
                - Original example 1
                - Original example 2
                - Original example 3
                
                #### Solution
                Original solution.
                ----
            """.trimIndent()
            val result1 = executeStoreMutation(adl1)

            assertThat(result1.storedExamplesCount).isEqualTo(3)

            // Store again with same ID but only 2 examples - should overwrite
            val adl2 = """
                ### UseCase: $useCaseId
                #### Description
                Updated use case.
                
                #### Examples
                - Updated example 1
                - Updated example 2
                
                #### Solution
                Updated solution.
                ----
            """.trimIndent()
            val result2 = executeStoreMutation(adl2)

            assertThat(result2.storedExamplesCount).isEqualTo(2)
            assertThat(result2.message).contains("successfully stored")
        }
    }

    @Test
    fun `delete deletes an existing use case`() {
        runBlocking {
            val useCaseId = "to_be_deleted"

            // First store a use case
            val adl = """
                ### UseCase: $useCaseId
                #### Description
                Test use case.
                
                #### Examples
                - Test example
                
                #### Solution
                Handle test case.
                ----
            """.trimIndent()
            executeStoreMutation(adl)

            // Then delete it
            val result = executeDeleteMutation(useCaseId)

            assertThat(result.useCaseId).isEqualTo(useCaseId)
            assertThat(result.message).contains("successfully deleted")
        }
    }

    @Test
    fun `clearAll clears all stored use cases`() {
        runBlocking {
            // Store some use cases first
            val adl1 = """
                ### UseCase: clear_test_1
                #### Description
                Test use case 1.
                
                #### Examples
                - Example 1
                
                #### Solution
                Handle case 1.
                ----
            """.trimIndent()
            executeStoreMutation(adl1)

            val adl2 = """
                ### UseCase: clear_test_2
                #### Description
                Test use case 2.
                
                #### Examples
                - Example 2
                
                #### Solution
                Handle case 2.
                ----
            """.trimIndent()
            executeStoreMutation(adl2)

            // Clear all
            val result = executeClearAllMutation()

            assertThat(result.message).contains("successfully cleared")
        }
    }

    private suspend fun executeStoreMutation(adl: String): StorageResultData {
        val useCases = adl.toUseCases()
        val id = useCases.firstOrNull()?.id ?: "unknown"

        val examplesJson = ""
        val idJson = "\"$id\""
        val contentJson = adl.toGraphQLString()
        val tagsJson = "[]"
        val outputJson = "\"\""

        val query = """
            mutation {
                store(id: $idJson, content: $contentJson, tags: $tagsJson, examples: [$examplesJson], output: $outputJson) {
                    storedExamplesCount
                    message
                }
            }
        """.trimIndent()

        val response = client.post("$baseUrl/graphql") {
            contentType(ContentType.Application.Json)
            setBody("""{"query": ${json.encodeToString(serializer<String>(), query)}}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val responseBody = response.bodyAsText()
        val graphqlResponse = json.decodeFromString<StoreGraphQLResponse>(responseBody)

        assertThat(graphqlResponse.errors).isNull()
        assertThat(graphqlResponse.data).isNotNull()

        return graphqlResponse.data!!.store
    }

    private suspend fun executeDeleteMutation(useCaseId: String): DeletionResultData {
        val query = """
            mutation {
                delete(id: "$useCaseId") {
                    useCaseId
                    message
                }
            }
        """.trimIndent()

        val response = client.post("$baseUrl/graphql") {
            contentType(ContentType.Application.Json)
            setBody("""{"query": ${json.encodeToString(serializer<String>(), query)}}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val responseBody = response.bodyAsText()
        val graphqlResponse = json.decodeFromString<DeleteGraphQLResponse>(responseBody)

        assertThat(graphqlResponse.errors).isNull()
        assertThat(graphqlResponse.data).isNotNull()

        return graphqlResponse.data!!.delete
    }

    private suspend fun executeClearAllMutation(): ClearResultData {
        val query = """
            mutation {
                clearAll {
                    message
                }
            }
        """.trimIndent()

        val response = client.post("$baseUrl/graphql") {
            contentType(ContentType.Application.Json)
            setBody("""{"query": ${json.encodeToString(serializer<String>(), query)}}""")
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val responseBody = response.bodyAsText()
        val graphqlResponse = json.decodeFromString<ClearAllGraphQLResponse>(responseBody)

        assertThat(graphqlResponse.errors).isNull()
        assertThat(graphqlResponse.data).isNotNull()

        return graphqlResponse.data!!.clearAll
    }

    private fun String.toGraphQLString(): String {
        return "\"\"\"${this.replace("\"\"\"", "\\\"\\\"\\\"")}\"\"\""
    }

    // Data classes for GraphQL responses
    @Serializable
    data class StoreGraphQLResponse(
        val data: StoreData? = null,
        val errors: List<GraphQLError>? = null,
    )

    @Serializable
    data class StoreData(
        val store: StorageResultData,
    )

    @Serializable
    data class StorageResultData(
        val storedExamplesCount: Int,
        val message: String,
    )

    @Serializable
    data class DeleteGraphQLResponse(
        val data: DeleteData? = null,
        val errors: List<GraphQLError>? = null,
    )

    @Serializable
    data class DeleteData(
        val delete: DeletionResultData,
    )

    @Serializable
    data class DeletionResultData(
        val useCaseId: String,
        val message: String,
    )

    @Serializable
    data class ClearAllGraphQLResponse(
        val data: ClearAllData? = null,
        val errors: List<GraphQLError>? = null,
    )

    @Serializable
    data class ClearAllData(
        val clearAll: ClearResultData,
    )

    @Serializable
    data class ClearResultData(
        val message: String,
    )

    @Serializable
    data class GraphQLError(
        val message: String,
    )
}
