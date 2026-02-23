// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server

// import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.adl.server.agents.createAssistantAgent
import org.eclipse.lmos.adl.server.agents.createEvalAgent
import org.eclipse.lmos.adl.server.agents.createExampleAgent
import org.eclipse.lmos.adl.server.agents.createImprovementAgent
import org.eclipse.lmos.adl.server.agents.createSpellingAgent
import org.eclipse.lmos.adl.server.agents.createTestCreatorAgent
import org.eclipse.lmos.adl.server.agents.createTestVariantCreatorAgent
import org.eclipse.lmos.adl.server.agents.createWidgetCreatorAgent
import org.eclipse.lmos.adl.server.inbound.GlobalExceptionHandler
import org.eclipse.lmos.adl.server.inbound.mutation.AdlAssistantMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlCompilerMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlEvalMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlExampleMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlStorageMutation
import org.eclipse.lmos.adl.server.inbound.mutation.AdlValidationMutation
import org.eclipse.lmos.adl.server.inbound.mutation.McpMutation
import org.eclipse.lmos.adl.server.inbound.mutation.RolePromptMutation
import org.eclipse.lmos.adl.server.inbound.mutation.SpellingMutation
import org.eclipse.lmos.adl.server.inbound.mutation.SystemPromptMutation
import org.eclipse.lmos.adl.server.inbound.mutation.TestCreatorMutation
import org.eclipse.lmos.adl.server.inbound.mutation.UseCaseImprovementMutation
import org.eclipse.lmos.adl.server.inbound.mutation.UserSettingsMutation
import org.eclipse.lmos.adl.server.inbound.mutation.WidgetsMutation
import org.eclipse.lmos.adl.server.inbound.query.AdlQuery
import org.eclipse.lmos.adl.server.inbound.query.DashboardQuery
import org.eclipse.lmos.adl.server.inbound.query.McpToolsQuery
import org.eclipse.lmos.adl.server.inbound.query.RolePromptQuery
import org.eclipse.lmos.adl.server.inbound.query.TestCaseQuery
import org.eclipse.lmos.adl.server.inbound.query.UserSettingsQuery
import org.eclipse.lmos.adl.server.inbound.query.WidgetQuery
import org.eclipse.lmos.adl.server.inbound.rest.clientEvents
import org.eclipse.lmos.adl.server.inbound.rest.openAICompletions
import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.RolePromptRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryAdlRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryRolePromptRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryStatisticsRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryTestCaseRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryUseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryUserSettingsRepository
import org.eclipse.lmos.adl.server.repositories.impl.InMemoryWidgetRepository
import org.eclipse.lmos.adl.server.services.ClientEventPublisher
import org.eclipse.lmos.adl.server.services.ConversationEvaluator
import org.eclipse.lmos.adl.server.services.McpService
import org.eclipse.lmos.adl.server.services.TestExecutor
import org.eclipse.lmos.adl.server.services.UserDefinedCompleterProvider
import org.eclipse.lmos.adl.server.sessions.InMemorySessions
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
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
    // val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    val embeddingModel = BgeSmallEnV15QuantizedEmbeddingModel()
    // val useCaseStore: UseCaseEmbeddingsRepository = QdrantUseCaseEmbeddingsStore(embeddingModel, qdrantConfig)
    val embeddingStore: UseCaseEmbeddingsRepository = InMemoryUseCaseEmbeddingsStore(embeddingModel)
    val adlStorage: AdlRepository = InMemoryAdlRepository()
    val rolePromptRepository: RolePromptRepository = InMemoryRolePromptRepository()
    val mcpService = McpService()
    val testCaseRepository = InMemoryTestCaseRepository()
    val userSettingsRepository = InMemoryUserSettingsRepository()
    val widgetRepository = InMemoryWidgetRepository()
    val clientEventPublisher = ClientEventPublisher()
    val completerProvider = UserDefinedCompleterProvider()
    val statisticsRepository = InMemoryStatisticsRepository()

    // Agents
    val exampleAgent = createExampleAgent(completerProvider)
    val evalAgent = createEvalAgent(completerProvider)
    val facesAgent = createWidgetCreatorAgent(completerProvider)
    val assistantAgent =
        createAssistantAgent(
            mcpService,
            testCaseRepository,
            embeddingStore,
            adlStorage,
            embeddingModel,
            widgetRepository,
            rolePromptRepository,
            clientEventPublisher,
            completerProvider
        )
    val conversationEvaluator = ConversationEvaluator(embeddingModel)
    val testCreatorAgent = createTestCreatorAgent(completerProvider)
    val improvementAgent = createImprovementAgent(completerProvider)
    val spellingAgent = createSpellingAgent(completerProvider)
    val testVariantAgent = createTestVariantCreatorAgent(completerProvider)

    val testExecutor =
        TestExecutor(assistantAgent, adlStorage, testCaseRepository, conversationEvaluator)

    // Initialize Qdrant collection
    runBlocking {
        embeddingStore.initialize()
    }

    // Add example data
    runBlocking {
        // log.info("Loading examples", id, examples.size)
        listOf("buy_a_car.md", "greeting.md", "unsure_customer.md").forEach { name ->
            val content = this::class.java.classLoader.getResource("examples/$name")!!.readText()
            val id = name.substringBeforeLast(".")
            val examples = content.toUseCases()
                .flatMap { it.examples.split("\n") }
                .filterNot { it.isBlank() }
                .map { it.removePrefix("- ") }
            var contentWithoutExamples = content.replace("#### Examples", "")
            examples.forEach {
                contentWithoutExamples = contentWithoutExamples.replace("- $it", "")
            }
            adlStorage.store(Adl(id, contentWithoutExamples.trim(), listOf(), now().toString(), examples))
            if (examples.isNotEmpty()) embeddingStore.storeUtterances(id, examples)
        }
    }

    return embeddedServer(CIO, port = port ?: EnvConfig.serverPort) {
        // Register shutdown hook to close resources
        monitor.subscribe(ApplicationStopping) {
            embeddingStore.close()
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
                    "org.eclipse.lmos.adl.server.models",
                )
                queries = listOf(
                    AdlQuery(embeddingStore, adlStorage),
                    McpToolsQuery(mcpService),
                    TestCaseQuery(testCaseRepository),
                    UserSettingsQuery(userSettingsRepository),
                    WidgetQuery(widgetRepository),
                    RolePromptQuery(rolePromptRepository),
                    DashboardQuery(adlStorage, statisticsRepository)
                )
                mutations = listOf(
                    AdlCompilerMutation(),
                    SystemPromptMutation(sessions, templateLoader, rolePromptRepository),
                    AdlStorageMutation(embeddingStore, adlStorage),
                    McpMutation(mcpService),
                    UserSettingsMutation(userSettingsRepository, completerProvider),
                    UseCaseImprovementMutation(improvementAgent),
                    SpellingMutation(spellingAgent),
                    AdlEvalMutation(evalAgent, conversationEvaluator),
                    AdlExampleMutation(exampleAgent),
                    TestCreatorMutation(
                        testCreatorAgent,
                        testCaseRepository,
                        testExecutor,
                        adlStorage,
                        testVariantAgent
                    ),
                    AdlValidationMutation(),
                    AdlAssistantMutation(assistantAgent, adlStorage, statisticsRepository),
                    WidgetsMutation(facesAgent, widgetRepository),
                    RolePromptMutation(rolePromptRepository),
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

        install(SSE)

        routing {
            openAICompletions(assistantAgent)
            clientEvents(clientEventPublisher)
            graphiQLRoute()
            graphQLPostRoute()

            staticResources("/", "static") {
                fallback { requestedPath, call ->
                    // read from classpath
                    if (requestedPath.startsWith("prompts")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/prompts.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                    if (requestedPath.startsWith("roles")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/roles.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                    if (requestedPath.startsWith("widgets")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/widgets.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                    if (requestedPath.startsWith("faces")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/faces.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                    if (requestedPath.startsWith("assistant")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/assistant.html")!!.readText(),
                        contentType = ContentType.Text.Html,
                    )
                    if (requestedPath.startsWith("analytics")) call.respondText(
                        text = this::class.java.classLoader.getResource("static/analytics.html")!!.readText(),
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
