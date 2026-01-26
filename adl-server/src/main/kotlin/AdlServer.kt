// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.routing
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.adl.server.agents.createAssistantAgent
import org.eclipse.lmos.adl.server.agents.createEvalAgent
import org.eclipse.lmos.adl.server.agents.createExampleAgent
import org.eclipse.lmos.adl.server.agents.createTestCreatorAgent
import org.eclipse.lmos.adl.server.embeddings.QdrantUseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.inbound.mutation.AdlAssistantMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlCompilerMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlEvalMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlStorageMutation
import org.eclipse.lmos.adl.server.inbound.query.AdlQuery
import org.eclipse.lmos.adl.server.inbound.mutation.TestCreatorMutation
import org.eclipse.lmos.adl.server.inbound.mutation.UseCaseImprovementMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlValidationMutation
import org.eclipse.lmos.adl.server.inbound.GlobalExceptionHandler
import org.eclipse.lmos.adl.server.inbound.mutation.SystemPromptMutation
import org.eclipse.lmos.adl.server.inbound.query.TestCaseQuery
import org.eclipse.lmos.adl.server.services.ConversationEvaluator
import org.eclipse.lmos.adl.server.services.TestExecutor
import org.eclipse.lmos.adl.server.sessions.InMemorySessions
import org.eclipse.lmos.adl.server.storage.memory.InMemoryAdlStorage
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.eclipse.lmos.adl.server.agents.createImprovementAgent
import org.eclipse.lmos.adl.server.inbound.mutation.AdlExampleMutation
import org.eclipse.lmos.adl.server.repositories.InMemoryTestCaseRepository

fun startServer(
    wait: Boolean = true,
    port: Int? = null,
    qdrantConfig: QdrantConfig = QdrantConfig(),
    module: Application.() -> Unit = {},
): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    // Dependencies
    val templateLoader = TemplateLoader()
    val sessions = InMemorySessions()
    val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    val useCaseStore = QdrantUseCaseEmbeddingsStore(embeddingModel, qdrantConfig)
    val adlStorage = InMemoryAdlStorage()

    // Agents
    val exampleAgent = createExampleAgent()
    val evalAgent = createEvalAgent()
    val assistantAgent = createAssistantAgent()
    val testCreatorAgent = createTestCreatorAgent()
    val conversationEvaluator = ConversationEvaluator(embeddingModel)
    val improvementAgent = createImprovementAgent()
    val testCaseRepository = InMemoryTestCaseRepository()
    val testExecutor = TestExecutor(assistantAgent, adlStorage, testCaseRepository, conversationEvaluator)

    // Initialize Qdrant collection
    runBlocking {
        useCaseStore.initialize()
    }

    return embeddedServer(CIO, port = port ?: EnvConfig.serverPort) {
        // Register shutdown hook to close resources
        monitor.subscribe(ApplicationStopping) {
            useCaseStore.close()
        }

        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            anyHost()
        }

        install(GraphQL) {
            schema {
                packages = listOf(
                    "org.eclipse.lmos.adl.server.inbound",
                    "org.eclipse.lmos.adl.server.agents",
                    "org.eclipse.lmos.arc.api",
                    "org.eclipse.lmos.adl.server.model",
                )
                queries = listOf(
                    AdlQuery(useCaseStore, adlStorage),
                    TestCaseQuery(testCaseRepository),
                )
                mutations = listOf(
                    AdlCompilerMutation(),
                    AdlStorageMutation(useCaseStore, adlStorage),
                    SystemPromptMutation(sessions, templateLoader),
                    AdlEvalMutation(evalAgent, conversationEvaluator),
                    AdlAssistantMutation(assistantAgent),
                    AdlValidationMutation(),
                    TestCreatorMutation(testCreatorAgent, testCaseRepository, testExecutor),
                    UseCaseImprovementMutation(improvementAgent),
                    AdlExampleMutation(exampleAgent),
                    )
            }
            server {

            }
            engine {
                exceptionHandler = GlobalExceptionHandler()
            }
        }

        install(StatusPages) {
            defaultGraphQLStatusPages()
        }

        routing {
            graphiQLRoute()
            graphQLPostRoute()
        }

        module()
    }.start(wait = wait)
}

fun main() {
    startServer()
}
