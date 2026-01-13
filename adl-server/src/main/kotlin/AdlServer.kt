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
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.adl.server.agents.createAssistantAgent
import org.eclipse.lmos.adl.server.agents.createEvalAgent
import org.eclipse.lmos.adl.server.agents.createExampleAgent
import org.eclipse.lmos.adl.server.agents.createTestCreatorAgent
import org.eclipse.lmos.adl.server.embeddings.QdrantUseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.inbound.AdlAssistantMutation
import org.eclipse.lmos.adl.server.inbound.AdlCompilerMutation
import org.eclipse.lmos.adl.server.inbound.AdlEvalMutation
import org.eclipse.lmos.adl.server.inbound.AdlExampleQuery
import org.eclipse.lmos.adl.server.inbound.AdlMutation
import org.eclipse.lmos.adl.server.inbound.AdlQuery
import org.eclipse.lmos.adl.server.inbound.AdlTestCreatorMutation
import org.eclipse.lmos.adl.server.inbound.AdlValidationMutation
import org.eclipse.lmos.adl.server.inbound.GlobalExceptionHandler
import org.eclipse.lmos.adl.server.inbound.SystemPromptMutation
import org.eclipse.lmos.adl.server.sessions.InMemorySessions
import org.eclipse.lmos.adl.server.templates.TemplateLoader

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

    // Agents
    val exampleAgent = createExampleAgent()
    val evalAgent = createEvalAgent()
    val assistantAgent = createAssistantAgent()
    val testCreatorAgent = createTestCreatorAgent()

    // Initialize Qdrant collection
    runBlocking {
        useCaseStore.initialize()
    }

    return embeddedServer(CIO, port = port ?: EnvConfig.serverPort) {
        // Register shutdown hook to close resources
        monitor.subscribe(ApplicationStopping) {
            useCaseStore.close()
        }

        install(GraphQL) {
            schema {
                packages = listOf(
                    "org.eclipse.lmos.adl.server.inbound",
                    "org.eclipse.lmos.adl.server.agents",
                    "org.eclipse.lmos.arc.api",
                )
                queries = listOf(
                    AdlQuery(useCaseStore),
                    AdlExampleQuery(exampleAgent),
                )
                mutations = listOf(
                    AdlCompilerMutation(),
                    AdlMutation(useCaseStore),
                    SystemPromptMutation(sessions, templateLoader),
                    AdlEvalMutation(evalAgent),
                    AdlAssistantMutation(assistantAgent),
                    AdlValidationMutation(),
                    AdlTestCreatorMutation(testCreatorAgent),
                )
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
