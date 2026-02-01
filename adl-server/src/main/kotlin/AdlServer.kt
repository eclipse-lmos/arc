// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.routing
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondText
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.adl.server.agents.createAssistantAgent
import org.eclipse.lmos.adl.server.agents.createEvalAgent
import org.eclipse.lmos.adl.server.agents.createExampleAgent
import org.eclipse.lmos.adl.server.agents.createTestCreatorAgent
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
import org.eclipse.lmos.adl.server.services.McpService
import org.eclipse.lmos.adl.server.services.TestExecutor
import org.eclipse.lmos.adl.server.sessions.InMemorySessions
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryAdlRepository
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.eclipse.lmos.adl.server.agents.createImprovementAgent
import org.eclipse.lmos.adl.server.inbound.mutation.AdlExampleMutation
import org.eclipse.lmos.adl.server.inbound.mutation.McpMutation
import org.eclipse.lmos.adl.server.inbound.query.McpToolsQuery
import org.eclipse.lmos.adl.server.inbound.rest.openAICompletions
import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryTestCaseRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryUseCaseEmbeddingsStore
import java.time.Instant.now

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
    // val useCaseStore: UseCaseEmbeddingsRepository = QdrantUseCaseEmbeddingsStore(embeddingModel, qdrantConfig)
    val useCaseStore: UseCaseEmbeddingsRepository = InMemoryUseCaseEmbeddingsStore(embeddingModel)
    val adlStorage: AdlRepository = InMemoryAdlRepository()
    val mcpService = McpService()
    val testCaseRepository = InMemoryTestCaseRepository()

    // Agents
    val exampleAgent = createExampleAgent()
    val evalAgent = createEvalAgent()
    val assistantAgent = createAssistantAgent(mcpService, testCaseRepository, useCaseStore, adlStorage)
    val testCreatorAgent = createTestCreatorAgent()
    val conversationEvaluator = ConversationEvaluator(embeddingModel)
    val improvementAgent = createImprovementAgent()
    val testExecutor = TestExecutor(assistantAgent, adlStorage, testCaseRepository, conversationEvaluator)

    // Initialize Qdrant collection
    runBlocking {
        useCaseStore.initialize()
    }

    // Add example data
    runBlocking {
        // log.info("Loading examples", id, examples.size)
        listOf("buy_a_car.md").forEach { example ->
            val id = example.substringBeforeLast(".")
            val content = this::class.java.classLoader.getResource("examples/$example")!!.readText()
            adlStorage.store(Adl(id, content.trim(), listOf(), now().toString(), emptyList()))
        }
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
                    McpToolsQuery(mcpService),
                )
                mutations = listOf(
                    AdlCompilerMutation(),
                    AdlStorageMutation(useCaseStore, adlStorage),
                    SystemPromptMutation(sessions, templateLoader),
                    AdlEvalMutation(evalAgent, conversationEvaluator),
                    AdlAssistantMutation(assistantAgent, adlStorage),
                    AdlValidationMutation(),
                    TestCreatorMutation(testCreatorAgent, testCaseRepository, testExecutor, adlStorage),
                    UseCaseImprovementMutation(improvementAgent),
                    AdlExampleMutation(exampleAgent),
                    McpMutation(mcpService),
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
            openAICompletions(assistantAgent)
            graphiQLRoute()
            graphQLPostRoute()

            staticResources("/", "static") {
                fallback { requestedPath, call ->
                    // read from classpath
                    if (requestedPath.startsWith("prompts")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/prompts.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                }
            }
        }

        module()
    }.start(wait = wait)
}

fun main() {
    startServer()
}
