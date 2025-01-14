// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.router

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.llm.TextEmbedder
import org.eclipse.lmos.arc.agents.llm.TextEmbedding
import org.eclipse.lmos.arc.agents.llm.TextEmbeddings
import org.eclipse.lmos.arc.core.result
import org.junit.jupiter.api.Test

class RouterTest {

    val textEmbedder = object : TextEmbedder {
        override suspend fun embed(text: List<String>) = result<TextEmbeddings, ArcException> {
            TextEmbeddings(text.map { TextEmbedding(it, listOf(0.0, 0.1, it.hashCode().toDouble())) })
        }
    }

    // @Test
    fun `test route correctly routes requests`(): Unit = runBlocking {
        val subject = SemanticRouter(textEmbedder)
        subject.addRoute("destination1".routeBy("route1", "route2"))
        subject.addRoute("destination2".routeBy("route3", "route4"))

        assertThat(subject.route("route4")?.destination).isEqualTo("destination2")
    }

    @Test
    fun `test router publishes RouterRoutedEvent`(): Unit = runBlocking {
        var event: Event? = null
        val subject = SemanticRouter(
            textEmbedder,
            semanticRoutes("destination1".routeBy("route1", "route2")),
            eventPublisher = { event = it },
        )

        delay(100)
        subject.route("route1")

        assertThat(event).isInstanceOf(RouterRoutedEvent::class.java)
        assertThat((event as RouterRoutedEvent).destination?.destination).isEqualTo("destination1")
    }
}
