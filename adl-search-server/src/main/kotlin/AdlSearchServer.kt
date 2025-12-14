// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.search

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.adl.server.search.embeddings.QdrantUseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.search.inbound.AdlMutation
import org.eclipse.lmos.adl.server.search.inbound.AdlQuery
import org.eclipse.lmos.adl.server.search.inbound.GlobalExceptionHandler

fun startServer(
    wait: Boolean = true,
    port: Int? = null,
    qdrantConfig: QdrantConfig = QdrantConfig(),
    module: Application.() -> Unit = {},
): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    // Dependencies
    val embeddingModel = AllMiniLmL6V2EmbeddingModel()
    val useCaseStore = QdrantUseCaseEmbeddingsStore(embeddingModel, qdrantConfig)

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
                packages = listOf("org.eclipse.lmos.adl.server.search.inbound")
                queries = listOf(
                    AdlQuery(useCaseStore),
                )
                mutations = listOf(
                    AdlMutation(useCaseStore),
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
