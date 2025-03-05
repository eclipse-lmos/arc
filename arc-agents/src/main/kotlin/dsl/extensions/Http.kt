// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.future.await
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse

/**
 * Performs a HTTP GET request.
 */
suspend fun DSLContext.httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
    return tracer().withSpan("GET $url", emptyMap()) { _, _ ->
        val request = HttpRequest.newBuilder().uri(URI(url)).apply {
            headers.forEach { (k, v) -> setHeader(k, v) }
        }.GET().build()
        val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        response.body()
    }
}

/**
 * Performs a HTTP POST request.
 */
suspend fun DSLContext.httpPost(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
    return tracer().withSpan("POST $url", emptyMap()) { _, _ ->
        val request = HttpRequest.newBuilder().uri(URI(url)).apply {
            headers.forEach { (k, v) -> setHeader(k, v) }
        }.POST(BodyPublishers.ofString(body)).build()
        val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        response.body()
    }
}
