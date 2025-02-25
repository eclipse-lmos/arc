// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import org.slf4j.LoggerFactory
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Adds CORs and other headers for running the agent.
 */
class AccessControlHeaders(
    private val allowOrigin: String,
    private val allowMethods: String,
    private val allowHeaders: String,
) : WebFilter {

    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun filter(serverWebExchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
        log.debug("Adding CORs headers")
        serverWebExchange.response.headers.add("Access-Control-Allow-Origin", allowOrigin)
        serverWebExchange.response.headers.add("Access-Control-Allow-Methods", allowMethods)
        serverWebExchange.response.headers.add("Access-Control-Allow-Headers", allowHeaders)
        return webFilterChain.filter(serverWebExchange)
    }
}
