// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphiQLRoute
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.routing
import org.eclipse.lmos.adl.server.inbound.AdlCompilerMutation
import org.eclipse.lmos.adl.server.inbound.AdlQuery
import org.eclipse.lmos.adl.server.inbound.AdlValidationMutation

fun startServer(
    wait: Boolean = true,
    port: Int? = null,
    module: Application.() -> Unit = {},
) {
    embeddedServer(CIO, port = port ?: EnvConfig.serverPort) {
        install(GraphQL) {
            schema {
                packages = listOf("org.eclipse.lmos.adl.server.inbound")
                queries = listOf(
                    AdlQuery(),
                )
                mutations = listOf(
                    AdlCompilerMutation(),
                    AdlValidationMutation(),
                )
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
