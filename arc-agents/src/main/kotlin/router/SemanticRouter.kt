// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.router

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.TextEmbedder
import org.eclipse.lmos.arc.agents.llm.TextEmbedding
import org.eclipse.lmos.arc.agents.llm.TextEmbeddings
import org.eclipse.lmos.arc.agents.llm.embed
import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTime

/**
 * SemanticRouter that uses semantic similarity to find destinations for routes.
 */
class SemanticRouter(
    private val textEmbedder: TextEmbedder,
    initialRoutes: SemanticRoutes? = null,
    private val eventPublisher: EventPublisher? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val allRoutes = AtomicReference(TextEmbeddings(emptyList()))
    private val scope = CoroutineScope(Dispatchers.Default)
    private val ready = AtomicBoolean(false)

    init {
        log.debug("Initializing SemanticRouter with ${initialRoutes?.routes?.size ?: 0} routes...")
        scope.launch {
            initialRoutes?.routes?.forEach { addRoute(it) }
            log.debug("SemanticRouter initialized")
            ready.set(true)
            eventPublisher?.publish(RouterReadyEvent())
        }
    }

    /**
     * Returns true if the router is ready, i.e. all routes have been loaded.
     */
    fun isReady() = ready.get()

    /**
     * Adds a new route to the router.
     * @param destination The destination of the route.
     * @param routes The routes to the destination.
     */
    suspend fun addRoute(route: Route) {
        val embeddings = when (route) {
            is SemanticRoute -> textEmbedder.embed(route.routes)
                .getOrThrow().embeddings.map { it.copy(labels = setOf(route.destination)) }

            is SemanticRouteTextEmbeddings -> route.routes.map {
                TextEmbedding(
                    it.text,
                    it.embedding,
                    labels = setOf(route.destination),
                )
            }
        }
        allRoutes.updateAndGet { TextEmbeddings(it.embeddings + embeddings) }
    }

    /**
     * Routes a request to the closest destination.
     * @param request The request to route.
     * @param default The default destination if no route is found.
     * @return The destination of the route.
     */
    suspend fun route(request: String, default: String? = null): Destination? {
        val result: Destination?
        val duration = measureTime {
            if (ready.get().not()) {
                log.warn("SemanticRouter not ready yet. Returning default $default")
                return default?.let { Destination(it) }
            }
            val (embedding, accuracy) = allRoutes.get().findClosest(textEmbedder.embed(request).getOrThrow())
            val destination = embedding.labels.firstOrNull()

            if (destination != null) {
                log.debug("Routing [$request] to $destination")
            } else if (default == null) {
                log.warn("Cannot route [$request] to any destination. No default provided!")
            } else {
                log.debug("Cannot route [$request] to any destination. Using default $default")
            }
            result = destination?.let { Destination(it, accuracy) } ?: default?.let { Destination(it) }
        }
        eventPublisher?.publish(RouterRoutedEvent(request, result, duration))
        return result
    }
}

@Serializable
data class SemanticRoutes(val routes: List<Route>)

@Serializable
sealed class Route {
    abstract val destination: String
}

/**
 * A semantic route without TextEmbeddings.
 */
@Serializable
data class SemanticRoute(override val destination: String, val routes: List<String>) : Route()

/**
 * A semantic route with TextEmbeddings.
 */
@Serializable
data class SemanticRouteTextEmbeddings(override val destination: String, val routes: List<TextEmbedding>) : Route()

/**
 * The destination determined by the SemanticRouter.
 * @param destination
 * @param accuracy the accuracy of the routing. A range between -1.0 and 1.0, with the latter indicating the better match.
 */
data class Destination(val destination: String, val accuracy: Double = -1.0)

/**
 * Extension function to create a SemanticRoutes.
 */
fun semanticRoutes(vararg routes: Route) = SemanticRoutes(routes.toList())
fun String.routeBy(vararg routes: String) = SemanticRoute(this, routes.toList())
